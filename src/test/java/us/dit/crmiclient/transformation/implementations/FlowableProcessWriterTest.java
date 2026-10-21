package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.ValuedDataObject;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.ParameterDefinition.ParameterUse;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.Flow;
import us.dit.crmiclient.model.ParameterWrapper;
import us.dit.crmiclient.model.ProcessEvent;
import us.dit.crmiclient.model.SimpleGateway;

class FlowableProcessWriterTest {

    private FlowableProcessWriter writer;

    @BeforeEach
    void setUp() {
        writer = new FlowableProcessWriter();
    }

    private BpmnModel parse(String xml) throws Exception {
        XMLStreamReader reader = XMLInputFactory.newInstance()
                .createXMLStreamReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return new BpmnXMLConverter().convertToBpmnModel(reader);
    }

    /** El proceso mínimo que genera cualquier plan: inicio → fin */
    private void minimalProcess() {
        writer.addNode(ProcessEvent.START);
        writer.addNode(ProcessEvent.END);
        writer.addFlow(new Flow(ProcessEvent.START, ProcessEvent.END));
    }

    @Test
    void constructorCreatesExecutableProcess() throws Exception {
        writer.setId("process");
        writer.setName("Process Name");
        writer.setDocumentation("Process Documentation");
        minimalProcess();
        String xml = writer.getModel();

        assertNotNull(xml);
        assertTrue(xml.contains("<definitions"));

        BpmnModel model = parse(xml);
        assertEquals(1, model.getProcesses().size());
        Process process = model.getMainProcess();
        assertEquals("process", process.getId());
        assertTrue(process.isExecutable());
    }

    @Test
    void setNameSetsProcessName() throws Exception {
        writer.setName("Mi proceso");
        minimalProcess();

        Process process = parse(writer.getModel()).getMainProcess();
        assertEquals("Mi proceso", process.getName());
    }

    @Test
    void setNameOverwritesPreviousName() throws Exception {
        writer.setName("Primero");
        writer.setName("Segundo");
        minimalProcess();

        Process process = parse(writer.getModel()).getMainProcess();
        assertEquals("Segundo", process.getName());
    }

    @Test
    void addNodeCreatesTheElementOfEachKindOfNode() throws Exception {
        writer.addNode(ProcessEvent.START);
        writer.addNode(new SimpleGateway("gw_and", SimpleGateway.GatewayType.AND));
        writer.addNode(new SimpleGateway("gw_or", SimpleGateway.GatewayType.OR));
        writer.addNode(ProcessEvent.END);
        writer.addFlow(new Flow(ProcessEvent.START, new SimpleGateway("gw_and", SimpleGateway.GatewayType.AND)));

        Process process = parse(writer.getModel()).getMainProcess();
        assertTrue(process.getFlowElement("startEvent") instanceof StartEvent);
        assertTrue(process.getFlowElement("gw_and") instanceof ParallelGateway);
        assertTrue(process.getFlowElement("gw_or") instanceof InclusiveGateway);
        assertTrue(process.getFlowElement("endEvent") instanceof EndEvent);
        SequenceFlow flow = (SequenceFlow) process.getFlowElement("flow_startEvent_gw_and");
        assertEquals("startEvent", flow.getSourceRef());
        assertEquals("gw_and", flow.getTargetRef());
    }

    /** proceso con un gateway de inicio y uno de fin, sin tareas */
    @Test
    void addDiagramGeneratesShapesForFlowElements() throws Exception {
        writer.setId("proceso");
        writer.setName("Proceso de prueba gateways");
        SimpleGateway startGateway = new SimpleGateway("gw_inicio", SimpleGateway.GatewayType.AND);
        SimpleGateway endGateway = new SimpleGateway("gw_fin", SimpleGateway.GatewayType.OR);
        writer.addNode(ProcessEvent.START);
        writer.addNode(startGateway);
        writer.addNode(endGateway);
        writer.addNode(ProcessEvent.END);
        writer.addFlow(new Flow(ProcessEvent.START, startGateway));
        writer.addFlow(new Flow(startGateway, endGateway));
        writer.addFlow(new Flow(endGateway, ProcessEvent.END));
        String xml = writer.getModel();
        assertTrue(xml.contains("BPMNDiagram"));
        System.out.println("xml generado: \n" + xml);

        BpmnModel model = parse(xml);
        assertNotNull(model.getGraphicInfo("gw_inicio"));
        assertNotNull(model.getGraphicInfo("gw_fin"));
    }

    /** Parámetro de entrada de tipo string con el texto de su valor (null si no tiene valor) */
    private static ParameterWrapper<Object> inputParameter(String name, String valueText) {
        ParameterWrapper<Object> parameter = new ParameterWrapper<>(null, name, ParameterWrapper.Use.IN, 0, null,
                "string", null, null, null);
        if (valueText != null) {
            parameter.setValue(null, valueText);
        }
        return parameter;
    }

    /** proceso con una sola tarea, sin gateways */
    @Test
    void addNodeAddsExternalWorkerTaskWithInputParameters() throws Exception {
        writer = new FlowableProcessWriter("notificar");
        writer.setId("proceso");
        writer.setName("Proceso de prueba acción");
        ActionWrapper<Object> action = new ActionWrapper<>(null, "a");
        action.setName("Notificar");
        action.setSubprocessCanonical("http://ejemplo/PlanDefinition/notificar|1.0.0");
        action.addParameter(inputParameter("paciente", "PACIENTE-TEST"));
        action.addParameter(inputParameter("motivo", "MOTIVO-TEST"));
        // Opcional sin valor: no se pasa a la tarea
        action.addParameter(inputParameter("prioridad", null));
        writer.addNode(ProcessEvent.START);
        writer.addNode(action);
        writer.addNode(ProcessEvent.END);
        writer.addFlow(new Flow(ProcessEvent.START, action));
        writer.addFlow(new Flow(action, ProcessEvent.END));

        String xml = writer.getModel();
        System.out.println("xml generado: \n" + xml);
        assertTrue(xml.contains("flowable:type=\"external-worker\""));

        Process process = parse(xml).getMainProcess();
        ExternalWorkerServiceTask task = (ExternalWorkerServiceTask) process.getFlowElement("a");
        assertEquals("Notificar", task.getName());
        assertEquals("notificar", task.getTopic());
        assertTrue(task.isDoNotIncludeVariables());

        // Primero la canónica del subproceso, y después un parámetro por cada parámetro con valor:
        // de la variable del proceso a la variable del trabajador
        assertEquals(List.of("a_subprocessCanonical->subprocessCanonical", "a_paciente->paciente",
                "a_motivo->motivo"), inputs(task));

        // Las variables del proceso general, con el valor de cada parámetro
        assertEquals(Map.of("a_subprocessCanonical", "http://ejemplo/PlanDefinition/notificar|1.0.0",
                "a_paciente", "PACIENTE-TEST", "a_motivo", "MOTIVO-TEST"), variables(process));
    }

    @Test
    void tasksUseTheDefaultTopicIfNoneIsGiven() throws Exception {
        ActionWrapper<Object> action = new ActionWrapper<>(null, "a");
        writer.addNode(ProcessEvent.START);
        writer.addNode(action);
        writer.addNode(ProcessEvent.END);
        writer.addFlow(new Flow(ProcessEvent.START, action));
        writer.addFlow(new Flow(action, ProcessEvent.END));

        ExternalWorkerServiceTask task = (ExternalWorkerServiceTask) parse(writer.getModel()).getMainProcess()
                .getFlowElement("a");
        assertEquals(FlowableProcessWriter.DEFAULT_TOPIC, task.getTopic());
        // Sin canónica ni parámetros con valor, la tarea no recibe nada
        assertTrue(task.getInParameters().isEmpty());
    }

    @Test
    void writesTheProcessReadByThePlanReader() throws Exception {
        // Servidor CRMI simulado: el subproceso "notificar" declara en su Library un parámetro
        // obligatorio (paciente), uno opcional (motivo) y uno de salida (enviada)
        String subplanCanonical = "http://ejemplo/PlanDefinition/notificar";
        String libraryCanonical = "http://ejemplo/Library/notificar";
        PlanDefinition subplan = new PlanDefinition();
        subplan.addLibrary(libraryCanonical);
        Library library = new Library();
        library.addParameter(new ParameterDefinition().setName("paciente").setUse(ParameterUse.IN)
                .setType("Patient").setMin(1));
        library.addParameter(new ParameterDefinition().setName("motivo").setUse(ParameterUse.IN).setType("string"));
        library.addParameter(new ParameterDefinition().setName("enviada").setUse(ParameterUse.OUT).setType("boolean"));
        InMemoryR4Resolver crmi = new InMemoryR4Resolver()
                .add(subplanCanonical, subplan)
                .add(libraryCanonical, library);

        // Plan con dos acciones del mismo subproceso, a y después b: "a" da valor a los dos
        // parámetros de entrada, "b" solo al obligatorio
        String plan = """
                {"resourceType": "PlanDefinition", "name": "PlanNotificaciones",
                 "action": [
                   {"id": "a", "title": "Notificar al paciente", "definitionCanonical": "%1$s",
                    "dynamicValue": [
                      {"path": "paciente", "expression": {"language": "text/fhirpath", "expression": "%%subject"}},
                      {"path": "motivo", "expression": {"language": "text/fhirpath", "expression": "'revisión'"}}]},
                   {"id": "b", "definitionCanonical": "%1$s",
                    "relatedAction": [{"actionId": "a", "relationship": "after"}],
                    "dynamicValue": [
                      {"path": "paciente", "expression": {"language": "text/fhirpath", "expression": "%%patient"}}]}]}
                """.formatted(subplanCanonical);
        R4PlanReader reader = new R4PlanReader(plan, crmi);

        writer.setId("plan-notificaciones");
        reader.getNodes().forEach(writer::addNode);
        reader.getFlows().forEach(writer::addFlow);
        String xml = writer.getModel();
        System.out.println("xml generado: \n" + xml);

        Process process = parse(xml).getMainProcess();
        ExternalWorkerServiceTask a = (ExternalWorkerServiceTask) process.getFlowElement("a");
        assertEquals("Notificar al paciente", a.getName());
        assertEquals(FlowableProcessWriter.DEFAULT_TOPIC, a.getTopic());
        assertEquals(List.of("a_subprocessCanonical->subprocessCanonical", "a_paciente->paciente",
                "a_motivo->motivo"), inputs(a));

        // Sin title, el nombre es el id; motivo es opcional y "b" no le da valor: no se pasa
        ExternalWorkerServiceTask b = (ExternalWorkerServiceTask) process.getFlowElement("b");
        assertEquals("b", b.getName());
        assertEquals(List.of("b_subprocessCanonical->subprocessCanonical", "b_paciente->paciente"), inputs(b));

        // Una variable del proceso por cada parámetro con valor, con el texto de su expresión
        assertEquals(Map.of("a_subprocessCanonical", subplanCanonical, "a_paciente", "%subject",
                "a_motivo", "'revisión'", "b_subprocessCanonical", subplanCanonical, "b_paciente", "%patient"),
                variables(process));

        // Los flujos son los del grafo del proceso: inicio → a → b → fin
        Set<String> flows = process.findFlowElementsOfType(SequenceFlow.class).stream()
                .map(f -> f.getSourceRef() + "->" + f.getTargetRef()).collect(Collectors.toSet());
        assertEquals(Set.of("startEvent->a", "a->b", "b->endEvent"), flows);
    }

    /** Parámetros de entrada de una tarea como "source->target" */
    private static List<String> inputs(ExternalWorkerServiceTask task) {
        return task.getInParameters().stream().map(p -> p.getSource() + "->" + p.getTarget()).toList();
    }

    /** Variables declaradas en el proceso (dataObject), con su valor */
    private static Map<String, Object> variables(Process process) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (ValuedDataObject dataObject : process.getDataObjects()) {
            variables.put(dataObject.getName(), dataObject.getValue());
        }
        return variables;
    }

    @Test
    void getModelKeepsNonAsciiCharacters() throws Exception {
        writer.setName("Detección de infección");
        minimalProcess();

        assertTrue(writer.getModel().contains("Detección de infección"));
        assertEquals("Detección de infección", parse(writer.getModel()).getMainProcess().getName());
    }
}
