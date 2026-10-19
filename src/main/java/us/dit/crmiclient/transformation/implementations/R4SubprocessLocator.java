package us.dit.crmiclient.transformation.implementations;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.crmiclient.model.SubprocessDeployment;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;
import us.dit.crmiclient.services.ResourceResolver;

/**
 * Localiza, a partir del PlanDefinition de un subproceso, su despliegue en un tipo de motor.
 * <p>
 * Los datos de ejecución no están en el PlanDefinition, sino en su despliegue en un motor:
 * <ol>
 * <li>PlanDefinition.library → una Library por tipo de motor, con el BPMN para ese motor.</li>
 * <li>Library.relatedArtifact de tipo depends-on → url del Endpoint del motor donde está
 * desplegada (lo escribe quien despliega la Library, no este proyecto).</li>
 * <li>Endpoint.connectionType → tipo de motor; Endpoint.address → url del motor.</li>
 * </ol>
 * Se usa la Library cuyo Endpoint es del tipo {@code engineType}; la clave del proceso se lee
 * del BPMN que contiene. Es específico de FHIR R4 y depende del motor de destino, por eso no lo
 * hace el reader sino el transformer de cada motor.
 */
public class R4SubprocessLocator {
    private static final Logger logger = LoggerFactory.getLogger(R4SubprocessLocator.class);

    /** Espacio de nombres de BPMN 2.0, en el que está el elemento process del modelo */
    private static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    /** Obtiene del servidor CRMI los PlanDefinition, Library y Endpoint */
    private final ResourceResolver resolver;
    /** Tipo de motor buscado: código de Endpoint.connectionType (p.ej. "flowable") */
    private final String engineType;

    public R4SubprocessLocator(ResourceResolver resolver, String engineType) {
        this.resolver = resolver;
        this.engineType = engineType;
    }

    /**
     * @param planCanonical canónica del PlanDefinition del subproceso
     * @return clave y url del motor del subproceso desplegado en un motor de tipo engineType
     * @throws InvalidPlanDefinitionException si el subproceso no existe o no está desplegado en
     *                                        exactamente un motor de ese tipo
     */
    public SubprocessDeployment locate(String planCanonical) {
        PlanDefinition definition = resolver.resolveCanonical(planCanonical, PlanDefinition.class);
        if (definition == null) {
            throw new InvalidPlanDefinitionException("No se encuentra el PlanDefinition " + planCanonical);
        }
        Deployment deployment = findDeployment(definition, planCanonical);
        SubprocessDeployment result = new SubprocessDeployment(
                readProcessKey(deployment.library(), planCanonical), deployment.endpoint().getAddress());
        logger.debug("El subproceso {} es el proceso {} en {}", planCanonical, result.processKey(), result.engineUrl());
        return result;
    }

    /** Library de un subproceso junto con el Endpoint del motor en el que está desplegada */
    private record Deployment(Library library, Endpoint endpoint) {
    }

    /**
     * Busca entre las Library del PlanDefinition la desplegada en un motor del tipo
     * {@code engineType}. Por ahora debe haber exactamente una.
     */
    private Deployment findDeployment(PlanDefinition definition, String canonical) {
        List<Deployment> found = new ArrayList<>();
        for (CanonicalType libraryCanonical : definition.getLibrary()) {
            Library library = resolver.resolveCanonical(libraryCanonical.getValue(), Library.class);
            if (library == null) {
                logger.warn("No se encuentra la Library {} del PlanDefinition {}", libraryCanonical.getValue(), canonical);
                continue;
            }
            Endpoint endpoint = findEndpoint(library);
            if (endpoint != null && endpoint.hasConnectionType()
                    && engineType.equals(endpoint.getConnectionType().getCode())) {
                found.add(new Deployment(library, endpoint));
            }
        }
        if (found.isEmpty()) {
            throw new InvalidPlanDefinitionException("El PlanDefinition " + canonical
                    + " no tiene ninguna Library desplegada en un motor " + engineType);
        }
        if (found.size() > 1) {
            throw new InvalidPlanDefinitionException("El PlanDefinition " + canonical + " tiene " + found.size()
                    + " Library desplegadas en motores " + engineType + ", solo se admite una");
        }
        return found.get(0);
    }

    /**
     * Endpoint en el que está desplegada una Library: el referenciado por un relatedArtifact de
     * tipo depends-on (convención del proyecto). Null si la Library aún no está desplegada.
     */
    private Endpoint findEndpoint(Library library) {
        for (RelatedArtifact artifact : library.getRelatedArtifact()) {
            if (artifact.getType() == RelatedArtifactType.DEPENDSON && artifact.hasUrl()) {
                Endpoint endpoint = resolver.resolveReference(artifact.getUrl(), Endpoint.class);
                if (endpoint != null) {
                    return endpoint;
                }
            }
        }
        return null;
    }

    /**
     * Clave del proceso (id del elemento process) del BPMN contenido en la Library. Se lee como
     * XML BPMN estándar, sin depender de ningún motor.
     */
    private static String readProcessKey(Library library, String planCanonical) {
        for (Attachment content : library.getContent()) {
            if (!content.hasData()) {
                continue;
            }
            try {
                XMLInputFactory factory = XMLInputFactory.newFactory();
                // El BPMN viene de fuera: sin DTD ni entidades externas (XXE)
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(content.getData()));
                try {
                    while (reader.hasNext()) {
                        if (reader.next() == XMLStreamConstants.START_ELEMENT
                                && "process".equals(reader.getLocalName())
                                && BPMN_NAMESPACE.equals(reader.getNamespaceURI())) {
                            String key = reader.getAttributeValue(null, "id");
                            if (key != null && !key.isBlank()) {
                                return key;
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            } catch (XMLStreamException e) {
                throw new InvalidPlanDefinitionException("El BPMN de la Library " + library.getUrl()
                        + " del subproceso " + planCanonical + " no es XML válido: " + e.getMessage());
            }
        }
        throw new InvalidPlanDefinitionException("La Library " + library.getUrl() + " del subproceso "
                + planCanonical + " no contiene un BPMN con un elemento process con id");
    }
}
