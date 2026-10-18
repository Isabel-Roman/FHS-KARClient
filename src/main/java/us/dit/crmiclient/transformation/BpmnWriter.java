package us.dit.crmiclient.transformation;

import us.dit.crmiclient.model.SimpleGateway;

/**
 * Interfaz que permite escribir un modelo en formato XML
 */
public interface BpmnWriter {
    /**
     * Devuelve el modelo en formato XML
     * @return String con el modelo en formato XML
     */
    String getModel();
    void setName(String name);
    void setId(String id);
    void setStartEvent(SimpleGateway startGateway);
    void setEndEvent(SimpleGateway endGateway);
    void setDocumentation(String documentation); // Nuevo método para establecer la documentación del proceso

}
