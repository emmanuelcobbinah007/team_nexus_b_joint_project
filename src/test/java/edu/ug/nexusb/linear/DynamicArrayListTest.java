package edu.ug.nexusb.linear;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DynamicArrayList.
 * Grouped by the three case types the rubric requires:
 *   1) normal case
 *   2) boundary case (empty, single element, resize point)
 *   3) invalid input case
 */
class DynamicArrayListTest {

    // ---------- NORMAL CASE ----------

    @Test
    void addAndGet_returnsElementsInInsertionOrder() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        list.add("Korle-Bu");
        list.add("Ridge Hospital");
        list.add("Nyaho Medical Centre");

        assertEquals(3, list.size());
        assertEquals("Korle-Bu", list.get(0));
        assertEquals("Ridge Hospital", list.get(1));
        assertEquals("Nyaho Medical Centre", list.get(2));
    }

    @Test
    void insertInMiddle_shiftsLaterElementsRight() {
        DynamicArrayList<Integer> list = new DynamicArrayList<>();
        list.add(1);
        list.add(2);
        list.add(4);
        list.insert(2, 3); // [1, 2, 3, 4]

        assertEquals(4, list.size());
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));
    }

    @Test
    void set_overwritesExistingValue() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        list.add("Clinic A");
        list.set(0, "Clinic B");
        assertEquals("Clinic B", list.get(0));
    }

    @Test
    void remove_shiftsLaterElementsLeftAndShrinksSize() {
        DynamicArrayList<Integer> list = new DynamicArrayList<>();
        list.add(10);
        list.add(20);
        list.add(30);

        int removed = list.remove(1);

        assertEquals(20, removed);
        assertEquals(2, list.size());
        assertEquals(10, list.get(0));
        assertEquals(30, list.get(1));
    }

    // ---------- BOUNDARY CASE ----------

    @Test
    void newList_isEmpty() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void singleElementList_addAndRemove() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        list.add("Solo Clinic");
        assertEquals(1, list.size());

        String removed = list.remove(0);
        assertEquals("Solo Clinic", removed);
        assertTrue(list.isEmpty());
    }

    @Test
    void resize_growsCapacityWhenFull() {
        // default capacity is 8 -> the 9th add must trigger a resize
        DynamicArrayList<Integer> list = new DynamicArrayList<>(4);
        int startCapacity = list.capacity();
        for (int i = 0; i < 4; i++) list.add(i);

        assertEquals(startCapacity, list.capacity()); // still full, not yet resized
        list.add(4); // triggers resize
        assertTrue(list.capacity() > startCapacity, "capacity should grow after exceeding initial size");
    }

    // ---------- INVALID INPUT CASE ----------

    @Test
    void get_withNegativeIndex_throws() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        list.add("A");
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void get_withIndexEqualToSize_throws() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        list.add("A");
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1)); // size is 1, valid index is only 0
    }

    @Test
    void get_onEmptyList_throws() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    @Test
    void constructor_withZeroOrNegativeCapacity_throws() {
        assertThrows(IllegalArgumentException.class, () -> new DynamicArrayList<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new DynamicArrayList<Integer>(-5));
    }

    // ---------- FAIL-FAST ITERATOR ----------

    @Test
    void iterator_throwsIfStructurallyModifiedDuringIteration() {
        DynamicArrayList<Integer> list = new DynamicArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        edu.ug.nexusb.core.MyIterator<Integer> it = list.iterator();
        it.next();
        list.add(4); // structural change mid-iteration

        assertThrows(edu.ug.nexusb.core.StructureException.class, it::next);
    }

    // ---------- INSTRUMENTED COUNTERS ----------

    @Test
    void resetCounters_zeroesComparisonsAndMovements() {
        DynamicArrayList<Integer> list = new DynamicArrayList<>();
        list.add(1);
        list.add(2);
        list.indexOf(2);

        assertTrue(list.comparisonCount() > 0);

        list.resetCounters();
        assertEquals(0, list.comparisonCount());
        assertEquals(0, list.movementCount());
    }

    @Test
    void indexOf_missingValue_returnsNegativeOne() {
        DynamicArrayList<String> list = new DynamicArrayList<>();
        list.add("A");
        assertEquals(-1, list.indexOf("Z"));
        assertFalse(list.contains("Z"));
    }
}