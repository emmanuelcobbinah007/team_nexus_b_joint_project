package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainedHashTableTest {

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void putThenGetReturnsStoredValue() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        table.put("korle-bu", 1);
        table.put("ridge", 2);
        table.put("achimota", 3);

        assertEquals(1, table.get("korle-bu"));
        assertEquals(2, table.get("ridge"));
        assertEquals(3, table.get("achimota"));
        assertEquals(3, table.size());
    }

    @Test
    void putOnExistingKeyUpdatesAndReturnsPreviousValue() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        table.put("F001", 1);
        Integer previous = table.put("F001", 100);

        assertEquals(1, previous);
        assertEquals(100, table.get("F001"));
        assertEquals(1, table.size());
    }

    @Test
    void removeExistingKeyReturnsValueAndShrinksSize() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        table.put("F001", 1);
        table.put("F002", 2);

        Integer removed = table.remove("F001");

        assertEquals(1, removed);
        assertEquals(1, table.size());
        assertNull(table.get("F001"));
        assertEquals(2, table.get("F002"));
    }

    @Test
    void entriesReturnsEveryStoredMapping() {
        ChainedHashTable<Integer, String> table = new ChainedHashTable<>();
        for (int k = 0; k < 30; k++) {
            table.put(k, "v" + k);
        }

        MyIterator<MyMap.MapEntry<Integer, String>> it = table.entries().iterator();
        boolean[] seen = new boolean[30];
        int count = 0;
        while (it.hasNext()) {
            MyMap.MapEntry<Integer, String> entry = it.next();
            seen[entry.getKey()] = true;
            assertEquals("v" + entry.getKey(), entry.getValue());
            count++;
        }
        assertEquals(30, count);
        for (boolean b : seen) {
            assertTrue(b);
        }
    }

    // ------------------------------------------------------------------
    // Collision / resize (the whole point of MyHashTable's extra methods)
    // ------------------------------------------------------------------

    @Test
    void collidingKeysLandInTheSameBucketAsAChain() {
        // Capacity 50, keys 5/55/105 all hash to bucket 5 (5 mod 50) - forced
        // collisions without approaching the 0.75 load-factor resize
        // threshold (3/50 = 0.06), so the bucket count stays fixed.
        ChainedHashTable<Integer, String> table = new ChainedHashTable<>(50);
        table.put(5, "a");
        table.put(55, "b");
        table.put(105, "c");

        assertEquals(2, table.collisionCount(), "3 inserts into 1 bucket = 2 collisions");
        assertEquals(3, table.longestBucket());
        assertEquals("a", table.get(5));
        assertEquals("b", table.get(55));
        assertEquals("c", table.get(105));
    }

    @Test
    void exceedingLoadFactorTriggersAResizeThatGrowsCapacity() {
        ChainedHashTable<Integer, String> table = new ChainedHashTable<>(7);
        int initialCapacity = table.capacity();

        for (int k = 0; k < 10; k++) {
            table.put(k, "v" + k);
        }

        assertTrue(table.resizeCount() >= 1, "load factor 10/7 must have triggered at least one resize");
        assertTrue(table.capacity() > initialCapacity);
        // every key must still resolve correctly after rehashing
        for (int k = 0; k < 10; k++) {
            assertEquals("v" + k, table.get(k));
        }
    }

    @Test
    void loadFactorReflectsSizeOverCapacity() {
        ChainedHashTable<Integer, String> table = new ChainedHashTable<>(10);
        table.put(1, "a");
        table.put(2, "b");

        assertEquals(0.2, table.loadFactor(), 1e-9);
    }

    @Test
    void defaultConstructorStartsAtIndexDerivedInitialSize() {
        ChainedHashTable<Integer, String> table = new ChainedHashTable<>();

        assertEquals(53, table.capacity());
        assertEquals(ChainedHashTable.INITIAL_TABLE_SIZE, table.capacity());
    }

    @Test
    void resetCountersZeroesCollisionAndResizeCountsToo() {
        // Capacity 50: three keys forced into bucket 5 (deterministic
        // collisions, see collidingKeysLandInTheSameBucketAsAChain), then
        // enough additional distinct keys to push load factor past 0.75
        // (39/50 = 0.78) and force at least one resize.
        ChainedHashTable<Integer, String> table = new ChainedHashTable<>(50);
        table.put(5, "a");
        table.put(55, "b");
        table.put(105, "c");
        for (int k = 200; k < 236; k++) {
            table.put(k, "v" + k);
        }

        assertTrue(table.collisionCount() > 0, "the three forced-collision keys must have counted");
        assertTrue(table.resizeCount() > 0, "39 entries at capacity 50 must have triggered a resize");

        table.resetCounters();

        assertEquals(0, table.comparisonCount());
        assertEquals(0, table.movementCount());
        assertEquals(0, table.collisionCount());
        assertEquals(0, table.resizeCount());
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void emptyTableGetAndRemoveReturnNullNotThrow() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();

        assertNull(table.get("missing"));
        assertNull(table.remove("missing"));
        assertFalse(table.containsKey("missing"));
        assertTrue(table.isEmpty());
        assertEquals(0, table.collisionCount());
        assertEquals(0, table.longestBucket());
        assertEquals(0.0, table.loadFactor());
    }

    @Test
    void singleEntryTableHasNoCollisions() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        table.put("only", 1);

        assertEquals(1, table.size());
        assertEquals(0, table.collisionCount());
        assertEquals(1, table.longestBucket());
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ChainedHashTable<String, Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new ChainedHashTable<String, Integer>(-5));
    }

    @Test
    void putRejectsNullKey() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        assertThrows(IllegalArgumentException.class, () -> table.put(null, 1));
    }

    @Test
    void getRejectsNullKey() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        assertThrows(IllegalArgumentException.class, () -> table.get(null));
    }

    @Test
    void removeRejectsNullKey() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        assertThrows(IllegalArgumentException.class, () -> table.remove(null));
    }

    @Test
    void containsKeyRejectsNullKey() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        assertThrows(IllegalArgumentException.class, () -> table.containsKey(null));
    }

    @Test
    void iteratorThrowsOnExhaustion() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        table.put("a", 1);

        MyIterator<MyMap.MapEntry<String, Integer>> it = table.entries().iterator();
        it.next();
        assertThrows(StructureException.class, it::next);
    }

    @Test
    void iteratorFailsFastOnStructuralModification() {
        ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
        table.put("a", 1);
        table.put("b", 2);

        MyIterator<MyMap.MapEntry<String, Integer>> it = table.entries().iterator();
        it.next();
        table.put("c", 3);

        assertThrows(StructureException.class, it::next);
    }
}
