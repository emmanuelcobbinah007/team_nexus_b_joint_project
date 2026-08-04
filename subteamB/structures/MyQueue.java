package gh.ug.smartops.structures;

/**
 * Custom FIFO queue. This is the base contract used for the plain (unbounded)
 * FIFO dispatch rule in the scheduling engine (Module M5).
 *
 * @param <T> the element type stored in this queue
 */
public interface MyQueue<T> {

    /**
     * Adds an element to the rear of the queue.
     * @param value element to enqueue
     */
    void enqueue(T value);

    /**
     * Removes and returns the element at the front of the queue.
     * @return the former front element
     * @throws java.util.NoSuchElementException if the queue is empty
     */
    T dequeue();

    /**
     * Returns the front element without removing it.
     * @return the current front element
     * @throws java.util.NoSuchElementException if the queue is empty
     */
    T peekFront();

    /**
     * @return true if the queue has no elements
     */
    boolean isEmpty();

    /**
     * @return the number of elements currently in the queue
     */
    int size();
}
