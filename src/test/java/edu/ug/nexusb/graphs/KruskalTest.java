package edu.ug.nexusb.graphs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KruskalTest {

    private Kruskal.Edge e(int src, int dest, int weight) {
        return new Kruskal.Edge(src, dest, weight);
    }

    // ---- Normal cases ----

    @Test
    void classicSixVertexGraph_producesCorrectMstWeight() {
        Kruskal.Edge[] edges = {
                e(0, 1, 4), e(0, 2, 4), e(1, 2, 2), e(2, 3, 3),
                e(2, 5, 2), e(2, 4, 4), e(3, 4, 3), e(5, 4, 3)
        };

        Kruskal.Result result = Kruskal.run(6, edges);

        assertTrue(result.isSpanning);
        assertEquals(5, result.mstEdges.length);
        assertEquals(14, result.totalWeight);
    }

    @Test
    void mstUsesExactlyNMinusOneEdges_forConnectedGraph() {
        Kruskal.Edge[] edges = {
                e(0, 1, 1), e(1, 2, 2), e(2, 3, 3), e(3, 0, 4) // last edge creates a cycle
        };

        Kruskal.Result result = Kruskal.run(4, edges);

        assertEquals(3, result.mstEdges.length);
        assertEquals(6, result.totalWeight);
        assertTrue(result.isSpanning);
    }

    @Test
    void duplicateAndParallelEdges_cheaperParallelEdgeIsChosen() {
        Kruskal.Edge[] edges = { e(0, 1, 10), e(0, 1, 2), e(1, 2, 1) };

        Kruskal.Result result = Kruskal.run(3, edges);

        assertEquals(2, result.mstEdges.length);
        assertEquals(3, result.totalWeight); // 2 + 1, not 10 + 1
        assertTrue(result.isSpanning);
    }

    @Test
    void edgesAlreadySortedDescending_stillProducesCorrectMst() {
        // Verifies merge sort works regardless of input order.
        Kruskal.Edge[] edges = { e(2, 3, 3), e(1, 2, 2), e(0, 1, 1) };

        Kruskal.Result result = Kruskal.run(4, edges);

        assertEquals(3, result.mstEdges.length);
        assertEquals(6, result.totalWeight);
    }

    @Test
    void inputEdgeArrayIsNotMutated() {
        Kruskal.Edge[] edges = { e(2, 3, 3), e(1, 2, 2), e(0, 1, 1) };
        Kruskal.Edge[] originalOrder = edges.clone();

        Kruskal.run(4, edges);

        // Kruskal.run should sort a copy, not the caller's array.
        assertArrayEquals(originalOrder, edges);
    }

    // ---- Boundary cases ----

    @Test
    void singleVertex_noEdges_isTriviallySpanning() {
        Kruskal.Result result = Kruskal.run(1, new Kruskal.Edge[0]);

        assertTrue(result.isSpanning);
        assertEquals(0, result.mstEdges.length);
        assertEquals(0, result.totalWeight);
    }

    @Test
    void zeroVertices_noEdges_isNotSpanning() {
        // setCount() returns 0 for an empty graph, which != 1, so isSpanning is false.
        // This is a deliberate edge case worth confirming against the spec: some
        // definitions treat the empty graph as trivially spanning.
        Kruskal.Result result = Kruskal.run(0, new Kruskal.Edge[0]);

        assertFalse(result.isSpanning);
        assertEquals(0, result.mstEdges.length);
    }

    @Test
    void twoVertices_noEdges_isNotSpanning() {
        Kruskal.Result result = Kruskal.run(2, new Kruskal.Edge[0]);

        assertFalse(result.isSpanning);
        assertEquals(0, result.mstEdges.length);
    }

    @Test
    void disconnectedGraph_isNotSpanning_andReturnsPartialForest() {
        // Two separate components: {0,1,2} and {3,4}
        Kruskal.Edge[] edges = { e(0, 1, 1), e(1, 2, 2), e(3, 4, 5) };

        Kruskal.Result result = Kruskal.run(5, edges);

        assertFalse(result.isSpanning);
        assertEquals(3, result.mstEdges.length);
        assertEquals(8, result.totalWeight);
    }

    @Test
    void selfLoop_isNeverIncludedInMst() {
        Kruskal.Edge[] edges = { e(0, 0, 1), e(0, 1, 5) };

        Kruskal.Result result = Kruskal.run(2, edges);

        assertEquals(1, result.mstEdges.length);
        assertEquals(5, result.totalWeight);
        assertTrue(result.isSpanning);
    }

    // ---- Invalid input cases ----
    // NOTE: [Guessing] behavior on invalid input (throw vs. skip) was not confirmed
    // against the assignment spec. Update these tests if the rubric specifies
    // different handling.

    @Test
    void negativeNumVertices_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Kruskal.run(-1, new Kruskal.Edge[0]));
    }

    @Test
    void nullEdgesArray_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Kruskal.run(4, null));
    }

    @Test
    void nullElementInEdgesArray_throwsIllegalArgumentException() {
        Kruskal.Edge[] edges = { e(0, 1, 1), null };

        assertThrows(IllegalArgumentException.class,
                () -> Kruskal.run(4, edges));
    }

    @Test
    void edgeWithNegativeVertexIndex_throwsIllegalArgumentException() {
        Kruskal.Edge[] edges = { e(-1, 1, 1) };

        assertThrows(IllegalArgumentException.class,
                () -> Kruskal.run(4, edges));
    }

    @Test
    void edgeWithVertexIndexAtOrAboveNumVertices_throwsIllegalArgumentException() {
        Kruskal.Edge[] edges = { e(0, 4, 1) }; // valid range is [0, 4) for numVertices=4

        assertThrows(IllegalArgumentException.class,
                () -> Kruskal.run(4, edges));
    }
}