package us.dit.crmiclient.transformation;

import us.dit.crmiclient.model.ActionWrapper;
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
    /**Crea el evento Start y el primer gateway, si hay
     * Devuelve el id del último elemento añadido al modelo, para usarlo para la siguiente conexión
     * 
     * @param startGateway Gateway de inicio, si hay
     * @return id del último elemento añadido al modelo
     */
    String setStartEvent(SimpleGateway startGateway);
    /**Recibe el gateway de fin, si lo hay, y el id del último elemento que se añadió al modelo, dond hay que conectar los elementos que se creen
     * 
     * @param endGateway Gateway de fin, si lo hay
     * @param lastId id del último elemento añadido al modelo, para conectar los elementos que se creen
     */
    void setEndEvent(SimpleGateway endGateway,String lastId);
    void setDocumentation(String documentation); // Nuevo método para establecer la documentación del proceso

    /**
     * Añade una tarea de servicio que ejecuta un trabajador externo (la acción), con sus
     * parámetros de entrada. Del wrapper se usan solo los datos independientes de la versión de
     * FHIR: id, nombre y parámetros de entrada con su valor como texto.
     *
     * @param action acción a añadir
     * @param topic  tema al que se suscriben los trabajadores que ejecutan la tarea
     */
    String setServiceTask(ActionWrapper<?> action, String topic);

}
