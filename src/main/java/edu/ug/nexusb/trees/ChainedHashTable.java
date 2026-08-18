package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * Separate-chaining hash table implementing {@link MyHashTable} (T030).
 * Each bucket is a singly-linked chain of hand-rolled nodes — no
 * {@code java.util} list backs it, consistent with the rest of this
 * package.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ChainedHashTable<K, V> implements MyHashTable<K, V> {

    /** Index-number-derived starting capacity — see docs/parameters.md. */
    public static final int INITIAL_TABLE_SIZE = 53;

    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final class Entry<K, V> implements MapEntry<K, V> {
        private final K key;
        private final V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }
    }

    private Node<K, V>[] buckets;
    private int size;
    private int collisionCount;
    private int resizeCount;
    private long comparisonCount;
    private long movementCount;

    /** Creates an empty table starting at {@link #INITIAL_TABLE_SIZE}. */
    public ChainedHashTable() {
        this(INITIAL_TABLE_SIZE);
    }

    /**
     * Creates an empty table with a given starting capacity.
     *
     * @param initialCapacity starting bucket-array length; must be positive
     * @throws IllegalArgumentException if {@code initialCapacity} is not positive
     */
    @SuppressWarnings("unchecked")
    public ChainedHashTable(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive");
        }
        this.buckets = new Node[initialCapacity];
        this.size = 0;
    }

    @Override
    public V put(K key, V value) {
        requireKey(key);
        int index = indexFor(key, buckets.length);
        Node<K, V> current = buckets[index];
        while (current != null) {
            comparisonCount++;
            if (current.key.equals(key)) {
                V previous = current.value;
                current.value = value;
                movementCount++;
                return previous;
            }
            current = current.next;
        }

        if (buckets[index] != null) {
            collisionCount++;
        }
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;
        movementCount++;

        if (loadFactor() > LOAD_FACTOR_THRESHOLD) {
            resize(buckets.length * 2);
        }
        return null;
    }

    @Override
    public V get(K key) {
        requireKey(key);
        Node<K, V> current = buckets[indexFor(key, buckets.length)];
        while (current != null) {
            comparisonCount++;
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    @Override
    public V remove(K key) {
        requireKey(key);
        int index = indexFor(key, buckets.length);
        Node<K, V> current = buckets[index];
        Node<K, V> previous = null;
        while (current != null) {
            comparisonCount++;
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                movementCount++;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    @Override
    public boolean containsKey(K key) {
        requireKey(key);
        Node<K, V> current = buckets[indexFor(key, buckets.length)];
        while (current != null) {
            comparisonCount++;
            if (current.key.equals(key)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public MyIterable<MapEntry<K, V>> entries() {
        @SuppressWarnings("unchecked")
        MapEntry<K, V>[] snapshot = new MapEntry[size];
        int i = 0;
        for (Node<K, V> bucket : buckets) {
            for (Node<K, V> current = bucket; current != null; current = current.next) {
                snapshot[i] = new Entry<>(current.key, current.value);
                i++;
            }
        }
        return arrayIterable(snapshot);
    }

    @Override
    public int collisionCount() {
        return collisionCount;
    }

    @Override
    public double loadFactor() {
        return ((double) size) / buckets.length;
    }

    @Override
    public int longestBucket() {
        int max = 0;
        for (Node<K, V> bucket : buckets) {
            int length = 0;
            for (Node<K, V> current = bucket; current != null; current = current.next) {
                length++;
            }
            if (length > max) {
                max = length;
            }
        }
        return max;
    }

    @Override
    public int resizeCount() {
        return resizeCount;
    }

    @Override
    public int capacity() {
        return buckets.length;
    }

    @Override
    public long comparisonCount() {
        return comparisonCount;
    }

    @Override
    public long movementCount() {
        return movementCount;
    }

    @Override
    public void resetCounters() {
        comparisonCount = 0;
        movementCount = 0;
        collisionCount = 0;
        resizeCount = 0;
    }

    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        Node<K, V>[] oldBuckets = buckets;
        buckets = new Node[newCapacity];
        for (Node<K, V> bucket : oldBuckets) {
            Node<K, V> current = bucket;
            while (current != null) {
                Node<K, V> next = current.next;
                int index = indexFor(current.key, newCapacity);
                current.next = buckets[index];
                buckets[index] = current;
                movementCount++;
                current = next;
            }
        }
        resizeCount++;
    }

    private static int indexFor(Object key, int capacity) {
        return Math.floorMod(key.hashCode(), capacity);
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
    }

    private static <T> MyIterable<T> arrayIterable(T[] items) {
        return () -> new MyIterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < items.length;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new StructureException("iterator has no more elements");
                }
                return items[index++];
            }
        };
    }
}
