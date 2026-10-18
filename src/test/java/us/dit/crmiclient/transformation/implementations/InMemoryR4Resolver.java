package us.dit.crmiclient.transformation.implementations;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;

import us.dit.crmiclient.transformation.ResourceResolver;

import org.hl7.fhir.r4.model.Resource;

/**
 * Servidor CRMI simulado para los tests: guarda los recursos en memoria, por
 * url canónica
 * (PlanDefinition, Library) o por url de referencia (Endpoint).
 */
class InMemoryR4Resolver implements ResourceResolver {

    private final Map<String, IBaseResource> resources = new HashMap<>();

    private <T extends IBaseResource> T find(String url, Class<T> type) {
        IBaseResource resource = resources.get(url);
        return type.isInstance(resource) ? type.cast(resource) : null;
    }

    InMemoryR4Resolver add(String url, Resource resource) {
        resources.put(url, resource);
        return this;
    }

    /**
     * Da de alta un subproceso desplegado: su PlanDefinition, una Library por cada
     * motor con un
     * BPMN cuyo process tiene la clave indicada, y el Endpoint de cada motor.
     *
     * @param engines pares tipo de motor / url del motor
     */
    InMemoryR4Resolver addSubprocess(String planCanonical, String processKey, String... engines) {
        PlanDefinition plan = new PlanDefinition();
        for (int i = 0; i < engines.length; i += 2) {
            String engineType = engines[i];
            String engineUrl = engines[i + 1];
            String libraryCanonical = planCanonical + "-library-" + engineType;
            String endpointUrl = "http://crmi.ejemplo/fhir/Endpoint/" + engineType;
            plan.addLibrary(libraryCanonical);
            add(libraryCanonical, library(libraryCanonical, processKey, endpointUrl));
            add(endpointUrl, endpoint(engineType, engineUrl));
        }
        return add(planCanonical, plan);
    }

    static Library library(String canonical, String processKey, String endpointUrl) {
        Library library = new Library();
        library.setUrl(canonical);
        library.addContent(new Attachment().setContentType("application/xml").setData(bpmn(processKey)));
        if (endpointUrl != null) {
            library.addRelatedArtifact(new RelatedArtifact()
                    .setType(RelatedArtifactType.DEPENDSON)
                    .setLabel("process-engine")
                    .setUrl(endpointUrl));
        }
        return library;
    }

    static Endpoint endpoint(String engineType, String engineUrl) {
        Endpoint endpoint = new Endpoint();
        endpoint.setConnectionType(new Coding().setCode(engineType));
        endpoint.setAddress(engineUrl);
        return endpoint;
    }

    static byte[] bpmn(String processKey) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="test">
                  <process id="%s" isExecutable="true"/>
                </definitions>
                """.formatted(processKey).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T extends IBaseResource> T resolveCanonical(String canonical, Class<T> type) {
        return find(canonical, type);
    }

    @Override
    public <T extends IBaseResource> T resolveReference(String url, Class<T> type) {
        return find(url, type);
    }
}
