package edu.ug.nexusb.linear;

import org.junit.jupiter.api.Test;
import java.util.Iterator;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MyLinkedList.
 * Grouped by the three case types the rubric requires:
 *   1) normal case
 *   2) boundary case (empty, single element)
 *   3) invalid input case
 */
class MyLinkedListTest {

    // ---------- NORMAL CASE ----------

    @Test
    void addFirstAndAddLast_buildCorrectOrder() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("Ridge Hospital");   // [Ridge]
        list.addFirst("Korle-Bu");        // [Korle-Bu, Ridge]
        list.addLast("Nyaho");            // [Korle-Bu, Ridge, Nyaho]

        assertEquals(3, list.size());
        assertEquals("Korle-Bu", list.getFirst());
        assertEquals("Nyaho", list.getLast());
    }

    @Test
    void insertAfter_placesNewNodeInCorrectPosition() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        list.addLast("C");
        list.insertAfter("A", "B"); // [A, B, C]

        Iterator<String> it = list.iterator();
        assertEquals("A", it.next());
        assertEquals("B", it.next());
        assertEquals("C", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void remove_middleElement_relinksNeighbours() {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);

        boolean removed = list.removeValue(2);

        assertTrue(removed);
        assertEquals(2, list.size());
        assertEquals(1, list.getFirst());
        assertEquals(3, list.getLast());
    }

    @Test
    void iterator_visitsEveryElementInOrder() {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        for (int i = 1; i <= 5; i++) list.addLast(i);

        int expected = 1;
        for (int value : list) {
            assertEquals(expected, value);
            expected++;
        }
        assertEquals(6, expected); // confirms we saw all 5 elements
    }

    // ---------- BOUNDARY CASE ----------

    @Test
    void newList_isEmpty() {
        MyLinkedList<String> list = new MyLinkedList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.iterator().hasNext());
    }

    @Test
    void singleElementList_headAndTailAreSameNode() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addFirst("Only Clinic");

        assertEquals("Only Clinic", list.getFirst());
        assertEquals("Only Clinic", list.getLast());

        list.removeValue("Only Clinic");
        assertTrue(list.isEmpty());
    }

    @Test
    void removeLastRemainingElement_resetsHeadAndTail() {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        list.addLast(42);
        list.removeValue(42);

        assertTrue(list.isEmpty());
        // list should still behave correctly after being emptied
        list.addLast(7);
        assertEquals(7, list.getFirst());
        assertEquals(7, list.getLast());
    }

    // ---------- INVALID INPUT CASE ----------

    @Test
    void getFirst_onEmptyList_throws() {
        MyLinkedList<String> list = new MyLinkedList<>();
        assertThrows(NoSuchElementException.class, list::getFirst);
    }

    @Test
    void getLast_onEmptyList_throws() {
        MyLinkedList<String> list = new MyLinkedList<>();
        assertThrows(NoSuchElementException.class, list::getLast);
    }

    @Test
    void insertAfter_withMissingTarget_throws() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        assertThrows(NoSuchElementException.class, () -> list.insertAfter("Z", "B"));
    }

    @Test
    void iterator_nextPastEnd_throws() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        Iterator<String> it = list.iterator();
        it.next(); // consume the only element
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void remove_valueNotPresent_returnsFalseWithoutThrowing() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        assertFalse(list.removeValue("does-not-exist"));
        assertEquals(1, list.size()); // list unchanged
    }

    // ---------- INDEXED ACCESS (mirrors DynamicArrayList contract) ----------

    @Test
    void indexedGetAndInsert_matchArraySemantics() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        list.addLast("C");
        list.insert(1, "B"); // [A, B, C]

        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
    }

    @Test
    void indexedRemove_returnsValueAndShrinksSize() {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        list.addLast(10);
        list.addLast(20);
        list.addLast(30);

        int removed = list.remove(1);

        assertEquals(20, removed);
        assertEquals(2, list.size());
        assertEquals(10, list.get(0));
        assertEquals(30, list.get(1));
    }

    @Test
    void get_withInvalidIndex_throws() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    // ---------- FAIL-FAST ITERATOR ----------

    @Test
    void iterator_throwsIfStructurallyModifiedDuringIteration() {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        list.addLast(1);
        list.addLast(2);

        Iterator<Integer> it = list.iterator();
        it.next();
        list.addLast(3); // structural change mid-iteration

        assertThrows(java.util.ConcurrentModificationException.class, it::next);
    }

    // ---------- INSTRUMENTED COUNTERS ----------

    @Test
    void resetCounters_zeroesComparisonsAndMovements() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        list.removeValue("A"); // triggers a comparison

        assertTrue(list.getComparisons() > 0);

        list.resetCounters();
        assertEquals(0, list.getComparisons());
        assertEquals(0, list.getMovements());
    }
}