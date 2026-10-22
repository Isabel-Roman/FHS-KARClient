package us.dit.crmiclient.transformation.implementations;

import us.dit.crmiclient.model.ActionWrapper;
import us.dit.crmiclient.model.Flow;
import us.dit.crmiclient.model.ParameterWrapper;
import us.dit.crmiclient.model.ProcessEvent;
import us.dit.crmiclient.model.ProcessNode;
import us.dit.crmiclient.model.SimpleGateway;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;
import us.dit.crmiclient.transformation.PlanReader;
import us.dit.crmiclient.services.ResourceResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionDynamicValueComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;

import org.jgrapht.Graphs;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lee un PlanDefinition FHIR R4 y construye los {@link ActionWrapper} de sus
 * acciones. Crea un grafo con esta información donde los nodos pueden ser: eventos, gateways o tareas (acciones)
 *
 * <h2>Identificadores de las acciones</h2>
 * Cada acción maneja dos identificadores distintos, que no deben confundirse:
 *
 * <h3>Id FHIR</h3>
 * <ul>
 * <li><b>Semántica:</b> identificador de la acción dentro del PlanDefinition.
 * Es la referencia que usan unas acciones para relacionarse con otras.</li>
 * <li><b>Origen:</b> el atributo {@code id} del elemento action en el recurso
 * ({@code action.getId()}). Lo escribe el autor del plan, es opcional y no
 * tiene restricción de formato más allá de la de FHIR (puede empezar por dígito, contener puntos o
 * guiones, etc.).</li>
 * <li><b>Uso:</b> resolver los {@code relatedAction.actionId}. Es la clave del
 * índice temporal que crea {@link #createActions()} y usa {@code shortActions} para
 * construir el grafo de dependencias. Debe ser único en el plan: si se repite, las
 * referencias serían ambiguas y el plan se rechaza. Una acción sin id FHIR no puede ser
 * referenciada por otras.
 * No se guarda directamente en el wrapper, que es de donde se va a sacar la info para bpmn: 
 * se consulta con {@code wrapper.getAction().getId()}.</li>
 * </ul>
 *
 * <h3>Id BPMN</h3>
 * <ul>
 * <li><b>Semántica:</b> identificador del elemento BPMN (tarea) que representará la acción en el
 * proceso generado.</li>
 * <li><b>Origen:</b> se calcula a partir del id FHIR con {@link #toUniqueBpmnId}, porque los ids
 * BPMN son NCName de XML (deben empezar por letra o '_' y no admiten espacios
 * ni la mayoría de símbolos). Si la acción no tiene id FHIR se genera {@code action_N}, con N su
 * posición en el plan. Si el saneado hace coincidir dos ids, se añade un sufijo {@code _2},
 * {@code _3}...</li>
 * <li><b>Uso:</b> es el que guarda {@code wrapper.getId()}, y es el que debe
 * usarse al construir el BPMN (ids de tareas, sequence flows, gateways). Es único dentro del proceso.</li>
 * </ul>
 *
 * En los casos habituales ambos coinciden; solo difieren cuando el id FHIR no
 * es un NCName válido, cuando el saneado provoca colisiones o cuando la acción no tiene id FHIR.
 * <p>
 * El id del propio proceso ({@link #getId()}) sigue la misma idea: se obtiene
 * del id del PlanDefinition (o de su name) y se sanea con {@link #toBpmnId(String)}.
 */
public class R4PlanReader implements PlanReader {
    private static final Logger logger = LoggerFactory.getLogger(R4PlanReader.class);
    private final PlanDefinition plan;
    private String processId;

    /**
     * Primeras acciones del plan (sin anteriores) y últimas (sin siguientes), 
     * en el orden del documento. Las calcula una vez solveGateways y las 
     * usa buildProcessGraph; 
     * no se guardan en el reader porque después no hacen falta.
     */
    private record PlanBoundary(List<ActionWrapper<PlanDefinitionActionComponent>> firsts,
            List<ActionWrapper<PlanDefinitionActionComponent>> lasts) {
    }

    /**
     * Todas las acciones del plan, en el orden del documento. Es la única colección de acciones del reader, el id de cada acción es único por diseño del código.
     */
    private final List<ActionWrapper<PlanDefinitionActionComponent>> actions = new ArrayList<>();
    /**
     * Split tras el evento de inicio hacia las primeras acciones; null si no hace
     * falta
     */
    private SimpleGateway startGateway;
    /**
     * Join de las últimas acciones antes del evento de fin; null si no hace falta
     */
    private SimpleGateway endGateway;

    /** Obtiene del servidor CRMI los PlanDefinition de las acciones y cualquier información adicional necesaria */
    private final ResourceResolver resolver;
    /**
     * 
     * Grafo de dependencias entre acciones: una arista A→B significa que A va antes
     * que B.  Basado en la librería jgrapht.
     * Es acíclico (DirectedAcyclicGraph rechaza las aristas que cerrarían un ciclo) y
     * se recorre en orden topológico. 
     * Es un detalle interno del reader: no se expone fuera.
     */
    private final DirectedAcyclicGraph<ActionWrapper<PlanDefinitionActionComponent>, DefaultEdge> dependencyGraph = new DirectedAcyclicGraph<>(
            DefaultEdge.class);
    /**
     * Grafo del proceso BPMN: los nodos son los eventos de inicio y fin, las
     * acciones y los
     * gateways, y las aristas los sequence flows. Se construye a partir de
     * dependencyGraph y de
     * los gateways. Las aristas se añaden siempre con su objeto Flow, así que no
     * hace falta
     * proveedor de aristas. Es interno: fuera se recorre con getNodes, getFlows...
     */
    private final DirectedAcyclicGraph<ProcessNode, Flow> processGraph = new DirectedAcyclicGraph<>(null, null, false);
    /**
     * Nodos del proceso por su id BPMN, para buscarlos directamente y detectar ids
     * repetidos
     */
    private final Map<String, ProcessNode> nodesById = new HashMap<>();
    /**
     * Splits (gateways de salida) de las acciones que lo necesitan; solo para
     * construir processGraph
     */
    private final Map<ActionWrapper<PlanDefinitionActionComponent>, SimpleGateway> splits = new HashMap<>();
    /**
     * Joins (gateways de entrada) de las acciones que lo necesitan; solo para
     * construir processGraph
     */
    private final Map<ActionWrapper<PlanDefinitionActionComponent>, SimpleGateway> joins = new HashMap<>();
  
    /**
     * El constructor necesita el plandefinition en formato crudo json y el ResourceResolver para contactar con Fhir
     * @param rawPlanDefinition planDefinition en json crudo
     * @param resolver permite acceso al servidor CRMI, obtiene los PlanDefinition referenciados por las acciones y cualquier otro recurso necesario
     *       
     */
    public R4PlanReader(String rawPlanDefinition, ResourceResolver resolver) {
        this.resolver = resolver;
        this.plan = FhirContext.forR4()
                .newJsonParser()
                .parseResource(PlanDefinition.class, rawPlanDefinition);
        setPlanId();

        // El índice por id FHIR solo hace falta para resolver los relatedAction: es local y se
        // descarta en cuanto se ha construido el grafo de dependencias
        Map<String, ActionWrapper<PlanDefinitionActionComponent>> actionsByFhirId = createActions();
        shortActions(actionsByFhirId);
        // Las primeras y últimas acciones se calculan una sola vez y solo se usan aquí
        PlanBoundary boundary = solveGateways();
        buildProcessGraph(boundary);
        solveSubprocessesCalls();
        logger.debug("PlanDefinition leído: id={}, name={}", plan.getId(), plan.getName());
    }

    /**
     * Crea un wrapper por acción y los indexa por su id FHIR.
     * El id del wrapper es el id BPMN: el id FHIR saneado como NCName y único en el
     * proceso.
     * Las acciones sin id reciben uno generado, pero no pueden ser referenciadas
     * por otras.
     * Un id FHIR duplicado hace ambiguas las referencias, así que el plan se
     * rechaza.
     * Restricción del proyecto: todas las acciones son de primer nivel, el orden se
     * expresa solo
     * con relatedAction. Un plan con acciones anidadas se rechaza para no
     * ignorarlas en silencio.
     *
     * @return índice de las acciones por su id FHIR (el que usan los relatedAction.actionId); las
     *         acciones sin id FHIR no están en él. Solo hace falta para construir el grafo de
     *         dependencias, por eso no se guarda en el reader
     */
    private Map<String, ActionWrapper<PlanDefinitionActionComponent>> createActions() {
        Map<String, ActionWrapper<PlanDefinitionActionComponent>> actionsByFhirId = new HashMap<>();
        // Conjunto de identificadores bpmn existentes, artefacto para asegurar que no están repetidos
        Set<String> bpmnIds = new HashSet<>();
        int index = 0;
        // Recorre todas las acciones incluidas en el planDefinition
        for (PlanDefinitionActionComponent action : plan.getAction()) {
            index++;
            String fhirId = action.getId();
            // No se soportan acciones anidadas, no se puede definir subprocesos dentro del
            // planDefinition
            if (action.hasAction()) {
                throw new InvalidPlanDefinitionException("La acción " + index + " (id=" + fhirId + ", '"
                        + action.getTitle()
                        + "') tiene acciones anidadas, no soportadas: todas las acciones deben ser de primer nivel");
            }
            String bpmnId;
            if (fhirId == null || fhirId.isBlank()) {
                // También el id generado tiene que ser único: otra acción podría tener de id FHIR "action_N"
                bpmnId = toUniqueBpmnId("action_" + index, bpmnIds);
                logger.warn("La acción {} ('{}') no tiene id, se le asigna {}", index, action.getTitle(), bpmnId);
            } else {
                if (actionsByFhirId.containsKey(fhirId)) {
                    throw new InvalidPlanDefinitionException("Id de acción duplicado en el PlanDefinition: " + fhirId);
                }
                // Limpieza del id para no violar las reglas de bpmn y asegurar qu eno hay
                // duplicados, si es necesario modifica el fhirId (lo devuelve modificado o igual)
                //Añade a bpmnIds el bpmnId creado
                bpmnId = toUniqueBpmnId(fhirId, bpmnIds);
            }      
            // El id se fija al crear el wrapper: es su identidad como nodo del grafo y no
            // puede cambiar, tal y como se ha construido se asegura que no está replicado
            ActionWrapper<PlanDefinitionActionComponent> wrapper = new ActionWrapper<>(action, bpmnId);
            if (fhirId != null && !fhirId.isBlank()) {
                actionsByFhirId.put(fhirId, wrapper);
            }
            // Nombre visible de la acción: su title, o su id si no tiene
            wrapper.setName(action.hasTitle() ? action.getTitle() : bpmnId);
            actions.add(wrapper);
            dependencyGraph.addVertex(wrapper);
        }
        return actionsByFhirId;
    }

    /**
     * Calcula los gateways del proceso, según la información de las acciones del plan: el split y
     * el join de cada acción que los necesita (en splits y joins) y los de inicio y fin.
     * Por ahora solo se generan gateways AND y OR (no XOR: no se puede saber si las
     * condiciones de
     * las ramas son excluyentes, y OR es correcto en ambos casos).
     * <ul>
     * <li>Split (outGateway): si la acción tiene varias siguientes, o alguna
     * siguiente tiene
     * condición applicability. OR si alguna siguiente es condicional, AND si
     * no.</li>
     * <li>Join (inGateway): si la acción tiene varias anteriores. OR si alguna rama
     * de entrada puede
     * no llegar (un AND esperaría para siempre esa rama), AND si no.</li>
     * <li>Las primeras y últimas acciones del plan no tienen un wrapper anterior o
     * siguiente donde
     * colgar el gateway: el split tras el evento de inicio y el join antes del de
     * fin se guardan
     * en el propio reader (startGateway y endGateway).</li>
     * </ul>
     *
     * @return primeras y últimas acciones del plan, que se calculan aquí de paso y necesita
     *         buildProcessGraph
     */
    private PlanBoundary solveGateways() {
        // Conjunto de acciones que podrían no ejecutarse, porque tienen condición o
        // solo se llega a ellas a través de acciones con condición
        Set<ActionWrapper<PlanDefinitionActionComponent>> mayNotRun = findActionsThatMayNotRun();
        // Lista de acciones que no tienen anteriores para el gateway de inicio
        List<ActionWrapper<PlanDefinitionActionComponent>> firsts = new ArrayList<>();
        // Lista de acciones que no tienen siguientes para el gateway de fin
        List<ActionWrapper<PlanDefinitionActionComponent>> lasts = new ArrayList<>();
        // Se recorren en el orden del documento, para que firsts y lasts lo conserven
        for (ActionWrapper<PlanDefinitionActionComponent> wrapper : actions) {
            Set<ActionWrapper<PlanDefinitionActionComponent>> next = successors(wrapper);
            Set<ActionWrapper<PlanDefinitionActionComponent>> previous = predecessors(wrapper);
            if (previous.isEmpty()) {
                firsts.add(wrapper);
            }
            if (next.isEmpty()) {
                lasts.add(wrapper);
            }
            // Si la acción no es la última, y tiene varias siguientes, o alguna siguiente
            // tiene condición applicability, necesita un split
            if (!next.isEmpty()) {
                SimpleGateway.GatewayType outGatewayType = solveSplitType(next);
                if (outGatewayType != null) {
                    // Split: entra desde la acción y sale hacia sus siguientes
                    splits.put(wrapper, new SimpleGateway("gw_out_" + wrapper.getId(), outGatewayType));
                }
            }
            // Si la acción no es la primera y tiene más de una anterior, necesita un join.
            // Si alguna de las acciones anteriores puede no ejecutarse, el join debe ser
            // OR, si no, AND
            if (previous.size() > 1) {
                // Join: entra desde las anteriores de la acción y sale hacia la acción
                joins.put(wrapper, new SimpleGateway("gw_in_" + wrapper.getId(), solveJoinType(previous, mayNotRun)));
            }
        }
        // Analizo el split de entrada mirando las primeras acciones. Las primeras
        // acciones son las de salida del evento de inicio...
        SimpleGateway.GatewayType startSplit = solveSplitType(firsts);
        startGateway = startSplit == null ? null : new SimpleGateway("gw_start", startSplit);
        // Analizo el join de salida mirando las últimas acciones. Las últimas acciones
        // son las de entrada para el evento de fin...
        endGateway = lasts.size() > 1 ? new SimpleGateway("gw_end", solveJoinType(lasts, mayNotRun)) : null;
        logger.debug("Gateways del plan: inicio={}, fin={}",
                startGateway == null ? "-" : startGateway.getType(), endGateway == null ? "-" : endGateway.getType());
        return new PlanBoundary(firsts, lasts);
    }

    /**
     * Construye el grafo del proceso a partir del de dependencias y de los
     * gateways:
     * <ul>
     * <li>Inicio → gateway de inicio (si hay) → primeras acciones.</li>
     * <li>Cada relación A→B del grafo de dependencias es un flujo desde la salida
     * de A (su split, si tiene) hasta la entrada de B (su join, si tiene).
     *      Si A tiene split, A → split;
     *      Si B tiene join, join → B.</li>
     * <li>Últimas acciones → gateway de fin (si hay) → fin.</li>
     * <li>Un plan sin acciones va directamente del inicio al fin.</li>
     * </ul>
     * Todos los nodos deben tener id distinto, porque es el id del elemento BPMN:
     * si una acción tiene el id de un gateway o de un evento (p.ej. una acción "gw_out_a" o
     * "startEvent"), el plan se rechaza.
     *
     * @param boundary primeras y últimas acciones del plan, calculadas por solveGateways
     */
    private void buildProcessGraph(PlanBoundary boundary) {
        List<ActionWrapper<PlanDefinitionActionComponent>> firsts = boundary.firsts();
        List<ActionWrapper<PlanDefinitionActionComponent>> lasts = boundary.lasts();
        addNode2Graph(ProcessEvent.START);
        actions.forEach(this::addNode2Graph);
        joins.values().forEach(this::addNode2Graph);
        splits.values().forEach(this::addNode2Graph);
        if (startGateway != null) {
            addNode2Graph(startGateway);
        }
        if (endGateway != null) {
            addNode2Graph(endGateway);
        }
        addNode2Graph(ProcessEvent.END);

        // Inicio
        if (startGateway != null) {
            addFlow2Graph(ProcessEvent.START, startGateway);
            firsts.forEach(first -> addFlow2Graph(startGateway, entry(first)));
        } else if (!firsts.isEmpty()) {
            // Sin gateway de inicio hay una única primera acción
            addFlow2Graph(ProcessEvent.START, entry(firsts.get(0)));
        }
        // Acciones y gateways de cada acción
        for (ActionWrapper<PlanDefinitionActionComponent> action : actions) {
            if (joins.containsKey(action)) {
                addFlow2Graph(joins.get(action), action);
            }
            if (splits.containsKey(action)) {
                addFlow2Graph(action, splits.get(action));
            }
            for (ActionWrapper<PlanDefinitionActionComponent> next : successors(action)) {
                addFlow2Graph(exit(action), entry(next));
            }
        }
        // Fin
        if (endGateway != null) {
            lasts.forEach(last -> addFlow2Graph(exit(last), endGateway));
            addFlow2Graph(endGateway, ProcessEvent.END);
        } else if (!lasts.isEmpty()) {
            // Sin gateway de fin hay una única última acción
            addFlow2Graph(exit(lasts.get(0)), ProcessEvent.END);
        }
        if (actions.isEmpty()) {
            addFlow2Graph(ProcessEvent.START, ProcessEvent.END);
        }
        logger.debug("Grafo del proceso: {} nodos, flujos {}", processGraph.vertexSet().size(), getFlows());
    }

    /**
     * Añade un nodo al grafo del proceso, rechazando el plan si su id ya lo usa
     * otro nodo
     * Los ProcessNode pueden ser: simplegateways, processevent o actionwrapper
     */
    private void addNode2Graph(ProcessNode node) {
        ProcessNode previous = nodesById.putIfAbsent(node.getId(), node);
        if (previous != null && !previous.equals(node)) {
            throw new InvalidPlanDefinitionException("El id " + node.getId() + " lo usan a la vez " + previous
                    + " y " + node + ": los elementos del proceso deben tener ids distintos");
        }
        processGraph.addVertex(node);
    }

    /** Añade un flujo al grafo del proceso; si ya existe no hace nada */
    private void addFlow2Graph(ProcessNode source, ProcessNode target) {
        if (!processGraph.containsEdge(source, target)) {
            processGraph.addEdge(source, target, new Flow(source, target));
        }
    }

    /**
     * Nodo por el que se entra a una acción: su join si tiene, o la propia acción
     */
    private ProcessNode entry(ActionWrapper<PlanDefinitionActionComponent> action) {
        return joins.containsKey(action) ? joins.get(action) : action;
    }

    /**
     * Nodo por el que se sale de una acción: su split si tiene, o la propia acción
     */
    private ProcessNode exit(ActionWrapper<PlanDefinitionActionComponent> action) {
        return splits.containsKey(action) ? splits.get(action) : action;
    }

    /**
     * Tipo del gateway de salida hacia las acciones dadas, o null si no hace falta
     * gateway (una única siguiente y sin condición).
     */
    private static SimpleGateway.GatewayType solveSplitType(
            Collection<ActionWrapper<PlanDefinitionActionComponent>> targets) {
        SimpleGateway.GatewayType type = null;
        boolean anyConditional = targets.stream().anyMatch(t -> hasApplicabilityCondition(t.getAction()));
        if (anyConditional) {
            type = SimpleGateway.GatewayType.OR;
        } else if (targets.size() > 1) {
            type = SimpleGateway.GatewayType.AND;
        }
        return type;
    }

    /**
     * Tipo del gateway de entrada (join) que reúne las ramas que llegan desde las
     * acciones dadas.
     * <p>
     * Un join AND espera a que lleguen <b>todas</b> sus ramas de entrada. Si alguna
     * rama no se
     * ejecuta (porque su condición applicability no se cumple), nunca llega, y el
     * AND se queda
     * esperando para siempre: el proceso se bloquea. Un join OR, en cambio, espera
     * solo a las ramas
     * que de verdad están en ejecución (el motor comprueba cuáles pueden aún
     * alcanzarlo).
     * <p>
     * Por eso:
     * <ul>
     * <li>OR si alguna de las acciones de origen está en {@code mayNotRun}, es
     * decir, puede no
     * ejecutarse (tiene condición o solo se llega a ella a través de acciones con
     * condición;
     * ver {@link #findActionsThatMayNotRun()}).</li>
     * <li>AND si todas se ejecutan siempre: llegarán todas las ramas.</li>
     * </ul>
     * Poner siempre OR también sería correcto (sin condiciones se comporta como un
     * AND), pero el
     * AND es más eficiente en el motor y deja más claro en el diagrama que llegan
     * todas las ramas.
     *
     * @param sources   acciones de las que salen las ramas que llegan al join
     * @param mayNotRun acciones que pueden no ejecutarse, calculadas por
     *                  {@link #findActionsThatMayNotRun()}
     */
    private static SimpleGateway.GatewayType solveJoinType(
            Collection<ActionWrapper<PlanDefinitionActionComponent>> sources,
            Set<ActionWrapper<PlanDefinitionActionComponent>> mayNotRun) {
        return sources.stream().anyMatch(mayNotRun::contains) ? SimpleGateway.GatewayType.OR
                : SimpleGateway.GatewayType.AND;
    }

    /**
     * Acciones que pueden no ejecutarse: las que tienen condición applicability y,
     * en cascada, las
     * que solo pueden alcanzarse a través de ellas (todas sus anteriores pueden no
     * ejecutarse).
     * Se calcula en una sola pasada recorriendo el grafo en orden topológico:
     * cuando se
     * llega a una acción ya se han visitado todas sus anteriores.
     * Supone que si no se cumple la condición de una acción tampoco se ejecuta su
     * rama: es la
     * semántica natural en BPMN, pero está pendiente de revisar en la gestión de
     * condiciones.
     */
    private Set<ActionWrapper<PlanDefinitionActionComponent>> findActionsThatMayNotRun() {
        Set<ActionWrapper<PlanDefinitionActionComponent>> mayNotRun = new HashSet<>();
        // El iterador de DirectedAcyclicGraph recorre los vértices en orden topológico
        for (ActionWrapper<PlanDefinitionActionComponent> wrapper : dependencyGraph) {
            Set<ActionWrapper<PlanDefinitionActionComponent>> previous = predecessors(wrapper);
            if (hasApplicabilityCondition(wrapper.getAction())
                    || (!previous.isEmpty() && mayNotRun.containsAll(previous))) {
                mayNotRun.add(wrapper);
            }
        }
        return mayNotRun;
    }

    /**
     * Acciones que van inmediatamente después de la dada, según el grafo de
     * dependencias
     */
    private Set<ActionWrapper<PlanDefinitionActionComponent>> successors(
            ActionWrapper<PlanDefinitionActionComponent> wrapper) {
        return new LinkedHashSet<>(Graphs.successorListOf(dependencyGraph, wrapper));
    }

    /**
     * Acciones que van inmediatamente antes de la dada, según el grafo de
     * dependencias
     */
    private Set<ActionWrapper<PlanDefinitionActionComponent>> predecessors(
            ActionWrapper<PlanDefinitionActionComponent> wrapper) {
        return new LinkedHashSet<>(Graphs.predecessorListOf(dependencyGraph, wrapper));
    }

    private static boolean hasApplicabilityCondition(PlanDefinitionActionComponent action) {
        return action.getCondition().stream()
                .anyMatch(c -> c.getKind() == ActionConditionKind.APPLICABILITY);
    }

    /**
     * Construye el grafo de dependencias entre acciones a partir de sus
     * relatedAction: una
     * arista A→B significa que A va antes que B.
     * <ul>
     * <li>Un ciclo (A antes que B y B antes que A, directa o indirectamente) no se
     * puede
     * convertir en un proceso: el plan se rechaza.</li>
     * <li>Se aplica la reducción transitiva: si A→B→C y además A→C, la arista A→C
     * sobra (ya
     * está implícita) y daría un split y un join innecesarios.</li>
     * </ul>
     * Las relaciones de concurrencia no son de orden: no entran en el grafo y se
     * guardan en
     * los wrappers.
     *
     * @param actionsByFhirId índice de las acciones por su id FHIR, para resolver los
     *                        relatedAction.actionId (ver {@link #createActions()})
     */
    private void shortActions(Map<String, ActionWrapper<PlanDefinitionActionComponent>> actionsByFhirId) {
        // Busco las relaciones antes y después de cada acción y añado las relaciones en
        // los wrappers correspondientes
        for (ActionWrapper<PlanDefinitionActionComponent> wrapper : actions) {
            PlanDefinitionActionComponent action = wrapper.getAction();
            List<PlanDefinitionActionRelatedActionComponent> relatedActions = action.getRelatedAction();
            for (PlanDefinitionActionRelatedActionComponent relatedAction : relatedActions) {
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
                        addDependency(wrapper, related);
                        break;
                    case AFTER, AFTERSTART, AFTEREND:
                        addDependency(related, wrapper);
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
        int edges = dependencyGraph.edgeSet().size();
        TransitiveReduction.INSTANCE.reduce(dependencyGraph);
        logger.debug("Grafo de dependencias: {} acciones, {} relaciones de orden ({} redundantes eliminadas)",
                dependencyGraph.vertexSet().size(), dependencyGraph.edgeSet().size(),
                edges - dependencyGraph.edgeSet().size());
    }

    /**
     * Añade al grafo que before va antes que after. DirectedAcyclicGraph rechaza la
     * arista si
     * cerraría un ciclo; una relación repetida (declarada desde las dos acciones)
     * se ignora.
     */
    private void addDependency(ActionWrapper<PlanDefinitionActionComponent> before,
            ActionWrapper<PlanDefinitionActionComponent> after) {
        if (dependencyGraph.containsEdge(before, after)) {
            return;
        }
        try {
            dependencyGraph.addEdge(before, after);
        } catch (IllegalArgumentException e) {
            throw new InvalidPlanDefinitionException("Las relaciones entre acciones forman un ciclo: "
                    + before.getId() + " no puede ir antes que " + after.getId()
                    + " porque " + after.getId() + " ya va antes que " + before.getId());
        }
    }

    /**
     * Para cada acción localiza el PlanDefinition asociado (campo definition) en el
     * servidor CRMI
     * y guarda en el wrapper su canónica, con la que se localizará después el
     * subproceso.
     * Toda acción debe tener un PlanDefinition existente: si no, el plan se
     * rechaza.
     * <p>
     * El reader no depende del motor: dónde y cómo arrancar el subproceso (su
     * Library y Endpoint
     * para un tipo de motor) lo resuelve el transformer de cada motor con
     * {@link R4SubprocessLocator}.
     * Pendiente: los parámetros de invocación, que se obtendrán de dynamicValue.
     * TENGO QUE LEER TODO LO QUE ME INTERESE DEL PLANDEFINITION DE LA ACCIÓN Y
     * PONERLO AQUÍ...
     */
    private void solveSubprocessesCalls() {
        for (ActionWrapper<PlanDefinitionActionComponent> wrapper : actions) {
            PlanDefinitionActionComponent action = wrapper.getAction();
            if (!action.hasDefinitionCanonicalType()) {
                throw new InvalidPlanDefinitionException("La acción " + wrapper.getId()
                        + " no tiene definitionCanonical: toda acción debe tener un PlanDefinition asociado");
            }
            String canonical = action.getDefinitionCanonicalType().getValue();
            logger.debug("Localizando el PlanDefinition {} que describe la acción {}", canonical, wrapper.getId());
            // Los PlanDefinition de las acciones están siempre en un servidor externo
            // (CRMI)
            PlanDefinition definition = resolver.resolveCanonical(canonical, PlanDefinition.class);
            if (definition == null) {
                throw new InvalidPlanDefinitionException("No se encuentra el PlanDefinition " + canonical
                        + " de la acción " + wrapper.getId());
            }
            logger.debug("PlanDefinition {} de la acción {} encontrado", definition.getId(), wrapper.getId());
            // Sin versión en la referencia se fija la del plan encontrado, para que al
            // localizar
            // el subproceso después se use el mismo que se ha validado aquí
            if (!canonical.contains("|") && definition.hasVersion()) {
                canonical = canonical + "|" + definition.getVersion();
            }
            wrapper.setSubprocessCanonical(canonical);
            logger.debug("A la acción {} se le añade la url canonica {}", wrapper.getId(), canonical);
            solveSubprocessParameters(wrapper, definition, canonical);
            solveParameterValues(wrapper);
        }
    }

    /**
     * Asigna a los parámetros de entrada del subproceso los valores de los
     * dynamicValue de la
     * acción.
     * <p>
     * CONVENCIÓN: dynamicValue.path es el nombre del parámetro de entrada, y su
     * expression es el
     * valor que recibe (se guarda la Expression tal cual, sin evaluar).
     * <p>
     * El plan se rechaza si un dynamicValue no tiene path o expression, si su path
     * no es un
     * parámetro de entrada (un error de escritura dejaría el parámetro sin valor
     * sin avisar), si
     * se asigna dos veces el mismo parámetro, o si queda sin valor un parámetro
     * obligatorio
     * (min mayor que 0) que no tiene valor por defecto.
     */
    private void solveParameterValues(ActionWrapper<PlanDefinitionActionComponent> wrapper) {
        for (PlanDefinitionActionDynamicValueComponent dynamicValue : wrapper.getAction().getDynamicValue()) {
            if (!dynamicValue.hasPath() || dynamicValue.getPath().isBlank() || !dynamicValue.hasExpression()) {
                throw new InvalidPlanDefinitionException("La acción " + wrapper.getId()
                        + " tiene un dynamicValue sin path o sin expression");
            }
            String name = dynamicValue.getPath();
            ParameterWrapper<?> parameter = wrapper.getInParameters().get(name);
            if (parameter == null) {
                throw new InvalidPlanDefinitionException("El dynamicValue " + name + " de la acción " + wrapper.getId()
                        + " no corresponde a ningún parámetro de entrada de su subproceso "
                        + wrapper.getSubprocessCanonical());
            }
            if (parameter.getValue() != null) {
                throw new InvalidPlanDefinitionException("La acción " + wrapper.getId()
                        + " asigna más de un valor al parámetro " + name);
            }
            parameter.setValue(dynamicValue.getExpression(), dynamicValue.getExpression().getExpression());
            logger.debug("La acción {} asigna al parámetro {} el valor {}", wrapper.getId(), name,
                    dynamicValue.getExpression().getExpression());
        }
        for (ParameterWrapper<?> parameter : wrapper.getInParameters().values()) {
            if (parameter.getMin() > 0 && parameter.getValue() == null && parameter.getDefaultValue() == null) {
                throw new InvalidPlanDefinitionException("La acción " + wrapper.getId()
                        + " no da valor al parámetro obligatorio " + parameter.getName() + " de su subproceso");
            }
        }
    }

    /**
     * Añade al wrapper los parámetros de entrada y salida del subproceso,
     * declarados en
     * Library.parameter de las Library de su PlanDefinition.
     * <p>
     * El PlanDefinition puede tener una Library por tipo de motor; todas describen
     * el mismo
     * proceso, así que se juntan sus parámetros. Un mismo parámetro declarado en
     * varias debe ser
     * igual en todas (mismo uso y tipo); si no, el plan se rechaza. Un subproceso
     * sin parámetros
     * es válido. Las Library se obtienen del servidor a través del resolver.
     */
    private void solveSubprocessParameters(ActionWrapper<PlanDefinitionActionComponent> wrapper,
            PlanDefinition definition, String canonical) {
        Map<String, ParameterWrapper<ParameterDefinition>> found = new LinkedHashMap<>();
        for (CanonicalType libraryCanonical : definition.getLibrary()) {
            Library library = resolver.resolveCanonical(libraryCanonical.getValue(), Library.class);
            if (library == null) {
                throw new InvalidPlanDefinitionException("No se encuentra la Library " + libraryCanonical.getValue()
                        + " del PlanDefinition " + canonical + " de la acción " + wrapper.getId());
            }
            for (ParameterDefinition r4Parameter : library.getParameter()) {
                ParameterWrapper<ParameterDefinition> parameter = wrapParameter(r4Parameter,
                        libraryCanonical.getValue());
                ParameterWrapper<ParameterDefinition> previous = found.putIfAbsent(parameter.getName(), parameter);
                if (previous != null && (previous.getUse() != parameter.getUse()
                        || !Objects.equals(previous.getType(), parameter.getType()))) {
                    throw new InvalidPlanDefinitionException("El parámetro " + parameter.getName()
                            + " se declara de forma distinta en las Library del PlanDefinition " + canonical);
                }
            }
        }
        found.values().forEach(wrapper::addParameter);
        logger.debug("La acción {} tiene los parámetros de entrada {} y de salida {}", wrapper.getId(),
                wrapper.getInParameters().keySet(), wrapper.getOutParameters().keySet());
    }

    /**
     * Envuelve un ParameterDefinition de R4, sacando fuera sus metadatos. En R4 no
     * hay valor por
     * defecto. El nombre y el uso son necesarios para enlazar el parámetro.
     */
    private static ParameterWrapper<ParameterDefinition> wrapParameter(ParameterDefinition r4Parameter,
            String libraryCanonical) {
        if (!r4Parameter.hasName() || r4Parameter.getName().isBlank()) {
            throw new InvalidPlanDefinitionException(
                    "La Library " + libraryCanonical + " tiene un parámetro sin nombre");
        }
        // Sin use, getUse() devuelve null y el switch fallaría: se trata como NULL
        ParameterDefinition.ParameterUse r4Use = r4Parameter.hasUse() ? r4Parameter.getUse()
                : ParameterDefinition.ParameterUse.NULL;
        ParameterWrapper.Use use = switch (r4Use) {
            case IN -> ParameterWrapper.Use.IN;
            case OUT -> ParameterWrapper.Use.OUT;
            case NULL -> throw new InvalidPlanDefinitionException("El parámetro " + r4Parameter.getName()
                    + " de la Library " + libraryCanonical + " no indica si es de entrada o de salida");
        };
        ParameterWrapper<ParameterDefinition> parameter = new ParameterWrapper<>(
                r4Parameter,
                r4Parameter.getName(),
                use,
                r4Parameter.hasMin() ? r4Parameter.getMin() : 0,
                r4Parameter.hasMax() ? r4Parameter.getMax() : null,
                r4Parameter.hasType() ? r4Parameter.getType() : null,
                r4Parameter.hasProfile() ? r4Parameter.getProfile() : null,
                r4Parameter.hasDocumentation() ? r4Parameter.getDocumentation() : null,
                null);
        logger.debug("ParameterWrapper creado={}", parameter);
        return parameter;
    }

    /**
     * Nodos del proceso en orden topológico: cada nodo aparece después de todos sus
     * anteriores
     */
    @Override
    public List<ProcessNode> getNodes() {
        List<ProcessNode> nodes = new ArrayList<>();
        processGraph.forEach(nodes::add);
        return nodes;
    }

    /** Flujos del proceso, agrupados por su origen en orden topológico */
    @Override
    public List<Flow> getFlows() {
        List<Flow> flows = new ArrayList<>();
        for (ProcessNode node : processGraph) {
            flows.addAll(processGraph.outgoingEdgesOf(node));
        }
        return flows;
    }

    @Override
    public List<ProcessNode> getSuccessors(ProcessNode node) {
        return List.copyOf(Graphs.successorListOf(processGraph, requireNode(node)));
    }

    @Override
    public List<ProcessNode> getPredecessors(ProcessNode node) {
        return List.copyOf(Graphs.predecessorListOf(processGraph, requireNode(node)));
    }

    @Override
    public ProcessNode getNode(String id) {
        return nodesById.get(id);
    }

    private ProcessNode requireNode(ProcessNode node) {
        if (!processGraph.containsVertex(node)) {
            throw new IllegalArgumentException("El nodo " + node + " no es del proceso");
        }
        return node;
    }

    /** Acciones del plan, en el orden del documento */
    public List<ActionWrapper<PlanDefinitionActionComponent>> getActions() {
        return Collections.unmodifiableList(actions);
    }

    @Override
    public String getDocumentation() {
        String documentation = "";
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
            documentation += "Purpose: " + purpose + " ";
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
     * Id del proceso, calculado al crear el reader (ver {@link #setId()})
     */
    @Override
    public String getId() {
        return processId;
    }

    /**
     * Calcula una única vez el id del proceso: el id del PlanDefinition; si no tiene (p.ej. un
     * plan nuevo que aún no está en el servidor) se usa su name, y si tampoco tiene name se
     * genera uno. Se sanea como id BPMN, porque un id FHIR o un name pueden no ser NCName válidos.
     */
    private void setPlanId() {
        if (plan.getIdElement().hasIdPart()) {
            // getIdPart descarta el prefijo "PlanDefinition/" y la versión.
            // Un id FHIR puede empezar por dígito, que no es un NCName válido
            processId = toBpmnId(plan.getIdElement().getIdPart());
        } else if (plan.hasName()) {
            processId = toBpmnId(plan.getName());
        } else {
            processId = "plan-" + UUID.randomUUID();
        }
        logger.debug("Id del proceso: {}", processId);
    }

    /**
     * Método para sanear el id de la acción
     * Intenta usar el id que se le puso en el PlanDefinition, pero se modifica para que sea válido
     * en BPMN y para que no haya duplicados. Se guarda en el conjunto bpmnIds para que no se repita.
     * Los ids BPMN son NCName de XML: deben empezar por letra o '_' y no admiten
     * espacios ni la mayoría de símbolos. Los caracteres no válidos se sustituyen por '_'.
     */
    private static String toUniqueBpmnId(String fhirId, Set<String> bpmnIds) {
        String bpmnId = toBpmnId(fhirId);
        // Aseguro que no existe el id BPMN: si ya existe, se añade un sufijo _2, _3, ... hasta que sea único
        String uniqueId = bpmnId;
        for (int suffix = 2; !bpmnIds.add(uniqueId); suffix++) {
            uniqueId = bpmnId + "_" + suffix;
        }
        if (!uniqueId.equals(bpmnId)) {
            logger.warn("El id BPMN {} de la acción {} ya existe, se usa {}", bpmnId, fhirId, uniqueId);
        }

        return uniqueId;
    }

    /**
     * Sanea un texto como id BPMN, sin comprobar si es único (lo usan las acciones, a través de
     * {@link #toUniqueBpmnId}, y el id del proceso). Los ids BPMN son NCName de XML: deben
     * empezar por letra o '_' y no admiten espacios ni la mayoría de símbolos. Los caracteres no
     * válidos se sustituyen por '_'.
     */
    private static String toBpmnId(String name) {
        String id = name.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        return id.matches("^[A-Za-z_].*") ? id : "_" + id;
    }

}
