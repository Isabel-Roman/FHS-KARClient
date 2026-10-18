package us.dit.crmiclient.model;

public class SimpleGateway {
    public enum GatewayType {
        AND,  
        OR
    };

    /** Id BPMN del gateway, necesario para enlazarlo con sequence flows */
    private final String id;
    private final GatewayType type;

    public SimpleGateway(String id, GatewayType and) {
        this.id = id;
        this.type = and;
    }

    public String getId() {
        return this.id;
    }

    public GatewayType getType() {
        return this.type;
    }

}
