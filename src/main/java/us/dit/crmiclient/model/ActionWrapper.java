package us.dit.crmiclient.model;

import java.util.List;

/**
*Envuelve acciones de cualquier versión fhir y añade metadatos para su gestión
*facilita la manipulación de estas acciones en una estructura de lista enlazada.
**/
public class ActionWrapper<T> {
    private T action;
    private String id;
  /**Acciones vinculadas directamente a continuación */
    private List<ActionWrapper<T>> next;
    /**Acciones vinculadas directamente antes */
    private List<ActionWrapper<T>> previous;

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
    public List<ActionWrapper<T>> getNext() {
        return next;
    }
    public void setNext(ActionWrapper<T> next) {
        this.next.add(next);
    }
    public List<ActionWrapper<T>> getPrevious() {
        return previous;
    }
    public void setPrevious(ActionWrapper<T> previous) {
        this.previous.add(previous);
    }
}