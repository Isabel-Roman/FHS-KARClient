package us.dit.crmiclient.services;

import us.dit.crmiclient.model.dtos.*;

import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

import us.dit.crmiclient.model.dtos.PlanSearchFilter;

public interface CrmiFacade {

    /**
     * 1. Publicación de un artefacto BPM+/CRMI.
     * Toma el PlanDefinition, genera la Library, la publica en crmi y la agrega al PlanDefinition, publica el plandefinition
     * cumpliendo el perfil crmi-publishable y los persiste en el repositorio.
     */
    void publishPlan(String rawPlanDefinition);

    /**
     * 2. Localización / Búsqueda de flujos en el catálogo.
     * Permite consultar el catálogo de procesos filtrando por título, estado, temática o editor.
     */
    List<PlanSummary> searchPlan(PlanSearchFilter filter);

    /**
     * 3. Descarga de un paquete completo ($package).
     * Invoca la operación $package del servidor CRMI y devuelve el Bundle con el PlanDefinition,
     * la Library asociada (con el XML BPMN) y todas sus dependencias transitivas.
     */
    Bundle downloadPackage(String canonicalUrl, String version);

    /**
     * 4. Despliegue de un paquete en el Motor de Procesos (Camunda/Kogito/Flowable).
     * Descarga el paquete desde el repositorio CRMI, extrae el modelo BPMN/DMN 
     * y lo despliega mediante la API del motor de ejecución.
     */
    DeploymentResult deployPackageToEngine(String canonicalUrl, String version, String targetEngineUrl);
}
