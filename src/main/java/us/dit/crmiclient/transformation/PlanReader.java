package us.dit.crmiclient.transformation;
public interface PlanReader {
  
    String getName();
    String getId();
    String getStartEvent(); 
    String getEndEvent();
    String getDocumentation(); // Nuevo método para obtener la documentación del plan
}
