package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;

public class SelectionSort<T extends Comparable<T>> implements Sorter<T> {

    /** Convenience overload: sorts by natural ordering. Not part of {@link Sorter}. */
    public void sort(T[] array) {
        if (array == null || array.length <= 1) return;

        for (int i = 0; i < array.length - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < array.length; j++) {
                if (array[j].compareTo(array[minIdx]) < 0) {
                    minIdx = j;
                }
            }
            swap(array, i, minIdx);
        }
    }

    @Override
    public void sort(T[] array, MyComparator<T> comparator) {
        if (array == null || array.length <= 1) return;

        for (int i = 0; i < array.length - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < array.length; j++) {
                if (comparator.compare(array[j], array[minIdx]) < 0) {
                    minIdx = j;
                }
            }
            swap(array, i, minIdx);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStable() {
        return false; // swapping the found minimum into place can reorder equal elements
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInPlace() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String bestCaseComplexity() {
        return "O(n^2)"; // always scans the remaining array for the minimum, regardless of input order
    }

    /** {@inheritDoc} */
    @Override
    public String worstCaseComplexity() {
        return "O(n^2)";
    }

    private void swap(T[] array, int i, int j) {
        if (i != j) {
            T temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
}