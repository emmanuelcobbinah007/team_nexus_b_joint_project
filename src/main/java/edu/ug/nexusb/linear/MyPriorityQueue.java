package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyComparator;

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
    MyComparator<? super T> comparator();

    /**
     * Notifies the queue that {@code value}'s priority has improved (its
     * comparator-order key got smaller / higher-priority) and restores the
     * heap property (sift-up from the element's current position).
     *
     * <p>Exists solely because Dijkstra needs it when a shorter route to an
     * already-discovered facility is found — nothing in this module calls
     * it directly. Do not remove it as "unused"; Sub-team D's shortest-path
     * implementation depends on it. See {@code docs/interfaces.md}.
     *
     * @param value the element whose priority has decreased; must already
     *              be present in the queue, compared with {@code equals()}
     * @throws KeyNotFoundException if {@code value} is not currently in the queue
     */
    void decreaseKey(T value);

    /**
     * @return true if the priority queue has no elements
     */
    boolean isEmpty();

    /**
     * @return the number of elements currently in the priority queue
     */
    int size();
}
