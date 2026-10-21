package us.dit.crmiclient.transformation;

import us.dit.crmiclient.model.Flow;
import us.dit.crmiclient.model.ProcessNode;
import java.util.List;

/**
 * Lee un plan y lo ofrece como un proceso: sus datos generales y su grafo de nodos (eventos,
 * acciones y gateways) y flujos, que se recorre con los métodos de abajo sin conocer cómo está
 * implementado.
 */
public interface PlanReader {

    String getName();
    String getId();
    String getDocumentation(); // Nuevo método para obtener la documentación del plan

    /**
     * Nodos del proceso (eventos de inicio y fin, acciones y gateways) en orden topológico:
     * cada nodo aparece después de todos sus anteriores. El primero es el evento de inicio y el
     * último el de fin.
     */
    List<ProcessNode> getNodes();

    /** Flujos (sequence flows) del proceso, agrupados por su origen en orden topológico */
    List<Flow> getFlows();

    /**
     * Nodos a los que se llega directamente desde el dado
     *
     * @throws IllegalArgumentException si el nodo no es del proceso
     */
    List<ProcessNode> getSuccessors(ProcessNode node);

    /**
     * Nodos desde los que se llega directamente al dado
     *
     * @throws IllegalArgumentException si el nodo no es del proceso
     */
    List<ProcessNode> getPredecessors(ProcessNode node);

    /** Nodo del proceso con el id BPMN dado, o null si no hay ninguno */
    ProcessNode getNode(String id);
}
