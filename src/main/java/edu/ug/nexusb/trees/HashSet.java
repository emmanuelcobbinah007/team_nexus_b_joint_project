package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyIterator;

/**
 * {@link MySet} implementation (T035) — a thin wrapper over {@link
 * ChainedHashTable}, storing each element as a key mapped to a shared
 * sentinel value. {@code add}/{@code remove} cost exactly one hash lookup
 * each: {@link MyMap#put} and {@link MyMap#remove} already report back the
 * previous value, so "was this already present?" falls out of that return
 * value for free rather than needing a separate {@code containsKey} check
 * first.
 *
 * <p>Instrumentation ({@link #comparisonCount()}, {@link #movementCount()},
 * {@link #resetCounters()}) delegates straight to the backing table — this
 * wrapper does no comparisons or movements of its own, so its counts are
 * exactly the table's counts.
 *
 * @param <T> the element type
 */
public class HashSet<T> implements MySet<T> {

    private static final Object PRESENT = new Object();

    private final int initialCapacity;
    private MyHashTable<T, Object> table;

    /** Creates an empty set starting at {@link ChainedHashTable#INITIAL_TABLE_SIZE}. */
    public HashSet() {
        this(ChainedHashTable.INITIAL_TABLE_SIZE);
    }

    /**
     * Creates an empty set with a given starting capacity.
     *
     * @param initialCapacity starting bucket-array length; must be positive
     * @throws IllegalArgumentException if {@code initialCapacity} is not positive
     */
    public HashSet(int initialCapacity) {
        this.initialCapacity = initialCapacity;
        this.table = new ChainedHashTable<>(initialCapacity);
    }

    @Override
    public boolean add(T value) {
        requireValue(value);
        return table.put(value, PRESENT) == null;
    }

    @Override
    public boolean remove(T value) {
        requireValue(value);
        return table.remove(value) != null;
    }

    @Override
    public boolean contains(T value) {
        requireValue(value);
        return table.containsKey(value);
    }

    @Override
    public void clear() {
        // MyMap has no clear() of its own; a fresh table is the simplest
        // correct way to empty this set without reaching into the
        // backing table's internals.
        table = new ChainedHashTable<>(initialCapacity);
    }

    @Override
    public int size() {
        return table.size();
    }

    @Override
    public boolean isEmpty() {
        return table.isEmpty();
    }

    @Override
    public MyIterator<T> iterator() {
        MyIterator<MyMap.MapEntry<T, Object>> entries = table.entries().iterator();
        return new MyIterator<T>() {
            @Override
            public boolean hasNext() {
                return entries.hasNext();
            }

            @Override
            public T next() {
                return entries.next().getKey();
            }
        };
    }

    @Override
    public long comparisonCount() {
        return table.comparisonCount();
    }

    @Override
    public long movementCount() {
        return table.movementCount();
    }

    @Override
    public void resetCounters() {
        table.resetCounters();
    }

    private static void requireValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
}
