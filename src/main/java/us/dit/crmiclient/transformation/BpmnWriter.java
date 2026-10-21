package us.dit.crmiclient.transformation;

import us.dit.crmiclient.model.Flow;
import us.dit.crmiclient.model.ProcessNode;

/**
 * Interfaz que permite escribir un modelo en formato XML.
 * <p>
 * El proceso se construye añadiendo sus nodos y sus flujos, tal como los da el
 * {@link PlanReader} (getNodes y getFlows). Los nodos deben añadirse antes que los flujos que los
 * unen.
 */
public interface BpmnWriter {
    /**
     * Devuelve el modelo en formato XML
     * @return String con el modelo en formato XML
     */
    String getModel();
    void setName(String name);
    void setId(String id);
    void setDocumentation(String documentation); // Nuevo método para establecer la documentación del proceso

    /**
     * Añade un nodo al proceso: el evento de inicio o fin, un gateway o una acción. Del nodo se
     * usan solo los datos independientes de la versión de FHIR.
     */
    void addNode(ProcessNode node);

    /** Añade un flujo (sequence flow) entre dos nodos ya añadidos */
    void addFlow(Flow flow);
}
