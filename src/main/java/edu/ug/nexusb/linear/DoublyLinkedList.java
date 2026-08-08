package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.Instrumented;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * Concrete doubly linked list implementing the frozen {@link MyLinkedList}
 * contract (see docs/interfaces.md, T018). Also implements
 * {@link Instrumented} so this structure can be timed and compared against
 * {@link DynamicArrayList} by the benchmark harness.
 *
 * @param <T> the element type stored in this list
 */
public class DoublyLinkedList<T> implements MyLinkedList<T>, Instrumented {

    private static class Node<T> {
        T value;
        Node<T> prev;
        Node<T> next;
        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;
    private int modCount; // backs the fail-fast iterator

    private long comparisons;
    private long movements;

    /** Creates an empty list. */
    public DoublyLinkedList() {
    }

    @Override
    public void addFirst(T value) {
        Node<T> n = new Node<>(value);
        if (isEmpty()) {
            head = tail = n;
        } else {
            n.next = head;
            head.prev = n;
            head = n;
        }
        size++;
        movements++;
        modCount++;
    }

    @Override
    public void addLast(T value) {
        Node<T> n = new Node<>(value);
        if (isEmpty()) {
            head = tail = n;
        } else {
            tail.next = n;
            n.prev = tail;
            tail = n;
        }
        size++;
        movements++;
        modCount++;
    }

    /**
     * {@inheritDoc}
     * Per the interface Javadoc this specifically throws
     * java.util.NoSuchElementException (not StructureException) when
     * target is not found — that's the documented contract, kept exact.
     */
    @Override
    public void insertAfter(T target, T newValue) {
        Node<T> cur = head;
        while (cur != null) {
            comparisons++;
            if (equalsSafe(cur.value, target)) {
                Node<T> n = new Node<>(newValue);
                n.prev = cur;
                n.next = cur.next;
                if (cur.next != null) {
                    cur.next.prev = n;
                } else {
                    tail = n;
                }
                cur.next = n;
                size++;
                movements++;
                modCount++;
                return;
            }
            cur = cur.next;
        }
        throw new java.util.NoSuchElementException("target value not found in list: " + target);
    }

    @Override
    public boolean remove(T target) {
        Node<T> cur = head;
        while (cur != null) {
            comparisons++;
            if (equalsSafe(cur.value, target)) {
                unlink(cur);
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    private void unlink(Node<T> n) {
        if (n.prev != null) {
            n.prev.next = n.next;
        } else {
            head = n.next;
        }
        if (n.next != null) {
            n.next.prev = n.prev;
        } else {
            tail = n.prev;
        }
        n.prev = null;
        n.next = null;
        size--;
        movements++;
        modCount++;
    }

    /**
     * Returns the value stored at the head of the list.
     *
     * @return the value stored at the head of the list
     * @throws StructureException if the list is empty
     */
    public T getFirst() {
        if (isEmpty()) throw new StructureException("list is empty");
        return head.value;
    }

    /**
     * Returns the value stored at the tail of the list.
     *
     * @return the value stored at the tail of the list
     * @throws StructureException if the list is empty
     */
    public T getLast() {
        if (isEmpty()) throw new StructureException("list is empty");
        return tail.value;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    private boolean equalsSafe(T a, T b) {
        return a == null ? b == null : a.equals(b);
    }

    // --- Instrumented ---

    /** {@inheritDoc} */
    @Override
    public long comparisonCount() {
        return comparisons;
    }

    /** {@inheritDoc} */
    @Override
    public long movementCount() {
        return movements;
    }

    /** {@inheritDoc} */
    @Override
    public void resetCounters() {
        comparisons = 0;
        movements = 0;
    }

    // --- MyIterable<T> / fail-fast MyIterator<T> ---

    @Override
    public MyIterator<T> iterator() {
        return new MyIterator<T>() {
            private Node<T> current = head;
            private final int expectedModCount = modCount;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (modCount != expectedModCount) {
                    throw new StructureException("list structurally modified during iteration");
                }
                if (!hasNext()) {
                    throw new StructureException("no more elements");
                }
                T val = current.value;
                current = current.next;
                return val;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> cur = head;
        while (cur != null) {
            sb.append(cur.value);
            if (cur.next != null) sb.append(", ");
            cur = cur.next;
        }
        return sb.append("]").toString();
    }
}