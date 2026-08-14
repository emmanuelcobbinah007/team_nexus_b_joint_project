package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MergeSort.
 * Grouped by the three case types the rubric requires:
 *   1) normal case
 *   2) boundary case
 *   3) invalid input case
 * plus a dedicated stability test, since Sorter.isStable() claims true.
 *
 * Random test data uses GENERATION_SEED (docs/parameters.md, 79731) so
 * results are reproducible and traceable to this team's roster.
 */
class MergeSortTest {

    private static final long GENERATION_SEED = 79731L;

    private static final MyComparator<Integer> ASCENDING = (a, b) -> Integer.compare(a, b);

    // ---------- NORMAL CASE ----------

    @Test
    void sortsRandomArrayIntoAscendingOrder() {
        Random rng = new Random(GENERATION_SEED);
        Integer[] array = new Integer[200];
        for (int i = 0; i < array.length; i++) {
            array[i] = rng.nextInt(10_000);
        }

        new MergeSort<Integer>().sort(array, ASCENDING);

        assertSorted(array);
    }

    @Test
    void sortsArrayWithDuplicateValues() {
        Integer[] array = {5, 3, 5, 1, 3, 5, 2};
        new MergeSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{1, 2, 3, 3, 5, 5, 5}, array);
    }

    // ---------- BOUNDARY CASE ----------

    @Test
    void emptyArray_remainsEmptyWithoutError() {
        Integer[] array = {};
        new MergeSort<Integer>().sort(array, ASCENDING);
        assertEquals(0, array.length);
    }

    @Test
    void singleElementArray_isUnchanged() {
        Integer[] array = {42};
        new MergeSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{42}, array);
    }

    @Test
    void alreadySortedArray_isUnchanged() {
        Integer[] array = {1, 2, 3, 4, 5};
        new MergeSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, array);
    }

    @Test
    void reverseSortedArray_isFullyReversed() {
        Integer[] array = {5, 4, 3, 2, 1};
        new MergeSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, array);
    }

    // ---------- INVALID INPUT CASE ----------

    @Test
    void nullArray_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new MergeSort<Integer>().sort(null, ASCENDING));
    }

    @Test
    void nullComparator_throws() {
        Integer[] array = {3, 1, 2};
        assertThrows(IllegalArgumentException.class,
                () -> new MergeSort<Integer>().sort(array, null));
    }

    // ---------- STABILITY (Sorter.isStable() claims true) ----------

    private record TriageCase(int level, int arrivalOrder) {
    }

    @Test
    void equalKeys_retainOriginalRelativeOrder() {
        // Simulates the real use case: cases with the same triage level
        // must stay in request-time (arrival) order after sorting by level.
        TriageCase[] cases = {
                new TriageCase(2, 0),
                new TriageCase(1, 1),
                new TriageCase(2, 2),
                new TriageCase(1, 3),
                new TriageCase(2, 4),
        };

        MyComparator<TriageCase> byLevel = (a, b) -> Integer.compare(a.level(), b.level());
        new MergeSort<TriageCase>().sort(cases, byLevel);

        // level-1 cases must appear in arrival order 1, then 3
        // level-2 cases must appear in arrival order 0, then 2, then 4
        int[] expectedArrivalOrder = {1, 3, 0, 2, 4};
        for (int i = 0; i < cases.length; i++) {
            assertEquals(expectedArrivalOrder[i], cases[i].arrivalOrder(),
                    "stability broken at position " + i);
        }
    }

    // ---------- Sorter self-reporting ----------

    @Test
    void reportsItsOwnDocumentedProperties() {
        MergeSort<Integer> sorter = new MergeSort<>();
        assertTrue(sorter.isStable());
        assertFalse(sorter.isInPlace());
        assertEquals("O(n log n)", sorter.bestCaseComplexity());
        assertEquals("O(n log n)", sorter.worstCaseComplexity());
    }

    // ---------- Instrumented ----------

    @Test
    void resetCounters_zeroesComparisonsAndMovements() {
        MergeSort<Integer> sorter = new MergeSort<>();
        Integer[] array = {3, 1, 2};
        sorter.sort(array, ASCENDING);

        assertTrue(sorter.comparisonCount() > 0);
        assertTrue(sorter.movementCount() > 0);

        sorter.resetCounters();
        assertEquals(0, sorter.comparisonCount());
        assertEquals(0, sorter.movementCount());
    }

    private void assertSorted(Integer[] array) {
        for (int i = 1; i < array.length; i++) {
            assertTrue(array[i - 1] <= array[i],
                    "array not sorted at index " + i);
        }
    }
}