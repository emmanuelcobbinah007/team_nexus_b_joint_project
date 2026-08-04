package edu.ug.nexusb.linear;

/**
 * Fixed-capacity queue that reuses freed slots by wrapping the front/rear
 * indices around a backing array, rather than shifting elements.
 *
 * Evidence required: a trace showing how front and rear move (including at
 * least one wrap-around) as elements are enqueued and dequeued.
 *
 * @param <T> the element type stored in this queue
 */
public interface CircularQueue<T> extends MyQueue<T> {

    /**
     * @return the fixed maximum number of elements this queue can hold
     */
    int capacity();

    /**
     * @return true if size() == capacity(), i.e. no more room without a
     *         dequeue first
     */
    boolean isFull();

    /**
     * Adds an element to the rear, wrapping the rear index if it reaches
     * the end of the backing array.
     *
     * @param value element to enqueue
     * @throws IllegalStateException if the queue is full
     */
    @Override
    void enqueue(T value);
}
