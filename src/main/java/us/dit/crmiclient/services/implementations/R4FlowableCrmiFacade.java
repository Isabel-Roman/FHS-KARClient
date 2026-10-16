package us.dit.crmiclient.services.implementations;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
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

    private final FhirContext fhirContext = FhirContext.forR4();
    private final IGenericClient fhirClient;

    public R4FlowableCrmiFacade(@Value("${crmiserver.url}") String serverUrl) {
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
    public void publishPlan(String rawPlanDefinition) {
        R4Plan2FlowablePlanTransformer transformer = new R4Plan2FlowablePlanTransformer();
        String bpmnModel = transformer.Plan2Bpmn(rawPlanDefinition);
        logger.debug("Modelo BPMN generado:\n{}", bpmnModel);
        
        PlanDefinition plan = fhirContext.newJsonParser().parseResource(PlanDefinition.class, rawPlanDefinition);
        logger.debug("PlanDefinition leído: id={}, name={}", plan.getId(), plan.getName());
        Library library = createLibrary(plan, bpmnModel);
        logger.debug("Library creada: name={}, version={}", library.getName(), library.getVersion());
        String libraryCanonical = publishLibrary(library);
        logger.debug("URL canónica de la Library: {}", libraryCanonical);
        plan.addLibrary(libraryCanonical);
        publishPlan(plan);
        // TODO publicar el PlanDefinition
    }
    private void publishPlan(PlanDefinition plan) {
        MethodOutcome outcome = fhirClient.create()
                .resource(plan)
                .execute();
        logger.info("PlanDefinition publicado: {}", outcome.getId().getValue());
    }

    /**
     * Publica la Library en el servidor CRMI (POST Library). El servidor asigna el id
     * y con él se compone la url: {servidor}/Library/{id}.
     * @return url de la Library publicada, para referenciarla desde PlanDefinition.library
     */
    private String publishLibrary(Library library) {
        MethodOutcome outcome = fhirClient.create()
                .resource(library)
                .execute();
        // getIdPart descarta la versión (_history/n) que devuelve el servidor
        String id = outcome.getId().getIdPart();
        String serverBase = fhirClient.getServerBase().replaceAll("/+$", "");
        String libraryUrl = serverBase + "/Library/" + id;
        logger.info("Library publicada: {} con url {}", outcome.getId().getValue(), libraryUrl);
        return libraryUrl;
    }

    /**
     * Crea una Library que contiene el modelo BPMN generado a partir del PlanDefinition.
     * Los metadatos (versión, nombre...) se toman del propio PlanDefinition.
     */
    private Library createLibrary(PlanDefinition plan, String bpmnModel) {
        String id = plan.getIdElement().getIdPart();
        logger.debug("Creando Library para el plan {}", id);

        Library library = new Library();
        library.setVersion(plan.getVersion());
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
