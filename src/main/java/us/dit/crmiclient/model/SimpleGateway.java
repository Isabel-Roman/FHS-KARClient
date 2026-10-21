package us.dit.crmiclient.model;

import java.util.HashSet;
import java.util.Set;

public class SimpleGateway {
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
        this.id = id;
        this.type = and;
        incomingActions = new HashSet<>();
        outgoingActions = new HashSet<>();
    }

    public String getId() {
        return this.id;
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
