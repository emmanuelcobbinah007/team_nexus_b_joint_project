package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;

/**
 * Interface representing sorting algorithms for the Ghana Smart Service
 * Operations Optimizer — selection sort, insertion sort, merge sort and
 * quicksort all implement this one contract, so the Week 4 sorting
 * comparison experiment runs every algorithm through the same harness.
 *
 * <p>{@link #isStable()}, {@link #isInPlace()} and the two complexity-label
 * methods exist so each implementation is honest about its own properties
 * rather than that only being asserted in the report: they populate the
 * sorting comparison table directly, and stability matters here because
 * cases of equal triage level must stay in request-time order — an
 * unstable sort needs an explicit tie-break to preserve that.
 *
 * @param <T> the type of elements being sorted
 */
public interface Sorter<T> {

    /**
     * Sorts {@code array} in place according to {@code comparator}.
     *
     * @param array the array to sort; mutated in place
     * @param comparator the ordering to sort by
     */
    void sort(T[] array, MyComparator<T> comparator);

    /**
     * @return {@code true} if elements that compare equal retain their
     *     relative input order after sorting
     */
    boolean isStable();

    /**
     * @return {@code true} if this sort uses O(1) auxiliary space beyond
     *     the recursion stack, {@code false} if it allocates auxiliary
     *     storage proportional to input size (e.g. merge sort's buffer)
     */
    boolean isInPlace();

    /**
     * @return a human-readable best-case time complexity label (e.g.
     *     {@code "O(n)"}, {@code "O(n log n)"}), for the report's
     *     theory-versus-practice comparison table
     */
    String bestCaseComplexity();

    /**
     * @return a human-readable worst-case time complexity label (e.g.
     *     {@code "O(n^2)"}, {@code "O(n log n)"})
     */
    String worstCaseComplexity();
}
