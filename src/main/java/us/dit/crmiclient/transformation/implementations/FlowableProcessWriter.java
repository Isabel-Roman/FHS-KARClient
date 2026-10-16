package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.transformation.BpmnWriter;

import java.nio.charset.StandardCharsets;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Process;
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
        //this.process.setId("process");
        this.process.setExecutable(true);
        this.model.addProcess(this.process);
        logger.debug("Creado proceso BPMN vacío");
    }

    @Override 
    public String getModel() {
        logger.debug("Convirtiendo a XML el proceso {}", process.getId());
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
    public void setStartEvent(String startEvent) {
        // Implementar la lógica para agregar un evento de inicio al proceso BPMN
        // Por ejemplo, podrías crear un StartEvent y agregarlo al proceso
        StartEvent start = new StartEvent();
        start.setId(startEvent);
        logger.debug("Añadido evento de inicio {}", startEvent);
        process.addFlowElement(start);
    }   
    @Override
    public void setEndEvent(String endEvent) {
        // Implementar la lógica para agregar un evento de fin al proceso BPMN
        // Por ejemplo, podrías crear un EndEvent y agregarlo al proceso
        EndEvent end = new EndEvent();      
        end.setId(endEvent);
        logger.debug("Añadido evento de fin {}", endEvent);
        process.addFlowElement(end);
    }
}
