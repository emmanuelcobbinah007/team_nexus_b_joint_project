package edu.ug.nexusb.graphs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KruskalTest {

    private Kruskal.Edge e(int src, int dest, int weight) {
        return new Kruskal.Edge(src, dest, weight);
    }

    @Test
    void classicSixVertexGraph_producesCorrectMstWeight() {
        // Standard textbook example: MST weight should be 15
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(0, 1, 4));
        edges.add(e(0, 2, 4));
        edges.add(e(1, 2, 2));
        edges.add(e(2, 3, 3));
        edges.add(e(2, 5, 2));
        edges.add(e(2, 4, 4));
        edges.add(e(3, 4, 3));
        edges.add(e(5, 4, 3));

        Kruskal.Result result = Kruskal.run(6, edges);

        assertTrue(result.isSpanning);
        assertEquals(5, result.mstEdges.size()); // n - 1 edges for 6 vertices
        assertEquals(15, result.totalWeight);
    }

    @Test
    void mstUsesExactlyNMinusOneEdges_forConnectedGraph() {
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(0, 1, 1));
        edges.add(e(1, 2, 2));
        edges.add(e(2, 3, 3));
        edges.add(e(3, 0, 4)); // creates a cycle — should be excluded

        Kruskal.Result result = Kruskal.run(4, edges);

        assertEquals(3, result.mstEdges.size());
        assertEquals(6, result.totalWeight); // 1 + 2 + 3
        assertTrue(result.isSpanning);
    }

    @Test
    void disconnectedGraph_isNotSpanning_andReturnsPartialForest() {
        // Two separate components: {0,1,2} and {3,4}
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(0, 1, 1));
        edges.add(e(1, 2, 2));
        edges.add(e(3, 4, 5));

        Kruskal.Result result = Kruskal.run(5, edges);

        assertFalse(result.isSpanning);
        assertEquals(3, result.mstEdges.size()); // 2 edges + 1 edge, no edge connects the two groups
        assertEquals(8, result.totalWeight);
    }

    @Test
    void singleVertex_noEdges_isTriviallySpanning() {
        Kruskal.Result result = Kruskal.run(1, new ArrayList<>());

        assertTrue(result.isSpanning);
        assertTrue(result.mstEdges.isEmpty());
        assertEquals(0, result.totalWeight);
    }

    @Test
    void twoVertices_noEdges_isNotSpanning() {
        Kruskal.Result result = Kruskal.run(2, new ArrayList<>());

        assertFalse(result.isSpanning);
        assertTrue(result.mstEdges.isEmpty());
    }

    @Test
    void duplicateAndParallelEdges_cheaperParallelEdgeIsChosen() {
        // Two parallel edges between 0 and 1: weight 10 and weight 2.
        // Kruskal should pick the cheaper one and skip the other as a cycle.
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(0, 1, 10));
        edges.add(e(0, 1, 2));
        edges.add(e(1, 2, 1));

        Kruskal.Result result = Kruskal.run(3, edges);

        assertEquals(2, result.mstEdges.size());
        assertEquals(3, result.totalWeight); // 2 + 1, not 10 + 1
        assertTrue(result.isSpanning);
    }

    @Test
    void selfLoop_isNeverIncludedInMst() {
        // A self-loop (0,0) should never merge anything and should be excluded.
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(0, 0, 1)); // self-loop, weight artificially low to tempt a buggy implementation
        edges.add(e(0, 1, 5));

        Kruskal.Result result = Kruskal.run(2, edges);

        assertEquals(1, result.mstEdges.size());
        assertEquals(5, result.totalWeight);
        assertTrue(result.isSpanning);
    }

    @Test
    void edgesAlreadySortedDescending_stillProducesCorrectMst() {
        // Verifies Kruskal sorts internally rather than relying on input order.
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(2, 3, 3));
        edges.add(e(1, 2, 2));
        edges.add(e(0, 1, 1));

        Kruskal.Result result = Kruskal.run(4, edges);

        assertEquals(3, result.mstEdges.size());
        assertEquals(6, result.totalWeight);
    }

    @Test
    void inputEdgeListIsNotMutated() {
        List<Kruskal.Edge> edges = new ArrayList<>();
        edges.add(e(2, 3, 3));
        edges.add(e(1, 2, 2));
        edges.add(e(0, 1, 1));

        List<Kruskal.Edge> originalOrder = new ArrayList<>(edges);
        Kruskal.run(4, edges);


        assertEquals(originalOrder, edges);
    }
}
