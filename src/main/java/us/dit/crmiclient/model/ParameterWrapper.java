package us.dit.crmiclient.model;

import org.hl7.fhir.instance.model.api.IBaseDatatype;

/**
 * Envuelve la definición de un parámetro de entrada o salida de un subproceso, de cualquier
 * versión de FHIR (Library.parameter), y añade fuera los metadatos que se usan al generar el
 * proceso, para no depender de la versión. Guarda también el valor que se le asigna en la
 * invocación.
 *
 * @param <T> tipo de la definición en la versión de FHIR (p.ej. ParameterDefinition de R4)
 */
public class ParameterWrapper<T> {

    public enum Use {
        IN,
        OUT
    }

    /** Definición original del parámetro, en su versión de FHIR */
    private final T definition;
    /** Nombre del parámetro, con el que se enlaza en la invocación */
    private final String name;
    /** Entrada o salida */
    private final Use use;
    /** Número mínimo de valores (0 si es opcional) */
    private final int min;
    /** Número máximo de valores: un número o "*"; null si no se indica */
    private final String max;
    /** Tipo FHIR del valor (p.ej. "string", "Patient"); null si no se indica */
    private final String type;
    /** Canónica del perfil que deben cumplir los valores; null si no se indica */
    private final String profile;
    /** Descripción del parámetro; null si no se indica */
    private final String documentation;
    /** Valor por defecto, solo en las versiones de FHIR que lo admiten (R5 en adelante); null si no tiene */
    private final IBaseDatatype defaultValue;
    /** Valor asignado en la invocación; null mientras no se asigne */
    private IBaseDatatype value;
    /**
     * El valor como texto, para quien lo usa sin conocer la versión de FHIR (p.ej. el writer):
     * si el valor es una expresión, su texto (FHIRPath, CQL...) sin evaluar. Null mientras no se
     * asigne valor.
     */
    private String valueText;

    public ParameterWrapper(T definition, String name, Use use, int min, String max, String type, String profile,
            String documentation, IBaseDatatype defaultValue) {
        this.definition = definition;
        this.name = name;
        this.use = use;
        this.min = min;
        this.max = max;
        this.type = type;
        this.profile = profile;
        this.documentation = documentation;
        this.defaultValue = defaultValue;
    }

    public T getDefinition() {
        return definition;
    }

    public String getName() {
        return name;
    }

    public Use getUse() {
        return use;
    }

    public int getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }

    public String getType() {
        return type;
    }

    public String getProfile() {
        return profile;
    }

    public String getDocumentation() {
        return documentation;
    }

    public IBaseDatatype getDefaultValue() {
        return defaultValue;
    }

    public IBaseDatatype getValue() {
        return value;
    }

    public String getValueText() {
        return valueText;
    }

    /**
     * Asigna el valor y su texto a la vez, para que no puedan quedar desalineados. Quien conoce la
     * versión de FHIR (el reader) es quien sabe sacar el texto del valor.
     */
    public void setValue(IBaseDatatype value, String valueText) {
        this.value = value;
        this.valueText = valueText;
    }
}
