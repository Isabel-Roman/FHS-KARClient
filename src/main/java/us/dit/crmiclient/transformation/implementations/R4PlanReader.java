package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.transformation.PlanReader;

import java.util.List;
import java.util.UUID;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4PlanReader implements PlanReader {
    private static final Logger logger = LoggerFactory.getLogger(R4PlanReader.class);
    private final PlanDefinition plan;
    private String generatedId;
   
    private List<ActionWrapper<PlanDefinitionActionComponent>> actions;

    public R4PlanReader(String rawPlanDefinition) {
        this.plan = FhirContext.forR4()
                .newJsonParser()
                .parseResource(PlanDefinition.class, rawPlanDefinition);
        
        createActions();
        actionsOrder();
        actionsReasoning();
        logger.debug("PlanDefinition leído: id={}, name={}", plan.getId(), plan.getName());
    }
    private void createActions() {
        actions=new java.util.ArrayList<>();
        for (PlanDefinitionActionComponent action : plan.getAction()) {
            ActionWrapper<PlanDefinitionActionComponent> wrapper = new ActionWrapper<>(action);
            wrapper.setId(action.getId());
           
            actions.add(wrapper);
        }
    }
    /**
     * Rellena metadatos de los wrappers
     */
    private void actionsReasoning() {
        
    }
    /**
     * Implementa ordenamiento de las acciones del plan, para completar la información de la lista de ActionWrappers con los metadatos necesarios para la gestión de las acciones y su manipulación en una estructura de lista enlazada.
     */
    private void actionsOrder() {
        //Busco las relaciones antes y después de cada acción y añado las relaciones en los wrappers correspondientes
        for(ActionWrapper<PlanDefinitionActionComponent> wrapper: actions) {
            PlanDefinitionActionComponent action = wrapper.getAction();
            List<PlanDefinitionActionRelatedActionComponent> relatedActions = action.getRelatedAction();
            for(PlanDefinitionActionRelatedActionComponent relatedAction : relatedActions) {
                String relatedActionId = relatedAction.getActionId();
                ActionRelationshipType relationship = relatedAction.getRelationship();
                switch (relationship) {
                    case BEFORE:
                        // Implement logic for BEFORE relationship
                        actions.stream()
                                .filter(a -> a.getId().equals(relatedActionId))
                                .findFirst()
                                .ifPresent(a -> {
                                    wrapper.setNext(a);
                                    a.setPrevious(wrapper);
                                });
                        break;
                    case AFTER:
                        // Implement logic for AFTER relationship
                        actions.stream()
                                .filter(a -> a.getId().equals(relatedActionId))
                                .findFirst()
                                .ifPresent(a -> {
                                    wrapper.setPrevious(a);
                                    a.setNext(wrapper);
                                });
                        break;
                    case CONCURRENT:
                        // Implement logic for CONCURRENT relationship
                        break;
                    default:
                        logger.warn("Unknown relationship type: {}", relationship);
                }
                
            }     
           
        }
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

    /**
     * Id del proceso: el id del PlanDefinition; si no tiene (p.ej. un plan nuevo que aún no está
     * en el servidor) se usa su name, y si tampoco tiene name se genera uno.
     * El id generado se guarda para que llamadas sucesivas devuelvan siempre el mismo.
     */
    @Override
    public String getId() {
        if (generatedId != null) {
            return generatedId;
        }
        String resultado;
        if (plan.getIdElement().hasIdPart()) {
            // getIdPart descarta el prefijo "PlanDefinition/" y la versión
            resultado = plan.getIdElement().getIdPart();
        } else if (plan.hasName()) {
            resultado = toBpmnId(plan.getName());
        } else {
            generatedId = "plan-" + UUID.randomUUID();
            resultado = generatedId;
        }
        logger.debug("Obteniendo ID del PlanDefinition: {}", resultado);
        return resultado;
    }

    /**
     * Los ids BPMN son NCName de XML: deben empezar por letra o '_' y no admiten espacios
     * ni la mayoría de símbolos. Los caracteres no válidos se sustituyen por '_'.
     */
    private static String toBpmnId(String name) {
        String id = name.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        return id.matches("^[A-Za-z_].*") ? id : "_" + id;
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
