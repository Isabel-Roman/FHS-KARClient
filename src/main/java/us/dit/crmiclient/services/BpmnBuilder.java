package us.dit.crmiclient.services;

import org.hl7.fhir.r4.model.PlanDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import science.aist.gtf.transformation.Transformer;

@Service
public class BpmnBuilder {

    final Transformer<PlanDefinition, String> bpmnTransformer;

    BpmnBuilder(@Qualifier("planDefinitionToBpmnStringTransformer") Transformer<PlanDefinition, String> bpmnTransformer) {
        this.bpmnTransformer = bpmnTransformer;
    }

    public String planDefinitionToBpmn(String rawPlanDefinition) {
        PlanDefinition planDefinition = FhirContext.forR4()
                .newJsonParser()
                .parseResource(PlanDefinition.class, rawPlanDefinition);

        return bpmnTransformer.applyTransformation(planDefinition);
    }

}
