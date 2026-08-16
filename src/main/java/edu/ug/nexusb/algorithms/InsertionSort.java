package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;

public class InsertionSort<T extends Comparable<T>> implements Sorter<T> {

    /** Convenience overload: sorts by natural ordering. Not part of {@link Sorter}. */
    public void sort(T[] array) {
        if (array == null || array.length <= 1) return;

        for (int i = 1; i < array.length; i++) {
            T key = array[i];
            int j = i - 1;

            while (j >= 0 && array[j].compareTo(key) > 0) {
                array[j + 1] = array[j];
                j = j - 1;
            }
            array[j + 1] = key;
        }
    }

    @Override
    public void sort(T[] array, MyComparator<T> comparator) {
        if (array == null || array.length <= 1) return;

        for (int i = 1; i < array.length; i++) {
            T key = array[i];
            int j = i - 1;

            while (j >= 0 && comparator.compare(array[j], key) > 0) {
                array[j + 1] = array[j];
                j = j - 1;
            }
            array[j + 1] = key;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStable() {
        return true; // only shifts elements strictly greater than key; equal elements keep their order
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInPlace() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String bestCaseComplexity() {
        return "O(n)"; // already-sorted input: inner while loop never runs
    }

    /** {@inheritDoc} */
    @Override
    public String worstCaseComplexity() {
        return "O(n^2)"; // reverse-sorted input
    }
}