package edu.ug.nexusb.trees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.trees.MyMap.MapEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChainedHashTableTest {

    /** Test-only key with a controllable hashCode, so bucket collisions can be forced deterministically. */
    private static final class FixedHashKey {
        final String label;
        final int hash;

        FixedHashKey(String label, int hash) {
            this.label = label;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FixedHashKey && ((FixedHashKey) other).label.equals(label);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ---- construction ----

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("no-arg constructor starts at INITIAL_TABLE_SIZE")
        void noArgConstructorStartsAtInitialTableSize() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertEquals(ChainedHashTable.INITIAL_TABLE_SIZE, table.capacity());
        }

        @Test
        @DisplayName("rejects a non-positive initial capacity")
        void rejectsNonPositiveInitialCapacity() {
            assertThrows(IllegalArgumentException.class, () -> new ChainedHashTable<String, Integer>(0));
            assertThrows(IllegalArgumentException.class, () -> new ChainedHashTable<String, Integer>(-1));
        }
    }

    // ---- normal case ----

    @Nested
    @DisplayName("normal case")
    class NormalCase {

        @Test
        @DisplayName("put then get returns the stored value")
        void putThenGetReturnsStoredValue() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("F001", 10);
            assertEquals(10, table.get("F001"));
        }

        @Test
        @DisplayName("put on an existing key overwrites and returns the previous value")
        void putOnExistingKeyOverwritesAndReturnsPrevious() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("F001", 10);
            Integer previous = table.put("F001", 20);
            assertEquals(10, previous);
            assertEquals(20, table.get("F001"));
            assertEquals(1, table.size());
        }

        @Test
        @DisplayName("remove returns the removed value and drops the key")
        void removeReturnsValueAndDropsKey() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("F001", 10);
            assertEquals(10, table.remove("F001"));
            assertNull(table.get("F001"));
            assertFalse(table.containsKey("F001"));
        }

        @Test
        @DisplayName("containsKey and size track insertions and removals")
        void containsKeyAndSizeTrackChanges() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertEquals(0, table.size());
            table.put("A", 1);
            table.put("B", 2);
            assertEquals(2, table.size());
            assertTrue(table.containsKey("A"));
            table.remove("A");
            assertEquals(1, table.size());
            assertFalse(table.containsKey("A"));
        }

        @Test
        @DisplayName("entries() visits every stored key-value pair exactly once")
        void entriesVisitsEveryPairOnce() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("A", 1);
            table.put("B", 2);
            table.put("C", 3);

            boolean[] seen = new boolean[3];
            MyIterator<MapEntry<String, Integer>> it = table.entries().iterator();
            int count = 0;
            while (it.hasNext()) {
                MapEntry<String, Integer> entry = it.next();
                count++;
                if (entry.getKey().equals("A")) {
                    assertEquals(1, entry.getValue());
                    seen[0] = true;
                } else if (entry.getKey().equals("B")) {
                    assertEquals(2, entry.getValue());
                    seen[1] = true;
                } else if (entry.getKey().equals("C")) {
                    assertEquals(3, entry.getValue());
                    seen[2] = true;
                }
            }
            assertEquals(3, count);
            assertTrue(seen[0] && seen[1] && seen[2]);
        }
    }

    // ---- boundary case ----

    @Nested
    @DisplayName("boundary case")
    class BoundaryCase {

        @Test
        @DisplayName("a new table is empty")
        void newTableIsEmpty() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertTrue(table.isEmpty());
            assertEquals(0, table.size());
        }

        @Test
        @DisplayName("get/remove/containsKey on an empty table behave like a miss, not an error")
        void missesOnEmptyTableBehaveLikeAMiss() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertNull(table.get("F001"));
            assertNull(table.remove("F001"));
            assertFalse(table.containsKey("F001"));
        }

        @Test
        @DisplayName("single entry: put, then remove, is empty again")
        void singleEntryPutThenRemoveIsEmptyAgain() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("F001", 1);
            assertFalse(table.isEmpty());
            table.remove("F001");
            assertTrue(table.isEmpty());
        }

        @Test
        @DisplayName("null value is permitted as a stored value")
        void nullValueIsPermitted() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("F001", null);
            assertTrue(table.containsKey("F001"));
            assertNull(table.get("F001"));
        }

        @Test
        @DisplayName("resizing preserves every existing entry")
        void resizingPreservesEveryExistingEntry() {
            ChainedHashTable<Integer, Integer> table = new ChainedHashTable<>(4);
            for (int i = 0; i < 50; i++) {
                table.put(i, i * i);
            }
            assertTrue(table.resizeCount() > 0, "50 entries into a table starting at 4 must trigger at least one resize");
            for (int i = 0; i < 50; i++) {
                assertEquals(i * i, table.get(i));
            }
            assertEquals(50, table.size());
        }
    }

    // ---- invalid input ----

    @Nested
    @DisplayName("invalid input")
    class InvalidInput {

        @Test
        @DisplayName("null key on put throws")
        void nullKeyOnPutThrows() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertThrows(IllegalArgumentException.class, () -> table.put(null, 1));
        }

        @Test
        @DisplayName("null key on get throws")
        void nullKeyOnGetThrows() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertThrows(IllegalArgumentException.class, () -> table.get(null));
        }

        @Test
        @DisplayName("null key on remove throws")
        void nullKeyOnRemoveThrows() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertThrows(IllegalArgumentException.class, () -> table.remove(null));
        }

        @Test
        @DisplayName("null key on containsKey throws")
        void nullKeyOnContainsKeyThrows() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            assertThrows(IllegalArgumentException.class, () -> table.containsKey(null));
        }
    }

    // ---- collision / load-factor evidence (T030's required deliverable) ----

    @Nested
    @DisplayName("collision and load-factor evidence")
    class CollisionEvidence {

        @Test
        @DisplayName("two keys forced into the same bucket are counted as one collision")
        void twoKeysInSameBucketCountAsOneCollision() {
            ChainedHashTable<FixedHashKey, String> table = new ChainedHashTable<>(4);
            assertEquals(0, table.collisionCount());

            table.put(new FixedHashKey("a", 0), "first"); // bucket 0, no collision
            assertEquals(0, table.collisionCount());

            table.put(new FixedHashKey("b", 4), "second"); // hash 4 % 4 == 0, same bucket
            assertEquals(1, table.collisionCount());

            table.put(new FixedHashKey("c", 8), "third"); // hash 8 % 4 == 0, same bucket again
            assertEquals(2, table.collisionCount());

            assertEquals(3, table.longestBucket());
        }

        @Test
        @DisplayName("re-putting an existing key is not counted as a collision")
        void rePuttingAnExistingKeyIsNotACollision() {
            ChainedHashTable<String, Integer> table = new ChainedHashTable<>();
            table.put("F001", 1);
            table.put("F001", 2); // update, not a new bucket entry
            assertEquals(0, table.collisionCount());
        }

        @Test
        @DisplayName("loadFactor reflects size/capacity and resets after a resize")
        void loadFactorReflectsSizeOverCapacityAndDropsAfterResize() {
            ChainedHashTable<Integer, Integer> table = new ChainedHashTable<>(4);
            table.put(1, 1);
            table.put(2, 2);
            assertEquals(2.0 / 4, table.loadFactor(), 1e-9);

            table.put(3, 3); // 3/4 = 0.75, at the threshold, not over it yet
            assertEquals(0, table.resizeCount());

            table.put(4, 4); // pushes load factor over 0.75, must trigger a resize
            assertTrue(table.resizeCount() > 0);
            assertTrue(table.loadFactor() <= 0.75);
        }

        @Test
        @DisplayName("resetCounters zeroes comparison, movement, collision, and resize counts")
        void resetCountersZeroesAllFourCounts() {
            ChainedHashTable<Integer, Integer> table = new ChainedHashTable<>(2);
            for (int i = 0; i < 10; i++) {
                table.put(i, i);
            }
            table.get(0);

            assertTrue(table.comparisonCount() > 0);
            assertTrue(table.movementCount() > 0);
            assertTrue(table.resizeCount() > 0);

            table.resetCounters();

            assertEquals(0, table.comparisonCount());
            assertEquals(0, table.movementCount());
            assertEquals(0, table.collisionCount());
            assertEquals(0, table.resizeCount());
            // resetting counters must not affect the actual stored data
            assertEquals(10, table.size());
            assertEquals(0, table.get(0));
        }
    }
}
