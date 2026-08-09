package edu.ug.nexusb.linear;

import java.util.NoSuchElementException;

public class ArrayQueue<T> implements MyQueue<T> {
    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elements;
    private int front;
    private int rear;
    private int size;

    public ArrayQueue() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    public ArrayQueue(int capacity) {
        this.elements = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    @Override
    public void enqueue(T value) {
        if (size == elements.length) {
            resize(2 * elements.length);
        }
        rear = (rear + 1) % elements.length;
        elements[rear] = value;
        size++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        T value = (T) elements[front];
        elements[front] = null; // Prevent memory loitering
        front = (front + 1) % elements.length;
        size--;
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T peekFront() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return (T) elements[front];
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
        this.elements = newArray;
        this.front = 0;
        this.rear = size - 1;
    }
}