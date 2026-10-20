package us.dit.crmiclient.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private SimpleGateway inGateway;
    /** Puerta de salida */
    private SimpleGateway outGateway;
    /**
     * Canónica (url|version) del PlanDefinition del subproceso que implementa la acción. A partir
     * de ella, el transformer de cada motor localiza dónde y cómo arrancarlo.
     */
    private String subprocessCanonical;
    /**
     * Parámetros de entrada del subproceso que implementa la acción, por nombre, en el orden en
     * que los declara su Library.
     * CONVENCIÓN: El id del parámetro de entrada en el bpmn es el id de la acción_key
     */
    private Map<String, ParameterWrapper<?>> inParameters = new LinkedHashMap<>();
    /**
     * Parámetros de salida del subproceso que implementa la acción, por nombre, en el orden en
     * que los declara su Library.
     * CONVENCIÓN: El id del parámetro de salida en el bpmn es el id de la acción_key
     */
    private Map<String, ParameterWrapper<?>> outParameters = new LinkedHashMap<>();

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


    public SimpleGateway getInGateway() {
        return inGateway;
    }

    public void setInGateway(SimpleGateway inGateway) {
        this.inGateway = inGateway;
    }

    public SimpleGateway getOutGateway() {
        return outGateway;
    }

    public void setOutGateway(SimpleGateway outGateway) {
        this.outGateway = outGateway;
    }

    public String getSubprocessCanonical() {
        return subprocessCanonical;
    }

    public void setSubprocessCanonical(String subprocessCanonical) {
        this.subprocessCanonical = subprocessCanonical;
    }

    public Map<String, ParameterWrapper<?>> getInParameters() {
        return Collections.unmodifiableMap(inParameters);
    }

    public Map<String, ParameterWrapper<?>> getOutParameters() {
        return Collections.unmodifiableMap(outParameters);
    }

    /** Añade un parámetro a los de entrada o a los de salida, según su uso */
    public void addParameter(ParameterWrapper<?> parameter) {
        Map<String, ParameterWrapper<?>> target = parameter.getUse() == ParameterWrapper.Use.IN
                ? inParameters : outParameters;
        target.put(parameter.getName(), parameter);
    }

}