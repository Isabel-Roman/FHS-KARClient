package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.ParameterWrapper;
import us.dit.crmiclient.model.SimpleGateway;
import us.dit.crmiclient.transformation.BpmnWriter;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.ItemDefinition;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.StringDataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowableProcessWriter implements BpmnWriter {
    private static final Logger logger = LoggerFactory.getLogger(FlowableProcessWriter.class);

    private final BpmnModel model;
    private final Process process;
    private Set<String> addedElements = null;

    public FlowableProcessWriter() {
        this.model = new BpmnModel();
        this.process = new Process();
        this.process.setExecutable(true);

        this.model.addProcess(this.process);
        addedElements = new HashSet<>();

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
    public String setStartEvent(SimpleGateway startGateway) {
        // Implementar la lógica para agregar un evento de inicio al proceso BPMN
        // Por ejemplo, podrías crear un StartEvent y agregarlo al proceso
        StartEvent start = new StartEvent();
        start.setId("startEvent");
        String lastId = start.getId();
       
        addFlowElement(start);
        addedElements.add(start.getId());
        logger.debug("Añadido evento de inicio {}", start.getId());
        //Si hay gatway de inicio se añade y se conecta con el eventto de inicio
        if (startGateway != null) {
            Gateway gateway = toFlowableGateway(startGateway);
            addFlowElement(gateway);          
            logger.debug("Añadido gateway de inicio {}", gateway.getId());
            addSequenceFlow(start.getId(), gateway.getId());           
            lastId= gateway.getId();            
        }else{
            logger.debug("No hay gateway de inicio, ninguna o solo una acción");}
        return lastId;
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
        logger.debug("Añadiendo flujo de secuencia desde {} hasta {}", sourceId, targetId);
        SequenceFlow flow = new SequenceFlow(sourceId, targetId);
        flow.setId("flow_" + sourceId + "_" + targetId);
        addFlowElement(flow);       
        
    }

    @Override
    public void setEndEvent(SimpleGateway endGateway,String lastId) {
        logger.debug("Añadiendo evento de fin al proceso {} tras {}", process.getId(), lastId);
        // Implementar la lógica para agregar un evento de fin al proceso BPMN
        // Por ejemplo, podrías crear un EndEvent y agregarlo al proceso
        EndEvent end = new EndEvent();
        end.setId("endEvent");
        addFlowElement(end);      
        logger.debug("Añadido evento de fin {}", end.getId());
        //Si hay gateway de salida se añade y se conecta con el evento de fin
        if (endGateway != null) {
            Gateway gateway = toFlowableGateway(endGateway);
            addFlowElement(gateway);        
            addSequenceFlow(lastId, gateway.getId());
            logger.debug("Añadido gateway de fin {} y enlace creado", gateway.getId());
            lastId= gateway.getId();            
        }        
        addSequenceFlow(lastId, end.getId()); 
        logger.debug("Enlace creado desde {} hasta evento de fin {}", lastId, end.getId());     
    }

    /**
     * Añade la acción como tarea de servicio de tipo external-worker: el motor crea un trabajo en
     * el topic, y un trabajador externo suscrito a él lo ejecuta (p.ej. arrancando el subproceso).
     * <p>
     * Cada parámetro de entrada se pasa en dos pasos:
     * <ol>
     * <li>Variable del proceso general {@code idAcción_parámetro}, declarada como dataObject con el
     * valor del parámetro. Se usa '_' y no '.': en las expresiones de Flowable un punto significa
     * acceder a una propiedad, y ${a.paciente} no leería la variable "a.paciente".</li>
     * <li>Parámetro de entrada de la tarea con source = esa variable y target = nombre del
     * parámetro, que es la variable que recibe el trabajador. Es el equivalente a flowable:in de
     * las callActivity; en las tareas external-worker Flowable lo escribe como
     * {@code flowable:externalWorkerInParameter}.</li>
     * </ol>
     * Con doNotIncludeVariables el trabajador recibe solo esos parámetros, no todas las variables
     * del proceso. Los valores son el texto del valor de cada parámetro (p.ej. la expresión
     * FHIRPath del dynamicValue, sin evaluar). Los parámetros de entrada sin valor (opcionales a
     * los que la acción no da valor) no se pasan.
     */
    @Override
    public String setServiceTask(ActionWrapper<?> action, String topic, String lastId) {
        String id = action.getId();
        ExternalWorkerServiceTask task = new ExternalWorkerServiceTask();
        task.setId(action.getId());
        task.setName(action.getName());
        task.setType(ServiceTask.EXTERNAL_WORKER_TASK);
        task.setTopic(topic);
        task.setDoNotIncludeVariables(true);
        for (ParameterWrapper<?> parameter : action.getInParameters().values()) {
            if (parameter.getValueText() == null) {
                continue;
            }
            String variable = id + "_" + parameter.getName();
            StringDataObject dataObject = new StringDataObject();
            dataObject.setId("do_" + variable);
            dataObject.setName(variable);
            dataObject.setItemSubjectRef(stringItem());
            dataObject.setValue(parameter.getValueText());
            // El conversor a XML escribe los dataObject que están entre los elementos del proceso
            addFlowElement(dataObject);
           

            IOParameter in = new IOParameter();
            in.setSource(variable);
            in.setTarget(parameter.getName());
            task.addInParameter(in);
            logger.debug("Tarea {}: parámetro {} desde la variable {} = {}", id, parameter.getName(), variable,
                    parameter.getValueText());
        }
        addFlowElement(task);
        SimpleGateway inGateway = action.getInGateway();
        //Si hay gateway de entrada y no está ya añadido
        Gateway gateway = null;
        if (inGateway != null)
        {
            gateway= toFlowableGateway(inGateway);
            addFlowElement(gateway);
        }  
      
        //Si hay gateway tengo que conectar el elemento anterior al gateway y el gateway a la tarea, en caso contrario solo el elemento anterior a la tarea
        if (gateway != null) {
            addSequenceFlow(lastId, gateway.getId());
            addSequenceFlow(gateway.getId(), task.getId());
        } else {
            addSequenceFlow(lastId, task.getId());
        }      
        
        logger.debug("Añadida tarea external-worker {} en el topic {}", id, topic);
        logger.debug("En este momento el aspecto del proceso es {}",getModel());
        return task.getId();
    }

    /** Tipo de los dataObject: cadena de XML Schema */
    private static ItemDefinition stringItem() {
        ItemDefinition item = new ItemDefinition();
        item.setStructureRef("xsd:string");
        return item;
    }

    private void setDiagram() {
        logger.debug("Generando diagrama del proceso {}", process.getId());
        new BpmnAutoLayout(model).execute();
    }
    /**
     * Se encarga de asegurarse de que los elementos no se añaden por duplicado, usa un conjunto de identificadores de elementos del proceso
     * @param element el nuevo elemento que se quiere añadir al proceso
     */
    private void addFlowElement(FlowElement element) {
        logger.debug("Intentando añadir elemento de tipo {} con id {}", element.getClass().getSimpleName(), element.getId());
        if (addedElements.contains(element.getId())) {
            logger.warn("Intento de añadir elemento duplicado: {}", element.getId());
        } else {            
            process.addFlowElement(element);
            addedElements.add(element.getId());
            logger.debug("Añadido elemento: {}", element.getId());
        }      
    }
}
