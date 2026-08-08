package edu.ug.nexusb.linear;

import edu.ug.nexusb.core.Instrumented;
import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * Custom dynamic (resizable) array-backed list.
 *
 * <p>Standalone for now: no {@code MyList<T>} interface exists in core/ as
 * of this writing (docs/interfaces.md mentions it, but the file was never
 * committed — flagged to the group). Wired to {@link MyIterable} and
 * {@link Instrumented} since those ARE real, committed core contracts.
 *
 * @param <T> the element type stored in this list
 */
public class DynamicArrayList<T> implements MyIterable<T>, Instrumented {

    private static final int DEFAULT_CAPACITY = 8;
    private static final int GROWTH_FACTOR = 2;

    private Object[] data;
    private int size;
    private int modCount;

    private long comparisons;
    private long movements;

    /** Creates an empty list with the default starting capacity. */
    public DynamicArrayList() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates an empty list with a given starting capacity.
     *
     * @param initialCapacity must be positive
     * @throws IllegalArgumentException if initialCapacity is not positive
     */
    public DynamicArrayList(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive, got: " + initialCapacity);
        }
        this.data = new Object[initialCapacity];
        this.size = 0;
    }

    /**
     * Returns how many elements are currently stored.
     *
     * @return the number of elements currently stored
     */
    public int size() {
        return size;
    }

    /**
     * Returns the current backing-array capacity, mainly for resize trace
     * tables in the report.
     *
     * @return the current backing-array capacity
     */
    public int capacity() {
        return data.length; // for resize trace tables
    }

    /**
     * Reports whether the list has no elements.
     *
     * @return true if the list has no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Appends value to the end of the list. Convenience wrapper around
     * {@link #insert(int, Object)}.
     *
     * @param value element to append
     */
    public void add(T value) {
        insert(size, value);
    }

    /**
     * Inserts value at index, shifting later elements one position right.
     *
     * @param index position to insert at, 0 to size inclusive
     * @param value element to insert
     * @throws IndexOutOfBoundsException if index is negative or greater than size
     */
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

    /**
     * Returns the element stored at the given index.
     *
     * @param index position to read, must be a valid existing index
     * @return the element currently stored at index
     * @throws IndexOutOfBoundsException if index is negative or out of range
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndexExists(index);
        return (T) data[index];
    }

    /**
     * Overwrites the element at index without changing the list's size.
     *
     * @param index position to overwrite, must be a valid existing index
     * @param value new value to store there
     * @throws IndexOutOfBoundsException if index is negative or out of range
     */
    public void set(int index, T value) {
        checkIndexExists(index);
        data[index] = value;
    }

    /**
     * Removes and returns the element at index, shifting later elements one
     * position left. Shrinks the backing array once occupancy drops to a
     * quarter of capacity.
     *
     * @param index position to remove, must be a valid existing index
     * @return the element that was removed
     * @throws IndexOutOfBoundsException if index is negative or out of range
     */
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

    /**
     * Linear search by {@code equals()}. Increments the comparison counter
     * once per element examined.
     *
     * @param value element to search for, compared with {@code equals()}
     * @return the index of the first match, or -1 if not found
     */
    @SuppressWarnings("unchecked")
    public int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            comparisons++;
            if (equalsSafe((T) data[i], value)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Reports whether an equal element is present anywhere in the list.
     *
     * @param value element to search for
     * @return true if an equal element is present anywhere in the list
     */
    public boolean contains(T value) {
        return indexOf(value) != -1;
    }

    private void resize(int newCapacity) {
        Object[] bigger = new Object[newCapacity];
        System.arraycopy(data, 0, bigger, 0, size);
        movements += size;
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

    /**
     * {@inheritDoc}
     * The returned iterator is fail-fast: any structural change to this
     * list (add/insert/remove) after the iterator is created causes the
     * next call to {@code next()} to throw {@link StructureException}.
     */
    @Override
    public MyIterator<T> iterator() {
        return new MyIterator<T>() {
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
                    throw new StructureException("list structurally modified during iteration");
                }
                if (!hasNext()) {
                    throw new StructureException("no more elements");
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