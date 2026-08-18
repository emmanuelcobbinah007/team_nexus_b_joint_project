package edu.ug.nexusb.linear;

import java.util.NoSuchElementException;

/**
 * Growable circular-buffer deque (T028) — used by the scheduling engine so
 * an urgent request can jump to the front of the line via {@link
 * #addFront} while normal requests keep entering at the rear via {@link
 * #addRear}. All four add/remove operations are O(1) amortized.
 *
 * @param <T> the element type stored in this deque
 */
public class ArrayDeque<T> implements MyDeque<T> {

    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elements;
    private int front;
    private int size;

    public ArrayDeque() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.front = 0;
        this.size = 0;
    }

    public ArrayDeque(int capacity) {
        this.elements = new Object[capacity];
        this.front = 0;
        this.size = 0;
    }

    @Override
    public void addFront(T value) {
        if (size == elements.length) {
            resize(2 * elements.length);
        }
        front = (front - 1 + elements.length) % elements.length;
        elements[front] = value;
        size++;
    }

    @Override
    public void addRear(T value) {
        if (size == elements.length) {
            resize(2 * elements.length);
        }
        int rear = (front + size) % elements.length;
        elements[rear] = value;
        size++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T removeFront() {
        if (isEmpty()) {
            throw new NoSuchElementException("Deque is empty");
        }
        T value = (T) elements[front];
        elements[front] = null;
        front = (front + 1) % elements.length;
        size--;
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T removeRear() {
        if (isEmpty()) {
            throw new NoSuchElementException("Deque is empty");
        }
        int rear = (front + size - 1) % elements.length;
        T value = (T) elements[rear];
        elements[rear] = null;
        size--;
        return value;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    private void resize(int newCapacity) {
        Object[] newArray = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newArray[i] = elements[(front + i) % elements.length];
        }
        elements = newArray;
        front = 0;
    }
}
