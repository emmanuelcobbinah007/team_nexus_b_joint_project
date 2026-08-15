package edu.ug.nexusb.linear;

import java.util.EmptyStackException;

/**
 * Array-backed LIFO stack (T026) — backs the examiner console's undo/audit
 * log and the iterative DFS traversal.
 *
 * @param <T> the element type stored in this stack
 */
public class ArrayStack<T> implements MyStack<T> {

    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elements;
    private int size;

    public ArrayStack() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    public ArrayStack(int capacity) {
        this.elements = new Object[capacity];
        this.size = 0;
    }

    @Override
    public void push(T value) {
        if (size == elements.length) {
            resize(2 * elements.length);
        }
        elements[size] = value;
        size++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        size--;
        T value = (T) elements[size];
        elements[size] = null;
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return (T) elements[size - 1];
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
            newArray[i] = elements[i];
        }
        elements = newArray;
    }
}
