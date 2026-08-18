package edu.ug.nexusb.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.MyComparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LinearBinarySearchTest {

    private static final MyComparator<Integer> DESCENDING = (a, b) -> b - a;

    private final LinearBinarySearch<Integer> searcher = new LinearBinarySearch<>();

    // ---- normal case ----

    @Nested
    @DisplayName("normal case")
    class NormalCase {

        @Test
        @DisplayName("linearSearch finds a present element, on unsorted input too")
        void linearSearchFindsPresentElement() {
            Integer[] array = {50, 10, 30, 20, 40};
            assertEquals(3, searcher.linearSearch(array, 20));
        }

        @Test
        @DisplayName("linearSearch returns -1 for an absent element")
        void linearSearchReturnsMinusOneForAbsentElement() {
            Integer[] array = {50, 10, 30, 20, 40};
            assertEquals(-1, searcher.linearSearch(array, 99));
        }

        @Test
        @DisplayName("binarySearch (natural order) finds a present element in sorted input")
        void binarySearchNaturalOrderFindsPresentElement() {
            Integer[] sorted = {10, 20, 30, 40, 50};
            assertEquals(3, searcher.binarySearch(sorted, 40));
        }

        @Test
        @DisplayName("binarySearch (natural order) returns -1 for an absent element")
        void binarySearchNaturalOrderReturnsMinusOneForAbsentElement() {
            Integer[] sorted = {10, 20, 30, 40, 50};
            assertEquals(-1, searcher.binarySearch(sorted, 25));
        }

        @Test
        @DisplayName("binarySearch with a custom comparator finds elements sorted by that order")
        void binarySearchWithComparatorFindsElementsInThatOrder() {
            Integer[] descendingSorted = {50, 40, 30, 20, 10};
            assertEquals(2, searcher.binarySearch(descendingSorted, 30, DESCENDING));
        }
    }

    // ---- boundary case ----

    @Nested
    @DisplayName("boundary case")
    class BoundaryCase {

        @Test
        @DisplayName("linearSearch on an empty array returns -1")
        void linearSearchOnEmptyArrayReturnsMinusOne() {
            assertEquals(-1, searcher.linearSearch(new Integer[0], 1));
        }

        @Test
        @DisplayName("binarySearch on an empty array returns -1")
        void binarySearchOnEmptyArrayReturnsMinusOne() {
            assertEquals(-1, searcher.binarySearch(new Integer[0], 1));
        }

        @Test
        @DisplayName("single-element array: found and not-found cases")
        void singleElementArrayFoundAndNotFound() {
            Integer[] single = {42};
            assertEquals(0, searcher.linearSearch(single, 42));
            assertEquals(-1, searcher.linearSearch(single, 7));
            assertEquals(0, searcher.binarySearch(single, 42));
            assertEquals(-1, searcher.binarySearch(single, 7));
        }

        @Test
        @DisplayName("binarySearch finds the target at the first and last index")
        void binarySearchFindsTargetAtFirstAndLastIndex() {
            Integer[] sorted = {1, 2, 3, 4, 5};
            assertEquals(0, searcher.binarySearch(sorted, 1));
            assertEquals(4, searcher.binarySearch(sorted, 5));
        }
    }

    // ---- invalid input ----

    @Nested
    @DisplayName("invalid input")
    class InvalidInput {

        @Test
        @DisplayName("linearSearch rejects a null array")
        void linearSearchRejectsNullArray() {
            assertThrows(IllegalArgumentException.class, () -> searcher.linearSearch(null, 1));
        }

        @Test
        @DisplayName("linearSearch rejects a null target")
        void linearSearchRejectsNullTarget() {
            assertThrows(IllegalArgumentException.class, () -> searcher.linearSearch(new Integer[] {1}, null));
        }

        @Test
        @DisplayName("binarySearch rejects a null array")
        void binarySearchRejectsNullArray() {
            assertThrows(IllegalArgumentException.class, () -> searcher.binarySearch(null, 1));
        }

        @Test
        @DisplayName("binarySearch rejects a null target")
        void binarySearchRejectsNullTarget() {
            assertThrows(IllegalArgumentException.class, () -> searcher.binarySearch(new Integer[] {1}, null));
        }

        @Test
        @DisplayName("binarySearch with a comparator rejects a null comparator")
        void binarySearchRejectsNullComparator() {
            assertThrows(IllegalArgumentException.class,
                () -> searcher.binarySearch(new Integer[] {1}, 1, null));
        }
    }

    // ---- precondition counterexample (required by the brief: "one invalid
    // precondition such as unsorted binary search input") ----

    @Nested
    @DisplayName("binary search precondition")
    class PreconditionCounterexample {

        /**
         * The counterexample the brief specifically asks for (Section 10:
         * "at least two [...] one invalid precondition such as unsorted
         * binary search input"). Binary search does not — and structurally
         * cannot, without giving up its O(log n) advantage — check that its
         * input is actually sorted, so violating {@link
         * Searcher#requiresSortedInput()}'s precondition doesn't throw: it
         * silently returns the wrong answer.
         *
         * <p>{@code [50, 10, 30, 20, 40]} is unsorted; {@code 20} genuinely
         * sits at index 3. Traced by hand: mid=2 -> array[2]=30; since
         * 20 &lt; 30, the algorithm halves to the LEFT sub-range [0,1] =
         * [50,10] under the (violated) sorted-ascending assumption --
         * discarding index 3 entirely, even though that is exactly where
         * the target actually is. The next two steps search only within
         * [50,10] and correctly find nothing there, so the overall result
         * is -1: "not found", for an element that {@link
         * NormalCase#linearSearchFindsPresentElement} already proved is
         * present in the same array.
         */
        @Test
        @DisplayName("binarySearch on unsorted input can silently miss a present element")
        void binarySearchOnUnsortedInputCanSilentlyMissAPresentElement() {
            Integer[] unsorted = {50, 10, 30, 20, 40};

            int linearResult = searcher.linearSearch(unsorted, 20);
            int binaryResult = searcher.binarySearch(unsorted, 20);

            assertEquals(3, linearResult, "20 is genuinely present at index 3");
            assertEquals(-1, binaryResult,
                "binarySearch wrongly reports 'not found' because the precondition (sorted input) was violated");
            assertNotEquals(linearResult, binaryResult,
                "the two searches disagree on the same array precisely because binarySearch's precondition doesn't hold here");
        }

        @Test
        @DisplayName("requiresSortedInput() reports true, matching binarySearch's actual precondition")
        void requiresSortedInputReportsTrue() {
            assertTrue(searcher.requiresSortedInput());
        }
    }
}
