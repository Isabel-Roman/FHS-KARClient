package us.dit.crmiclient.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ca.uhn.fhir.parser.DataFormatException;
import us.dit.crmiclient.services.CrmiFacade;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;

@WebMvcTest(CrmiController.class)
class CrmiControllerTest {

    private static final String PLAN = """
            {"resourceType": "PlanDefinition", "name": "PlanSencillo", "status": "draft"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrmiFacade crmiFacade;

    @Test
    void postPlanDefinitionCallsFacadeAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/planDefinition").contentType(MediaType.APPLICATION_JSON).content(PLAN))
                .andExpect(status().isCreated());

        verify(crmiFacade).publishPlan(PLAN);
    }

    @Test
    void postInvalidPlanDefinitionReturnsBadRequest() throws Exception {
        doThrow(new DataFormatException("JSON no válido")).when(crmiFacade).publishPlan(anyString());

        mockMvc.perform(post("/planDefinition").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postUnsupportedPlanDefinitionReturnsBadRequestWithMessage() throws Exception {
        doThrow(new InvalidPlanDefinitionException("acciones anidadas")).when(crmiFacade).publishPlan(anyString());

        mockMvc.perform(post("/planDefinition").contentType(MediaType.APPLICATION_JSON).content(PLAN))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("acciones anidadas"));
    }
}
