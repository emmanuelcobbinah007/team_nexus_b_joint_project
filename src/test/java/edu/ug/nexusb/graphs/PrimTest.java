package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.KeyNotFoundException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PrimTest {

    static Stream<Arguments> implementations() {
        return Stream.of(
                Arguments.of(
                        "ADJACENCY_LIST",
                        (Supplier<MyGraph>) AdjacencyListGraph::new),
                Arguments.of(
                        "ADJACENCY_MATRIX",
                        (Supplier<MyGraph>) AdjacencyMatrixGraph::new)
        );
    }

    // ---------- NORMAL CASES ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void findsMinimumSpanningTreeAndTotalWeight(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = connectedExample(factory);

        MstResult result =
                Prim.minimumSpanningTree(graph, "A");

        assertEquals(4, result.vertexCount());
        assertEquals(3, result.edgeCount());
        assertEquals(4.0, result.totalWeight(), 1e-9);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void totalWeightMatchesKruskalOnTheSameGraph(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = connectedExample(factory);

        MstResult prim =
                Prim.minimumSpanningTree(graph, "A");

        Kruskal.Edge[] edges = {
                new Kruskal.Edge(0, 1, 1),
                new Kruskal.Edge(0, 2, 4),
                new Kruskal.Edge(1, 2, 2),
                new Kruskal.Edge(1, 3, 5),
                new Kruskal.Edge(2, 3, 1)
        };

        Kruskal.Result kruskal =
                Kruskal.run(4, edges);

        assertTrue(kruskal.isSpanning);
        assertEquals(
                kruskal.totalWeight,
                prim.totalWeight(),
                1e-9);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void differentStartVertexKeepsSameMinimumTotal(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = connectedExample(factory);

        MstResult fromA =
                Prim.minimumSpanningTree(graph, "A");
        MstResult fromD =
                Prim.minimumSpanningTree(graph, "D");

        assertEquals(
                fromA.totalWeight(),
                fromD.totalWeight(),
                1e-9);
        assertEquals(3, fromD.edgeCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void treatsRoadLinksAsUndirectedConnections(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = factory.get();

        // The stored direction is B to A, but an MST treats
        // the road as a connection between both endpoints.
        graph.addEdge(new Edge("B", "A", 2.0));

        MstResult result =
                Prim.minimumSpanningTree(graph, "A");

        assertEquals(1, result.edgeCount());
        assertEquals(2.0, result.totalWeight(), 1e-9);
        assertEquals(
                new Edge("A", "B", 2.0),
                result.edges()[0]);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void expensiveAndSelfLoopEdgesAreNotSelected(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = connectedExample(factory);
        graph.addEdge(new Edge("A", "A", 0.0));

        MstResult result =
                Prim.minimumSpanningTree(graph, "A");

        assertEquals(4.0, result.totalWeight(), 1e-9);

        for (Edge edge : result.edges()) {
            assertNotEquals(
                    edge.fromId(),
                    edge.toId());
        }
    }

    // ---------- BOUNDARY CASES ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void singleVertexProducesEmptyZeroWeightTree(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = factory.get();
        graph.addVertex("A");

        MstResult result =
                Prim.minimumSpanningTree(graph, "A");

        assertEquals(1, result.vertexCount());
        assertEquals(0, result.edgeCount());
        assertEquals(0.0, result.totalWeight(), 1e-9);
    }

    @Test
    void resultReturnsDefensiveCopyOfEdges() {
        MstResult result =
                Prim.minimumSpanningTree(
                        connectedExample(
                                AdjacencyListGraph::new),
                        "A");

        Edge originalFirst = result.edges()[0];
        Edge[] changedCopy = result.edges();

        changedCopy[0] =
                new Edge("X", "Y", 99.0);

        assertEquals(
                originalFirst,
                result.edges()[0]);
    }

    // ---------- INVALID CASES ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void disconnectedGraphThrows(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = factory.get();
        addUndirectedEdge(graph, "A", "B", 1.0);
        graph.addVertex("Z");

        assertThrows(
                IllegalStateException.class,
                () -> Prim.minimumSpanningTree(
                        graph,
                        "A"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void missingStartVertexThrows(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = factory.get();
        graph.addVertex("A");

        assertThrows(
                KeyNotFoundException.class,
                () -> Prim.minimumSpanningTree(
                        graph,
                        "Z"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void nullStartVertexThrows(
            String name,
            Supplier<MyGraph> factory) {

        MyGraph graph = factory.get();
        graph.addVertex("A");

        assertThrows(
                IllegalArgumentException.class,
                () -> Prim.minimumSpanningTree(
                        graph,
                        null));
    }

    @Test
    void nullGraphThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Prim.minimumSpanningTree(
                        null,
                        "A"));
    }

    private static MyGraph connectedExample(
            Supplier<MyGraph> factory) {

        MyGraph graph = factory.get();

        addUndirectedEdge(graph, "A", "B", 1.0);
        addUndirectedEdge(graph, "A", "C", 4.0);
        addUndirectedEdge(graph, "B", "C", 2.0);
        addUndirectedEdge(graph, "B", "D", 5.0);
        addUndirectedEdge(graph, "C", "D", 1.0);

        return graph;
    }

    private static void addUndirectedEdge(
            MyGraph graph,
            String first,
            String second,
            double weight) {

        graph.addEdge(
                new Edge(first, second, weight));
        graph.addEdge(
                new Edge(second, first, weight));
    }
}