package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyComparator;

// My binary heap priority queue. Backed by a plain array (no java.util
// classes). The MyComparator decides what "highest priority" means:
// whatever compare() treats as "smaller" is what extractTop() returns.
public class BinaryHeapPriorityQueue<T> implements MyPriorityQueue<T> {

    private Object[] data;
    private int count;
    private final MyComparator<? super T> comparator;

    public BinaryHeapPriorityQueue(MyComparator<? super T> comparator) {
        if (comparator == null) {
            throw new RuntimeException("comparator cannot be null");
        }
        this.comparator = comparator;
        this.data = new Object[16];
        this.count = 0;
    }

    @Override
    public void insert(T value) {
        growIfNeeded();
        data[count] = value;
        siftUp(count);
        count++;
    }

    @Override
    public T extractTop() {
        if (isEmpty()) {
            throw new RuntimeException("cannot extractTop from an empty priority queue");
        }
        T top = (T) data[0];

        count--;
        data[0] = data[count];
        data[count] = null;
        if (count > 0) {
            siftDown(0);
        }
        return top;
    }

    @Override
    public T peekTop() {
        if (isEmpty()) {
            throw new RuntimeException("cannot peekTop on an empty priority queue");
        }
        return (T) data[0];
    }

    @Override
    public void heapify(T[] items) {
        if (items == null) {
            throw new RuntimeException("items cannot be null");
        }

        data = new Object[items.length > 16 ? items.length : 16];
        for (int i = 0; i < items.length; i++) {
            data[i] = items[i];
        }
        count = items.length;

        // start from the last parent and sift down towards the root
        for (int i = (count / 2) - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    @Override
    public void decreaseKey(T value) {
        int index = indexOf(value);
        if (index == -1) {
            throw new KeyNotFoundException("value not found in priority queue: " + value);
        }
        siftUp(index);
    }

    @Override
    public MyComparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public int size() {
        return count;
    }

    // ---- private helper methods ----

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (beats(i, parent)) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int best = i;

            if (left < count && beats(left, best)) {
                best = left;
            }
            if (right < count && beats(right, best)) {
                best = right;
            }
            if (best == i) {
                break;
            }
            swap(i, best);
            i = best;
        }
    }

    // true if the element at index a should be higher priority than index b
    private boolean beats(int a, int b) {
        T valueA = (T) data[a];
        T valueB = (T) data[b];
        return comparator.compare(valueA, valueB) < 0;
    }

    private void swap(int a, int b) {
        Object temp = data[a];
        data[a] = data[b];
        data[b] = temp;
    }

    private int indexOf(T value) {
        for (int i = 0; i < count; i++) {
            if (data[i] != null && data[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private void growIfNeeded() {
        if (count < data.length) {
            return;
        }
        Object[] bigger = new Object[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            bigger[i] = data[i];
        }
        data = bigger;
    }
}