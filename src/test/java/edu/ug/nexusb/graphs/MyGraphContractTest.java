package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyIterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs the same normal/boundary/invalid-input cases against both {@link
 * MyGraph} implementations, since the whole point of the contract is that
 * they behave identically — a test written against only one representation
 * would miss a bug specific to the other.
 */
class MyGraphContractTest {

    static Stream<Arguments> implementations() {
        return Stream.of(
            Arguments.of("ADJACENCY_LIST", (Supplier<MyGraph>) AdjacencyListGraph::new),
            Arguments.of("ADJACENCY_MATRIX", (Supplier<MyGraph>) AdjacencyMatrixGraph::new)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void reportsItsOwnRepresentationName(String expectedName, Supplier<MyGraph> factory) {
        assertEquals(expectedName, factory.get().representationName());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void newGraphIsEmpty(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
        assertFalse(graph.containsVertex("F001"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void addVertexIsIdempotent(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("F001");
        graph.addVertex("F001");

        assertEquals(1, graph.vertexCount());
        assertTrue(graph.containsVertex("F001"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void addEdgeAutoAddsBothEndpoints(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F002", 5.0));

        assertTrue(graph.containsVertex("F001"));
        assertTrue(graph.containsVertex("F002"));
        assertEquals(2, graph.vertexCount());
        assertTrue(graph.containsEdge("F001", "F002"));
        assertEquals(5.0, graph.weightOf("F001", "F002"));
        assertEquals(1, graph.edgeCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void edgeIsDirected(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F002", 5.0));

        assertTrue(graph.containsEdge("F001", "F002"));
        assertFalse(graph.containsEdge("F002", "F001"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void addingASecondEdgeBetweenTheSamePairOverwritesTheFirst(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F002", 5.0));
        graph.addEdge(new Edge("F001", "F002", 9.0));

        assertEquals(9.0, graph.weightOf("F001", "F002"));
        assertEquals(1, graph.edgeCount()); // overwrite, not a second edge
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void removeEdgeDropsOnlyThatDirectedEdge(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F002", 5.0));
        graph.addEdge(new Edge("F002", "F001", 5.0));

        graph.removeEdge("F001", "F002");

        assertFalse(graph.containsEdge("F001", "F002"));
        assertTrue(graph.containsEdge("F002", "F001"));
        assertEquals(1, graph.edgeCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void removingAnEdgeThatDoesNotExistIsANoOp(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("F001");
        graph.addVertex("F002");

        graph.removeEdge("F001", "F002"); // never added
        graph.removeEdge("F999", "F998"); // neither vertex exists

        assertEquals(0, graph.edgeCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void edgesFromReturnsExactlyTheOutgoingEdges(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F002", 1.0));
        graph.addEdge(new Edge("F001", "F003", 2.0));
        graph.addEdge(new Edge("F002", "F003", 3.0)); // not from F001

        int count = 0;
        boolean sawF002 = false;
        boolean sawF003 = false;
        MyIterator<Edge> it = graph.edgesFrom("F001").iterator();
        while (it.hasNext()) {
            Edge edge = it.next();
            assertEquals("F001", edge.fromId());
            if (edge.toId().equals("F002")) {
                sawF002 = true;
            }
            if (edge.toId().equals("F003")) {
                sawF003 = true;
            }
            count++;
        }
        assertEquals(2, count);
        assertTrue(sawF002);
        assertTrue(sawF003);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void edgesFromAVertexWithNoOutgoingEdgesIsEmpty(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("F001");

        assertFalse(graph.edgesFrom("F001").iterator().hasNext());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void growsPastInitialCapacity(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get(); // default capacity is small (8)
        for (int i = 0; i < 50; i++) {
            graph.addVertex("F" + i);
        }
        for (int i = 0; i < 49; i++) {
            graph.addEdge(new Edge("F" + i, "F" + (i + 1), 1.0));
        }

        assertEquals(50, graph.vertexCount());
        assertEquals(49, graph.edgeCount());
        assertTrue(graph.containsEdge("F0", "F1"));
        assertTrue(graph.containsEdge("F48", "F49"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void selfLoopIsAllowed(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F001", 0.0));

        assertTrue(graph.containsEdge("F001", "F001"));
        assertEquals(1, graph.vertexCount());
        assertEquals(1, graph.edgeCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void addVertexRejectsNull(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        assertThrows(IllegalArgumentException.class, () -> graph.addVertex(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void addEdgeRejectsNull(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void weightOfUnknownEdgeThrows(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("F001");
        graph.addVertex("F002");
        assertThrows(KeyNotFoundException.class, () -> graph.weightOf("F001", "F002"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void weightOfUnknownVertexThrows(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        assertThrows(KeyNotFoundException.class, () -> graph.weightOf("F001", "F002"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void edgesFromUnknownVertexThrows(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        assertThrows(KeyNotFoundException.class, () -> graph.edgesFrom("F999"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void resetCountersZeroesBoth(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("F001", "F002", 1.0));
        graph.containsEdge("F001", "F002");

        assertTrue(graph.comparisonCount() > 0 || graph.movementCount() > 0);
        graph.resetCounters();

        assertEquals(0, graph.comparisonCount());
        assertEquals(0, graph.movementCount());
    }
}
