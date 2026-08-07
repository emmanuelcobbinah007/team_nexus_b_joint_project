package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.KeyNotFoundException;
import org.junit.jupiter.api.Test;

class FacilityDisjointSetTest {

    @Test
    void newElementsStartInTheirOwnSet() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        ds.makeSet("F001");
        ds.makeSet("F002");

        assertEquals("F001", ds.find("F001"));
        assertEquals(2, ds.setCount());
    }

    @Test
    void unionMergesTwoSetsAndReturnsTrue() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        ds.makeSet("F001");
        ds.makeSet("F002");

        assertTrue(ds.union("F001", "F002"));
        assertTrue(ds.connected("F001", "F002"));
        assertEquals(1, ds.setCount());
    }

    @Test
    void unionOnAlreadyConnectedElementsReturnsFalse() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        ds.makeSet("F001");
        ds.makeSet("F002");
        ds.union("F001", "F002");

        assertFalse(ds.union("F001", "F002"));
        assertEquals(1, ds.setCount());
    }

    @Test
    void connectsAcrossAChainOfUnions() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        for (String id : new String[] {"F001", "F002", "F003", "F004"}) {
            ds.makeSet(id);
        }
        ds.union("F001", "F002");
        ds.union("F002", "F003");

        assertTrue(ds.connected("F001", "F003"));
        assertFalse(ds.connected("F001", "F004"));
        assertEquals(2, ds.setCount()); // {F001,F002,F003}, {F004}
    }

    @Test
    void makeSetOnAnExistingIdIsANoOp() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        ds.makeSet("F001");
        ds.makeSet("F002");
        ds.union("F001", "F002");

        ds.makeSet("F001"); // re-registering an existing id must not create a new set

        assertEquals(1, ds.setCount());
    }

    @Test
    void unionByRankAndPathCompressionKeepDepthSmall() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        String[] ids = new String[16];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = "F" + i;
            ds.makeSet(ids[i]);
        }
        for (int i = 0; i + 1 < ids.length; i += 2) {
            ds.union(ids[i], ids[i + 1]);
        }
        for (int i = 0; i + 3 < ids.length; i += 4) {
            ds.union(ids[i], ids[i + 2]);
        }
        for (String id : ids) {
            ds.find(id); // trigger path compression on every chain
        }

        assertTrue(ds.maxDepth() <= 2, "path compression should flatten chains to near-constant depth");
    }

    @Test
    void growsPastInitialCapacity() {
        FacilityDisjointSet ds = new FacilityDisjointSet(2);
        for (int i = 0; i < 10; i++) {
            ds.makeSet("F" + i);
        }

        assertEquals(10, ds.setCount());
        for (int i = 1; i < 10; i++) {
            ds.union("F0", "F" + i);
        }
        assertEquals(1, ds.setCount());
    }

    @Test
    void findOnUnregisteredIdThrows() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        assertThrows(KeyNotFoundException.class, () -> ds.find("F999"));
    }

    @Test
    void unionOnUnregisteredIdThrows() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        ds.makeSet("F001");
        assertThrows(KeyNotFoundException.class, () -> ds.union("F001", "F999"));
    }

    @Test
    void nullElementIdThrows() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        assertThrows(IllegalArgumentException.class, () -> ds.makeSet(null));
    }

    @Test
    void negativeInitialCapacityThrows() {
        assertThrows(IllegalArgumentException.class, () -> new FacilityDisjointSet(-1));
    }

    @Test
    void emptySetHasZeroSetCountAndZeroMaxDepth() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        assertEquals(0, ds.setCount());
        assertEquals(0, ds.maxDepth());
    }

    @Test
    void resetCountersZeroesBothCounters() {
        FacilityDisjointSet ds = new FacilityDisjointSet();
        ds.makeSet("F001");
        ds.makeSet("F002");
        ds.union("F001", "F002");

        assertTrue(ds.comparisonCount() > 0);
        ds.resetCounters();

        assertEquals(0, ds.comparisonCount());
        assertEquals(0, ds.movementCount());
    }
}
