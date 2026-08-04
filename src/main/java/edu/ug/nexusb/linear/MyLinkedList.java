package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;

/**
 * Custom singly or doubly linked list (implementer's choice — decide and
 * note the choice in the module README, since the brief allows either).
 *
 * Evidence required for M3 / Section 6: a diagram of the node structure
 * plus a live iterator demo.
 *
 * @param <T> the element type stored in this list
 */
public interface MyLinkedList<T> extends MyIterable<T> {

    /**
     * Inserts an element at the head of the list.
     * @param value element to insert, may be null unless the implementation
     *              documents otherwise
     */
    void addFirst(T value);

    /**
     * Inserts an element at the tail of the list.
     * @param value element to insert
     */
    void addLast(T value);

    /**
     * Inserts newValue immediately after the first node whose value equals
     * target (using .equals()).
     *
     * @param target    the value to search for
     * @param newValue  the value to insert after it
     * @throws java.util.NoSuchElementException if target is not found
     */
    void insertAfter(T target, T newValue);

    /**
     * Removes the first node whose value equals target.
     *
     * @param target the value to remove
     * @return true if a node was removed, false if target was not found
     */
    boolean remove(T target);

    /**
     * @return the number of elements currently in the list
     */
    int size();

    /**
     * @return true if the list has no elements
     */
    boolean isEmpty();

    /**
     * @return a fresh iterator positioned before the first element,
     *         traversing head-to-tail
     */
    @Override
    MyIterator<T> iterator();
}
