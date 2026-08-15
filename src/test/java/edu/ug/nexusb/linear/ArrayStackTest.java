package edu.ug.nexusb.linear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EmptyStackException;
import org.junit.jupiter.api.Test;

class ArrayStackTest {

    // ---- normal case ----

    @Test
    void pushThenPopReturnsElementsInLifoOrder() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);

        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
    }

    @Test
    void peekReturnsTopWithoutRemovingIt() {
        ArrayStack<String> stack = new ArrayStack<>();
        stack.push("a");
        stack.push("b");

        assertEquals("b", stack.peek());
        assertEquals(2, stack.size());
        assertEquals("b", stack.pop());
    }

    @Test
    void sizeTracksPushesAndPops() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        assertEquals(0, stack.size());
        stack.push(10);
        stack.push(20);
        assertEquals(2, stack.size());
        stack.pop();
        assertEquals(1, stack.size());
    }

    @Test
    void growsPastInitialCapacity() {
        ArrayStack<Integer> stack = new ArrayStack<>(2);
        for (int i = 0; i < 20; i++) {
            stack.push(i);
        }
        assertEquals(20, stack.size());
        for (int i = 19; i >= 0; i--) {
            assertEquals(i, stack.pop());
        }
    }

    // ---- boundary case ----

    @Test
    void newStackIsEmpty() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    void singleElementPushAndPop() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        stack.push(42);
        assertFalse(stack.isEmpty());
        assertEquals(42, stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void stackIsEmptyAgainAfterPoppingEverything() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        stack.push(1);
        stack.push(2);
        stack.pop();
        stack.pop();
        assertTrue(stack.isEmpty());
    }

    // ---- invalid input ----

    @Test
    void popOnEmptyStackThrows() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        assertThrows(EmptyStackException.class, stack::pop);
    }

    @Test
    void peekOnEmptyStackThrows() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        assertThrows(EmptyStackException.class, stack::peek);
    }

    @Test
    void popAfterDrainingThrows() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        stack.push(1);
        stack.pop();
        assertThrows(EmptyStackException.class, stack::pop);
    }

    // ---- undo-log demo (evidence required by MyStack's javadoc) ----

    /**
     * Demonstrates the exact use case {@link MyStack}'s javadoc names: an
     * audit trail where each action is pushed as it happens, and "undo"
     * means popping — which always reverses the *most recent* action
     * first, never an earlier one, regardless of how many actions happened
     * in between. This is the property that makes a stack (not a queue)
     * the right structure for undo.
     */
    @Test
    void undoLogReversesActionsMostRecentFirst() {
        ArrayStack<String> auditLog = new ArrayStack<>();

        auditLog.push("TRIAGED case REQ0001");
        auditLog.push("ASSIGNED resource R012 to REQ0001");
        auditLog.push("STATUS_CHANGED REQ0001 -> IN_TRANSIT");

        // Undo the most recent action: the status change, not the triage
        // or the assignment, even though those happened first.
        assertEquals("STATUS_CHANGED REQ0001 -> IN_TRANSIT", auditLog.pop());

        // The assignment is now most recent; undoing again reverses that,
        // not the original triage.
        assertEquals("ASSIGNED resource R012 to REQ0001", auditLog.pop());

        // One action left: the original triage.
        assertEquals("TRIAGED case REQ0001", auditLog.peek());
        assertEquals(1, auditLog.size());
    }
}
