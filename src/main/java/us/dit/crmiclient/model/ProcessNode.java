package us.dit.crmiclient.model;

/**
 * Nodo del proceso que se genera: una acción, un gateway o un evento de inicio o fin. Es el tipo
 * de los vértices del grafo del proceso.
 * <p>
 * IDENTIDAD: dos nodos son el mismo si son del mismo tipo y tienen el mismo id. El id es el id
 * BPMN del elemento, es inmutable (se fija al crear el nodo) y no puede cambiar mientras el nodo
 * esté en el grafo, porque el grafo los localiza por equals/hashCode. Al comparar también el tipo,
 * una acción y un gateway con el mismo id no se confunden.
 */
public sealed interface ProcessNode permits ActionWrapper, SimpleGateway, ProcessEvent {

    /** Id BPMN del elemento, único en el proceso */
    String getId();
}
