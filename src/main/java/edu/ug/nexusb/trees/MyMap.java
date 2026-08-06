package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.Instrumented;
import edu.ug.nexusb.core.MyIterable;

/**
 * A key-value mapping, shared as the common contract between the hash
 * table and the tree-backed implementations built in this package.
 *
 * <p>Both {@code MyHashTable} and {@code MyTree} implement this interface
 * so the Week 4 experiment comparing them — by comparison count, not
 * wall-clock time, per {@link Instrumented} — can run both through the
 * exact same operations and the exact same harness. A fair comparison
 * requires a shared contract; this is it.
 *
 * <p>{@code null} is reserved to mean "no value at this key": {@link #get}
 * and {@link #remove} return {@code null} on a miss rather than throwing,
 * because lookups are a hot path in the dispatch engine (case-by-reference,
 * facility-by-ID) and forcing every miss through an exception there would
 * be awkward for callers who expect misses to be routine. Since
 * {@code null} already carries that meaning, a stored key of {@code null}
 * would be indistinguishable from "not present" at the call site — so
 * {@code null} keys are rejected outright rather than left to misbehave
 * silently.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface MyMap<K, V> extends Instrumented {

    /**
     * A read-only key-value pair, as returned by {@link #entries()}.
     *
     * <p>This is a snapshot of one mapping at the time it was produced,
     * not a live view — mutating the map afterward does not change an
     * already-returned entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    interface MapEntry<K, V> {

        /**
         * Returns this entry's key.
         *
         * @return the key; never {@code null}, since {@code MyMap} rejects
         *     {@code null} keys
         */
        K getKey();

        /**
         * Returns this entry's value.
         *
         * @return the value; may be {@code null} if the implementation
         *     permits {@code null} values
         */
        V getValue();
    }

    /**
     * Associates {@code value} with {@code key}, replacing any existing
     * value at that key.
     *
     * @param key the key to associate the value with
     * @param value the value to store; {@code null} is permitted
     * @return the previous value associated with {@code key}, or
     *     {@code null} if the key had no previous mapping
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    V put(K key, V value);

    /**
     * Returns the value associated with {@code key}.
     *
     * @param key the key to look up
     * @return the value associated with {@code key}, or {@code null} if
     *     the key is not present
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    V get(K key);

    /**
     * Removes the mapping for {@code key}, if one exists.
     *
     * @param key the key to remove
     * @return the value that was associated with {@code key}, or
     *     {@code null} if the key was not present
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    V remove(K key);

    /**
     * Reports whether {@code key} currently has a mapping.
     *
     * @param key the key to check
     * @return {@code true} if the key is present
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    boolean containsKey(K key);

    /**
     * Returns how many key-value mappings are currently stored.
     *
     * @return the mapping count, never negative
     */
    int size();

    /**
     * Reports whether this map holds no mappings.
     *
     * @return {@code true} if {@link #size()} is {@code 0}
     */
    boolean isEmpty();

    /**
     * Returns every key-value mapping currently stored.
     *
     * <p>Iteration order is unspecified — a hash table cannot offer a
     * meaningful order cheaply, so no caller should rely on one.
     * Implementations that do have a natural order (e.g. a tree) are free
     * to honor it, but callers that need a guaranteed order should not
     * depend on that here.
     *
     * @return an iterable over this map's entries, in
     *     implementation-defined order
     */
    MyIterable<MapEntry<K, V>> entries();
}
