package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link DisjointSet} is Kruskal's actual union-find dependency
 * ({@link Kruskal#run} constructs one directly), so these checks matter
 * beyond {@link FacilityDisjointSetTest}, which covers the newer id-based
 * wrapper rather than this index-based implementation itself.
 */
class DisjointSetTest {

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void initialStateHasEachElementInItsOwnSet() {
        DisjointSet ds = new DisjointSet(5);
        for (int i = 0; i < 5; i++) {
            assertEquals(i, ds.find(i));
        }
        assertEquals(5, ds.setCount());
    }

    @Test
    void unionMergesTwoSetsAndReturnsTrue() {
        DisjointSet ds = new DisjointSet(5);
        assertTrue(ds.union(0, 1));
        assertTrue(ds.connected(0, 1));
        assertEquals(4, ds.setCount());
    }

    @Test
    void unionOnAlreadyConnectedElementsReturnsFalse() {
        DisjointSet ds = new DisjointSet(5);
        ds.union(0, 1);
        assertFalse(ds.union(0, 1));
        assertEquals(4, ds.setCount());
    }

    @Test
    void unionChainConnectsAllElementsInTheChain() {
        DisjointSet ds = new DisjointSet(6);
        ds.union(0, 1);
        ds.union(1, 2);
        ds.union(2, 3);
        assertTrue(ds.connected(0, 3));
        assertFalse(ds.connected(0, 4));
        assertEquals(3, ds.setCount()); // {0,1,2,3}, {4}, {5}
    }

    @Test
    void disjointGroupsStayDisjointUntilUnioned() {
        DisjointSet ds = new DisjointSet(6);
        ds.union(0, 1);
        ds.union(2, 3);
        ds.union(4, 5);
        assertTrue(ds.connected(0, 1));
        assertTrue(ds.connected(2, 3));
        assertTrue(ds.connected(4, 5));
        assertFalse(ds.connected(0, 2));
        assertFalse(ds.connected(2, 4));
        assertEquals(3, ds.setCount());
    }

    @Test
    void unioningEveryElementCollapsesToASingleSet() {
        int n = 10;
        DisjointSet ds = new DisjointSet(n);
        for (int i = 0; i < n - 1; i++) {
            ds.union(i, i + 1);
        }
        assertEquals(1, ds.setCount());
        for (int i = 0; i < n; i++) {
            assertTrue(ds.connected(0, i));
        }
    }

    @Test
    void pathCompressionFlattensTheTree() {
        DisjointSet ds = new DisjointSet(5);
        ds.union(0, 1);
        ds.union(1, 2);
        ds.union(2, 3);
        ds.union(3, 4);
        int root = ds.find(4);
        for (int i = 0; i < 5; i++) {
            assertEquals(root, ds.parentOf(i));
        }
    }

    @Test
    void unionByRankKeepsTheTreeShallow() {
        DisjointSet ds = new DisjointSet(8);
        ds.union(0, 1);
        ds.union(2, 3);
        ds.union(4, 5);
        ds.union(6, 7);
        ds.union(0, 2);
        ds.union(4, 6);
        ds.union(0, 4);
        assertEquals(1, ds.setCount());
        int root = ds.find(0);
        assertEquals(3, ds.rankOf(root));
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void singleElementIsTriviallyConnectedToItself() {
        DisjointSet ds = new DisjointSet(1);
        assertTrue(ds.connected(0, 0));
        assertEquals(1, ds.setCount());
    }

    @Test
    void zeroElementsHasZeroSetCount() {
        DisjointSet ds = new DisjointSet(0);
        assertEquals(0, ds.setCount());
        assertThrows(IndexOutOfBoundsException.class, () -> ds.find(0));
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void outOfRangeElementThrows() {
        DisjointSet ds = new DisjointSet(3);
        assertThrows(IndexOutOfBoundsException.class, () -> ds.find(5));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.union(0, 10));
    }

    @Test
    void negativeNThrows() {
        assertThrows(IllegalArgumentException.class, () -> new DisjointSet(-1));
    }
}
