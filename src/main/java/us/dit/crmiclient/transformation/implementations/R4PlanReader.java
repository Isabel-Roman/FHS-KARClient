package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.transformation.PlanReader;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4PlanReader implements PlanReader {
    private static final Logger logger = LoggerFactory.getLogger(R4PlanReader.class);
    private final PlanDefinition plan;

    public R4PlanReader(String rawPlanDefinition) {
        this.plan = FhirContext.forR4()
                .newJsonParser()
                .parseResource(PlanDefinition.class, rawPlanDefinition);
        logger.debug("PlanDefinition leído: id={}, name={}", plan.getId(), plan.getName());
    }
    @Override 
    public String getDocumentation() {        
        String documentation="";
        String publisher = plan.getPublisher();
        if (publisher != null && !publisher.isEmpty()) {
            documentation += "Publisher: " + publisher + " ";
        }
        String description = plan.getDescription();
      
        if (description != null && !description.isEmpty()) {
            documentation += "Description: " + description + " ";
        }
        String usage = plan.getUsage();
        if (usage != null && !usage.isEmpty()) {
            documentation += "Usage: " + usage + " ";
        }
        String purpose = plan.getPurpose();
        if (purpose != null && !purpose.isEmpty()) {
            documentation += "Purpose: " + purpose+ " ";
        }
        // Implement logic to extract the documentation from the planDefinition
        return documentation; // Placeholder implementation
    }

    @Override
    public String getName() {
        // Implement logic to extract the name from the planDefinition
        return plan.getName(); // Placeholder implementation
    }

    @Override
    public String getId() {
        String id = plan.getId();
        String prefijo = "PlanDefinition/";

        // Si empieza por el prefijo, recortamos la longitud del prefijo; si no, dejamos
        // el texto original
        String resultado = id.startsWith(prefijo)
                ? id.substring(prefijo.length())
                : id;
        logger.debug("Obteniendo ID del PlanDefinition: {}", resultado);
        // Implement logic to extract the ID from the planDefinition
        return resultado; // Placeholder implementation
    }

    @Override
    public String getStartEvent() {
        // Implement logic to extract the start event from the planDefinition
        return "Extracted Start Event"; // Placeholder implementation
    }

    @Override
    public String getEndEvent() {
        // Implement logic to extract the end event from the planDefinition
        return "Extracted End Event"; // Placeholder implementation
    }

}
