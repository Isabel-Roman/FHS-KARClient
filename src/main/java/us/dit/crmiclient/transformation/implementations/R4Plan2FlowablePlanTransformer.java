package us.dit.crmiclient.transformation.implementations;
import us.dit.crmiclient.transformation.BpmnWriter;
import us.dit.crmiclient.transformation.PlanReader;
import us.dit.crmiclient.transformation.PlanTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforma un PlanDefinition de fhir R4 en un proceso bpmn ejecutable en motores Flowable.
 */
public class R4Plan2FlowablePlanTransformer implements PlanTransformer {
    private static final Logger logger = LoggerFactory.getLogger(R4Plan2FlowablePlanTransformer.class);

    private PlanReader planReader=null;
    private BpmnWriter processWriter=null;

    @Override
    public String Plan2Bpmn(String rawPlanDefinition) {
        logger.info("Iniciando transformación de PlanDefinition a BPMN");
        this.planReader=new R4PlanReader(rawPlanDefinition);
        this.processWriter= new FlowableProcessWriter();
        // La lógica de transformación se descompone en pasos
        mapName();
        mapId();
        mapStart();
        mapEnd();

        logger.info("Transformación completada para el plan {}", planReader.getId());
        return processWriter.getModel();
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
        String startEvent = planReader.getStartEvent();
        processWriter.setStartEvent(startEvent);
    }
    private void mapEnd(){
        // Implementar la lógica para mapear el final del plan al proceso BPMN
        String endEvent = planReader.getEndEvent();
        processWriter.setEndEvent(endEvent);
    }

}
