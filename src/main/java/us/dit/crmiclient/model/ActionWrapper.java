package us.dit.crmiclient.model;

import java.util.Set;

/**
 * Envuelve acciones de cualquier versión fhir y añade metadatos para su gestión
 * facilita la manipulación de estas acciones en una estructura de lista
 * enlazada.
 **/
public class ActionWrapper<T> {
    private T action;
    private String id;
    /** Acciones vinculadas directamente a continuación */
    private Set<ActionWrapper<T>> next;
    /** Acciones vinculadas directamente antes */
    private Set<ActionWrapper<T>> previous;
    /** Acciones que se ejecutan concurrentemente */
    private Set<ActionWrapper<T>> concurrent;
    /** Puerta de entrada */
    private Gateway inGateway;
    /** Puerta de salida */
    private Gateway outGateway;

    public ActionWrapper(T action) {
        this.action = action;
    }

    public T getAction() {
        return action;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isFirst() {

        return previous == null;
    }

    public boolean isLast() {
        return next == null;
    }

    public Set<ActionWrapper<T>> getNext() {
        return next;
    }

    public void setNext(ActionWrapper<T> next) {
        if (this.next == null) {
            this.next = new java.util.HashSet<>();
        }
        this.next.add(next);
    }

    public Set<ActionWrapper<T>> getPrevious() {
        return previous;
    }

    public void setPrevious(ActionWrapper<T> previous) {
        if (this.previous == null) {
            this.previous = new java.util.HashSet<>();
        }
        this.previous.add(previous);
    }

    public Set<ActionWrapper<T>> getConcurrent() {
        return concurrent;
    }

    public void setConcurrent(ActionWrapper<T> concurrent) {
        if (this.concurrent == null) {
            this.concurrent = new java.util.HashSet<>();
        }
        this.concurrent.add(concurrent);
    }


    public Gateway getInGateway() {
        return inGateway;
    }

    public void setInGateway(Gateway inGateway) {
        this.inGateway = inGateway;
    }

    public Gateway getOutGateway() {
        return outGateway;
    }

    public void setOutGateway(Gateway outGateway) {
        this.outGateway = outGateway;
    }

}