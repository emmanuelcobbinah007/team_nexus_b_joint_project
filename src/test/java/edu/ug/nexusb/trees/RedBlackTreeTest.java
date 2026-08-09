package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedBlackTreeTest {

    private static final MyComparator<Integer> INT_ORDER = (a, b) -> Integer.compare(a, b);

    private RedBlackTree<Integer, String> newTree() {
        return new RedBlackTree<>(INT_ORDER);
    }

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void putThenGetReturnsStoredValue() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(10, "ten");
        tree.put(5, "five");
        tree.put(15, "fifteen");

        assertEquals("ten", tree.get(10));
        assertEquals("five", tree.get(5));
        assertEquals("fifteen", tree.get(15));
        assertEquals(3, tree.size());
    }

    @Test
    void putOnExistingKeyUpdatesAndReturnsPreviousValue() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(10, "ten");
        String previous = tree.put(10, "TEN");

        assertEquals("ten", previous);
        assertEquals("TEN", tree.get(10));
        assertEquals(1, tree.size());
    }

    @Test
    void removeExistingKeyReturnsValueAndShrinksSize() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(10, "ten");
        tree.put(5, "five");

        String removed = tree.remove(10);

        assertEquals("ten", removed);
        assertEquals(1, tree.size());
        assertNull(tree.get(10));
        assertEquals("five", tree.get(5));
    }

    @Test
    void entriesTraversesInAscendingKeyOrder() {
        RedBlackTree<Integer, String> tree = newTree();
        int[] insertOrder = {50, 20, 70, 10, 30, 60, 80};
        for (int k : insertOrder) {
            tree.put(k, "v" + k);
        }

        MyIterator<MyMap.MapEntry<Integer, String>> it = tree.entries().iterator();
        int previous = Integer.MIN_VALUE;
        int count = 0;
        while (it.hasNext()) {
            MyMap.MapEntry<Integer, String> entry = it.next();
            assertTrue(entry.getKey() > previous, "entries() must be ascending");
            previous = entry.getKey();
            count++;
        }
        assertEquals(insertOrder.length, count);
    }

    @Test
    void rangeKeysReturnsOnlyKeysWithinInclusiveBounds() {
        RedBlackTree<Integer, String> tree = newTree();
        for (int k = 1; k <= 20; k++) {
            tree.put(k, "v" + k);
        }

        MyIterator<Integer> it = tree.rangeKeys(5, 10).iterator();
        int expected = 5;
        int count = 0;
        while (it.hasNext()) {
            assertEquals(expected, it.next());
            expected++;
            count++;
        }
        assertEquals(6, count); // 5,6,7,8,9,10 inclusive
    }

    // ------------------------------------------------------------------
    // Red-Black invariants (this is what actually proves the
    // implementation, not just that it behaves like any old BST)
    // ------------------------------------------------------------------

    @Test
    void staysBalancedUnderSortedInsertion() {
        // A plain unbalanced BST fed sorted input degenerates into a
        // linked list (height n-1). A correct Red-Black tree must not.
        RedBlackTree<Integer, String> tree = newTree();
        int n = 1000;
        for (int k = 0; k < n; k++) {
            tree.put(k, "v" + k);
            assertTrue(tree.isBalanced(), "RB invariants must hold after every insert, key=" + k);
        }
        // RB-tree height is bounded by 2*log2(n+1)
        int bound = (int) (2 * (Math.log(n + 1) / Math.log(2))) + 1;
        assertTrue(tree.height() <= bound,
                "height " + tree.height() + " exceeds RB bound " + bound + " for n=" + n);
    }

    @Test
    void staysBalancedUnderInterleavedInsertAndDelete() {
        RedBlackTree<Integer, String> tree = newTree();
        for (int k = 0; k < 500; k++) {
            tree.put(k, "v" + k);
        }
        for (int k = 0; k < 500; k += 2) {
            tree.remove(k);
            assertTrue(tree.isBalanced(), "RB invariants must hold after removing key=" + k);
        }
        assertEquals(250, tree.size());
        for (int k = 1; k < 500; k += 2) {
            assertEquals("v" + k, tree.get(k));
        }
    }

    @Test
    void removingEveryNodeLeavesAnEmptyValidTree() {
        RedBlackTree<Integer, String> tree = newTree();
        for (int k = 0; k < 100; k++) {
            tree.put(k, "v" + k);
        }
        for (int k = 0; k < 100; k++) {
            tree.remove(k);
        }
        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertTrue(tree.isBalanced());
        assertEquals(-1, tree.height());
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void emptyTreeGetAndRemoveReturnNullNotThrow() {
        RedBlackTree<Integer, String> tree = newTree();

        assertNull(tree.get(1));
        assertNull(tree.remove(1));
        assertFalse(tree.containsKey(1));
        assertTrue(tree.isEmpty());
        assertEquals(-1, tree.height());
        assertTrue(tree.isBalanced());
    }

    @Test
    void singleNodeTreeHasZeroHeightAndIsBalanced() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(42, "answer");

        assertEquals(0, tree.height());
        assertTrue(tree.isBalanced());
        assertEquals(1, tree.size());
    }

    @Test
    void rangeKeysWithFromGreaterThanToReturnsEmpty() {
        RedBlackTree<Integer, String> tree = newTree();
        for (int k = 1; k <= 10; k++) {
            tree.put(k, "v" + k);
        }

        assertFalse(tree.rangeKeys(8, 3).iterator().hasNext());
    }

    @Test
    void rangeKeysWithNoMatchesReturnsEmpty() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(1, "a");
        tree.put(2, "b");

        assertFalse(tree.rangeKeys(100, 200).iterator().hasNext());
    }

    @Test
    void rangeKeysFromEqualsToReturnsSingleMatchIfPresent() {
        RedBlackTree<Integer, String> tree = newTree();
        for (int k = 1; k <= 5; k++) {
            tree.put(k, "v" + k);
        }

        MyIterator<Integer> it = tree.rangeKeys(3, 3).iterator();
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void constructorRejectsNullComparator() {
        assertThrows(IllegalArgumentException.class, () -> new RedBlackTree<Integer, String>(null));
    }

    @Test
    void putRejectsNullKey() {
        RedBlackTree<Integer, String> tree = newTree();
        assertThrows(IllegalArgumentException.class, () -> tree.put(null, "x"));
    }

    @Test
    void getRejectsNullKey() {
        RedBlackTree<Integer, String> tree = newTree();
        assertThrows(IllegalArgumentException.class, () -> tree.get(null));
    }

    @Test
    void removeRejectsNullKey() {
        RedBlackTree<Integer, String> tree = newTree();
        assertThrows(IllegalArgumentException.class, () -> tree.remove(null));
    }

    @Test
    void containsKeyRejectsNullKey() {
        RedBlackTree<Integer, String> tree = newTree();
        assertThrows(IllegalArgumentException.class, () -> tree.containsKey(null));
    }

    @Test
    void rangeKeysRejectsNullBounds() {
        RedBlackTree<Integer, String> tree = newTree();
        assertThrows(IllegalArgumentException.class, () -> tree.rangeKeys(null, 5));
        assertThrows(IllegalArgumentException.class, () -> tree.rangeKeys(5, null));
    }

    @Test
    void iteratorThrowsOnExhaustion() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(1, "a");

        MyIterator<MyMap.MapEntry<Integer, String>> it = tree.entries().iterator();
        it.next();
        assertThrows(StructureException.class, it::next);
    }

    @Test
    void iteratorFailsFastOnStructuralModification() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(1, "a");
        tree.put(2, "b");

        MyIterator<MyMap.MapEntry<Integer, String>> it = tree.entries().iterator();
        it.next();
        tree.put(3, "c"); // structural change after the snapshot's modCount was captured

        assertThrows(StructureException.class, it::next);
    }

    // ------------------------------------------------------------------
    // Instrumented
    // ------------------------------------------------------------------

    @Test
    void resetCountersZeroesBothCounters() {
        RedBlackTree<Integer, String> tree = newTree();
        tree.put(1, "a");
        tree.put(2, "b");
        tree.get(1);

        assertTrue(tree.comparisonCount() > 0);
        assertTrue(tree.movementCount() > 0);

        tree.resetCounters();

        assertEquals(0, tree.comparisonCount());
        assertEquals(0, tree.movementCount());
    }
}
