package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuickSort.
 * Grouped by the three case types the rubric requires:
 *   1) normal case
 *   2) boundary case (includes already-sorted / reverse-sorted, the
 *      classic pivot-choice pitfall this implementation's median-of-three
 *      is specifically meant to survive)
 *   3) invalid input case
 *
 * Random test data uses GENERATION_SEED (docs/parameters.md, 79731) so
 * results are reproducible and traceable to this team's roster.
 */
class QuickSortTest {

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

        new QuickSort<Integer>().sort(array, ASCENDING);

        assertSorted(array);
    }

    @Test
    void sortsArrayWithDuplicateValues() {
        Integer[] array = {5, 3, 5, 1, 3, 5, 2};
        new QuickSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{1, 2, 3, 3, 5, 5, 5}, array);
    }

    @Test
    void sortsArrayWhereAllElementsAreEqual() {
        Integer[] array = {7, 7, 7, 7, 7};
        new QuickSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{7, 7, 7, 7, 7}, array);
    }

    // ---------- BOUNDARY CASE ----------

    @Test
    void emptyArray_remainsEmptyWithoutError() {
        Integer[] array = {};
        new QuickSort<Integer>().sort(array, ASCENDING);
        assertEquals(0, array.length);
    }

    @Test
    void singleElementArray_isUnchanged() {
        Integer[] array = {42};
        new QuickSort<Integer>().sort(array, ASCENDING);
        assertArrayEquals(new Integer[]{42}, array);
    }

    @Test
    void alreadySortedArray_isStillCorrectlySorted() {
        // The exact case a naive fixed-pivot quicksort degrades on.
        Integer[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        new QuickSort<Integer>().sort(array, ASCENDING);
        assertSorted(array);
    }

    @Test
    void reverseSortedArray_isFullyReversed() {
        Integer[] array = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        new QuickSort<Integer>().sort(array, ASCENDING);
        assertSorted(array);
        assertEquals(1, array[0]);
        assertEquals(10, array[array.length - 1]);
    }

    // ---------- INVALID INPUT CASE ----------

    @Test
    void nullArray_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new QuickSort<Integer>().sort(null, ASCENDING));
    }

    @Test
    void nullComparator_throws() {
        Integer[] array = {3, 1, 2};
        assertThrows(IllegalArgumentException.class,
                () -> new QuickSort<Integer>().sort(array, null));
    }

    // ---------- Sorter self-reporting ----------

    @Test
    void reportsItsOwnDocumentedProperties() {
        QuickSort<Integer> sorter = new QuickSort<>();
        assertFalse(sorter.isStable());
        assertTrue(sorter.isInPlace());
        assertEquals("O(n log n)", sorter.bestCaseComplexity());
        assertEquals("O(n^2)", sorter.worstCaseComplexity());
    }

    // ---------- Instrumented ----------

    @Test
    void resetCounters_zeroesComparisonsAndMovements() {
        QuickSort<Integer> sorter = new QuickSort<>();
        Integer[] array = {3, 1, 2};
        sorter.sort(array, ASCENDING);

        assertTrue(sorter.comparisonCount() > 0);

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