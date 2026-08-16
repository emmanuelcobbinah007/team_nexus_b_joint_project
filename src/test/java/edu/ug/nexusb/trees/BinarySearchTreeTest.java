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


class BinarySearchTreeTest {

    private static final MyComparator<Integer> NATURAL_ORDER = Integer::compare;

    private BinarySearchTree<Integer, String> tree;

    @BeforeEach
    void setUp() {
        tree = new BinarySearchTree<>(NATURAL_ORDER);
    }

    // ---- construction ----

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects a null comparator")
        void rejectsNullComparator() {
            assertThrows(IllegalArgumentException.class, () -> new BinarySearchTree<Integer, String>(null));
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
        @DisplayName("an empty tree is trivially balanced")
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
        @DisplayName("a single node is balanced")
        void isBalanced() {
            assertTrue(tree.isBalanced());
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
            // 10 is the root; 5/15 its children; 3/7/12/20 their leaves.
            tree.put(10, "ten");
            tree.put(5, "five");
            tree.put(15, "fifteen");
            tree.put(3, "three");
            tree.put(7, "seven");
            tree.put(12, "twelve");
            tree.put(20, "twenty");
        }

        @Test
        @DisplayName("size reflects the number of distinct keys inserted")
        void sizeReflectsInserts() {
            assertEquals(7, tree.size());
        }

        @Test
        @DisplayName("get finds every inserted key")
        void getFindsEveryKey() {
            assertEquals("ten", tree.get(10));
            assertEquals("five", tree.get(5));
            assertEquals("fifteen", tree.get(15));
            assertEquals("three", tree.get(3));
            assertEquals("seven", tree.get(7));
            assertEquals("twelve", tree.get(12));
            assertEquals("twenty", tree.get(20));
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
            assertEquals("ten", old);
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
        @DisplayName("removing a leaf key does not disturb other keys")
        void removeLeaf() {
            assertEquals("three", tree.remove(3));
            assertEquals(6, tree.size());
            assertFalse(tree.containsKey(3));
            assertEquals("ten", tree.get(10));
        }

        @Test
        @DisplayName("removing a two-child key preserves in-order structure")
        void removeTwoChildNode() {
            assertEquals("ten", tree.remove(10));
            assertEquals(6, tree.size());
            assertFalse(tree.containsKey(10));
            // in-order sequence must still be sorted after successor splice
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
        @DisplayName("height grows with structure, never below single-node height for 7 keys")
        void heightIsNonNegative() {
            assertTrue(tree.height() >= 0);
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

    // ---- balance oracle on randomized-ish input ----

    @Test
    @DisplayName("isBalanced reflects the tree's actual shape, not a stub")
    void isBalancedReflectsActualShape() {
        // Strictly increasing inserts degrade a plain BST toward a linked
        // list, which must NOT be reported as balanced.
        for (int i = 0; i < 20; i++) {
            tree.put(i, "v" + i);
        }
        assertFalse(tree.isBalanced());
        assertEquals(19, tree.height());
    }
}


