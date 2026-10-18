package us.dit.crmiclient.transformation;

/**
 * El PlanDefinition es un recurso FHIR bien formado, pero no cumple las reglas que exige el
 * proyecto para transformarlo en BPMN (ids de acción duplicados, acciones anidadas...).
 * Es un error del contenido enviado por el cliente, no un fallo del servidor.
 */
public class InvalidPlanDefinitionException extends RuntimeException {

    public InvalidPlanDefinitionException(String message) {
        super(message);
    }
}
