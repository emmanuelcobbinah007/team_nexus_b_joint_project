package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InsertionSortTest {

    private InsertionSort<Integer> sorter;

    @BeforeEach
    void setUp() {
        sorter = new InsertionSort<>();
    }

    @Test
    void testSortNormalCase() {
        Integer[] array = {4, 3, 2, 10, 12, 1, 5, 6};
        Integer[] expected = {1, 2, 3, 4, 5, 6, 10, 12};
        sorter.sort(array);
        assertArrayEquals(expected, array);
    }

    @Test
    void testSortBoundaryAlreadySorted() {
        Integer[] array = {1, 2, 3, 4, 5};
        Integer[] expected = {1, 2, 3, 4, 5};
        sorter.sort(array);
        assertArrayEquals(expected, array);
    }

    @Test
    void testSortReverseSorted() {
        Integer[] array = {9, 8, 7, 6, 5};
        Integer[] expected = {5, 6, 7, 8, 9};
        sorter.sort(array);
        assertArrayEquals(expected, array);
    }

    @Test
    void testSortWithComparator() {
        Integer[] array = {1, 5, 3, 2, 4};
        Integer[] expectedDescending = {5, 4, 3, 2, 1};

        sorter.sort(array, (MyComparator<Integer>) (a, b) -> b - a);
        assertArrayEquals(expectedDescending, array);
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
    void testSortNullArrayIsSafeNoOp() {
        // Both sort() overloads treat null as a documented no-op rather than throwing.
        sorter.sort((Integer[]) null);
        sorter.sort(null, (MyComparator<Integer>) (a, b) -> a - b);
    }
}
