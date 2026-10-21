package us.dit.crmiclient.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Envuelve acciones de cualquier versión fhir y añade los metadatos que se usan al generar el
 * proceso (nombre, subproceso, parámetros), sin depender de la versión.
 * Las conexiones con el resto del proceso (anteriores, siguientes, gateways) no están aquí: las
 * guarda el grafo del proceso del reader (ver PlanReader).
 **/
public final class ActionWrapper<T> implements ProcessNode {
    private final T action;
    /** Id BPMN de la acción: inmutable, porque define la identidad del nodo (ver {@link ProcessNode}) */
    private final String id;
    /** Acciones que se ejecutan concurrentemente (no son relaciones de orden, no están en el grafo) */
    private Set<ActionWrapper<T>> concurrent;
    /**
     * Canónica (url|version) del PlanDefinition del subproceso que implementa la acción. A partir
     * de ella, el transformer de cada motor localiza dónde y cómo arrancarlo.
     */
    private String subprocessCanonical;
    /** Nombre visible de la acción (su title), para mostrarlo en el proceso generado */
    private String name;
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

    /**
     * @param action acción en su versión de FHIR
     * @param id     id BPMN de la acción, único en el proceso
     */
    public ActionWrapper(T action, String id) {
        this.action = action;
        this.id = Objects.requireNonNull(id, "El id de la acción es obligatorio");
    }

    public T getAction() {
        return action;
    }

    @Override
    public String getId() {
        return id;
    }

    /** Dos acciones son la misma si tienen el mismo id (ver {@link ProcessNode}) */
    @Override
    public boolean equals(Object o) {
        return o instanceof ActionWrapper<?> other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ActionWrapper[" + id + "]";
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


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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