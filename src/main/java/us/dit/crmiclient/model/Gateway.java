package us.dit.crmiclient.model;

public class Gateway {
    public enum GatewayType {
        AND,  
        OR
    };

    /** Id BPMN del gateway, necesario para enlazarlo con sequence flows */
    private final String id;
    private final GatewayType type;

    public Gateway(String id, GatewayType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public GatewayType getType() {
        return this.type;
    }

}
