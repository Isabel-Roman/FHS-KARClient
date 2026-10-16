package us.dit.crmiclient.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Test de integración: publica contra el servidor CRMI real.
 */
@Tag("integration")
class R4FlowableCrmiFacadeTest {

    private static final String SERVER_URL = "https://hapi.fhir.org/baseR4";

    @Test
    void publishPlanCreatesLibraryWithUrlInServer() {
        // Nombre único para poder localizar la Library creada en el servidor
        String planName = "PlanSencilloTest" + System.currentTimeMillis();
        String rawPlan = """
                {
                  "resourceType": "PlanDefinition",
                  "id": "plansencillo",
                  "name": "%s",
                  "status": "draft"
                }
                """.formatted(planName);

        new R4FlowableCrmiFacade(SERVER_URL).publishPlan(rawPlan);

        IGenericClient client = FhirContext.forR4().newRestfulGenericClient(SERVER_URL);
        Bundle result = client.search()
                .forResource(Library.class)
                .where(Library.NAME.matchesExactly().value(planName))
                .returnBundle(Bundle.class)
                .execute();

        assertEquals(1, result.getEntry().size());
        Library library = (Library) result.getEntryFirstRep().getResource();
        System.out.println("Library creada: " + library.toString());
       
        String bpmn = new String(library.getContentFirstRep().getData(), StandardCharsets.UTF_8);
        assertTrue(bpmn.contains("<definitions"));
    }
}
