package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.KeyNotFoundException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Correctness cases for {@link Dijkstra}, parameterized across both {@link
 * MyGraph} representations — the same shortest-path answer must come out
 * regardless of which one built the graph.
 */
class DijkstraTest {

    static Stream<Arguments> implementations() {
        return Stream.of(
            Arguments.of("ADJACENCY_LIST", (Supplier<MyGraph>) AdjacencyListGraph::new),
            Arguments.of("ADJACENCY_MATRIX", (Supplier<MyGraph>) AdjacencyMatrixGraph::new)
        );
    }

    // Textbook example (CLRS-style): shortest distances from A are
    // A=0, B=4 (A->C->B, not the direct A->B=8), C=2, D=9 (A->C->B->D), E=12 (...->D->E)
    private static MyGraph classicExample(Supplier<MyGraph> factory) {
        MyGraph g = factory.get();
        g.addEdge(new Edge("A", "B", 8));
        g.addEdge(new Edge("A", "C", 2));
        g.addEdge(new Edge("C", "B", 2));
        g.addEdge(new Edge("B", "D", 5));
        g.addEdge(new Edge("D", "E", 3));
        g.addEdge(new Edge("C", "D", 11));
        return g;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void findsShortestDistancesOnAClassicExample(String name, Supplier<MyGraph> factory) {
        MyGraph graph = classicExample(factory);

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        assertEquals(0.0, result.distanceTo("A"));
        assertEquals(2.0, result.distanceTo("C"));
        assertEquals(4.0, result.distanceTo("B")); // via C, not the direct 8
        assertEquals(9.0, result.distanceTo("D")); // via C->B->D (2+2+5), not C->D (2+11)
        assertEquals(12.0, result.distanceTo("E"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void reconstructsThePathViaThePredecessorChain(String name, Supplier<MyGraph> factory) {
        MyGraph graph = classicExample(factory);

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        assertArrayEquals(new String[] {"A", "C", "B", "D"}, result.pathTo("D"));
        assertEquals("C", result.predecessorOf("B"));
        assertEquals("A", result.predecessorOf("C"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void decreaseKeyProducesTheShorterRouteFoundLater(String name, Supplier<MyGraph> factory) {
        // B is first discovered via the direct A->B edge (weight 10). Only
        // after A->C and C->B are relaxed does a shorter A->C->B route
        // (2+2=4) appear — B must already be in the queue at that point,
        // so this only comes out right if decreaseKey() actually re-sifts it.
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 10));
        graph.addEdge(new Edge("A", "C", 2));
        graph.addEdge(new Edge("C", "B", 2));

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        assertEquals(4.0, result.distanceTo("B"));
        assertEquals("C", result.predecessorOf("B"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void edgeDirectionIsRespected(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 1.0)); // one-way only

        PathResult result = Dijkstra.shortestPaths(graph, "B");

        assertTrue(result.isReachable("B"));
        assertFalse(result.isReachable("A")); // no B->A edge
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void singleVertexNoEdges(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("A");

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        assertEquals(0.0, result.distanceTo("A"));
        assertArrayEquals(new String[] {"A"}, result.pathTo("A"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void unreachableVertexIsReportedAsUnreachable(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 1.0));
        graph.addVertex("Z"); // isolated, no edges at all

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        assertFalse(result.isReachable("Z"));
        assertEquals(0, result.pathTo("Z").length);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void selfLoopDoesNotBreakAnything(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "A", 0.0));
        graph.addEdge(new Edge("A", "B", 3.0));

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        assertEquals(0.0, result.distanceTo("A"));
        assertEquals(3.0, result.distanceTo("B"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void visitOrderStartsAtSourceAndIsNonDecreasingByDistance(String name, Supplier<MyGraph> factory) {
        MyGraph graph = classicExample(factory);

        PathResult result = Dijkstra.shortestPaths(graph, "A");

        String[] order = new String[5];
        int i = 0;
        var it = result.visitOrder().iterator();
        while (it.hasNext()) {
            order[i] = it.next();
            i++;
        }
        assertEquals(5, i);
        assertEquals("A", order[0]);
        for (int j = 1; j < i; j++) {
            assertTrue(result.distanceTo(order[j - 1]) <= result.distanceTo(order[j]),
                "visit order should be non-decreasing by finalized distance");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void sourceNotInGraphThrows(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("A");

        assertThrows(KeyNotFoundException.class, () -> Dijkstra.shortestPaths(graph, "Z"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void nullSourceThrows(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("A");

        assertThrows(IllegalArgumentException.class, () -> Dijkstra.shortestPaths(graph, null));
    }

    @Test
    void nullGraphThrows() {
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.shortestPaths(null, "A"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void bothRepresentationsAgreeOnTheClassicExample(String name, Supplier<MyGraph> factory) {
        // Redundant with findsShortestDistancesOnAClassicExample by design:
        // cross-checks this representation's result against the other's.
        MyGraph thisRepresentation = classicExample(factory);
        MyGraph otherRepresentation = classicExample(
            factory.get() instanceof AdjacencyListGraph
                ? (Supplier<MyGraph>) AdjacencyMatrixGraph::new
                : (Supplier<MyGraph>) AdjacencyListGraph::new);

        PathResult a = Dijkstra.shortestPaths(thisRepresentation, "A");
        PathResult b = Dijkstra.shortestPaths(otherRepresentation, "A");

        for (String id : new String[] {"A", "B", "C", "D", "E"}) {
            assertEquals(a.distanceTo(id), b.distanceTo(id), 1e-9, "distance to " + id + " disagreed");
        }
    }
}
