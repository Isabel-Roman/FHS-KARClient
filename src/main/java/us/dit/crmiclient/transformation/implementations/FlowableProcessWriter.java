package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.Flow;
import us.dit.crmiclient.model.ParameterWrapper;
import us.dit.crmiclient.model.ProcessEvent;
import us.dit.crmiclient.model.ProcessNode;
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

    /**
     * Topic por defecto de las tareas: un único trabajador genérico, suscrito a él, arranca el
     * subproceso de cualquier acción a partir de su canónica.
     */
    public static final String DEFAULT_TOPIC = "launch-subprocess";
    /**
     * Nombre del parámetro con el que cada tarea pasa al trabajador la canónica del PlanDefinition
     * de su subproceso, para que sepa cuál arrancar.
     */
    public static final String SUBPROCESS_CANONICAL_PARAMETER = "subprocessCanonical";
    /** Id del proceso mientras no se fije con setId: BPMN exige que el proceso tenga id */
    private static final String DEFAULT_PROCESS_ID = "process";

    private final BpmnModel model;
    private final Process process;
    /** Topic de todas las tareas external-worker del proceso */
    private final String topic;
    private Set<String> addedElements = null;

    /** Writer con el topic por defecto, {@link #DEFAULT_TOPIC} */
    public FlowableProcessWriter() {
        this(DEFAULT_TOPIC);
    }

    /** @param topic topic de las tareas external-worker del proceso */
    public FlowableProcessWriter(String topic) {
        this.topic = topic;
        this.model = new BpmnModel();
        this.process = new Process();
        this.process.setId(DEFAULT_PROCESS_ID);
        this.process.setExecutable(true);

        this.model.addProcess(this.process);
        addedElements = new HashSet<>();

        logger.debug("Creado proceso BPMN vacío, con tareas en el topic {}", topic);
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

    /**
     * Crea el elemento BPMN que corresponde al nodo. Como ProcessNode es una interfaz sellada, el
     * switch cubre todos los tipos de nodo sin default: si se añade uno nuevo, no compila hasta
     * tratarlo aquí.
     */
    @Override
    public void addNode(ProcessNode node) {
        switch (node) {
            case ProcessEvent event -> addEvent(event);
            case SimpleGateway gateway -> addFlowElement(toFlowableGateway(gateway));
            case ActionWrapper<?> action -> addServiceTask(action);
        }
    }

    @Override
    public void addFlow(Flow flow) {
        String sourceId = flow.source().getId();
        String targetId = flow.target().getId();
        logger.debug("Añadiendo flujo de secuencia desde {} hasta {}", sourceId, targetId);
        SequenceFlow sequenceFlow = new SequenceFlow(sourceId, targetId);
        sequenceFlow.setId("flow_" + sourceId + "_" + targetId);
        addFlowElement(sequenceFlow);
    }

    private void addEvent(ProcessEvent event) {
        FlowElement element = switch (event.kind()) {
            case START -> new StartEvent();
            case END -> new EndEvent();
        };
        element.setId(event.getId());
        addFlowElement(element);
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

    /**
     * Añade la acción como tarea de servicio de tipo external-worker: el motor crea un trabajo en
     * el topic, y un trabajador externo suscrito a él lo ejecuta (arrancando el subproceso).
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
     * Además de los parámetros del subproceso, se pasa la canónica de su PlanDefinition en
     * {@link #SUBPROCESS_CANONICAL_PARAMETER}, para que el trabajador sepa qué subproceso arrancar.
     * <p>
     * Con doNotIncludeVariables el trabajador recibe solo esos parámetros, no todas las variables
     * del proceso. Los valores son el texto del valor de cada parámetro (p.ej. la expresión
     * FHIRPath del dynamicValue, sin evaluar). Los parámetros de entrada sin valor (opcionales a
     * los que la acción no da valor) no se pasan.
     */
    private void addServiceTask(ActionWrapper<?> action) {
        ExternalWorkerServiceTask task = new ExternalWorkerServiceTask();
        task.setId(action.getId());
        task.setName(action.getName());
        task.setType(ServiceTask.EXTERNAL_WORKER_TASK);
        task.setTopic(topic);
        task.setDoNotIncludeVariables(true);
        if (action.getSubprocessCanonical() != null) {
            addInput(task, SUBPROCESS_CANONICAL_PARAMETER, action.getSubprocessCanonical());
        }
        for (ParameterWrapper<?> parameter : action.getInParameters().values()) {
            if (parameter.getValueText() != null) {
                addInput(task, parameter.getName(), parameter.getValueText());
            }
        }
        addFlowElement(task);
        logger.debug("Añadida tarea external-worker {} en el topic {}", action.getId(), topic);
    }

    /** Declara la variable idTarea_nombre con el valor dado y la pasa a la tarea como nombre */
    private void addInput(ExternalWorkerServiceTask task, String name, String value) {
        String variable = task.getId() + "_" + name;
        StringDataObject dataObject = new StringDataObject();
        dataObject.setId("do_" + variable);
        dataObject.setName(variable);
        dataObject.setItemSubjectRef(stringItem());
        dataObject.setValue(value);
        // El conversor a XML escribe los dataObject que están entre los elementos del proceso
        addFlowElement(dataObject);

        IOParameter in = new IOParameter();
        in.setSource(variable);
        in.setTarget(name);
        task.addInParameter(in);
        logger.debug("Tarea {}: parámetro {} desde la variable {} = {}", task.getId(), name, variable, value);
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
