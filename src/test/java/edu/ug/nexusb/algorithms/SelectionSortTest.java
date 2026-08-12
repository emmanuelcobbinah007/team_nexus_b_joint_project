package edu.ug.nexusb.algorithms;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelectionSortTest {

    private SelectionSort<Integer> sorter;

    @BeforeEach
    void setUp() {
        sorter = new SelectionSort<>();
    }

    @Test
    void testSortNormalCase() {
        Integer[] array = {64, 25, 12, 22, 11};
        Integer[] expected = {11, 12, 22, 25, 64};
        sorter.sort(array);
        assertArrayEquals(expected, array);
    }

    @Test
    void testSortBoundaryEmptyAndSingle() {
        Integer[] emptyArray = {};
        sorter.sort(emptyArray);
        assertArrayEquals(new Integer[]{}, emptyArray);

        Integer[] singleElement = {42};
        sorter.sort(singleElement);
        assertArrayEquals(new Integer[]{42}, singleElement);
    }

    @Test
    void testSortWithDuplicates() {
        Integer[] array = {5, 1, 4, 2, 8, 1, 5};
        Integer[] expected = {1, 1, 2, 4, 5, 5, 8};
        sorter.sort(array);
        assertArrayEquals(expected, array);
    }

    @Test
    void testSortWithComparator() {
        Integer[] array = {64, 25, 12, 22, 11};
        Integer[] expectedDescending = {64, 25, 22, 12, 11};
        
        sorter.sort(array, Comparator.reverseOrder());
        assertArrayEquals(expectedDescending, array);
    }
}
