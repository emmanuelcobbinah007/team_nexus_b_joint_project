package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.Instrumented;
import edu.ug.nexusb.core.MyComparator;

/**
 * In-place quicksort using Lomuto partitioning with a deterministic
 * median-of-three pivot (first, middle, last element of each subrange).
 *
 * <p>Median-of-three avoids the classic worst case on already-sorted or
 * reverse-sorted input that a fixed first/last pivot suffers from, without
 * needing a random-number source — the worst case is still O(n^2) for a
 * pathologically constructed input, but that input no longer includes
 * "already sorted," which is the input this project's data is most likely
 * to resemble after an earlier sort or a naturally ordered facility list.
 *
 * @param <T> the type of elements being sorted
 */
public class QuickSort<T> implements Sorter<T>, Instrumented {

    private long comparisons;
    private long movements;

    /** Creates a new QuickSort instance. No setup required. */
    public QuickSort() {
    }

    /**
     * Sorts array in place, ascending according to comparator.
     *
     * @param array the array to sort; mutated in place
     * @param comparator the ordering to sort by
     * @throws IllegalArgumentException if array or comparator is null
     */
    @Override
    public void sort(T[] array, MyComparator<T> comparator) {
        validate(array, comparator);
        if (array.length < 2) {
            return;
        }
        quickSort(array, 0, array.length - 1, comparator);
    }

    private void quickSort(T[] array, int lo, int hi, MyComparator<T> comparator) {
        if (lo >= hi) {
            return;
        }
        int pivotIndex = medianOfThreeIndex(array, lo, hi, comparator);
        swap(array, pivotIndex, hi); // Lomuto partitioning expects the pivot at the end
        int p = partition(array, lo, hi, comparator);
        quickSort(array, lo, p - 1, comparator);
        quickSort(array, p + 1, hi, comparator);
    }

    /** Returns the index (lo, mid, or hi) holding the median of the three values. */
    private int medianOfThreeIndex(T[] array, int lo, int hi, MyComparator<T> comparator) {
        int mid = lo + (hi - lo) / 2;
        T a = array[lo];
        T b = array[mid];
        T c = array[hi];

        comparisons++;
        if (comparator.compare(a, b) > 0) {
            comparisons++;
            if (comparator.compare(b, c) > 0) {
                return mid;
            }
            comparisons++;
            return comparator.compare(a, c) > 0 ? hi : lo;
        } else {
            comparisons++;
            if (comparator.compare(a, c) > 0) {
                return lo;
            }
            comparisons++;
            return comparator.compare(b, c) > 0 ? hi : mid;
        }
    }

    private int partition(T[] array, int lo, int hi, MyComparator<T> comparator) {
        T pivot = array[hi];
        int boundary = lo - 1;
        for (int j = lo; j < hi; j++) {
            comparisons++;
            if (comparator.compare(array[j], pivot) <= 0) {
                boundary++;
                swap(array, boundary, j);
            }
        }
        swap(array, boundary + 1, hi);
        return boundary + 1;
    }

    private void swap(T[] array, int x, int y) {
        if (x == y) {
            return;
        }
        T tmp = array[x];
        array[x] = array[y];
        array[y] = tmp;
        movements += 2; // two array slots overwritten
    }

    private void validate(T[] array, MyComparator<T> comparator) {
        if (array == null) {
            throw new IllegalArgumentException("array must not be null");
        }
        if (comparator == null) {
            throw new IllegalArgumentException("comparator must not be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStable() {
        return false; // Lomuto partitioning can reorder equal elements
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInPlace() {
        return true; // O(1) auxiliary space beyond the recursion stack
    }

    /** {@inheritDoc} */
    @Override
    public String bestCaseComplexity() {
        return "O(n log n)";
    }

    /** {@inheritDoc} */
    @Override
    public String worstCaseComplexity() {
        return "O(n^2)";
    }

    /** {@inheritDoc} */
    @Override
    public long comparisonCount() {
        return comparisons;
    }

    /** {@inheritDoc} */
    @Override
    public long movementCount() {
        return movements;
    }

    /** {@inheritDoc} */
    @Override
    public void resetCounters() {
        comparisons = 0;
        movements = 0;
    }
}