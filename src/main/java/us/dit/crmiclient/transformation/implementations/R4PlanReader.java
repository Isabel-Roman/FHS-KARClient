package us.dit.crmiclient.transformation.implementations;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.PlanDefinition;

import us.dit.crmiclient.transformation.PlanReader;

public class R4PlanReader implements PlanReader {
    private final PlanDefinition plan;

    public R4PlanReader(String rawPlanDefinition) {
        this.plan = FhirContext.forR4()
                .newJsonParser()
                .parseResource(PlanDefinition.class, rawPlanDefinition);
    }

    @Override
    public String getName() {
        // Implement logic to extract the name from the planDefinition
        return "Extracted Name"; // Placeholder implementation
    }

}
