package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.services.implementations.R4ResourceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.junit.jupiter.api.Test;

import us.dit.crmiclient.services.ResourceResolver;

class R4Plan2FlowablePlanTransformerTest {

    @Test
    void simplePlan2Bpmn() throws Exception {

        String SIMPLE_PLAN = """
               {
  "resourceType": "PlanDefinition",
  "id": "plan-sencillo",
  "name": "PlanSencillo",
  "status": "draft",
  "purpose": "Este es un plan sencillo para pruebas.",
  "description": "Este plan tiene solo dos acciones paralelas.",
  "usage": "Usar solo en entorno de tests.",
  "action": [
    {
      "id": "a",
      "definitionCanonical": "https://hapi.fhir.org/baseR4/PlanDefinition/135587",
      "dynamicValue": [
        {
          "path": "inputStringParamName",
          "expression": {
            "language": "text/fhirpath",
            "expression": "'PARAM-TESTa'"
          }
        }
      ]
    },
    {
      "id": "b",
      "definitionCanonical": "https://hapi.fhir.org/baseR4/PlanDefinition/135583",
      "dynamicValue": [
        {
          "path": "inputStringParamName",
          "expression": {
            "language": "text/fhirpath",
            "expression": "'PARAM-TESTb'"
          }
        }
      ]
    }
  ]
}
                """;
        ResourceResolver resolver = new R4ResourceResolver("https://hapi.fhir.org/baseR4");
        String xml = new R4Plan2FlowablePlanTransformer(resolver).Plan2Bpmn(SIMPLE_PLAN);
        System.out.println(xml); // Imprime el XML generado para depuración

        assertNotNull(xml);
        BpmnModel model = parse(xml);
        assertEquals(1, model.getProcesses().size());
        Process process = model.getMainProcess();
        assertEquals("PlanSencillo", process.getName());
        assertEquals("plan-sencillo", process.getId());
    }

    private BpmnModel parse(String xml) throws Exception {
        XMLStreamReader reader = XMLInputFactory.newInstance()
                .createXMLStreamReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return new BpmnXMLConverter().convertToBpmnModel(reader);
    }
}
