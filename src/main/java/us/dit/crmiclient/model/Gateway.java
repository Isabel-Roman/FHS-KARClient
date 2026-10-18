package us.dit.crmiclient.model;

public class Gateway {
    public enum GatewayType {
        AND,
        XOR,
        OR
    };

    public Gateway(GatewayType type) {
        this.type = type;
    }

    private GatewayType type;

    public GatewayType getType() {
        return this.type;
    }   

}
