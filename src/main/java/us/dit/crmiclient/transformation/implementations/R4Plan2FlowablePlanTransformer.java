package us.dit.crmiclient.transformation.implementations;
import us.dit.crmiclient.model.SimpleGateway;
import us.dit.crmiclient.transformation.BpmnWriter;
import us.dit.crmiclient.transformation.PlanReader;
import us.dit.crmiclient.transformation.PlanTransformer;
import us.dit.crmiclient.services.ResourceResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforma un PlanDefinition de fhir R4 en un proceso bpmn ejecutable en motores Flowable.
 */
public class R4Plan2FlowablePlanTransformer implements PlanTransformer {
    private static final Logger logger = LoggerFactory.getLogger(R4Plan2FlowablePlanTransformer.class);

    /** Tipo de motor (código de Endpoint.connectionType) en el que deben estar desplegados los subprocesos */
    public static final String ENGINE_TYPE = "flowable";

    private PlanReader planReader=null;
    private BpmnWriter processWriter=null;
    /** Obtiene del servidor CRMI los recursos de los subprocesos de las acciones */
    private final ResourceResolver resolver;
    /**
     * Localiza el despliegue en Flowable del subproceso de cada acción, a partir de la canónica que
     * deja el reader en el wrapper. Lo usará el mapeo de las acciones a tareas BPMN.
     */
    private final R4SubprocessLocator subprocessLocator;

    public R4Plan2FlowablePlanTransformer() {
        this.resolver = null;
        this.subprocessLocator = null;
        
        this.processWriter= new FlowableProcessWriter();
    }

    public R4Plan2FlowablePlanTransformer(ResourceResolver resolver) {
        this.resolver = resolver;
        this.subprocessLocator = new R4SubprocessLocator(resolver, ENGINE_TYPE);        
        this.processWriter= new FlowableProcessWriter();
        
    }

    @Override
    public String Plan2Bpmn(String rawPlanDefinition) {
        logger.info("Iniciando transformación de PlanDefinition a BPMN");
        this.planReader=new R4PlanReader(rawPlanDefinition, resolver);
        // La lógica de transformación se descompone en pasos
        mapName();
        mapId();
        mapDocumentation();
        mapStart();
        mapEnd();
        logger.info("Transformación completada para el plan {}", planReader.getId());
        return processWriter.getModel();
    }
    private void mapDocumentation(){
        // Implementar la lógica para mapear la documentación del plan al proceso BPMN
        String documentation = planReader.getDocumentation(); // Placeholder implementation
        processWriter.setDocumentation(documentation);
    }
    private void mapName(){
        String planName = planReader.getName();
        processWriter.setName(planName);
    }
    private void mapId(){
        // Implementar la lógica para mapear el ID del plan al proceso BPMN
        String planId = planReader.getId();
        processWriter.setId(planId);
    }
    private void mapStart(){
        // Implementar la lógica para mapear el inicio del plan al proceso BPMN
        SimpleGateway startGateway = planReader.getStartGateway();
        processWriter.setStartEvent(startGateway);
    }
    private void mapEnd(){
        // Implementar la lógica para mapear el final del plan al proceso BPMN
        SimpleGateway endGateway = planReader.getEndGateway();
        processWriter.setEndEvent(endGateway);
    }

   
}
