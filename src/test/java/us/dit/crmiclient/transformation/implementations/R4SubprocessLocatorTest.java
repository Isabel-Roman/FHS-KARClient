package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.Test;

import us.dit.crmiclient.model.SubprocessDeployment;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;

class R4SubprocessLocatorTest {

    private static final String ENGINE = "flowable";
    private static final String FLOWABLE_URL = "https://flowable.ejemplo/flowable-rest/service";
    private static final String SUBPLAN = "http://ejemplo/PlanDefinition/notificar";

    @Test
    void urlAndKeyComeFromLibraryDeployedInEngine() {
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().addSubprocess(SUBPLAN, "notificar", ENGINE, FLOWABLE_URL);
        SubprocessDeployment deployment = new R4SubprocessLocator(crmi, ENGINE).locate(SUBPLAN);
        assertEquals(FLOWABLE_URL, deployment.engineUrl());
        assertEquals("notificar", deployment.processKey());
    }

    @Test
    void usesTheLibraryOfTheTargetEngine() {
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().addSubprocess(SUBPLAN, "notificar",
                "camunda", "https://camunda.ejemplo/engine-rest", ENGINE, FLOWABLE_URL);
        assertEquals(FLOWABLE_URL, new R4SubprocessLocator(crmi, ENGINE).locate(SUBPLAN).engineUrl());
    }

    @Test
    void rejectsPlanDefinitionNotFound() {
        R4SubprocessLocator locator = new R4SubprocessLocator(new InMemoryR4Resolver(), ENGINE);
        assertThrows(InvalidPlanDefinitionException.class, () -> locator.locate(SUBPLAN));
    }

    @Test
    void rejectsSubprocessNotDeployedInTargetEngine() {
        InMemoryR4Resolver crmi = new InMemoryR4Resolver()
                .addSubprocess(SUBPLAN, "notificar", "camunda", "https://camunda.ejemplo/engine-rest");
        R4SubprocessLocator locator = new R4SubprocessLocator(crmi, ENGINE);
        assertThrows(InvalidPlanDefinitionException.class, () -> locator.locate(SUBPLAN));
    }

    @Test
    void rejectsLibraryWithoutEndpoint() {
        // La Library existe pero aún no está desplegada: no tiene relatedArtifact al Endpoint
        String libraryCanonical = SUBPLAN + "-library";
        PlanDefinition subplan = new PlanDefinition();
        subplan.addLibrary(libraryCanonical);
        InMemoryR4Resolver crmi = new InMemoryR4Resolver()
                .add(SUBPLAN, subplan)
                .add(libraryCanonical, InMemoryR4Resolver.library(libraryCanonical, "notificar", null));
        R4SubprocessLocator locator = new R4SubprocessLocator(crmi, ENGINE);
        assertThrows(InvalidPlanDefinitionException.class, () -> locator.locate(SUBPLAN));
    }

    @Test
    void rejectsTwoLibrariesForTheSameEngine() {
        InMemoryR4Resolver crmi = new InMemoryR4Resolver().addSubprocess(SUBPLAN, "notificar", ENGINE, FLOWABLE_URL);
        PlanDefinition subplan = crmi.resolveCanonical(SUBPLAN, PlanDefinition.class);
        String secondLibrary = SUBPLAN + "-library-flowable-2";
        subplan.addLibrary(secondLibrary);
        crmi.add(secondLibrary, InMemoryR4Resolver.library(secondLibrary, "notificar",
                "http://crmi.ejemplo/fhir/Endpoint/" + ENGINE));
        R4SubprocessLocator locator = new R4SubprocessLocator(crmi, ENGINE);
        assertThrows(InvalidPlanDefinitionException.class, () -> locator.locate(SUBPLAN));
    }
}
