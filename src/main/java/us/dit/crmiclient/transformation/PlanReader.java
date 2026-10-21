package us.dit.crmiclient.transformation;

import us.dit.crmiclient.model.SimpleGateway;
import us.dit.crmiclient.model.ActionWrapper;
import java.util.List;

public interface PlanReader {
  
    String getName();
    String getId();
    String getDocumentation(); // Nuevo método para obtener la documentación del plan
    SimpleGateway getStartGateway(); 
    SimpleGateway getEndGateway();
    List<ActionWrapper> getFirstsActions();
    List<ActionWrapper> getLastsActions();

}
