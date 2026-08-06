package edu.ug.nexusb.interfaces;

import java.util.Comparator;

/**
 * Interface representing searching engines (Linear and Binary Search) 
 * for the Ghana Smart Service Operations Optimizer.
 * 
 * @param <T> the type of elements being searched
 * @author Johnson Kuzagbe (Sub-group E Leader)
 */
public interface Searcher<T> {

    /**
     * Performs a linear search on an array to find the index of a target element.
     * Time Complexity: O(n)
     * 
     * @param array the array to search within
     * @param target the element to search for
     * @return the index of the target if found, or -1 if not present
     */
    int linearSearch(T[] array, T target);

    /**
     * Performs a binary search on a sorted array to find the index of a target element.
     * Precondition: The array must be sorted according to the natural ordering of elements.
     * Time Complexity: O(log n)
     * 
     * @param array the sorted array to search within
     * @param target the element to search for
     * @return the index of the target if found, or -1 if not present
     */
    int binarySearch(T[] array, T target);

    /**
     * Performs a binary search on a sorted array using a custom comparator.
     * Precondition: The array must be sorted according to the specified comparator.
     * 
     * @param array the sorted array to search within
     * @param target the element to search for
     * @param comparator the comparator used to determine element order
     * @return the index of the target if found, or -1 if not present
     */
    int binarySearch(T[] array, T target, Comparator<T> comparator);
}