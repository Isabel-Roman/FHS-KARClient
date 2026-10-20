package us.dit.crmiclient.services.implementations;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.gclient.UriClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

import us.dit.crmiclient.services.ResourceResolver;

/**
 * Acceso al servidor CRMI (FHIR R4): concentra todas las peticiones al servidor remoto, tanto
 * las de lectura (las del interfaz {@link ResourceResolver}, que usa la transformación) como las
 * de publicación (que usa la fachada).
 */
@Service
public class R4ResourceResolver implements ResourceResolver {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(R4ResourceResolver.class);

    private static final UriClientParam URL = new UriClientParam("url");
    private static final TokenClientParam VERSION = new TokenClientParam("version");

    private final FhirContext fhirContext = FhirContext.forR4();
    private final IGenericClient fhirClient;
    private final String serverUrl;

    public R4ResourceResolver(@Value("${crmiserver.url}") String serverUrl) {
        this.serverUrl = serverUrl;
        // Evita que el cliente consulte /metadata antes de cada primera petición
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        this.fhirClient = fhirContext.newRestfulGenericClient(serverUrl);
        // Traza las peticiones y respuestas HTTP con el servidor CRMI (cabeceras y cuerpo)
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogger(logger);
        loggingInterceptor.setLogRequestSummary(true);
        loggingInterceptor.setLogRequestHeaders(true);
        loggingInterceptor.setLogRequestBody(true);
        loggingInterceptor.setLogResponseSummary(true);
        loggingInterceptor.setLogResponseHeaders(true);
        loggingInterceptor.setLogResponseBody(true);
        this.fhirClient.registerInterceptor(loggingInterceptor);
        logger.debug("Cliente FHIR configurado contra {}", serverUrl);
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Busca por el parámetro url (y version, si la canónica la incluye como "url|version").
     * Sin versión, si hay varias se devuelve la primera que da el servidor.
     */
    @Override
    public <T extends IBaseResource> T resolveCanonical(String canonical, Class<T> type) {
        logger.debug("Resolviendo {} por canónica {}", type.getSimpleName(), canonical);
        int bar = canonical.indexOf('|');
        String url = bar < 0 ? canonical : canonical.substring(0, bar);
        url.trim();
        logger.debug("Buscando {} con url={} y version={}", type.getSimpleName(), url,
                bar < 0 ? "ninguna" : canonical.substring(bar + 1));
        IQuery<Bundle> query = fhirClient.search()
                .forResource(type)
                .where(URL.matches().value(url))
                .returnBundle(Bundle.class);
        if (bar >= 0) {
            query = query.and(VERSION.exactly().code(canonical.substring(bar + 1)));
        }
        Bundle result = query.execute();
        if (result.getEntry().isEmpty()) {
            logger.debug("No se encuentra {} con canónica {}", type.getSimpleName(), canonical);
            return null;
        }
        if (result.getEntry().size() > 1) {
            logger.warn("Hay {} {} con canónica {}, se usa el primero", result.getEntry().size(),
                    type.getSimpleName(), canonical);
        }
        return type.cast(result.getEntryFirstRep().getResource());
    }

    /** Lee el recurso de la url de su referencia literal */
    @Override
    public <T extends IBaseResource> T resolveReference(String url, Class<T> type) {
        try {
            return fhirClient.read().resource(type).withUrl(url).execute();
        } catch (ResourceNotFoundException | ResourceGoneException e) {
            logger.debug("No se encuentra {} en {}", type.getSimpleName(), url);
            return null;
        }
    }

    /**
     * Publica un recurso en el servidor CRMI (POST). El servidor asigna el id.
     *
     * @return url del recurso publicado, {servidor}/{tipo}/{id}, sin versión
     */
    @Override 
    public String publish(IBaseResource resource) {
        MethodOutcome outcome = fhirClient.create()
                .resource(resource)
                .execute();
        // getIdPart descarta la versión (_history/n) que devuelve el servidor
        String serverBase = fhirClient.getServerBase().replaceAll("/+$", "");
        String url = serverBase + "/" + fhirContext.getResourceType(resource) + "/" + outcome.getId().getIdPart();
        logger.info("Publicado {} con url {}", outcome.getId().getValue(), url);
        return url;
    }

    /** PUT con el id dado: si no existe un recurso con ese id, el servidor lo crea */
    @Override
    public String publish(IBaseResource resource, String id) {
        MethodOutcome outcome = fhirClient.update()
                .resource(resource)
                .withId(id)
                .execute();
        String serverBase = fhirClient.getServerBase().replaceAll("/+$", "");
        String url = serverBase + "/" + fhirContext.getResourceType(resource) + "/" + id;
        logger.info("Publicado {} con url {} ({})", outcome.getId().getValue(), url,
                Boolean.TRUE.equals(outcome.getCreated()) ? "creado" : "actualizado");
        return url;
    }
}
