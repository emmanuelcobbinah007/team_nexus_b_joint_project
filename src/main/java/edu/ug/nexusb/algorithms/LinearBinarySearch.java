package edu.ug.nexusb.algorithms;

import edu.ug.nexusb.core.MyComparator;

/**
 * {@link Searcher} implementation (T039): linear search (no precondition on
 * the input) and binary search (requires the input already sorted by the
 * ordering being searched with — natural order for the two-argument
 * overload, {@code comparator}'s order for the three-argument one).
 *
 * <p>{@link #requiresSortedInput()} answers for this class's binary-search
 * capability specifically, since that is the capability the precondition
 * actually applies to — {@link #linearSearch} has no such requirement.
 * Binary search does not check sortedness at runtime (a check would cost
 * O(n) and defeat the entire point of running in O(log n)); violating the
 * precondition is a silent correctness bug, not an exception, which is
 * exactly why the brief requires it demonstrated as a counterexample rather
 * than left as an assumption — see
 * {@code binarySearchOnUnsortedInputCanSilentlyMissAPresentElement} in this
 * class's test.
 *
 * @param <T> the type of elements being searched
 */
public class LinearBinarySearch<T> implements Searcher<T> {

    @Override
    public int linearSearch(T[] array, T target) {
        requireArray(array);
        requireTarget(target);
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int binarySearch(T[] array, T target) {
        return binarySearch(array, target, LinearBinarySearch::naturalOrder);
    }

    @Override
    public int binarySearch(T[] array, T target, MyComparator<T> comparator) {
        requireArray(array);
        requireTarget(target);
        if (comparator == null) {
            throw new IllegalArgumentException("comparator must not be null");
        }

        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int cmp = comparator.compare(array[mid], target);
            if (cmp == 0) {
                return mid;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    @Override
    public boolean requiresSortedInput() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> int naturalOrder(T a, T b) {
        return ((Comparable<T>) a).compareTo(b);
    }

    private static void requireArray(Object[] array) {
        if (array == null) {
            throw new IllegalArgumentException("array must not be null");
        }
    }

    private static void requireTarget(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
    }
}
