package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyComparator;

/**
 * Interface for a priority queue.
 *
 * @param <T> the type of elements stored in the queue
 */
public interface MyPriorityQueue<T> {

    /**
     * Inserts an element into the priority queue.
     *
     * @param value the element to insert
     */
    void insert(T value);

    /**
     * Removes and returns the highest-priority element.
     *
     * @return the top element
     * @throws RuntimeException if the queue is empty
     */
    T extractTop();

    /**
     * Returns the highest-priority element without removing it.
     *
     * @return the top element
     * @throws RuntimeException if the queue is empty
     */
    T peekTop();

    /**
     * Builds a heap from an array of elements.
     *
     * @param items the array of elements
     */
    void heapify(T[] items);

    /**
     * Returns the comparator used by the priority queue.
     *
     * @return the comparator
     */
    MyComparator<? super T> comparator();

    /**
     * Updates the position of an element whose priority has changed.
     *
     * @param value the element to update
     * @throws KeyNotFoundException if the element is not found
     */
    void decreaseKey(T value);

    /**
     * Checks whether the queue is empty.
     *
     * @return true if the queue is empty, otherwise false
     */
    boolean isEmpty();

    /**
     * Returns the number of elements in the queue.
     *
     * @return the size of the queue
     */
    int size();
}