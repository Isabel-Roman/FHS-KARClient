package us.dit.crmiclient.transformation.implementations;

import java.nio.charset.StandardCharsets;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;

public class FlowableProcessWriter implements BpmnWriter {
    private final BpmnModel model;
    private final Process process;

    public FlowableProcessWriter() {
        this.model = new BpmnModel();
        this.process = new Process();
        this.process.setId("process");
        this.process.setExecutable(true);
        this.model.addProcess(this.process);
    }

    @Override
    public void setName(String name) {
        this.process.setName(name);
    }

    public String getModel() {
        byte[] xml = new BpmnXMLConverter().convertToXML(model);
        return new String(xml, StandardCharsets.UTF_8);
    }

}
