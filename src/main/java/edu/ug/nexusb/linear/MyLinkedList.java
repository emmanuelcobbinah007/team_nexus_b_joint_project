package edu.ug.nexusb.linear;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom doubly linked list with indexed access and a fail-fast iterator.
 *
 * TODO once core is committed: change signature to
 *   public class MyLinkedList<T> implements MyList<T>, Instrumented
 * and swap standard exceptions for the real core.StructureException
 * subclasses once Cobbinah commits them.
 *
 * Indexed methods (get/set/insert/remove by index) exist so the report can
 * fairly compare this against DynamicArrayList's O(1) indexing, per
 * docs/interfaces.md ("The array offers O(1) indexing ... the linked list
 * offers ... O(n) indexing. The report compares them.").
 */
public class MyLinkedList<T> implements Iterable<T> {

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
    private int modCount;

    // --- Instrumented-style counters ---
    private long comparisons;
    private long movements; // one increment per node pointer we traverse or relink

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

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

    public void add(T value) {
        addLast(value);
    }

    // --- indexed access (O(n)) — mirrors DynamicArrayList's contract ---

    public T get(int index) {
        checkIndexExists(index);
        return nodeAt(index).value;
    }

    public void set(int index, T value) {
        checkIndexExists(index);
        nodeAt(index).value = value;
        // does not change structure -> modCount untouched
    }

    public void insert(int index, T value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (index == 0) {
            addFirst(value);
        } else if (index == size) {
            addLast(value);
        } else {
            Node<T> after = nodeAt(index);
            Node<T> before = after.prev;
            Node<T> n = new Node<>(value);
            n.prev = before;
            n.next = after;
            before.next = n;
            after.prev = n;
            size++;
            movements++;
            modCount++;
        }
    }

    public T remove(int index) {
        checkIndexExists(index);
        Node<T> target = nodeAt(index);
        T removed = target.value;
        unlink(target);
        return removed;
    }

    /** Walks from head, counting each hop as a movement (the O(n) cost the report measures). */
    private Node<T> nodeAt(int index) {
        Node<T> cur = head;
        for (int i = 0; i < index; i++) {
            cur = cur.next;
            movements++;
        }
        return cur;
    }

    // --- value-based convenience operations (named removeValue to avoid
    // colliding with remove(int index) when T = Integer) ---

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
        throw new NoSuchElementException("target value not found in list: " + target);
    }

    public boolean removeValue(T value) {
        Node<T> cur = head;
        while (cur != null) {
            comparisons++;
            if (equalsSafe(cur.value, value)) {
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

    public T getFirst() {
        if (isEmpty()) throw new NoSuchElementException("list is empty");
        return head.value;
    }

    public T getLast() {
        if (isEmpty()) throw new NoSuchElementException("list is empty");
        return tail.value;
    }

    private void checkIndexExists(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private boolean equalsSafe(T a, T b) {
        return a == null ? b == null : a.equals(b);
    }

    // --- Instrumented ---

    public long getComparisons() {
        return comparisons;
    }

    public long getMovements() {
        return movements;
    }

    public void resetCounters() {
        comparisons = 0;
        movements = 0;
    }

    // --- fail-fast iteration ---

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;
            private final int expectedModCount = modCount;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException(
                            "list structurally modified during iteration");
                }
                if (!hasNext()) {
                    throw new NoSuchElementException("no more elements");
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