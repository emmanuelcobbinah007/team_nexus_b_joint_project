package edu.ug.nexusb.interfaces;

import java.util.Comparator;

/**
 * Interface representing sorting algorithms for the Ghana Smart Service Operations Optimizer.
 * Defines contracts for in-place and comparative sorting of comparable elements.
 * 
 * @param <T> the type of elements to be sorted, extending Comparable
 * @author Johnson Kuzagbe (Sub-group E Leader)
 */
public interface Sorter<T extends Comparable<T>> {

    /**
     * Sorts the provided array of elements in ascending order using their natural ordering.
     * 
     * @param array the array of elements to be sorted
     */
    void sort(T[] array);

    /**
     * Sorts the provided array of elements in order defined by the specified comparator.
     * 
     * @param array the array of elements to be sorted
     * @param comparator the comparator determining the ordering of the elements
     */
    void sort(T[] array, Comparator<T> comparator);
}