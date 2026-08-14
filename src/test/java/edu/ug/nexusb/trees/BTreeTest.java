package edu.ug.nexusb.trees;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


    class BTreeTest {

        private static final MyComparator<Integer> NATURAL_ORDER = Integer::compare;

        private BTree<Integer, String> tree;

        @BeforeEach
        void setUp() {
            tree = new BTree<>(NATURAL_ORDER);
        }

        // ---- construction ----

        @Nested
        @DisplayName("construction")
        class Construction {

            @Test
            @DisplayName("rejects a null comparator")
            void rejectsNullComparator() {
                assertThrows(IllegalArgumentException.class, () -> new BTree<Integer, String>(null));
            }

            @Test
            @DisplayName("rejects a null comparator with an explicit degree")
            void rejectsNullComparatorWithDegree() {
                assertThrows(IllegalArgumentException.class, () -> new BTree<Integer, String>(null, 4));
            }

            @Test
            @DisplayName("rejects a minimum degree below 2")
            void rejectsTooSmallDegree() {
                assertThrows(IllegalArgumentException.class, () -> new BTree<Integer, String>(NATURAL_ORDER, 1));
            }

            @Test
            @DisplayName("accepts the smallest legal minimum degree")
            void acceptsMinimumLegalDegree() {
                BTree<Integer, String> t = new BTree<>(NATURAL_ORDER, 2);
                assertTrue(t.isEmpty());
            }

            @Test
            @DisplayName("exposes the comparator it was built with")
            void exposesComparator() {
                assertTrue(tree.comparator() == NATURAL_ORDER);
            }
        }

        // ---- empty-tree boundary cases ----

        @Nested
        @DisplayName("empty tree")
        class EmptyTree {

            @Test
            @DisplayName("starts empty with size 0")
            void startsEmpty() {
                assertTrue(tree.isEmpty());
                assertEquals(0, tree.size());
            }

            @Test
            @DisplayName("height is -1 for an empty tree")
            void heightIsMinusOne() {
                assertEquals(-1, tree.height());
            }

            @Test
            @DisplayName("get on an empty tree returns null rather than throwing")
            void getReturnsNull() {
                assertNull(tree.get(1));
            }

            @Test
            @DisplayName("remove on an empty tree returns null rather than throwing")
            void removeReturnsNull() {
                assertNull(tree.remove(1));
            }

            @Test
            @DisplayName("containsKey on an empty tree is false")
            void containsKeyIsFalse() {
                assertFalse(tree.containsKey(1));
            }

            @Test
            @DisplayName("entries() on an empty tree yields nothing")
            void entriesIsEmpty() {
                assertFalse(tree.entries().iterator().hasNext());
            }

            @Test
            @DisplayName("rangeKeys() on an empty tree yields nothing")
            void rangeKeysIsEmpty() {
                assertFalse(tree.rangeKeys(0, 100).iterator().hasNext());
            }

            @Test
            @DisplayName("a B-tree is balanced by construction, even when empty")
            void emptyIsBalanced() {
                assertTrue(tree.isBalanced());
            }
        }

        // ---- single-node boundary case ----

        @Nested
        @DisplayName("single node")
        class SingleNode {

            @BeforeEach
            void insertOne() {
                tree.put(10, "ten");
            }

            @Test
            @DisplayName("height is 0 for a single-node tree")
            void heightIsZero() {
                assertEquals(0, tree.height());
            }

            @Test
            @DisplayName("size is 1 and isEmpty is false")
            void sizeIsOne() {
                assertEquals(1, tree.size());
                assertFalse(tree.isEmpty());
            }

            @Test
            @DisplayName("removing the only node empties the tree")
            void removeEmptiesTree() {
                assertEquals("ten", tree.remove(10));
                assertTrue(tree.isEmpty());
                assertEquals(-1, tree.height());
            }
        }

        // ---- normal-case behavior on a populated tree ----

        @Nested
        @DisplayName("populated tree")
        class PopulatedTree {

            @BeforeEach
            void insertSeveral() {
                int[] keys = {10, 5, 15, 3, 7, 12, 20};
                for (int k : keys) {
                    tree.put(k, "v" + k);
                }
            }

            @Test
            @DisplayName("size reflects the number of distinct keys inserted")
            void sizeReflectsInserts() {
                assertEquals(7, tree.size());
            }

            @Test
            @DisplayName("get finds every inserted key")
            void getFindsEveryKey() {
                int[] keys = {10, 5, 15, 3, 7, 12, 20};
                for (int k : keys) {
                    assertEquals("v" + k, tree.get(k));
                }
            }

            @Test
            @DisplayName("get on a missing key returns null")
            void getMissingKeyReturnsNull() {
                assertNull(tree.get(999));
            }

            @Test
            @DisplayName("containsKey distinguishes present from absent keys")
            void containsKeyDistinguishes() {
                assertTrue(tree.containsKey(7));
                assertFalse(tree.containsKey(999));
            }

            @Test
            @DisplayName("put on an existing key replaces the value and returns the old one")
            void putOnExistingKeyReplaces() {
                String old = tree.put(10, "TEN");
                assertEquals("v10", old);
                assertEquals("TEN", tree.get(10));
                assertEquals(7, tree.size(), "size must not grow on a value replacement");
            }

            @Test
            @DisplayName("entries() visits every key in ascending order")
            void entriesInAscendingOrder() {
                MyIterator<MyMap.MapEntry<Integer, String>> it = tree.entries().iterator();
                int[] expectedKeys = {3, 5, 7, 10, 12, 15, 20};
                for (int expected : expectedKeys) {
                    assertTrue(it.hasNext());
                    assertEquals(expected, it.next().getKey());
                }
                assertFalse(it.hasNext());
            }

            @Test
            @DisplayName("rangeKeys is inclusive of both bounds")
            void rangeKeysInclusiveBounds() {
                MyIterator<Integer> it = tree.rangeKeys(5, 15).iterator();
                int[] expected = {5, 7, 10, 12, 15};
                for (int e : expected) {
                    assertTrue(it.hasNext());
                    assertEquals(e, it.next());
                }
                assertFalse(it.hasNext());
            }

            @Test
            @DisplayName("rangeKeys with from > to yields an empty result")
            void rangeKeysFromGreaterThanToIsEmpty() {
                assertFalse(tree.rangeKeys(100, 1).iterator().hasNext());
            }

            @Test
            @DisplayName("removing a key preserves in-order structure")
            void removeKeepsOrder() {
                assertEquals("v10", tree.remove(10));
                assertEquals(6, tree.size());
                assertFalse(tree.containsKey(10));
                MyIterator<Integer> it = tree.rangeKeys(0, 100).iterator();
                int previous = Integer.MIN_VALUE;
                while (it.hasNext()) {
                    int k = it.next();
                    assertTrue(k > previous);
                    previous = k;
                }
            }

            @Test
            @DisplayName("removing a missing key is a no-op that returns null")
            void removeMissingKeyIsNoOp() {
                assertNull(tree.remove(999));
                assertEquals(7, tree.size());
            }

            @Test
            @DisplayName("a B-tree stays balanced by construction after inserts")
            void staysBalanced() {
                assertTrue(tree.isBalanced());
            }
        }

        // ---- structural boundary cases: splits, merges, and borrows ----

        @Nested
        @DisplayName("node splits and merges")
        class SplitsAndMerges {

            @Test
            @DisplayName("inserting past one node's capacity forces a split and grows height")
            void insertForcesSplit() {
                // Default minimum degree is 3, so a node holds at most 5 keys;
                // the 6th forces the root to split and height to grow from 0 to 1.
                BTree<Integer, String> t = new BTree<>(NATURAL_ORDER);
                for (int i = 1; i <= 5; i++) {
                    t.put(i, "v" + i);
                }
                assertEquals(0, t.height());
                t.put(6, "v6");
                assertEquals(1, t.height());
                assertEquals(6, t.size());
                for (int i = 1; i <= 6; i++) {
                    assertEquals("v" + i, t.get(i));
                }
            }

            @Test
            @DisplayName("deleting down to below minimum degree triggers merges without losing keys")
            void deleteTriggersMergesWithoutLosingKeys() {
                BTree<Integer, String> t = new BTree<>(NATURAL_ORDER, 2); // t=2: max 3 keys/node
                int n = 40;
                for (int i = 0; i < n; i++) {
                    t.put(i, "v" + i);
                }
                // remove every even key, forcing repeated borrow/merge rebalancing
                for (int i = 0; i < n; i += 2) {
                    assertEquals("v" + i, t.remove(i));
                }
                assertEquals(n / 2, t.size());
                for (int i = 0; i < n; i++) {
                    if (i % 2 == 0) {
                        assertFalse(t.containsKey(i));
                    } else {
                        assertEquals("v" + i, t.get(i));
                    }
                }
                assertTrue(t.isBalanced());
                // in-order sequence must still be sorted after all the churn
                MyIterator<Integer> it = t.rangeKeys(Integer.MIN_VALUE, Integer.MAX_VALUE).iterator();
                int previous = Integer.MIN_VALUE;
                int count = 0;
                while (it.hasNext()) {
                    int k = it.next();
                    assertTrue(k > previous);
                    previous = k;
                    count++;
                }
                assertEquals(n / 2, count);
            }

            @Test
            @DisplayName("removing every key shrinks the tree back to empty")
            void removingEveryKeyEmptiesTree() {
                BTree<Integer, String> t = new BTree<>(NATURAL_ORDER, 2);
                int n = 30;
                for (int i = 0; i < n; i++) {
                    t.put(i, "v" + i);
                }
                for (int i = 0; i < n; i++) {
                    assertEquals("v" + i, t.remove(i));
                }
                assertTrue(t.isEmpty());
                assertEquals(-1, t.height());
                assertTrue(t.isBalanced());
            }
        }

        // ---- invalid input ----

        @Nested
        @DisplayName("invalid input")
        class InvalidInput {

            @Test
            @DisplayName("put rejects a null key")
            void putRejectsNullKey() {
                assertThrows(IllegalArgumentException.class, () -> tree.put(null, "x"));
            }

            @Test
            @DisplayName("get rejects a null key")
            void getRejectsNullKey() {
                assertThrows(IllegalArgumentException.class, () -> tree.get(null));
            }

            @Test
            @DisplayName("remove rejects a null key")
            void removeRejectsNullKey() {
                assertThrows(IllegalArgumentException.class, () -> tree.remove(null));
            }

            @Test
            @DisplayName("containsKey rejects a null key")
            void containsKeyRejectsNullKey() {
                assertThrows(IllegalArgumentException.class, () -> tree.containsKey(null));
            }

            @Test
            @DisplayName("rangeKeys rejects a null lower bound")
            void rangeKeysRejectsNullFrom() {
                assertThrows(IllegalArgumentException.class, () -> tree.rangeKeys(null, 5));
            }

            @Test
            @DisplayName("rangeKeys rejects a null upper bound")
            void rangeKeysRejectsNullTo() {
                assertThrows(IllegalArgumentException.class, () -> tree.rangeKeys(5, null));
            }

            @Test
            @DisplayName("calling next() past the end of an iterator fails fast")
            void iteratorNextPastEndThrows() {
                tree.put(1, "one");
                MyIterator<MyMap.MapEntry<Integer, String>> it = tree.entries().iterator();
                it.next();
                assertThrows(StructureException.class, it::next);
            }
        }

        // ---- Instrumented counters ----

        @Nested
        @DisplayName("instrumentation")
        class Instrumentation {

            @Test
            @DisplayName("counters start at zero")
            void countersStartAtZero() {
                assertEquals(0, tree.comparisonCount());
                assertEquals(0, tree.movementCount());
            }

            @Test
            @DisplayName("comparisons and movements accrue during use")
            void countersAccrue() {
                tree.put(5, "five");
                tree.put(3, "three");
                tree.get(3);
                assertTrue(tree.comparisonCount() > 0);
                assertTrue(tree.movementCount() > 0);
            }

            @Test
            @DisplayName("resetCounters zeroes both counters without affecting content")
            void resetCountersZeroesBoth() {
                tree.put(5, "five");
                tree.put(3, "three");
                tree.resetCounters();
                assertEquals(0, tree.comparisonCount());
                assertEquals(0, tree.movementCount());
                assertEquals(2, tree.size());
            }
        }
    }

