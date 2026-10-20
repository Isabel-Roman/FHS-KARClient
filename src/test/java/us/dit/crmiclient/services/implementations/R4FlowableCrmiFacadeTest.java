package us.dit.crmiclient.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;

/**
 * Test de integración: publica contra el servidor CRMI real.
 * Tras cada test se borran del servidor los recursos creados.
 */
@Tag("integration")
class R4FlowableCrmiFacadeTest {

    private static final String SERVER_URL = "https://hapi.fhir.org/baseR4";
    //private static final String SERVER_URL = "https://ait01.us.es/hapi/fhir";


    private IGenericClient client;
    // Nombre único por test para poder localizar (y borrar) los recursos creados en el servidor
    private String planName;

    @BeforeEach
    void setUp() {
        client = FhirContext.forR4().newRestfulGenericClient(SERVER_URL);
        planName = "PlanSencilloTest" + System.currentTimeMillis();
    }

    @AfterEach
    void cleanUp() {
        // Primero el PlanDefinition, que referencia a la Library, para no violar la integridad referencial
        deleteByName("PlanDefinition", PlanDefinition.NAME);
        deleteByName("Library", Library.NAME);
    }

    private void deleteByName(String resourceType, StringClientParam nameParam) {
        Bundle found = client.search()
                .forResource(resourceType)
                .where(nameParam.matchesExactly().value(planName))
                .returnBundle(Bundle.class)
                .execute();
        for (Bundle.BundleEntryComponent entry : found.getEntry()) {
            client.delete().resourceById(entry.getResource().getIdElement().toUnqualifiedVersionless()).execute();
            System.out.println("Borrado " + entry.getResource().getIdElement().toUnqualifiedVersionless().getValue());
        }
    }

    @Test
    void publishPlanCreatesLibraryWithUrlInServer() {
        String rawPlan = """
                {
                            "resourceType": "PlanDefinition",
                            "id": "plan-sencillo",
                            "name": "%s",
                            "status": "draft",
                            "purpose": "Este es un plan sencillo para pruebas.",
                            "description": "Este plan tiene solo evento de inicio y de fin.",
                            "usage": "Usar solo en entorno de tests.",
                            "action": [{"id": "a","definitionCanonical": "https://hapi.fhir.org/baseR4/PlanDefinition/82074"}]
                                        }
                              """.formatted(planName);

        new R4FlowableCrmiFacade(new R4ResourceResolver(SERVER_URL)).publishPlan(rawPlan);

        Bundle result = client.search()
                .forResource(Library.class)
                .where(Library.NAME.matchesExactly().value(planName))
                .returnBundle(Bundle.class)
                .execute();

        assertEquals(1, result.getEntry().size());
        Library library = (Library) result.getEntryFirstRep().getResource();

        String bpmn = new String(library.getContentFirstRep().getData(), StandardCharsets.UTF_8);
        assertTrue(bpmn.contains("<definitions"));

        // La url canónica es la url de la Library en el servidor; el plan no tiene versión: 1.0.0
        String libraryUrl = SERVER_URL + "/Library/" + library.getIdElement().getIdPart();
        assertEquals(libraryUrl, library.getUrl());
        assertEquals("1.0.0", library.getVersion());

        // El plan referencia la Library por canónica con versión
        Bundle plans = client.search()
                .forResource(PlanDefinition.class)
                .where(PlanDefinition.NAME.matchesExactly().value(planName))
                .returnBundle(Bundle.class)
                .execute();
        PlanDefinition plan = (PlanDefinition) plans.getEntryFirstRep().getResource();
        assertEquals(libraryUrl + "|1.0.0", plan.getLibrary().get(0).getValue());
    }
}
