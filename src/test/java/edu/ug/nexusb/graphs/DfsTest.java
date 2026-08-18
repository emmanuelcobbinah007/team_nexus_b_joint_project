package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.MyIterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Correctness cases for {@link Dfs}, parameterized across both {@link
 * MyGraph} representations, mirroring {@code DijkstraTest}/{@code
 * MyGraphContractTest}'s approach elsewhere in this package.
 */
class DfsTest {

    static Stream<Arguments> implementations() {
        return Stream.of(
            Arguments.of("ADJACENCY_LIST", (Supplier<MyGraph>) AdjacencyListGraph::new),
            Arguments.of("ADJACENCY_MATRIX", (Supplier<MyGraph>) AdjacencyMatrixGraph::new)
        );
    }

    // ---- normal case ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void aDagReportsNoCycleAndVisitsEveryVertex(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 1));
        graph.addEdge(new Edge("B", "C", 1));

        Dfs.Result result = Dfs.traverse(graph);

        assertFalse(result.hasCycle());
        assertNull(result.cycleFromId());
        assertNull(result.cycleToId());
        assertEquals(3, countVisited(result));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void detectsAThreeVertexReferralLoop(String name, Supplier<MyGraph> factory) {
        // The exact scenario named in the project brief: A refers to B,
        // B refers to C, C refers back to A.
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 1));
        graph.addEdge(new Edge("B", "C", 1));
        graph.addEdge(new Edge("C", "A", 1));

        Dfs.Result result = Dfs.traverse(graph);

        assertTrue(result.hasCycle());
        // The back edge closes the loop: its destination must be an
        // ancestor of its source somewhere on the same A-B-C chain.
        assertTrue(isPartOfTheLoop(result.cycleFromId()) && isPartOfTheLoop(result.cycleToId()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void diamondShapedDagIsNotAFalseCycle(String name, Supplier<MyGraph> factory) {
        // A -> B -> D and A -> C -> D: D is reached twice, by two different
        // paths, but never revisited while still an open ancestor. A naive
        // visited-only check would wrongly flag this as a cycle.
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 1));
        graph.addEdge(new Edge("A", "C", 1));
        graph.addEdge(new Edge("B", "D", 1));
        graph.addEdge(new Edge("C", "D", 1));

        Dfs.Result result = Dfs.traverse(graph);

        assertFalse(result.hasCycle(), "reaching D via two different paths is not a cycle");
        assertEquals(4, countVisited(result));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void selfLoopIsDetectedAsACycle(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "A", 0));

        Dfs.Result result = Dfs.traverse(graph);

        assertTrue(result.hasCycle());
        assertEquals("A", result.cycleFromId());
        assertEquals("A", result.cycleToId());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void visitOrderContainsEveryVertexExactlyOnce(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addEdge(new Edge("A", "B", 1));
        graph.addEdge(new Edge("A", "C", 1));
        graph.addVertex("D"); // isolated vertex, no edges at all

        Dfs.Result result = Dfs.traverse(graph);

        boolean sawA = false;
        boolean sawB = false;
        boolean sawC = false;
        boolean sawD = false;
        int count = 0;
        MyIterator<String> it = result.visitOrder().iterator();
        while (it.hasNext()) {
            String v = it.next();
            count++;
            sawA |= v.equals("A");
            sawB |= v.equals("B");
            sawC |= v.equals("C");
            sawD |= v.equals("D");
        }
        assertEquals(4, count);
        assertTrue(sawA && sawB && sawC && sawD);
    }

    // ---- boundary case ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void emptyGraphHasNoCycleAndEmptyVisitOrder(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();

        Dfs.Result result = Dfs.traverse(graph);

        assertFalse(result.hasCycle());
        assertFalse(result.visitOrder().iterator().hasNext());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void singleVertexNoEdgesHasNoCycle(String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        graph.addVertex("A");

        Dfs.Result result = Dfs.traverse(graph);

        assertFalse(result.hasCycle());
        assertEquals(1, countVisited(result));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void disconnectedGraphStillVisitsEveryComponentAndFindsACycleInEither(
            String name, Supplier<MyGraph> factory) {
        MyGraph graph = factory.get();
        // Component 1: a cycle.
        graph.addEdge(new Edge("A", "B", 1));
        graph.addEdge(new Edge("B", "A", 1));
        // Component 2: a separate, acyclic chain.
        graph.addEdge(new Edge("X", "Y", 1));

        Dfs.Result result = Dfs.traverse(graph);

        assertTrue(result.hasCycle(), "the cycle in the first component must still be found");
        assertEquals(4, countVisited(result), "both components must be fully visited");
    }

    // ---- invalid input ----

    @Test
    void nullGraphThrows() {
        assertThrows(IllegalArgumentException.class, () -> Dfs.traverse(null));
    }

    private static boolean isPartOfTheLoop(String vertexId) {
        return "A".equals(vertexId) || "B".equals(vertexId) || "C".equals(vertexId);
    }

    private static int countVisited(Dfs.Result result) {
        int count = 0;
        MyIterator<String> it = result.visitOrder().iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }
}
