package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * A {@link MyHashTable} implementation using separate chaining, sized on
 * construction with {@link #INITIAL_TABLE_SIZE} - the hash-table-size
 * parameter derived from the team's index numbers in {@code docs/parameters.md}
 * (T005), so this table never starts at a hardcoded literal.
 *
 * <p>Each bucket is a singly-linked chain of {@link Node}s. A lookup walks
 * the target bucket's chain comparing keys with {@link Object#equals}; a
 * chain longer than one entry is exactly what {@link #collisionCount()} is
 * counting, and {@link #longestBucket()} is the longest such chain right
 * now. When the load factor exceeds {@value #LOAD_FACTOR_THRESHOLD} after an
 * insert, every entry is rehashed into a new bucket array roughly double the
 * size (rounded up to the next prime, same reasoning
 * {@code docs/parameters.md} used to pick 53 - a prime table length spreads
 * hash values more evenly than a power of two when the hash function itself
 * has structure, e.g. sequential integer keys).
 *
 * <p>As documented on {@link MyHashTable}, {@link #resetCounters()} widens
 * the inherited {@code Instrumented} contract to also zero
 * {@link #collisionCount()} and {@link #resizeCount()}, so a benchmark
 * repetition never inherits collision/resize counts from the previous one.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class ChainedHashTable<K, V> implements MyHashTable<K, V> {

    /**
     * The starting bucket-array length, derived in {@code docs/parameters.md}
     * (T005) from the team roster's index numbers rather than chosen
     * arbitrarily.
     */
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

    private static final class SimpleEntry<K, V> implements MapEntry<K, V> {
        private final K key;
        private final V value;

        SimpleEntry(K key, V value) {
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

    /** A minimal growable {@code Object[]} buffer - the {@code java.util}-free substitute for a list. */
    private static final class ObjBuffer {
        private Object[] data = new Object[8];
        private int count = 0;

        void add(Object o) {
            if (count == data.length) {
                Object[] grown = new Object[data.length * 2];
                System.arraycopy(data, 0, grown, 0, count);
                data = grown;
            }
            data[count++] = o;
        }
    }

    private final class SnapshotIterable<T> implements MyIterable<T> {
        private final Object[] data;
        private final int count;
        private final int snapshotModCount;

        SnapshotIterable(ObjBuffer buffer, int snapshotModCount) {
            this.data = buffer.data;
            this.count = buffer.count;
            this.snapshotModCount = snapshotModCount;
        }

        @Override
        public MyIterator<T> iterator() {
            return new MyIterator<T>() {
                private int position = 0;

                @Override
                public boolean hasNext() {
                    return position < count;
                }

                @Override
                @SuppressWarnings("unchecked")
                public T next() {
                    if (modCount != snapshotModCount) {
                        throw new StructureException(
                                "table was structurally modified since this iterator was created");
                    }
                    if (position >= count) {
                        throw new StructureException("no more elements");
                    }
                    return (T) data[position++];
                }
            };
        }
    }

    private Node<K, V>[] buckets;
    private int size;
    private int collisions;
    private int resizes;
    private long comparisons;
    private long movements;
    private int modCount;

    /** Creates an empty table starting at {@link #INITIAL_TABLE_SIZE} buckets. */
    public ChainedHashTable() {
        this(INITIAL_TABLE_SIZE);
    }

    /**
     * Creates an empty table starting at {@code initialCapacity} buckets -
     * package-private, so tests can exercise resize behavior without
     * needing to insert past the real {@link #INITIAL_TABLE_SIZE}.
     *
     * @param initialCapacity the starting bucket-array length
     * @throws IllegalArgumentException if {@code initialCapacity} is not positive
     */
    ChainedHashTable(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive");
        }
        this.buckets = newBucketArray(initialCapacity);
    }

    @SuppressWarnings("unchecked")
    private Node<K, V>[] newBucketArray(int capacity) {
        return (Node<K, V>[]) new Node[capacity];
    }

    private int bucketIndex(K key, int tableLength) {
        int h = key.hashCode();
        h ^= (h >>> 16); // spread high bits into low bits, same idea java.util.HashMap uses
        return (h & 0x7fffffff) % tableLength;
    }

    private boolean sameKey(K a, K b) {
        comparisons++;
        return a.equals(b);
    }

    // ------------------------------------------------------------------
    // Instrumented
    // ------------------------------------------------------------------

    @Override
    public long comparisonCount() {
        return comparisons;
    }

    @Override
    public long movementCount() {
        return movements;
    }

    @Override
    public void resetCounters() {
        comparisons = 0;
        movements = 0;
        collisions = 0;
        resizes = 0;
    }

    // ------------------------------------------------------------------
    // MyHashTable
    // ------------------------------------------------------------------

    @Override
    public int collisionCount() {
        return collisions;
    }

    @Override
    public double loadFactor() {
        return (double) size / buckets.length;
    }

    @Override
    public int longestBucket() {
        int longest = 0;
        for (Node<K, V> head : buckets) {
            int length = 0;
            for (Node<K, V> node = head; node != null; node = node.next) {
                length++;
            }
            longest = Math.max(longest, length);
        }
        return longest;
    }

    @Override
    public int resizeCount() {
        return resizes;
    }

    @Override
    public int capacity() {
        return buckets.length;
    }

    // ------------------------------------------------------------------
    // MyMap
    // ------------------------------------------------------------------

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        int idx = bucketIndex(key, buckets.length);
        for (Node<K, V> node = buckets[idx]; node != null; node = node.next) {
            if (sameKey(node.key, key)) {
                V old = node.value;
                node.value = value;
                movements++;
                return old;
            }
        }
        if (buckets[idx] != null) {
            collisions++;
        }
        buckets[idx] = new Node<>(key, value, buckets[idx]);
        movements++;
        size++;
        modCount++;
        if (loadFactor() > LOAD_FACTOR_THRESHOLD) {
            resize();
        }
        return null;
    }

    private void resize() {
        Node<K, V>[] old = buckets;
        int newCapacity = nextPrime(old.length * 2);
        Node<K, V>[] fresh = newBucketArray(newCapacity);
        for (Node<K, V> head : old) {
            Node<K, V> node = head;
            while (node != null) {
                Node<K, V> next = node.next;
                int idx = bucketIndex(node.key, newCapacity);
                node.next = fresh[idx];
                fresh[idx] = node;
                movements++;
                node = next;
            }
        }
        buckets = fresh;
        resizes++;
    }

    private static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        for (int i = 2; (long) i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    private static int nextPrime(int from) {
        int candidate = Math.max(from, 2);
        while (!isPrime(candidate)) {
            candidate++;
        }
        return candidate;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        Node<K, V> node = findNode(key);
        return node == null ? null : node.value;
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return findNode(key) != null;
    }

    private Node<K, V> findNode(K key) {
        for (Node<K, V> node = buckets[bucketIndex(key, buckets.length)]; node != null; node = node.next) {
            if (sameKey(node.key, key)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public V remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        int idx = bucketIndex(key, buckets.length);
        Node<K, V> node = buckets[idx];
        Node<K, V> prev = null;
        while (node != null) {
            if (sameKey(node.key, key)) {
                if (prev == null) {
                    buckets[idx] = node.next;
                } else {
                    prev.next = node.next;
                }
                movements++;
                size--;
                modCount++;
                return node.value;
            }
            prev = node;
            node = node.next;
        }
        return null;
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
        ObjBuffer buffer = new ObjBuffer();
        for (Node<K, V> head : buckets) {
            for (Node<K, V> node = head; node != null; node = node.next) {
                buffer.add(new SimpleEntry<>(node.key, node.value));
            }
        }
        return new SnapshotIterable<>(buffer, modCount);
    }
}
