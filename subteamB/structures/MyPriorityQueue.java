package gh.ug.smartops.structures;

import java.util.Comparator;

/**
 * Custom binary heap-backed priority queue.
 *
 * This is my personal defense structure — I must be able to
 * insert/extract and explain heapify live, including sift-up on insert and
 * sift-down on extraction, and produce a dispatch-order trace as evidence.
 *
 * The ordering (min-heap vs max-heap) is determined by the Comparator
 * supplied at construction time by the implementing class; whichever
 * "smallest" under that comparator means "highest priority" is extracted
 * first by extractTop().
 *
 * @param <T> the element type stored in this priority queue
 */
public interface MyPriorityQueue<T> {

    /**
     * Inserts an element and restores the heap property (sift-up).
     * @param value element to insert
     */
    void insert(T value);

    /**
     * Removes and returns the highest-priority element and restores the
     * heap property (sift-down), also called extractMin/extractMax
     * depending on the comparator direction.
     *
     * @return the removed highest-priority element
     * @throws java.util.NoSuchElementException if the queue is empty
     */
    T extractTop();

    /**
     * Returns the highest-priority element without removing it.
     * @return the current top element
     * @throws java.util.NoSuchElementException if the queue is empty
     */
    T peekTop();

    /**
     * Rebuilds the heap property over an arbitrary backing array in
     * O(n) time (bottom-up heapify), used when bulk-loading requests
     * from the database rather than inserting one at a time.
     *
     * @param items the elements to heapify; implementation takes ownership
     *              of a working copy
     */
    void heapify(T[] items);

    /**
     * @return the comparator used to order elements (defines priority
     *         direction: urgency, deadline, weighted score, etc.)
     */
    Comparator<? super T> comparator();

    /**
     * @return true if the priority queue has no elements
     */
    boolean isEmpty();

    /**
     * @return the number of elements currently in the priority queue
     */
    int size();
}
