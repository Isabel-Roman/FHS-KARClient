package us.dit.crmiclient.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;

/**
 * Test de integración: arranca la aplicación completa, con la fachada R4FlowableCrmiFacade real,
 * y publica contra el servidor FHIR. Tras cada test se borran del servidor los recursos creados.
 */
@Tag("integration")
@SpringBootTest(properties = "crmiserver.url=" + CrmiControllerIntegrationTest.SERVER_URL)
@AutoConfigureMockMvc
class CrmiControllerIntegrationTest {

    static final String SERVER_URL = "https://hapi.fhir.org/baseR4";

    @Autowired
    private MockMvc mockMvc;

    private final FhirContext fhirContext = FhirContext.forR4();
    private IGenericClient client;
    // Nombre único por test para poder localizar (y borrar) los recursos creados en el servidor
    private String planName;

    @BeforeEach
    void setUp() {
        client = fhirContext.newRestfulGenericClient(SERVER_URL);
        planName = "PlanControllerIT" + System.currentTimeMillis();
    }

    @AfterEach
    void cleanUp() {
        // Primero el PlanDefinition, que referencia a la Library, para no violar la integridad referencial
        deleteByName("PlanDefinition", PlanDefinition.NAME);
        deleteByName("Library", Library.NAME);
    }

    private Bundle searchByName(String resourceType, StringClientParam nameParam) {
        return client.search()
                .forResource(resourceType)
                .where(nameParam.matchesExactly().value(planName))
                .returnBundle(Bundle.class)
                .execute();
    }

    private void deleteByName(String resourceType, StringClientParam nameParam) {
        for (Bundle.BundleEntryComponent entry : searchByName(resourceType, nameParam).getEntry()) {
            client.delete().resourceById(entry.getResource().getIdElement().toUnqualifiedVersionless()).execute();
            System.out.println("Borrado " + entry.getResource().getIdElement().toUnqualifiedVersionless().getValue());
        }
    }

    @Test
    void postPlanDefinitionPublishesPlanAndLibraryInServer() throws Exception {
        String rawPlan = """
                {
                  "resourceType": "PlanDefinition",
                  "name": "%s",
                  "status": "draft",
                  "description": "Plan publicado a través del controlador REST en un test de integración."
                }
                """.formatted(planName);

        mockMvc.perform(post("/planDefinition").contentType(MediaType.APPLICATION_JSON).content(rawPlan))
                .andExpect(status().isCreated());

        // La Library existe en el servidor y contiene el BPMN
        Bundle libraries = searchByName("Library", Library.NAME);
        assertEquals(1, libraries.getEntry().size());
        Library library = (Library) libraries.getEntryFirstRep().getResource();
        String bpmn = new String(library.getContentFirstRep().getData(), StandardCharsets.UTF_8);
        assertTrue(bpmn.contains("<definitions"));

        // El PlanDefinition existe en el servidor y referencia a esa Library
        Bundle plans = searchByName("PlanDefinition", PlanDefinition.NAME);
        assertEquals(1, plans.getEntry().size());
        PlanDefinition plan = (PlanDefinition) plans.getEntryFirstRep().getResource();
        assertEquals(1, plan.getLibrary().size());
        String libraryRef = plan.getLibrary().get(0).getValue();
        assertTrue(libraryRef.endsWith("/Library/" + library.getIdElement().getIdPart()),
                "PlanDefinition.library (" + libraryRef + ") no apunta a la Library creada");
    }
}
