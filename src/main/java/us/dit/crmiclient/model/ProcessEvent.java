package us.dit.crmiclient.model;

/**
 * Evento de inicio o de fin del proceso, como nodo del grafo del proceso. Al ser un record, su
 * igualdad ya es por tipo de evento e id.
 */
public record ProcessEvent(Kind kind, String id) implements ProcessNode {

    public enum Kind {
        START,
        END
    }

    public static final ProcessEvent START = new ProcessEvent(Kind.START, "startEvent");
    public static final ProcessEvent END = new ProcessEvent(Kind.END, "endEvent");

    @Override
    public String getId() {
        return id;
    }
}
