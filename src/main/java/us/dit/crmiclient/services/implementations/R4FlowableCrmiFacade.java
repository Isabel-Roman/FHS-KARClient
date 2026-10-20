package us.dit.crmiclient.services.implementations;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

import us.dit.crmiclient.model.dtos.DeploymentResult;
import us.dit.crmiclient.model.dtos.PlanSearchFilter;
import us.dit.crmiclient.model.dtos.PlanSummary;

import us.dit.crmiclient.services.CrmiFacade;
import us.dit.crmiclient.transformation.implementations.R4Plan2FlowablePlanTransformer;

@Service
public class R4FlowableCrmiFacade implements CrmiFacade{
    private static final Logger logger = LoggerFactory.getLogger(R4FlowableCrmiFacade.class);

    private static final String LIBRARY_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/library-type";
    private static final String BPMN_CONTENT_TYPE = "application/bpmn+xml";
    /** Versión de la Library cuando el plan no tiene versión */
    private static final String FIRST_VERSION = "1.0.0";

    /** Solo para leer el JSON recibido; las peticiones al servidor las hace el resolver */
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Todo el acceso al servidor CRMI: resuelve los recursos de las acciones (para la
     * transformación) y publica la Library y el PlanDefinition.
     */
    private final R4ResourceResolver resolver;

    public R4FlowableCrmiFacade(R4ResourceResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void publishPlan(String rawPlanDefinition) {
        // El transformer resuelve contra el servidor los PlanDefinition de las acciones
        R4Plan2FlowablePlanTransformer transformer = new R4Plan2FlowablePlanTransformer(resolver);
        String bpmnModel = transformer.Plan2Bpmn(rawPlanDefinition);
        logger.debug("Modelo BPMN generado:\n{}", bpmnModel);

        PlanDefinition plan = fhirContext.newJsonParser().parseResource(PlanDefinition.class, rawPlanDefinition);
        logger.debug("PlanDefinition leído: id={}, name={}, version={}", plan.getId(), plan.getName(), plan.getVersion());
        // El id de la Library lo elegimos aquí para poder poner su url canónica antes de publicarla
        String libraryId = UUID.randomUUID().toString();
        Library library = createLibrary(plan, bpmnModel, libraryId);
        logger.debug("Library creada: url={}, version={}", library.getUrl(), library.getVersion());
        resolver.publish(library, libraryId);
        // PlanDefinition.library es canónica: se referencia con versión para ligar el plan a esta Library
        String libraryCanonical = library.getUrl() + "|" + library.getVersion();
        plan.addLibrary(libraryCanonical);
        String planUrl = resolver.publish(plan);
        logger.debug("PlanDefinition publicado en {} con la Library {}", planUrl, libraryCanonical);
    }

    /**
     * Crea una Library que contiene el modelo BPMN generado a partir del PlanDefinition.
     * Los metadatos (versión, nombre...) se toman del propio PlanDefinition.
     */
    private Library createLibrary(PlanDefinition plan, String bpmnModel, String libraryId) {
        // Un plan nuevo puede no traer id: se usa el name, como en R4PlanReader.getId()
        String id = plan.getIdElement().hasIdPart() ? plan.getIdElement().getIdPart()
                : plan.hasName() ? plan.getName() : "plan";
        logger.debug("Creando Library para el plan {}", id);

        Library library = new Library();
        // Url canónica = url de la Library en el servidor donde se publica, para que el plan
        // (y quien lo use como subproceso) la encuentre por canónica
        library.setUrl(resolver.getServerUrl().replaceAll("/+$", "") + "/Library/" + libraryId);
        // La Library se genera a partir del plan: misma versión, o la primera si el plan no tiene
        library.setVersion(plan.hasVersion() ? plan.getVersion() : FIRST_VERSION);
        library.setName(plan.getName());
        library.setTitle(plan.getTitle());
        library.setDescription(plan.getDescription());
        library.setPublisher(plan.getPublisher());
        library.setStatus(PublicationStatus.DRAFT);
        library.setType(new CodeableConcept().addCoding(
                new Coding(LIBRARY_TYPE_SYSTEM, "module-definition", "Module Definition")));

        // setData recibe los bytes sin codificar: HAPI los serializa en base64
        library.addContent(new Attachment()
                .setContentType(BPMN_CONTENT_TYPE)
                .setTitle(id + ".bpmn20.xml")
                .setData(bpmnModel.getBytes(StandardCharsets.UTF_8)));
        return library;
    }

    @Override
    public List<PlanSummary> searchPlan(PlanSearchFilter filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchPlan'");
    }

    @Override
    public Bundle downloadPackage(String canonicalUrl, String version) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'downloadPackage'");
    }

    @Override
    public DeploymentResult deployPackageToEngine(String canonicalUrl, String version, String targetEngineUrl) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deployPackageToEngine'");
    }

}
