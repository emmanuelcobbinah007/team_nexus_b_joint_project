package gh.ug.smartops.structures;

/**
 * Custom double-ended queue, used in the scheduling engine to let an urgent
 * request jump to the front of the line rather than waiting behind normal
 * FIFO requests.
 *
 * Evidence required: a trace of an urgent-request insertion at the front
 * while normal requests still enter at the rear.
 *
 * @param <T> the element type stored in this deque
 */
public interface MyDeque<T> {

    /** Adds an element at the front. @param value element to add */
    void addFront(T value);

    /** Adds an element at the rear. @param value element to add */
    void addRear(T value);

    /**
     * Removes and returns the front element.
     * @return the former front element
     * @throws java.util.NoSuchElementException if the deque is empty
     */
    T removeFront();

    /**
     * Removes and returns the rear element.
     * @return the former rear element
     * @throws java.util.NoSuchElementException if the deque is empty
     */
    T removeRear();

    /**
     * @return true if the deque has no elements
     */
    boolean isEmpty();

    /**
     * @return the number of elements currently in the deque
     */
    int size();
}
