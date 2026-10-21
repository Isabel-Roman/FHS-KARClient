package us.dit.crmiclient.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class SimpleGateway implements ProcessNode {
    public enum GatewayType {
        AND,  
        OR
    };
    Set<ActionWrapper<?>> incomingActions;
    Set<ActionWrapper<?>> outgoingActions;

    /** Id BPMN del gateway, necesario para enlazarlo con sequence flows */
    private final String id;
    private final GatewayType type;

    public SimpleGateway(String id, GatewayType and) {
        this.id = Objects.requireNonNull(id, "El id del gateway es obligatorio");
        this.type = and;
        incomingActions = new HashSet<>();
        outgoingActions = new HashSet<>();
    }

    @Override
    public String getId() {
        return this.id;
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

    public GatewayType getType() {
        return this.type;
    }
    public void addIncomingAction(ActionWrapper<?> action) {
        incomingActions.add(action);
    }

    public void addOutgoingAction(ActionWrapper<?> action) {
        outgoingActions.add(action);
    }
    public Set<ActionWrapper<?>> getIncomingActions() {
        return incomingActions;
    }
    public Set<ActionWrapper<?>> getOutgoingActions() {
        return outgoingActions;
    }

}
