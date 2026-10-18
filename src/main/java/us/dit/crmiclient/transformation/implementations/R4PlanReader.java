package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.Gateway;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;
import us.dit.crmiclient.transformation.PlanReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lee un PlanDefinition FHIR R4 y construye los {@link ActionWrapper} de sus acciones.
 *
 * <h2>Identificadores de las acciones</h2>
 * Cada acción maneja dos identificadores distintos, que no deben confundirse:
 *
 * <h3>Id FHIR</h3>
 * <ul>
 * <li><b>Semántica:</b> identificador de la acción dentro del PlanDefinition. Es la referencia
 * que usan unas acciones para relacionarse con otras.</li>
 * <li><b>Origen:</b> el atributo {@code id} del elemento action en el recurso
 * ({@code action.getId()}). Lo escribe el autor del plan, es opcional y no tiene restricción de
 * formato más allá de la de FHIR (puede empezar por dígito, contener puntos o guiones, etc.).</li>
 * <li><b>Uso:</b> resolver los {@code relatedAction.actionId}. Es la clave del índice
 * {@code actionsByFhirId}. Debe ser único en el plan: si se repite, las referencias serían
 * ambiguas y el plan se rechaza. Una acción sin id FHIR no puede ser referenciada por otras.
 * No se guarda en el wrapper: se consulta con {@code wrapper.getAction().getId()}.</li>
 * </ul>
 *
 * <h3>Id BPMN</h3>
 * <ul>
 * <li><b>Semántica:</b> identificador del elemento BPMN (tarea) que representará la acción en el
 * proceso generado.</li>
 * <li><b>Origen:</b> se calcula a partir del id FHIR con {@link #toBpmnId(String)}, porque los ids
 * BPMN son NCName de XML (deben empezar por letra o '_' y no admiten espacios ni la mayoría de
 * símbolos). Si la acción no tiene id FHIR se genera {@code action_N}, con N su posición en el
 * plan. Si el saneado hace coincidir dos ids, se añade un sufijo {@code _2}, {@code _3}...</li>
 * <li><b>Uso:</b> es el que guarda {@code wrapper.getId()}, y es el que debe usarse al construir el
 * BPMN (ids de tareas, sequence flows, gateways). Es único dentro del proceso.</li>
 * </ul>
 *
 * En los casos habituales ambos coinciden; solo difieren cuando el id FHIR no es un NCName válido,
 * cuando el saneado provoca colisiones o cuando la acción no tiene id FHIR.
 * <p>
 * El id del propio proceso ({@link #getId()}) sigue la misma idea: se obtiene del id del
 * PlanDefinition (o de su name) y se sanea con {@link #toBpmnId(String)}.
 */
public class R4PlanReader implements PlanReader {
    private static final Logger logger = LoggerFactory.getLogger(R4PlanReader.class);
    private final PlanDefinition plan;
    private String generatedId;
   
    /** LinkedHashSet para conservar el orden de las acciones en el documento */
    private Set<ActionWrapper<PlanDefinitionActionComponent>> actions;
    /** Índice de las acciones por su id FHIR (el que usan los relatedAction.actionId) */
    private Map<String, ActionWrapper<PlanDefinitionActionComponent>> actionsByFhirId;
    /** Split tras el evento de inicio hacia las primeras acciones; null si no hace falta */
    private Gateway startGateway;
    /** Join de las últimas acciones antes del evento de fin; null si no hace falta */
    private Gateway endGateway;

    public R4PlanReader(String rawPlanDefinition) {
        this.plan = FhirContext.forR4()
                .newJsonParser()
                .parseResource(PlanDefinition.class, rawPlanDefinition);

        createActions();
        shortActions();
        findGateways();
        logger.debug("PlanDefinition leído: id={}, name={}", plan.getId(), plan.getName());
    }

    /**
     * Crea un wrapper por acción y los indexa por su id FHIR.
     * El id del wrapper es el id BPMN: el id FHIR saneado como NCName y único en el proceso.
     * Las acciones sin id reciben uno generado, pero no pueden ser referenciadas por otras.
     * Un id FHIR duplicado hace ambiguas las referencias, así que el plan se rechaza.
     * Restricción del proyecto: todas las acciones son de primer nivel, el orden se expresa solo
     * con relatedAction. Un plan con acciones anidadas se rechaza para no ignorarlas en silencio.
     */
    private void createActions() {
        actions = new LinkedHashSet<>();
        actionsByFhirId = new HashMap<>();
        Set<String> bpmnIds = new HashSet<>();
        int index = 0;
        for (PlanDefinitionActionComponent action : plan.getAction()) {
            index++;
            String fhirId = action.getId();
            if (action.hasAction()) {
                throw new InvalidPlanDefinitionException("La acción " + index + " (id=" + fhirId + ", '" + action.getTitle()
                        + "') tiene acciones anidadas, no soportadas: todas las acciones deben ser de primer nivel");
            }
            ActionWrapper<PlanDefinitionActionComponent> wrapper = new ActionWrapper<>(action);
            String bpmnId;
            if (fhirId == null || fhirId.isBlank()) {
                bpmnId = "action_" + index;
                logger.warn("La acción {} ('{}') no tiene id, se le asigna {}", index, action.getTitle(), bpmnId);
            } else {
                if (actionsByFhirId.containsKey(fhirId)) {
                    throw new InvalidPlanDefinitionException("Id de acción duplicado en el PlanDefinition: " + fhirId);
                }
                actionsByFhirId.put(fhirId, wrapper);
                bpmnId = toBpmnId(fhirId);
            }
            // El saneado puede hacer coincidir ids distintos ("a b" y "a_b"), o con uno generado
            String uniqueId = bpmnId;
            for (int suffix = 2; !bpmnIds.add(uniqueId); suffix++) {
                uniqueId = bpmnId + "_" + suffix;
            }
            if (!uniqueId.equals(bpmnId)) {
                logger.warn("El id BPMN {} de la acción {} ya existe, se usa {}", bpmnId, fhirId, uniqueId);
            }
            wrapper.setId(uniqueId);
            actions.add(wrapper);
        }
    }
    /**
     * Rellena los gateways en los ActionWrappers correspondientes, según la información de las acciones del plan.
     * Por ahora solo se generan gateways AND y OR (no XOR: no se puede saber si las condiciones de
     * las ramas son excluyentes, y OR es correcto en ambos casos).
     * <ul>
     * <li>Split (outGateway): si la acción tiene varias siguientes, o alguna siguiente tiene
     * condición applicability. OR si alguna siguiente es condicional, AND si no.</li>
     * <li>Join (inGateway): si la acción tiene varias anteriores. OR si alguna rama de entrada puede
     * no llegar (un AND esperaría para siempre esa rama), AND si no.</li>
     * <li>Las primeras y últimas acciones del plan no tienen un wrapper anterior o siguiente donde
     * colgar el gateway: el split tras el evento de inicio y el join antes del de fin se guardan
     * en el propio reader (startGateway y endGateway).</li>
     * </ul>
     */
    private void findGateways() {
        //Conjunto de acciones que podrían no ejecutarse, porque tienen condición o solo se llega a ellas a través de acciones con condición
        Set<ActionWrapper<PlanDefinitionActionComponent>> mayNotRun = findActionsThatMayNotRun();
        //Lista de acciones que no tienen anteriores para el gateway de inicio
        List<ActionWrapper<PlanDefinitionActionComponent>> firsts = new ArrayList<>();
        //Lista de acciones que no tienen siguientes para el gateway de fin
        List<ActionWrapper<PlanDefinitionActionComponent>> lasts = new ArrayList<>();
        for (ActionWrapper<PlanDefinitionActionComponent> wrapper : actions) {
            if (wrapper.isFirst()) {
                firsts.add(wrapper);
            }
            if (wrapper.isLast()) {
                lasts.add(wrapper);
            }
            // Si la acción tiene varias siguientes, o alguna siguiente tiene condición applicability, necesita un split
            if (!wrapper.isLast()) {
                Gateway.GatewayType outGatewayType = solveSplitType(wrapper.getNext());
                if (outGatewayType != null) {
                    wrapper.setOutGateway(new Gateway("gw_out_" + wrapper.getId(), outGatewayType));
                }
            }
            //Las acciones que tengan más de una previa necesitan un join. Si alguna de las acciones previas puede no ejecutarse, el join debe ser OR, si no, AND
            if (!wrapper.isFirst() && wrapper.getPrevious().size() > 1) {
                wrapper.setInGateway(new Gateway("gw_in_" + wrapper.getId(), solveJoinType(wrapper.getPrevious(), mayNotRun)));
            }
        }
        Gateway.GatewayType startSplit = solveSplitType(firsts);
        startGateway = startSplit == null ? null : new Gateway("gw_start", startSplit);
        endGateway = lasts.size() > 1 ? new Gateway("gw_end", solveJoinType(lasts, mayNotRun)) : null;
        logger.debug("Gateways del plan: inicio={}, fin={}",
                startGateway == null ? "-" : startGateway.getType(), endGateway == null ? "-" : endGateway.getType());
    }

    /**
     * Tipo del gateway de salida hacia las acciones dadas, o null si no hace falta gateway (una única siguiente y sin condición).
     */
    private static Gateway.GatewayType solveSplitType(Collection<ActionWrapper<PlanDefinitionActionComponent>> targets) {
        Gateway.GatewayType type = null;
        boolean anyConditional = targets.stream().anyMatch(t -> hasApplicabilityCondition(t.getAction()));
        if (anyConditional) {
            type = Gateway.GatewayType.OR;
        }else if (targets.size() > 1) {
            type = Gateway.GatewayType.AND;
        }
        return type;        
    }

    /**
     * Tipo del gateway de entrada (join) que reúne las ramas que llegan desde las acciones dadas.
     * <p>
     * Un join AND espera a que lleguen <b>todas</b> sus ramas de entrada. Si alguna rama no se
     * ejecuta (porque su condición applicability no se cumple), nunca llega, y el AND se queda
     * esperando para siempre: el proceso se bloquea. Un join OR, en cambio, espera solo a las ramas
     * que de verdad están en ejecución (el motor comprueba cuáles pueden aún alcanzarlo).
     * <p>
     * Por eso:
     * <ul>
     * <li>OR si alguna de las acciones de origen está en {@code mayNotRun}, es decir, puede no
     * ejecutarse (tiene condición o solo se llega a ella a través de acciones con condición;
     * ver {@link #findActionsThatMayNotRun()}).</li>
     * <li>AND si todas se ejecutan siempre: llegarán todas las ramas.</li>
     * </ul>
     * Poner siempre OR también sería correcto (sin condiciones se comporta como un AND), pero el
     * AND es más eficiente en el motor y deja más claro en el diagrama que llegan todas las ramas.
     *
     * @param sources   acciones de las que salen las ramas que llegan al join
     * @param mayNotRun acciones que pueden no ejecutarse, calculadas por {@link #findActionsThatMayNotRun()}
     */
    private static Gateway.GatewayType solveJoinType(Collection<ActionWrapper<PlanDefinitionActionComponent>> sources,
            Set<ActionWrapper<PlanDefinitionActionComponent>> mayNotRun) {
        return sources.stream().anyMatch(mayNotRun::contains) ? Gateway.GatewayType.OR : Gateway.GatewayType.AND;
    }

    /**
     * Acciones que pueden no ejecutarse: las que tienen condición applicability y, en cascada, las
     * que solo pueden alcanzarse a través de ellas (todas sus anteriores pueden no ejecutarse).
     * Se calcula como punto fijo iterando hasta que no cambia, lo que termina aunque haya ciclos.
     * Supone que si no se cumple la condición de una acción tampoco se ejecuta su rama: es la
     * semántica natural en BPMN, pero está pendiente de revisar en la gestión de condiciones.
     */
    private Set<ActionWrapper<PlanDefinitionActionComponent>> findActionsThatMayNotRun() {
        Set<ActionWrapper<PlanDefinitionActionComponent>> mayNotRun = new HashSet<>();
        for (ActionWrapper<PlanDefinitionActionComponent> wrapper : actions) {
            if (hasApplicabilityCondition(wrapper.getAction())) {
                mayNotRun.add(wrapper);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ActionWrapper<PlanDefinitionActionComponent> wrapper : actions) {
                if (!mayNotRun.contains(wrapper) && !wrapper.isFirst()
                        && mayNotRun.containsAll(wrapper.getPrevious())) {
                    mayNotRun.add(wrapper);
                    changed = true;
                }
            }
        }
        return mayNotRun;
    }

    private static boolean hasApplicabilityCondition(PlanDefinitionActionComponent action) {
        return action.getCondition().stream()
                .anyMatch(c -> c.getKind() == ActionConditionKind.APPLICABILITY);
    }
    /**
     * Implementa ordenación de las acciones del plan, para completar la información de la lista de ActionWrappers con los metadatos necesarios para la gestión de las acciones y su manipulación en una estructura de lista enlazada.
     */
    private void shortActions() {
        //Busco las relaciones antes y después de cada acción y añado las relaciones en los wrappers correspondientes
        for(ActionWrapper<PlanDefinitionActionComponent> wrapper: actions) {
            PlanDefinitionActionComponent action = wrapper.getAction();
            List<PlanDefinitionActionRelatedActionComponent> relatedActions = action.getRelatedAction();
            for(PlanDefinitionActionRelatedActionComponent relatedAction : relatedActions) {
                String relatedActionId = relatedAction.getActionId();
                ActionRelationshipType relationship = relatedAction.getRelationship();
                if (relationship == null || relationship == ActionRelationshipType.NULL) {
                    logger.warn("La acción {} tiene un relatedAction hacia {} sin tipo de relación, se ignora",
                            wrapper.getId(), relatedActionId);
                    // deja de analizar esta relación y pasa a la siguiente
                    continue;
                }
                if (relatedActionId == null || relatedActionId.isBlank()) {
                    logger.warn("La acción {} tiene un relatedAction {} sin actionId, se ignora",
                            wrapper.getId(), relationship.toCode());
                    continue;
                }
                ActionWrapper<PlanDefinitionActionComponent> related = actionsByFhirId.get(relatedActionId);
                if (related == null) {
                    logger.warn("La acción {} referencia a la acción inexistente {} ({}), se ignora",
                            wrapper.getId(), relatedActionId, relationship.toCode());
                    continue;
                }
                if (related == wrapper) {
                    logger.warn("La acción {} se referencia a sí misma ({}), se ignora",
                            wrapper.getId(), relationship.toCode());
                    continue;
                }
                switch (relationship) {
                    case BEFORE, BEFORESTART, BEFOREEND:
                        wrapper.setNext(related);
                        related.setPrevious(wrapper);
                        break;
                    case AFTER, AFTERSTART, AFTEREND:
                        wrapper.setPrevious(related);
                        related.setNext(wrapper);
                        break;
                    case CONCURRENT, CONCURRENTWITHSTART, CONCURRENTWITHEND:
                        wrapper.setConcurrent(related);
                        related.setConcurrent(wrapper);
                        break;
                    default:
                        logger.warn("Tipo de relación aún no soportado: {}", relationship.toCode());
                }
            }
           
        }
    }
    /** Acciones del plan, en el orden del documento */
    public Set<ActionWrapper<PlanDefinitionActionComponent>> getActions() {
        return Collections.unmodifiableSet(actions);
    }

    public Gateway getStartGateway() {
        return startGateway;
    }

    public Gateway getEndGateway() {
        return endGateway;
    }

    @Override
    public String getDocumentation() {
        String documentation="";
        String publisher = plan.getPublisher();
        if (publisher != null && !publisher.isEmpty()) {
            documentation += "Publisher: " + publisher + " ";
        }
        String description = plan.getDescription();
      
        if (description != null && !description.isEmpty()) {
            documentation += "Description: " + description + " ";
        }
        String usage = plan.getUsage();
        if (usage != null && !usage.isEmpty()) {
            documentation += "Usage: " + usage + " ";
        }
        String purpose = plan.getPurpose();
        if (purpose != null && !purpose.isEmpty()) {
            documentation += "Purpose: " + purpose+ " ";
        }
        // Implement logic to extract the documentation from the planDefinition
        return documentation; // Placeholder implementation
    }

    @Override
    public String getName() {
        // Implement logic to extract the name from the planDefinition
        return plan.getName(); // Placeholder implementation
    }

    /**
     * Id del proceso: el id del PlanDefinition; si no tiene (p.ej. un plan nuevo que aún no está
     * en el servidor) se usa su name, y si tampoco tiene name se genera uno.
     * El id generado se guarda para que llamadas sucesivas devuelvan siempre el mismo.
     */
    @Override
    public String getId() {
        if (generatedId != null) {
            return generatedId;
        }
        String resultado;
        if (plan.getIdElement().hasIdPart()) {
            // getIdPart descarta el prefijo "PlanDefinition/" y la versión.
            // Un id FHIR puede empezar por dígito, que no es un NCName válido
            resultado = toBpmnId(plan.getIdElement().getIdPart());
        } else if (plan.hasName()) {
            resultado = toBpmnId(plan.getName());
        } else {
            generatedId = "plan-" + UUID.randomUUID();
            resultado = generatedId;
        }
        logger.debug("Obteniendo ID del PlanDefinition: {}", resultado);
        return resultado;
    }

    /**
     * Los ids BPMN son NCName de XML: deben empezar por letra o '_' y no admiten espacios
     * ni la mayoría de símbolos. Los caracteres no válidos se sustituyen por '_'.
     */
    private static String toBpmnId(String name) {
        String id = name.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        return id.matches("^[A-Za-z_].*") ? id : "_" + id;
    }

    @Override
    public String getStartEvent() {
        // Implement logic to extract the start event from the planDefinition
        return "Extracted Start Event"; // Placeholder implementation
    }

    @Override
    public String getEndEvent() {
        // Implement logic to extract the end event from the planDefinition
        return "Extracted End Event"; // Placeholder implementation
    }

}
