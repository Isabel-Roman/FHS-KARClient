package us.dit.crmiclient.transformation;
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
}
