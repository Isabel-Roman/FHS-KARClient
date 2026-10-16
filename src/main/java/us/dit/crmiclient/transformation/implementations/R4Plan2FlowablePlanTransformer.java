package us.dit.crmiclient.transformation.implementations;
import us.dit.crmiclient.transformation.BpmnWriter;
import us.dit.crmiclient.transformation.PlanReader;
import us.dit.crmiclient.transformation.PlanTransformer;
/**
 * Transforma un PlanDefinition de fhir R4 en un proceso bpmn ejecutable en motores Flowable.
 */
public class R4Plan2FlowablePlanTransformer implements PlanTransformer {
    private R4PlanReader planReader;
    private FlowableProcessWriter processWriter;

    public R4Plan2FlowablePlanTransformer(String rawPlanDefinition) {
        this.planReader = new R4PlanReader();
        this.processWriter = new FlowableProcessWriter();
    }

}
