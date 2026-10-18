package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        writer.setStartEvent("Inicio");
        writer.setEndEvent("Fin");
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
        writer.setStartEvent("inicio");
        writer.setEndEvent("fin");
      

        String xml = writer.getModel();
        assertTrue(xml.contains("BPMNDiagram"));

        BpmnModel model = parse(xml);
        assertNotNull(model.getGraphicInfo("inicio"));
        assertNotNull(model.getGraphicInfo("fin"));
    }

    @Test
    void getModelKeepsNonAsciiCharacters() throws Exception {
        writer.setName("Detección de infección");

        assertTrue(writer.getModel().contains("Detección de infección"));
        assertEquals("Detección de infección", parse(writer.getModel()).getMainProcess().getName());
    }
}
