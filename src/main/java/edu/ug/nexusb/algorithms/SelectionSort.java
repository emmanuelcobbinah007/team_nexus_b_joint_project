package edu.ug.nexusb.algorithms;

import java.util.Comparator;

import edu.ug.nexusb.interfaces.Sorter;

public class SelectionSort<T extends Comparable<T>> implements Sorter<T> {

    @Override
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
    public void sort(T[] array, Comparator<T> comparator) {
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

    private void swap(T[] array, int i, int j) {
        if (i != j) {
            T temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
}