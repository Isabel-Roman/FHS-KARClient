package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ValuedDataObject;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.ParameterDefinition.ParameterUse;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.ParameterWrapper;
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

    @Test
    void constructorCreatesExecutableProcess() throws Exception {
        writer.setId("process");
        writer.setName("Process Name");
        writer.setDocumentation("Process Documentation");
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

        Process process = parse(writer.getModel()).getMainProcess();
        assertEquals("Mi proceso", process.getName());
    }

    @Test
    void setNameOverwritesPreviousName() throws Exception {
        writer.setName("Primero");
        writer.setName("Segundo");

        Process process = parse(writer.getModel()).getMainProcess();
        assertEquals("Segundo", process.getName());
    }

    @Test
    void addDiagramGeneratesShapesForFlowElements() throws Exception {
        writer.setId("proceso");
        writer.setName("Proceso de prueba");
        SimpleGateway startGateway = new SimpleGateway("gw_inicio", SimpleGateway.GatewayType.AND);
        SimpleGateway endGateway = new SimpleGateway("gw_fin", SimpleGateway.GatewayType.OR);
        String lastId=writer.setStartEvent(startGateway);      
        writer.setEndEvent(endGateway, lastId);
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

    @Test
    void setServiceTaskAddsExternalWorkerTaskWithInputParameters() throws Exception {
        writer.setId("proceso");
        String lastId = writer.setStartEvent(null);
        ActionWrapper<Object> action = new ActionWrapper<>(null);
        action.setId("a");
        action.setName("Notificar");
        action.addParameter(inputParameter("paciente", "%subject"));
        action.addParameter(inputParameter("motivo", "'revisión'"));
        // Opcional sin valor: no se pasa a la tarea
        action.addParameter(inputParameter("prioridad", null));
        writer.setServiceTask(action, "notificar");
        writer.setEndEvent(null,lastId);

        String xml = writer.getModel();
        System.out.println("xml generado: \n" + xml);
        assertTrue(xml.contains("flowable:type=\"external-worker\""));

        Process process = parse(xml).getMainProcess();
        ExternalWorkerServiceTask task = (ExternalWorkerServiceTask) process.getFlowElement("a");
        assertEquals("Notificar", task.getName());
        assertEquals("notificar", task.getTopic());
        assertTrue(task.isDoNotIncludeVariables());

        // Un flowable:in por parámetro: de la variable del proceso a la variable del trabajador
        assertEquals(2, task.getInParameters().size());
        IOParameter paciente = task.getInParameters().get(0);
        assertEquals("a_paciente", paciente.getSource());
        assertEquals("paciente", paciente.getTarget());
        assertEquals("a_motivo", task.getInParameters().get(1).getSource());

        // Las variables del proceso general, con el valor de cada parámetro
        Map<String, Object> variables = new LinkedHashMap<>();
        for (ValuedDataObject dataObject : process.getDataObjects()) {
            variables.put(dataObject.getName(), dataObject.getValue());
        }
        assertEquals(Map.of("a_paciente", "%subject", "a_motivo", "'revisión'"), variables);
    }

    @Test
    void setServiceTaskWritesTheActionsReadByThePlanReader() throws Exception {
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

        // Plan con dos acciones del mismo subproceso: "a" da valor a los dos parámetros de entrada,
        // "b" solo al obligatorio
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
        String lastId = writer.setStartEvent(null);
        reader.getActions().forEach(action -> writer.setServiceTask(action, "launch-subprocess"));
        writer.setEndEvent(null,lastId);
        String xml = writer.getModel();
        System.out.println("xml generado: \n" + xml);

        Process process = parse(xml).getMainProcess();
        ExternalWorkerServiceTask a = (ExternalWorkerServiceTask) process.getFlowElement("a");
        assertEquals("Notificar al paciente", a.getName());
        assertEquals("launch-subprocess", a.getTopic());
        assertEquals(List.of("a_paciente->paciente", "a_motivo->motivo"), inputs(a));

        // Sin title, el nombre es el id; motivo es opcional y "b" no le da valor: no se pasa
        ExternalWorkerServiceTask b = (ExternalWorkerServiceTask) process.getFlowElement("b");
        assertEquals("b", b.getName());
        assertEquals(List.of("b_paciente->paciente"), inputs(b));

        // Una variable del proceso por cada parámetro con valor, con el texto de su expresión
        Map<String, Object> variables = new LinkedHashMap<>();
        for (ValuedDataObject dataObject : process.getDataObjects()) {
            variables.put(dataObject.getName(), dataObject.getValue());
        }
        assertEquals(Map.of("a_paciente", "%subject", "a_motivo", "'revisión'", "b_paciente", "%patient"),
                variables);
    }

    /** Parámetros de entrada de una tarea como "source->target" */
    private static List<String> inputs(ExternalWorkerServiceTask task) {
        return task.getInParameters().stream().map(p -> p.getSource() + "->" + p.getTarget()).toList();
    }

    @Test
    void getModelKeepsNonAsciiCharacters() throws Exception {
        writer.setName("Detección de infección");

        assertTrue(writer.getModel().contains("Detección de infección"));
        assertEquals("Detección de infección", parse(writer.getModel()).getMainProcess().getName());
    }
}
