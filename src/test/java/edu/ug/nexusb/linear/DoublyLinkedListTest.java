package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DoublyLinkedList, the concrete implementation of the
 * frozen MyLinkedList<T> interface (see docs/interfaces.md, T018).
 * Grouped by the three case types the rubric requires:
 *   1) normal case
 *   2) boundary case (empty, single element)
 *   3) invalid input case
 */
class DoublyLinkedListTest {

    // ---------- NORMAL CASE ----------

    @Test
    void addFirstAndAddLast_buildCorrectOrder() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("Ridge Hospital");   // [Ridge]
        list.addFirst("Korle-Bu");        // [Korle-Bu, Ridge]
        list.addLast("Nyaho");            // [Korle-Bu, Ridge, Nyaho]

        assertEquals(3, list.size());
        assertEquals("Korle-Bu", list.getFirst());
        assertEquals("Nyaho", list.getLast());
    }

    @Test
    void insertAfter_placesNewNodeInCorrectPosition() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("A");
        list.addLast("C");
        list.insertAfter("A", "B"); // [A, B, C]

        MyIterator<String> it = list.iterator();
        assertEquals("A", it.next());
        assertEquals("B", it.next());
        assertEquals("C", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void remove_middleElement_relinksNeighbours() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);

        boolean removed = list.remove(2);

        assertTrue(removed);
        assertEquals(2, list.size());
        assertEquals(1, list.getFirst());
        assertEquals(3, list.getLast());
    }

    @Test
    void iterator_visitsEveryElementInOrder() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();
        for (int i = 1; i <= 5; i++) list.addLast(i);

        MyIterator<Integer> it = list.iterator();
        int expected = 1;
        while (it.hasNext()) {
            assertEquals(expected, it.next());
            expected++;
        }
        assertEquals(6, expected); // confirms we saw all 5 elements
    }

    // ---------- BOUNDARY CASE ----------

    @Test
    void newList_isEmpty() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.iterator().hasNext());
    }

    @Test
    void singleElementList_headAndTailAreSameNode() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addFirst("Only Clinic");

        assertEquals("Only Clinic", list.getFirst());
        assertEquals("Only Clinic", list.getLast());

        list.remove("Only Clinic");
        assertTrue(list.isEmpty());
    }

    @Test
    void removeLastRemainingElement_resetsHeadAndTail() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();
        list.addLast(42);
        list.remove(42);

        assertTrue(list.isEmpty());
        // list should still behave correctly after being emptied
        list.addLast(7);
        assertEquals(7, list.getFirst());
        assertEquals(7, list.getLast());
    }

    // ---------- INVALID INPUT CASE ----------

    @Test
    void getFirst_onEmptyList_throws() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        assertThrows(StructureException.class, list::getFirst);
    }

    @Test
    void getLast_onEmptyList_throws() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        assertThrows(StructureException.class, list::getLast);
    }

    @Test
    void insertAfter_withMissingTarget_throws() {
        // Interface Javadoc specifies java.util.NoSuchElementException here,
        // not StructureException - kept exact to the frozen contract.
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("A");
        assertThrows(java.util.NoSuchElementException.class, () -> list.insertAfter("Z", "B"));
    }

    @Test
    void iterator_nextPastEnd_throws() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("A");
        MyIterator<String> it = list.iterator();
        it.next(); // consume the only element
        assertThrows(StructureException.class, it::next);
    }

    @Test
    void remove_valueNotPresent_returnsFalseWithoutThrowing() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("A");
        assertFalse(list.remove("does-not-exist"));
        assertEquals(1, list.size()); // list unchanged
    }

    // ---------- FAIL-FAST ITERATOR ----------

    @Test
    void iterator_throwsIfStructurallyModifiedDuringIteration() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();
        list.addLast(1);
        list.addLast(2);

        MyIterator<Integer> it = list.iterator();
        it.next();
        list.addLast(3); // structural change mid-iteration

        assertThrows(StructureException.class, it::next);
    }

    // ---------- INSTRUMENTED COUNTERS ----------

    @Test
    void resetCounters_zeroesComparisonsAndMovements() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        list.remove("A"); // triggers a comparison

        assertTrue(list.comparisonCount() > 0);

        list.resetCounters();
        assertEquals(0, list.comparisonCount());
        assertEquals(0, list.movementCount());
    }
}