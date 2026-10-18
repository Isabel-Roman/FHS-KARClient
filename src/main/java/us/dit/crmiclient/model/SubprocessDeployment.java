package us.dit.crmiclient.model;

/**
 * Dónde y cómo arrancar el proceso que implementa una acción, en un motor concreto.
 * No depende de FHIR: es lo que necesita el writer para escribir la llamada al subproceso.
 *
 * @param processKey clave del proceso en el motor (id del elemento process de su BPMN)
 * @param engineUrl  url del motor en el que está desplegado
 */
public record SubprocessDeployment(String processKey, String engineUrl) {
}
