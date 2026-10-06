package us.dit.crmiclient.services;

import us.dit.crmiclient.model.dtos.*;

import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

import us.dit.crmiclient.model.dtos.PlanSearchFilter;

public interface CrmiClient {

    /**
     * 1. Publicación de un artefacto BPM+/CRMI.
     * Toma el diagrama (BPMN/DMN/CMMN) y sus metadatos, genera la Library y el PlanDefinition
     * cumpliendo el perfil crmi-publishable y los persiste en el repositorio.
     */
    PublishPlanResponse publishWorkflow(PublishPlanRequest request);

    /**
     * 2. Localización / Búsqueda de flujos en el catálogo.
     * Permite consultar el catálogo de procesos filtrando por título, estado, temática o editor.
     */
    List<PlanSummary> searchWorkflows(PlanSearchFilter filter);

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
