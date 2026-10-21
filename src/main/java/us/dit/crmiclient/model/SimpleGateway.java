package us.dit.crmiclient.model;

import java.util.Objects;

/**
 * Gateway del proceso, independiente del motor. Solo guarda su id y su tipo: las acciones y
 * nodos que entran y salen de él están en el grafo del proceso del reader (ver PlanReader).
 */
public final class SimpleGateway implements ProcessNode {
    public enum GatewayType {
        AND,
        OR
    };

    /** Id BPMN del gateway, necesario para enlazarlo con sequence flows */
    private final String id;
    private final GatewayType type;

    public SimpleGateway(String id, GatewayType type) {
        this.id = Objects.requireNonNull(id, "El id del gateway es obligatorio");
        this.type = type;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public GatewayType getType() {
        return this.type;
    }

    /** Dos gateways son el mismo si tienen el mismo id (ver {@link ProcessNode}) */
    @Override
    public boolean equals(Object o) {
        return o instanceof SimpleGateway other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "SimpleGateway[" + id + ", " + type + "]";
    }
}
