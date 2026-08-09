package edu.ug.nexusb.linear;

import java.util.NoSuchElementException;

public class ArrayCircularQueue<T> implements CircularQueue<T> {
    private final Object[] elements;
    private final int capacity;
    private int front;
    private int rear;
    private int size;

    public ArrayCircularQueue(int capacity) {
        this.capacity = capacity;
        this.elements = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public boolean isFull() {
        return this.size == this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void enqueue(T value) {
        if (isFull()) {
            throw new IllegalStateException("Queue is full");
        }
        rear = (rear + 1) % capacity;
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
        elements[front] = null; // Prevent memory leaks
        front = (front + 1) % capacity;
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
}