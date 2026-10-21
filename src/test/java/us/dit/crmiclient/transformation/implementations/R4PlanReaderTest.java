package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ParameterDefinition.ParameterUse;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.Flow;
import us.dit.crmiclient.model.ParameterWrapper;
import us.dit.crmiclient.model.ProcessEvent;
import us.dit.crmiclient.model.ProcessNode;
import us.dit.crmiclient.model.SimpleGateway;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;

class R4PlanReaderTest {

    @Test
    void getIdUsesPlanIdWithoutPrefix() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "id": "plan-sencillo", "name": "PlanSencillo"}
                """);
        assertEquals("plan-sencillo", reader.getId());
    }

    @Test
    void getIdUsesNameWhenPlanHasNoId() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "PlanSencillo"}
                """);
        assertEquals("PlanSencillo", reader.getId());
    }

    @Test
    void getIdTurnsNameIntoValidBpmnId() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "1 plan con espacios"}
                """);
        assertEquals("_1_plan_con_espacios", reader.getId());
    }

    @Test
    void getIdGeneratesStableIdWhenPlanHasNoIdNorName() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "status": "draft"}
                """);
        String id = reader.getId();
        assertTrue(id.startsWith("plan-"));
        assertEquals(id, reader.getId());
    }

    @Test
    void rejectsNestedActions() {
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "PlanAnidado",
                 "action": [{"id": "grupo", "action": [{"id": "hija"}]}]}
                """));
    }

    /** Condición applicability para meter en una acción del JSON */
    private static final String CONDITION = """
            "condition": [{"kind": "applicability",
                           "expression": {"language": "text/fhirpath", "expression": "true"}}]""";

    private static ActionWrapper<PlanDefinitionActionComponent> action(R4PlanReader reader, String id) {
        return reader.getActions().stream().filter(a -> a.getId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void sequenceHasNoGateways() {
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Secuencia",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before-start"}]},
                            {"id": "b"}]}
                """);
        assertNull(gateway(reader, "gw_out_a"));
        assertNull(gateway(reader, "gw_in_b"));
        assertNull(gateway(reader, "gw_start"));
        assertNull(gateway(reader, "gw_end"));
    }

    @Test
    void forkAndJoinWithoutConditionsAreAnd() {
        // a -> (b, c) -> d
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "ForkJoin",
                 "action": [{"id": "a"},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "b", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """);
        SimpleGateway split = gateway(reader, "gw_out_a");
        SimpleGateway join = gateway(reader, "gw_in_d");
        assertEquals(SimpleGateway.GatewayType.AND, split.getType());
        assertEquals("gw_out_a", split.getId());
        assertEquals(SimpleGateway.GatewayType.AND, join.getType());
        assertEquals("gw_in_d", join.getId());
        // El split entra desde a y sale hacia b y c; el join entra desde b y c y sale hacia d
        assertEquals(Set.of("a"), ids(reader.getPredecessors(split)));
        assertEquals(Set.of("b", "c"), ids(reader.getSuccessors(split)));
        assertEquals(Set.of("b", "c"), ids(reader.getPredecessors(join)));
        assertEquals(Set.of("d"), ids(reader.getSuccessors(join)));
    }

    /** Ids de unos nodos, para comparar sin depender del orden */
    private static Set<String> ids(Collection<? extends ProcessNode> nodes) {
        return nodes.stream().map(ProcessNode::getId).collect(Collectors.toSet());
    }

    /** Gateway del proceso con el id dado, o null si no existe */
    private static SimpleGateway gateway(R4PlanReader reader, String id) {
        return (SimpleGateway) reader.getNode(id);
    }

    @Test
    void conditionalBranchMakesSplitAndJoinOr() {
        // a -> (b [condición], c) -> d
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Condicional",
                 "action": [{"id": "a"},
                            {"id": "b", %s, "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "b", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_out_a").getType());
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_in_d").getType());
    }

    @Test
    void joinIsOrWhenAConditionIsFurtherUpTheBranch() {
        // a -> (b [condición] -> e, c) -> d : la rama que llega desde e puede no llegar
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Cascada",
                 "action": [{"id": "a"},
                            {"id": "b", %s, "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "e", "relatedAction": [{"actionId": "b", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "e", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """.formatted(CONDITION));
        assertNull(gateway(reader, "gw_out_b"));
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_in_d").getType());
    }

    @Test
    void singleConditionalSuccessorNeedsOrSplit() {
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "UnaCondicional",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"}]},
                            {"id": "b", %s}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_out_a").getType());
    }

    @Test
    void independentActionsGetStartAndEndGateways() {
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Independientes",
                 "action": [{"id": "x"}, {"id": "y", %s}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_start").getType());
        assertEquals("gw_start", gateway(reader, "gw_start").getId());
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_end").getType());
        assertEquals("gw_end", gateway(reader, "gw_end").getId());
        // Al de inicio le llega el evento de inicio y sale hacia las primeras acciones;
        // al de fin le llegan las últimas y sale hacia el evento de fin
        assertEquals(Set.of("startEvent"), ids(reader.getPredecessors(gateway(reader, "gw_start"))));
        assertEquals(Set.of("x", "y"), ids(reader.getSuccessors(gateway(reader, "gw_start"))));
        assertEquals(Set.of("x", "y"), ids(reader.getPredecessors(gateway(reader, "gw_end"))));
        assertEquals(Set.of("endEvent"), ids(reader.getSuccessors(gateway(reader, "gw_end"))));
    }

    /** Flujos del proceso como "origen->destino", para comparar sin depender del orden */
    private static Set<String> flows(R4PlanReader reader) {
        return reader.getFlows().stream().map(Flow::toString).collect(Collectors.toSet());
    }

    @Test
    void processGraphOfASequence() {
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Secuencia",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"}]},
                            {"id": "b"}]}
                """);
        assertEquals(Set.of("startEvent->a", "a->b", "b->endEvent"), flows(reader));
        // Los nodos, en orden topológico: inicio, a, b, fin
        assertEquals(List.of("startEvent", "a", "b", "endEvent"),
                reader.getNodes().stream().map(ProcessNode::getId).toList());
    }

    @Test
    void processGraphOfAForkAndJoin() {
        // a -> (b, c) -> d
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "ForkJoin",
                 "action": [{"id": "a"},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "b", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """);
        assertEquals(Set.of("startEvent->a", "a->gw_out_a", "gw_out_a->b", "gw_out_a->c",
                "b->gw_in_d", "c->gw_in_d", "gw_in_d->d", "d->endEvent"), flows(reader));
    }

    @Test
    void processGraphOfIndependentActions() {
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Independientes",
                 "action": [{"id": "x"}, {"id": "y"}]}
                """);
        assertEquals(Set.of("startEvent->gw_start", "gw_start->x", "gw_start->y",
                "x->gw_end", "y->gw_end", "gw_end->endEvent"), flows(reader));
    }

    @Test
    void processGraphConnectsASplitDirectlyToAJoin() {
        // a -> (b, c) y x -> c: el split de a va directamente al join de c
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "SplitJoin",
                 "action": [{"id": "a"}, {"id": "x"},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"},
                                                          {"actionId": "x", "relationship": "after"}]}]}
                """);
        assertEquals(Set.of("startEvent->gw_start", "gw_start->a", "gw_start->x",
                "a->gw_out_a", "gw_out_a->b", "gw_out_a->gw_in_c", "x->gw_in_c", "gw_in_c->c",
                "b->gw_end", "c->gw_end", "gw_end->endEvent"), flows(reader));
    }

    @Test
    void planWithoutActionsGoesFromStartToEnd() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "Vacio"}
                """);
        assertEquals(Set.of("startEvent->endEvent"), flows(reader));
    }

    @Test
    void nodesCanBeFoundAndTraversed() {
        // a -> (b, c)
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Recorrido",
                 "action": [{"id": "a"},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]}]}
                """);
        ProcessNode split = reader.getNode("gw_out_a");
        assertTrue(split instanceof SimpleGateway);
        assertEquals(List.of("a"), reader.getPredecessors(split).stream().map(ProcessNode::getId).toList());
        assertEquals(Set.of("b", "c"),
                reader.getSuccessors(split).stream().map(ProcessNode::getId).collect(Collectors.toSet()));
        assertEquals(ProcessEvent.START, reader.getNode("startEvent"));
        assertNull(reader.getNode("noExiste"));
        assertThrows(IllegalArgumentException.class,
                () -> reader.getSuccessors(new SimpleGateway("ajeno", SimpleGateway.GatewayType.AND)));
    }

    @Test
    void rejectsActionWithTheIdOfAGateway() {
        // La acción "gw_out_a" choca con el split de a, que va hacia b y hacia gw_out_a
        assertThrows(InvalidPlanDefinitionException.class, () -> readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "ChoqueGateway",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"},
                                                          {"actionId": "gw_out_a", "relationship": "before"}]},
                            {"id": "b"}, {"id": "gw_out_a"}]}
                """));
    }

    @Test
    void rejectsActionWithTheIdOfAnEvent() {
        assertThrows(InvalidPlanDefinitionException.class, () -> readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "ChoqueEvento",
                 "action": [{"id": "startEvent"}]}
                """));
    }

    @Test
    void rejectsDirectCycle() {
        // a antes que b, y b antes que a
        assertThrows(InvalidPlanDefinitionException.class, () -> readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "CicloDirecto",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"}]},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "before"}]}]}
                """));
    }

    @Test
    void rejectsIndirectCycle() {
        // a -> b -> c -> a
        assertThrows(InvalidPlanDefinitionException.class, () -> readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "CicloIndirecto",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"}]},
                            {"id": "b", "relatedAction": [{"actionId": "c", "relationship": "before"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "before"}]}]}
                """));
    }

    @Test
    void sameRelationDeclaredFromBothActionsIsNotACycle() {
        // a antes que b, declarado desde a (before) y desde b (after): es la misma relación
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "DosVeces",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"}]},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "after"}]}]}
                """);
        assertEquals(Set.of("b"), ids(reader.getSuccessors(action(reader, "a"))));
    }

    @Test
    void redundantRelationIsRemoved() {
        // a -> b -> c, y además a -> c: esa relación ya está implícita y no debe dar split ni join
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "Redundante",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"},
                                                          {"actionId": "c", "relationship": "before"}]},
                            {"id": "b", "relatedAction": [{"actionId": "c", "relationship": "before"}]},
                            {"id": "c"}]}
                """);
        assertNull(gateway(reader, "gw_out_a"));
        assertNull(gateway(reader, "gw_in_c"));
        assertEquals(Set.of("b"), ids(reader.getSuccessors(action(reader, "a"))));
        assertEquals(Set.of("b"), ids(reader.getPredecessors(action(reader, "c"))));
    }

    @Test
    void mayNotRunDoesNotDependOnTheOrderInTheDocument() {
        // El mismo caso que joinIsOrWhenAConditionIsFurtherUpTheBranch, con las acciones al revés:
        // d -> (e, c) se declaran antes que b [condición] y a
        R4PlanReader reader = readerWithProcesses("""
                {"resourceType": "PlanDefinition", "name": "CascadaDesordenada",
                 "action": [{"id": "d", "relatedAction": [{"actionId": "e", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]},
                            {"id": "e", "relatedAction": [{"actionId": "b", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "b", %s, "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "a"}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, gateway(reader, "gw_in_d").getType());
    }

    @Test
    void rejectsDuplicatedActionIds() {
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "PlanDuplicado",
                 "action": [{"id": "a"}, {"id": "a"}]}
                """));
    }

    private static final String SUBPLAN = "http://ejemplo/PlanDefinition/notificar";

    /** Plan con una única acción "a" cuyo PlanDefinition es SUBPLAN */
    private static final String PLAN_WITH_SUBPROCESS = """
            {"resourceType": "PlanDefinition", "name": "ConSubproceso",
             "action": [{"id": "a", "definitionCanonical": "%s"}]}
            """.formatted(SUBPLAN);

    @Test
    void storesCanonicalOfTheActionPlanDefinition() {
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().add(SUBPLAN, new PlanDefinition());
        R4PlanReader reader = new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi);
        assertEquals(SUBPLAN, action(reader, "a").getSubprocessCanonical());
    }

    @Test
    void pinsTheVersionOfTheFoundPlanDefinition() {
        PlanDefinition subplan = new PlanDefinition();
        subplan.setVersion("1.2.0");
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().add(SUBPLAN, subplan);
        R4PlanReader reader = new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi);
        assertEquals(SUBPLAN + "|1.2.0", action(reader, "a").getSubprocessCanonical());
    }

    @Test
    void rejectsActionWithoutDefinition() {
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "SinDefinition",
                 "action": [{"id": "a"}]}
                """, new InMemoryR4Resolver()));
    }

    @Test
    void rejectsActionWhoseDefinitionIsNotFound() {
        assertThrows(InvalidPlanDefinitionException.class,
                () -> new R4PlanReader(PLAN_WITH_SUBPROCESS, new InMemoryR4Resolver()));
    }

    /** Parámetro R4 de Library */
    private static org.hl7.fhir.r4.model.ParameterDefinition r4Parameter(String name, ParameterUse use, String type) {
        return new org.hl7.fhir.r4.model.ParameterDefinition().setName(name).setUse(use).setType(type);
    }

    /** Library con los parámetros dados */
    private static Library libraryWith(org.hl7.fhir.r4.model.ParameterDefinition... parameters) {
        Library library = new Library();
        for (org.hl7.fhir.r4.model.ParameterDefinition parameter : parameters) {
            library.addParameter(parameter);
        }
        return library;
    }

    /** Resolver con SUBPLAN y una Library por cada elemento de libraries */
    private static InMemoryR4Resolver crmiWithLibraries(Library... libraries) {
        PlanDefinition subplan = new PlanDefinition();
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().add(SUBPLAN, subplan);
        for (int i = 0; i < libraries.length; i++) {
            String libraryCanonical = SUBPLAN + "-library-" + i;
            subplan.addLibrary(libraryCanonical);
            crmi.add(libraryCanonical, libraries[i]);
        }
        return crmi;
    }

    @Test
    void readsInputAndOutputParametersFromLibrary() {
        InMemoryR4Resolver crmi = crmiWithLibraries(libraryWith(
                r4Parameter("paciente", ParameterUse.IN, "Patient").setMin(1).setMax("1")
                        .setDocumentation("Paciente a notificar"),
                r4Parameter("motivo", ParameterUse.IN, "string"),
                r4Parameter("enviada", ParameterUse.OUT, "boolean")));
        // paciente es obligatorio (min 1): la acción tiene que darle valor
        ActionWrapper<PlanDefinitionActionComponent> a =
                action(new R4PlanReader(planWithDynamicValues("paciente", "%subject"), crmi), "a");

        assertEquals(List.of("paciente", "motivo"), List.copyOf(a.getInParameters().keySet()));
        assertEquals(List.of("enviada"), List.copyOf(a.getOutParameters().keySet()));
        ParameterWrapper<?> paciente = a.getInParameters().get("paciente");
        assertEquals(ParameterWrapper.Use.IN, paciente.getUse());
        assertEquals("Patient", paciente.getType());
        assertEquals(1, paciente.getMin());
        assertEquals("1", paciente.getMax());
        assertEquals("Paciente a notificar", paciente.getDocumentation());
        assertNull(paciente.getDefaultValue());
        assertEquals("%subject", ((Expression) paciente.getValue()).getExpression());
        // El wrapper conserva la definición original de R4
        assertEquals("Paciente a notificar",
                ((org.hl7.fhir.r4.model.ParameterDefinition) paciente.getDefinition()).getDocumentation());
        // Sin min ni max en la Library: opcional y sin máximo indicado
        assertEquals(0, a.getInParameters().get("motivo").getMin());
        assertNull(a.getInParameters().get("motivo").getMax());
    }

    @Test
    void subprocessWithoutParametersIsValid() {
        InMemoryR4Resolver crmi = crmiWithLibraries(libraryWith());
        ActionWrapper<PlanDefinitionActionComponent> a = action(new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi), "a");
        assertTrue(a.getInParameters().isEmpty());
        assertTrue(a.getOutParameters().isEmpty());
    }

    @Test
    void joinsParametersOfTheLibrariesOfDifferentEngines() {
        // Una Library por motor: el parámetro común aparece una vez
        InMemoryR4Resolver crmi = crmiWithLibraries(
                libraryWith(r4Parameter("paciente", ParameterUse.IN, "Patient")),
                libraryWith(r4Parameter("paciente", ParameterUse.IN, "Patient"),
                        r4Parameter("enviada", ParameterUse.OUT, "boolean")));
        ActionWrapper<PlanDefinitionActionComponent> a = action(new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi), "a");
        assertEquals(List.of("paciente"), List.copyOf(a.getInParameters().keySet()));
        assertEquals(List.of("enviada"), List.copyOf(a.getOutParameters().keySet()));
    }

    @Test
    void rejectsParameterDeclaredDifferentlyInTwoLibraries() {
        InMemoryR4Resolver crmi = crmiWithLibraries(
                libraryWith(r4Parameter("paciente", ParameterUse.IN, "Patient")),
                libraryWith(r4Parameter("paciente", ParameterUse.IN, "string")));
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi));
    }

    @Test
    void rejectsParameterWithoutUse() {
        InMemoryR4Resolver crmi = crmiWithLibraries(
                libraryWith(new org.hl7.fhir.r4.model.ParameterDefinition().setName("paciente").setType("Patient")));
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi));
    }

    @Test
    void rejectsLibraryNotFound() {
        PlanDefinition subplan = new PlanDefinition();
        subplan.addLibrary(SUBPLAN + "-library-inexistente");
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().add(SUBPLAN, subplan);
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader(PLAN_WITH_SUBPROCESS, crmi));
    }

    /**
     * Plan con una única acción "a" cuyo PlanDefinition es SUBPLAN, con un dynamicValue por cada
     * par path / expresión FHIRPath
     */
    private static String planWithDynamicValues(String... pathsAndExpressions) {
        StringBuilder dynamicValues = new StringBuilder();
        for (int i = 0; i < pathsAndExpressions.length; i += 2) {
            dynamicValues.append(i == 0 ? "" : ", ").append("""
                    {"path": "%s", "expression": {"language": "text/fhirpath", "expression": "%s"}}"""
                    .formatted(pathsAndExpressions[i], pathsAndExpressions[i + 1]));
        }
        return """
                {"resourceType": "PlanDefinition", "name": "ConValores",
                 "action": [{"id": "a", "definitionCanonical": "%s", "dynamicValue": [%s]}]}
                """.formatted(SUBPLAN, dynamicValues);
    }

    /** Resolver con SUBPLAN y una Library con un parámetro obligatorio, uno opcional y uno de salida */
    private static InMemoryR4Resolver crmiWithNotificationParameters() {
        return crmiWithLibraries(libraryWith(
                r4Parameter("paciente", ParameterUse.IN, "Patient").setMin(1),
                r4Parameter("motivo", ParameterUse.IN, "string"),
                r4Parameter("enviada", ParameterUse.OUT, "boolean")));
    }

    @Test
    void dynamicValuesBecomeValuesOfInputParameters() {
        String plan = planWithDynamicValues("paciente", "%subject", "motivo", "'revisión'");
        ActionWrapper<PlanDefinitionActionComponent> a =
                action(new R4PlanReader(plan, crmiWithNotificationParameters()), "a");

        Expression paciente = (Expression) a.getInParameters().get("paciente").getValue();
        assertEquals("text/fhirpath", paciente.getLanguage());
        assertEquals("%subject", paciente.getExpression());
        // El texto del valor, para quien no conoce la versión de FHIR
        assertEquals("%subject", a.getInParameters().get("paciente").getValueText());
        // La acción no tiene title: su nombre es su id
        assertEquals("a", a.getName());
        assertEquals("'revisión'", ((Expression) a.getInParameters().get("motivo").getValue()).getExpression());
        assertNull(a.getOutParameters().get("enviada").getValue());
    }

    @Test
    void optionalParameterMayHaveNoValue() {
        String plan = planWithDynamicValues("paciente", "%subject");
        ActionWrapper<PlanDefinitionActionComponent> a =
                action(new R4PlanReader(plan, crmiWithNotificationParameters()), "a");
        assertNull(a.getInParameters().get("motivo").getValue());
    }

    @Test
    void rejectsRequiredParameterWithoutValue() {
        String plan = planWithDynamicValues("motivo", "'revisión'");
        assertThrows(InvalidPlanDefinitionException.class,
                () -> new R4PlanReader(plan, crmiWithNotificationParameters()));
    }

    @Test
    void rejectsDynamicValueThatIsNotAnInputParameter() {
        // "pacinte" mal escrito, y "enviada" es de salida
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader(
                planWithDynamicValues("paciente", "%subject", "pacinte", "%subject"), crmiWithNotificationParameters()));
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader(
                planWithDynamicValues("paciente", "%subject", "enviada", "true"), crmiWithNotificationParameters()));
    }

    @Test
    void rejectsTwoValuesForTheSameParameter() {
        String plan = planWithDynamicValues("paciente", "%subject", "paciente", "%patient");
        assertThrows(InvalidPlanDefinitionException.class,
                () -> new R4PlanReader(plan, crmiWithNotificationParameters()));
    }

    /**
     * Para los tests que no tratan de subprocesos: da a cada acción sin definitionCanonical un
     * PlanDefinition existente, ya que toda acción debe tenerlo.
     */
    private static R4PlanReader readerWithProcesses(String rawPlan) {
        String subplan = "http://ejemplo/PlanDefinition/tarea";
        IParser parser = FhirContext.forR4().newJsonParser();
        PlanDefinition plan = parser.parseResource(PlanDefinition.class, rawPlan);
        plan.getAction().stream()
                .filter(a -> !a.hasDefinition())
                .forEach(a -> a.setDefinition(new CanonicalType(subplan)));
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().add(subplan, new PlanDefinition());
        return new R4PlanReader(parser.encodeResourceToString(plan), crmi);
    }
}
