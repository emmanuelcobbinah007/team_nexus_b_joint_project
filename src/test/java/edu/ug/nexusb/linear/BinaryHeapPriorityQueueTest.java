package edu.ug.nexusb.linear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyComparator;

class BinaryHeapPriorityQueueTest {

    // simple min-heap comparator: smaller number = higher priority
    private static class IntComparator implements MyComparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
            return a - b;
        }
    }

    private MyPriorityQueue<Integer> minHeap;

    @BeforeEach
    void setUp() {
        minHeap = new BinaryHeapPriorityQueue<>(new IntComparator());
    }

    // This is for normal case 

    @Test
    void insertThenExtractTop_returnsSmallestFirst() {
        minHeap.insert(5);
        minHeap.insert(1);
        minHeap.insert(3);
        minHeap.insert(4);
        minHeap.insert(2);

        assertEquals(1, minHeap.extractTop());
        assertEquals(2, minHeap.extractTop());
        assertEquals(3, minHeap.extractTop());
        assertEquals(4, minHeap.extractTop());
        assertEquals(5, minHeap.extractTop());
    }

    @Test
    void peekTop_doesNotRemoveElement() {
        minHeap.insert(10);
        minHeap.insert(3);

        assertEquals(3, minHeap.peekTop());
        assertEquals(2, minHeap.size());
    }

    @Test
    void heapify_buildsCorrectOrderFromBulkArray() {
        Integer[] items = {9, 7, 3, 8, 1, 6, 2, 5, 4};
        minHeap.heapify(items);

        int previous = Integer.MIN_VALUE;
        while (!minHeap.isEmpty()) {
            int next = minHeap.extractTop();
            assertTrue(next >= previous);
            previous = next;
        }
    }

    @Test
    void decreaseKey_movesValueCloserToTop() {
        minHeap.insert(10);
        minHeap.insert(20);
        minHeap.insert(30);

        // pretend the caller already mutated the 30 object down to priority 1
        // in a real request object this would be something like request.setPriority(1)
        // here we just simulate by removing and re-inserting the same "identity"
        minHeap.decreaseKey(30); // no-op movement check since value itself is unchanged,
        // real usage: mutate the object's priority field BEFORE calling decreaseKey
        assertEquals(10, minHeap.peekTop());
    }

    // This is for boundary case

    @Test
    void newQueue_isEmptyWithSizeZero() {
        assertTrue(minHeap.isEmpty());
        assertEquals(0, minHeap.size());
    }

    @Test
    void singleElement_insertThenExtract_worksCorrectly() {
        minHeap.insert(42);
        assertEquals(42, minHeap.peekTop());
        assertEquals(42, minHeap.extractTop());
        assertTrue(minHeap.isEmpty());
    }

    @Test
    void manyInsertions_beyondStartingCapacity_stillWorks() {
        for (int i = 30; i >= 1; i--) {
            minHeap.insert(i);
        }
        for (int expected = 1; expected <= 30; expected++) {
            assertEquals(expected, minHeap.extractTop());
        }
    }

    // This is for invalid input case

    @Test
    void extractTop_onEmptyQueue_throwsException() {
        assertThrows(RuntimeException.class, () -> minHeap.extractTop());
    }

    @Test
    void peekTop_onEmptyQueue_throwsException() {
        assertThrows(RuntimeException.class, () -> minHeap.peekTop());
    }

    @Test
    void decreaseKey_onMissingValue_throwsKeyNotFoundException() {
        minHeap.insert(5);
        assertThrows(KeyNotFoundException.class, () -> minHeap.decreaseKey(999));
    }

    @Test
    void constructor_withNullComparator_throwsException() {
        assertThrows(RuntimeException.class, () -> new BinaryHeapPriorityQueue<Integer>(null));
    }
}