package us.dit.crmiclient.transformation;

import us.dit.crmiclient.model.SimpleGateway;

public interface PlanReader {
  
    String getName();
    String getId();
    SimpleGateway getStartGateway(); 
    SimpleGateway getEndGateway();
    String getDocumentation(); // Nuevo método para obtener la documentación del plan
}
