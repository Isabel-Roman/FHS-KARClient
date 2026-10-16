package us.dit.crmiclient.transformation.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class R4PlanReaderTest {

    @Test
    void getIdUsesPlanIdWithoutPrefix() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "id": "plan-sencillo", "name": "PlanSencillo"}
                """);
        assertEquals("plan-sencillo", reader.getId());
    }

    @Test
    void getIdUsesNameWhenPlanHasNoId() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "PlanSencillo"}
                """);
        assertEquals("PlanSencillo", reader.getId());
    }

    @Test
    void getIdTurnsNameIntoValidBpmnId() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "name": "1 plan con espacios"}
                """);
        assertEquals("_1_plan_con_espacios", reader.getId());
    }

    @Test
    void getIdGeneratesStableIdWhenPlanHasNoIdNorName() {
        R4PlanReader reader = new R4PlanReader("""
                {"resourceType": "PlanDefinition", "status": "draft"}
                """);
        String id = reader.getId();
        assertTrue(id.startsWith("plan-"));
        assertEquals(id, reader.getId());
    }
}
