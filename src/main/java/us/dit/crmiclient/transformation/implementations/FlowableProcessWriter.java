package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.model.SimpleGateway;
import us.dit.crmiclient.transformation.BpmnWriter;

import java.nio.charset.StandardCharsets;

import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowableProcessWriter implements BpmnWriter {
    private static final Logger logger = LoggerFactory.getLogger(FlowableProcessWriter.class);

    private final BpmnModel model;
    private final Process process;

    public FlowableProcessWriter() {
        this.model = new BpmnModel();
        this.process = new Process();
        this.process.setExecutable(true);

        this.model.addProcess(this.process);

        logger.debug("Creado proceso BPMN vacío");
    }

    @Override
    public void setDocumentation(String documentation) {
        logger.debug("Documentación del proceso: {}", documentation);
        this.process.setDocumentation(documentation);
    }

    @Override
    public String getModel() {
        logger.debug("Convirtiendo a XML el proceso {}", process.getId());
        setDiagram(); // Genera el diagrama antes de convertir a XML
        byte[] xml = new BpmnXMLConverter().convertToXML(model);
        return new String(xml, StandardCharsets.UTF_8);
    }

    @Override
    public void setName(String name) {
        logger.debug("Nombre del proceso: {}", name);
        this.process.setName(name);
    }

    @Override
    public void setId(String id) {
        logger.debug("Id del proceso: {}", id);
        this.process.setId(id);
    }

    @Override
    public void setStartEvent(SimpleGateway startGateway) {
        // Implementar la lógica para agregar un evento de inicio al proceso BPMN
        // Por ejemplo, podrías crear un StartEvent y agregarlo al proceso
        StartEvent start = new StartEvent();
        start.setId("startEvent");
       
        process.addFlowElement(start);
        logger.debug("Añadido evento de inicio {}", start.getId());
        //Si hay gatway de inicio se añade y se conecta con el eventto de inicio
        if (startGateway != null) {
            Gateway gateway = toFlowableGateway(startGateway);
            process.addFlowElement(gateway);
            logger.debug("Añadido gateway de inicio {}", gateway.getId());
            addSequenceFlow(start.getId(), gateway.getId());
        }
    }

    

    /**
     * Crea el gateway de Flowable que corresponde a un SimpleGateway, con su mismo
     * id
     */
    private static Gateway toFlowableGateway(SimpleGateway simple) {
        Gateway gateway = switch (simple.getType()) {
            case AND -> new ParallelGateway();
            case OR -> new InclusiveGateway();
        };
        gateway.setId(simple.getId());
        return gateway;
    }

    private void addSequenceFlow(String sourceId, String targetId) {
        SequenceFlow flow = new SequenceFlow(sourceId, targetId);
        flow.setId("flow_" + sourceId + "_" + targetId);
        process.addFlowElement(flow);
    }

    @Override
    public void setEndEvent(SimpleGateway endGateway) {
        // Implementar la lógica para agregar un evento de fin al proceso BPMN
        // Por ejemplo, podrías crear un EndEvent y agregarlo al proceso
        EndEvent end = new EndEvent();
        end.setId("endEvent");
        logger.debug("Añadido evento de fin {}", end.getId());
        //Si hay gateway de salida se añade y se conecta con el evento de fin
        if (endGateway != null) {
            Gateway gateway = toFlowableGateway(endGateway);
            process.addFlowElement(gateway);
            logger.debug("Añadido gateway de fin {}", gateway.getId());
            addSequenceFlow(gateway.getId(), end.getId());
        }
        process.addFlowElement(end);
    }

    private void setDiagram() {
        logger.debug("Generando diagrama del proceso {}", process.getId());
        new BpmnAutoLayout(model).execute();
    }
}
