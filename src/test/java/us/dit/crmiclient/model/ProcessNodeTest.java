package us.dit.crmiclient.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Identidad de los nodos del proceso: mismo tipo y mismo id */
class ProcessNodeTest {

    @Test
    void actionsWithTheSameIdAreTheSameNode() {
        // Dos objetos distintos con el mismo id son la misma acción, aunque envuelvan cosas distintas
        ActionWrapper<String> a1 = new ActionWrapper<>("una", "a");
        ActionWrapper<String> a2 = new ActionWrapper<>("otra", "a");
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        Set<ProcessNode> nodes = new HashSet<>(Set.of(a1));
        nodes.add(a2);
        assertEquals(1, nodes.size());
        assertNotEquals(a1, new ActionWrapper<>("una", "b"));
    }

    @Test
    void gatewaysWithTheSameIdAreTheSameNode() {
        assertEquals(new SimpleGateway("gw", SimpleGateway.GatewayType.AND),
                new SimpleGateway("gw", SimpleGateway.GatewayType.AND));
    }

    @Test
    void nodesOfDifferentKindsAreNeverEqual() {
        // Una acción llamada como un gateway, o como un evento, no se confunde con ellos
        assertNotEquals(new ActionWrapper<>(null, "gw_out_a"), new SimpleGateway("gw_out_a", SimpleGateway.GatewayType.AND));
        assertNotEquals(new ActionWrapper<>(null, "startEvent"), ProcessEvent.START);
        assertNotEquals(ProcessEvent.START, ProcessEvent.END);
    }

    @Test
    void idIsRequired() {
        assertThrows(NullPointerException.class, () -> new ActionWrapper<>(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleGateway(null, SimpleGateway.GatewayType.OR));
    }
}
