package us.dit.crmiclient.services;

import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Obtiene recursos FHIR del servidor CRMI. Separa al reader del acceso al servidor: el reader
 * no hace peticiones, quien lo crea decide de dónde se sacan los recursos.
 */
public interface ResourceResolver {

   

    /**
     * Recurso de conocimiento (PlanDefinition, Library...) por su url canónica.
     * El recurso buscado tiene en el campo url la canonica proporcionada al método
     *
     * @param canonical url canónica, puede incluir versión ("url|version") (valor del campo url del recurso buscado)
     * @return el recurso, o null si no se encuentra
     */
    <T extends IBaseResource> T resolveCanonical(String canonical, Class<T> type);

    /**
     * Recurso sin url canónica (Endpoint...) por la url de su referencia literal,
     * p.ej. "https://servidor/fhir/Endpoint/123".
     *
     * @return el recurso, o null si no se encuentra
     */
    <T extends IBaseResource> T resolveReference(String url, Class<T> type);
    String getServerUrl();
    String publish(IBaseResource resource);
}
