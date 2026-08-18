package edu.ug.nexusb.linear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ArrayDequeTest {

    // ---- normal case ----

    @Test
    void addRearThenRemoveFrontIsFifoOrder() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addRear(1);
        deque.addRear(2);
        deque.addRear(3);

        assertEquals(1, deque.removeFront());
        assertEquals(2, deque.removeFront());
        assertEquals(3, deque.removeFront());
    }

    @Test
    void addFrontThenRemoveFrontIsLifoOrder() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addFront(1);
        deque.addFront(2);
        deque.addFront(3);

        assertEquals(3, deque.removeFront());
        assertEquals(2, deque.removeFront());
        assertEquals(1, deque.removeFront());
    }

    @Test
    void removeRearReturnsMostRecentlyAddedRearElement() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addRear(1);
        deque.addRear(2);
        deque.addRear(3);

        assertEquals(3, deque.removeRear());
        assertEquals(2, deque.removeRear());
        assertEquals(1, deque.removeRear());
    }

    @Test
    void mixingAddFrontAndAddRearKeepsBothEndsCorrect() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addRear(2);   // [2]
        deque.addFront(1);  // [1, 2]
        deque.addRear(3);   // [1, 2, 3]
        deque.addFront(0);  // [0, 1, 2, 3]

        assertEquals(4, deque.size());
        assertEquals(3, deque.removeRear());
        assertEquals(0, deque.removeFront());
        assertEquals(1, deque.removeFront());
        assertEquals(2, deque.removeRear());
        assertTrue(deque.isEmpty());
    }

    @Test
    void growsPastInitialCapacityFromBothEnds() {
        ArrayDeque<Integer> deque = new ArrayDeque<>(2);
        for (int i = 0; i < 10; i++) {
            deque.addRear(i);
        }
        for (int i = -1; i >= -10; i--) {
            deque.addFront(i);
        }
        assertEquals(20, deque.size());

        for (int i = -10; i < 10; i++) {
            assertEquals(i, deque.removeFront());
        }
        assertTrue(deque.isEmpty());
    }

    // ---- boundary case ----

    @Test
    void newDequeIsEmpty() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    @Test
    void singleElementAddFrontRemoveRear() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addFront(42);
        assertEquals(42, deque.removeRear());
        assertTrue(deque.isEmpty());
    }

    @Test
    void singleElementAddRearRemoveFront() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addRear(42);
        assertEquals(42, deque.removeFront());
        assertTrue(deque.isEmpty());
    }

    @Test
    void dequeIsEmptyAgainAfterDrainingFromBothEnds() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addFront(1);
        deque.addRear(2);
        deque.addFront(0);
        deque.removeRear();
        deque.removeFront();
        deque.removeFront();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    // ---- invalid input ----

    @Test
    void removeFrontOnEmptyDequeThrows() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        assertThrows(NoSuchElementException.class, deque::removeFront);
    }

    @Test
    void removeRearOnEmptyDequeThrows() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        assertThrows(NoSuchElementException.class, deque::removeRear);
    }

    @Test
    void removeFrontAfterDrainingThrows() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addRear(1);
        deque.removeFront();
        assertThrows(NoSuchElementException.class, deque::removeFront);
    }

    // ---- urgent-request demo (evidence required by MyDeque's javadoc) ----

    /**
     * Demonstrates the exact scheduling scenario {@link MyDeque}'s javadoc
     * names: normal requests enter at the rear and are served in that
     * order, but an urgent request pushed to the front jumps every normal
     * request already waiting, without disturbing their relative order.
     */
    @Test
    void urgentRequestJumpsTheLineAheadOfNormalRequests() {
        ArrayDeque<String> waitingList = new ArrayDeque<>();

        waitingList.addRear("REQ0001 (routine)");
        waitingList.addRear("REQ0002 (routine)");
        waitingList.addRear("REQ0003 (routine)");

        // An urgent case arrives and must be served before any routine one.
        waitingList.addFront("REQ0099 (URGENT)");

        assertEquals("REQ0099 (URGENT)", waitingList.removeFront());

        // The three routine requests are served next, in the same relative
        // order they arrived in -- the urgent insertion didn't reorder them.
        assertEquals("REQ0001 (routine)", waitingList.removeFront());
        assertEquals("REQ0002 (routine)", waitingList.removeFront());
        assertEquals("REQ0003 (routine)", waitingList.removeFront());
        assertTrue(waitingList.isEmpty());
    }
}
