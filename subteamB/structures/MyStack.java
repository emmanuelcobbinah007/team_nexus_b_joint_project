package gh.ug.smartops.structures;

/**
 * Custom LIFO stack, used in this project for the examiner console's
 * undo/audit log (backs the audit_events table).
 *
 * Evidence required: an undo-log or recursion-simulation demo.
 *
 * @param <T> the element type stored in this stack
 */
public interface MyStack<T> {

    /**
     * Pushes an element onto the top of the stack.
     * @param value element to push
     */
    void push(T value);

    /**
     * Removes and returns the top element.
     * @return the former top element
     * @throws java.util.EmptyStackException if the stack is empty
     */
    T pop();

    /**
     * Returns the top element without removing it.
     * @return the current top element
     * @throws java.util.EmptyStackException if the stack is empty
     */
    T peek();

    /**
     * @return true if the stack has no elements
     */
    boolean isEmpty();

    /**
     * @return the number of elements currently on the stack
     */
    int size();
}
