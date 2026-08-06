package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.Instrumented;

/**
 * A {@link MyMap} backed by a hash table using separate chaining, with its
 * internals exposed for measurement rather than hidden as implementation
 * detail.
 *
 * <p>{@link #collisionCount()}, {@link #loadFactor()},
 * {@link #longestBucket()} and {@link #resizeCount()} are not debugging
 * aids — they are the Week 4 load-factor experiment. In particular,
 * {@link #resizeCount()} is what lets the report explain a timing spike in
 * the benchmark graph as "a resize happened here" instead of leaving it as
 * unexplained noise. All four exist from the first commit for that reason,
 * and {@link #capacity()} is included alongside them so a test can confirm
 * this table actually started at the seeded {@code INITIAL_TABLE_SIZE}
 * (see {@code docs/parameters.md}) and grew as expected, rather than
 * back-deriving capacity from {@link #loadFactor()} and {@link #size()}.
 *
 * <p>A no-arg constructor on the implementing class is expected to use
 * {@code INITIAL_TABLE_SIZE} as its starting capacity, so that value is
 * never a hardcoded literal inside the implementation itself.
 *
 * <p>Implementations of this interface are expected to widen
 * {@link Instrumented#resetCounters()} to also zero
 * {@link #collisionCount()} and {@link #resizeCount()}, in addition to the
 * comparison and movement counts it already resets. This does not change
 * the {@code Instrumented} contract itself — {@code resetCounters()} still
 * guarantees what it always has — it is purely additional behavior local
 * to this implementation, so it has no effect on any other structure that
 * implements {@code Instrumented}. The reason it matters here: the T021
 * benchmark harness resets counters immediately before each of its three
 * timed repetitions, and without this widening, collision/resize counts
 * from an earlier repetition would leak into the next one's measurement.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface MyHashTable<K, V> extends MyMap<K, V> {

    /**
     * Returns the number of collision events recorded since the counters
     * were last reset (or since construction, if never reset). A collision
     * event is counted each time an insert lands on a bucket that already
     * holds at least one entry.
     *
     * @return the current collision count, never negative
     */
    int collisionCount();

    /**
     * Returns the current load factor: {@link #size()} divided by
     * {@link #capacity()}.
     *
     * @return the current load factor, never negative
     */
    double loadFactor();

    /**
     * Returns the length of the longest chain currently in the table.
     *
     * @return the longest bucket's chain length; {@code 0} if the table is
     *     empty
     */
    int longestBucket();

    /**
     * Returns the number of times this table has resized since the
     * counters were last reset (or since construction, if never reset).
     *
     * @return the current resize count, never negative
     */
    int resizeCount();

    /**
     * Returns the current length of the backing bucket array.
     *
     * @return the current capacity, always positive
     */
    int capacity();
}
