package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.junit.jupiter.api.Test;

import us.dit.crmiclient.model.ActionWrapper;
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
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "Secuencia",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before-start"}]},
                            {"id": "b"}]}
                """);
        assertNull(action(reader, "a").getOutGateway());
        assertNull(action(reader, "b").getInGateway());
        assertNull(reader.getStartGateway());
        assertNull(reader.getEndGateway());
    }

    @Test
    void forkAndJoinWithoutConditionsAreAnd() {
        // a -> (b, c) -> d
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "ForkJoin",
                 "action": [{"id": "a"},
                            {"id": "b", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "b", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """);
        SimpleGateway split = action(reader, "a").getOutGateway();
        SimpleGateway join = action(reader, "d").getInGateway();
        assertEquals(SimpleGateway.GatewayType.AND, split.getType());
        assertEquals("gw_out_a", split.getId());
        assertEquals(SimpleGateway.GatewayType.AND, join.getType());
        assertEquals("gw_in_d", join.getId());
    }

    @Test
    void conditionalBranchMakesSplitAndJoinOr() {
        // a -> (b [condición], c) -> d
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "Condicional",
                 "action": [{"id": "a"},
                            {"id": "b", %s, "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "b", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, action(reader, "a").getOutGateway().getType());
        assertEquals(SimpleGateway.GatewayType.OR, action(reader, "d").getInGateway().getType());
    }

    @Test
    void joinIsOrWhenAConditionIsFurtherUpTheBranch() {
        // a -> (b [condición] -> e, c) -> d : la rama que llega desde e puede no llegar
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "Cascada",
                 "action": [{"id": "a"},
                            {"id": "b", %s, "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "e", "relatedAction": [{"actionId": "b", "relationship": "after"}]},
                            {"id": "c", "relatedAction": [{"actionId": "a", "relationship": "after"}]},
                            {"id": "d", "relatedAction": [{"actionId": "e", "relationship": "after"},
                                                          {"actionId": "c", "relationship": "after"}]}]}
                """.formatted(CONDITION));
        assertNull(action(reader, "b").getOutGateway());
        assertEquals(SimpleGateway.GatewayType.OR, action(reader, "d").getInGateway().getType());
    }

    @Test
    void singleConditionalSuccessorNeedsOrSplit() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "UnaCondicional",
                 "action": [{"id": "a", "relatedAction": [{"actionId": "b", "relationship": "before"}]},
                            {"id": "b", %s}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, action(reader, "a").getOutGateway().getType());
    }

    @Test
    void independentActionsGetStartAndEndGateways() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "Independientes",
                 "action": [{"id": "x"}, {"id": "y", %s}]}
                """.formatted(CONDITION));
        assertEquals(SimpleGateway.GatewayType.OR, reader.getStartGateway().getType());
        assertEquals("gw_start", reader.getStartGateway().getId());
        assertEquals(SimpleGateway.GatewayType.OR, reader.getEndGateway().getType());
        assertEquals("gw_end", reader.getEndGateway().getId());
    }

    @Test
    void rejectsDuplicatedActionIds() {
        assertThrows(InvalidPlanDefinitionException.class, () -> new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "PlanDuplicado",
                 "action": [{"id": "a"}, {"id": "a"}]}
                """));
    }
}
