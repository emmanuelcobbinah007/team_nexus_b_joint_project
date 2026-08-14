package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.Instrumented;
import edu.ug.nexusb.core.MyComparator;

/**
 * Top-down recursive merge sort.
 *
 * <p>Stable and O(n log n) in every case, at the cost of an auxiliary
 * buffer the size of the input — the classic trade-off against quicksort,
 * and the pairing this project's sorting comparison experiment is built
 * to demonstrate. Stability matters directly here: cases of equal triage
 * level must stay in request-time order, and merge sort provides that for
 * free by taking the left run on a tie during the merge step.
 *
 * @param <T> the type of elements being sorted
 */
public class MergeSort<T> implements Sorter<T>, Instrumented {

    private long comparisons;
    private long movements;

    /** Creates a new MergeSort instance. No setup required. */
    public MergeSort() {
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
            return; // already sorted by definition
        }
        @SuppressWarnings("unchecked")
        T[] buffer = (T[]) new Object[array.length];
        mergeSort(array, buffer, 0, array.length - 1, comparator);
    }

    private void mergeSort(T[] array, T[] buffer, int lo, int hi, MyComparator<T> comparator) {
        if (lo >= hi) {
            return;
        }
        int mid = lo + (hi - lo) / 2;
        mergeSort(array, buffer, lo, mid, comparator);
        mergeSort(array, buffer, mid + 1, hi, comparator);
        merge(array, buffer, lo, mid, hi, comparator);
    }

    private void merge(T[] array, T[] buffer, int lo, int mid, int hi, MyComparator<T> comparator) {
        for (int k = lo; k <= hi; k++) {
            buffer[k] = array[k];
            movements++;
        }

        int left = lo;
        int right = mid + 1;

        for (int k = lo; k <= hi; k++) {
            if (left > mid) {
                array[k] = buffer[right++];
            } else if (right > hi) {
                array[k] = buffer[left++];
            } else {
                comparisons++;
                // <= (not <) is what makes this stable: on a tie, the left
                // run (earlier input position) is taken first.
                if (comparator.compare(buffer[left], buffer[right]) <= 0) {
                    array[k] = buffer[left++];
                } else {
                    array[k] = buffer[right++];
                }
            }
            movements++;
        }
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
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInPlace() {
        return false; // allocates an auxiliary buffer proportional to n
    }

    /** {@inheritDoc} */
    @Override
    public String bestCaseComplexity() {
        return "O(n log n)";
    }

    /** {@inheritDoc} */
    @Override
    public String worstCaseComplexity() {
        return "O(n log n)";
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