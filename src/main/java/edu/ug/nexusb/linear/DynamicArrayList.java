package edu.ug.nexusb.linear;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom dynamic (resizable) array-backed list.
 *
 * TODO once core is committed: change signature to
 *   public class DynamicArrayList<T> implements MyList<T>, Instrumented
 * and swap IndexOutOfBoundsException / NoSuchElementException for the real
 * edu.ug.nexusb.core.StructureException subclasses (EmptyStructureException,
 * KeyNotFoundException) once Cobbinah commits them. Method names/signatures
 * below may also need to change to exactly match core.MyList<T> — check
 * docs/interfaces.md again once the .java files exist.
 */
public class DynamicArrayList<T> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final int GROWTH_FACTOR = 2;

    private Object[] data;
    private int size;
    private int modCount; // structural-change counter, backs the fail-fast iterator

    // --- Instrumented-style counters (see docs/interfaces.md: "Instrumented") ---
    private long comparisons;
    private long movements;

    public DynamicArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public DynamicArrayList(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive, got: " + initialCapacity);
        }
        this.data = new Object[initialCapacity];
        this.size = 0;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length; // for resize trace tables
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(T value) {
        insert(size, value);
    }

    public void insert(int index, T value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (size == data.length) {
            resize(data.length * GROWTH_FACTOR);
        }
        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
            movements++;
        }
        data[index] = value;
        size++;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndexExists(index);
        return (T) data[index];
    }

    public void set(int index, T value) {
        checkIndexExists(index);
        data[index] = value;
        // set() does not change structure, so modCount is untouched (matches Java convention)
    }

    @SuppressWarnings("unchecked")
    public T remove(int index) {
        checkIndexExists(index);
        T removed = (T) data[index];
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
            movements++;
        }
        data[size - 1] = null;
        size--;
        modCount++;

        if (size > 0 && size == data.length / 4) {
            resize(Math.max(DEFAULT_CAPACITY, data.length / 2));
        }
        return removed;
    }

    /** Linear search by equality. Returns -1 if not found. Increments the comparison counter. */
    public int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            comparisons++;
            if (equalsSafe((T) data[i], value)) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(T value) {
        return indexOf(value) != -1;
    }

    private void resize(int newCapacity) {
        Object[] bigger = new Object[newCapacity];
        System.arraycopy(data, 0, bigger, 0, size);
        movements += size; // relocating every element counts as work done
        data = bigger;
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

    /** Resettable by the benchmark harness between repetitions (see docs/interfaces.md). */
    public void resetCounters() {
        comparisons = 0;
        movements = 0;
    }

    // --- fail-fast iteration ---

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int cursor = 0;
            private final int expectedModCount = modCount;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException(
                            "list structurally modified during iteration");
                }
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (T) data[cursor++];
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(data[i]);
            if (i < size - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}