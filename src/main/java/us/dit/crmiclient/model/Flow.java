package us.dit.crmiclient.model;

import java.util.Objects;

/**
 * Flujo del proceso (sequence flow en BPMN) entre dos nodos. Es el tipo de las aristas del grafo
 * del proceso.
 *
 * @param source    nodo de origen
 * @param target    nodo de destino
 * @param condition condición para tomar el flujo, para los que salen de un split OR; null si no
 *                  tiene (pendiente de la gestión de condiciones)
 */
public record Flow(ProcessNode source, ProcessNode target, String condition) {

    public Flow {
        Objects.requireNonNull(source, "El origen del flujo es obligatorio");
        Objects.requireNonNull(target, "El destino del flujo es obligatorio");
    }

    /** Flujo sin condición */
    public Flow(ProcessNode source, ProcessNode target) {
        this(source, target, null);
    }

    @Override
    public String toString() {
        return source.getId() + "->" + target.getId();
    }
}
