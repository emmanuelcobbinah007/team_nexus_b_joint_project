package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.Instrumented;
import edu.ug.nexusb.core.MyIterable;

/**
 * A collection of distinct, non-{@code null} elements — a thin wrapper over
 * the hash table built in this package.
 *
 * <p>Two consumers outside this sub-team already depend on this exact
 * contract: BFS/DFS traversal marks visited vertices in a {@code MySet}
 * rather than re-scanning already-explored nodes, and the data loader uses
 * it to detect duplicate records on load. Both are why {@link #add} and
 * {@link #remove} report back whether they actually changed anything,
 * rather than being {@code void} — a caller doing duplicate detection needs
 * to know "was this already here?" inline, without a separate
 * {@link #contains} check first.
 *
 * <p>{@code null} elements are rejected, for the same reason {@code MyMap}
 * rejects {@code null} keys: nothing here needs it, and allowing it would
 * only create ambiguity to guard against later.
 *
 * <p>{@link #clear()} exists because BFS/DFS need a fresh, empty visited-set
 * on every traversal run. Without it, a caller running repeated benchmark
 * passes would have to discard and reallocate a whole new {@code MySet}
 * each time rather than reset one.
 *
 * <p>This interface extends {@link MyIterable} directly rather than
 * exposing a separate accessor method, since a set's only natural
 * iteration view is its own elements. Iteration order is unspecified — a
 * hash table cannot offer a meaningful order cheaply, so no caller should
 * rely on one.
 *
 * @param <T> the element type
 */
public interface MySet<T> extends Instrumented, MyIterable<T> {

    /**
     * Adds {@code value} to this set if it is not already present.
     *
     * @param value the element to add
     * @return {@code true} if the element was not already present and has
     *     now been added; {@code false} if it was already present, in
     *     which case this set is unchanged
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    boolean add(T value);

    /**
     * Removes {@code value} from this set, if present.
     *
     * @param value the element to remove
     * @return {@code true} if the element was present and has now been
     *     removed; {@code false} if it was not present, in which case this
     *     set is unchanged
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    boolean remove(T value);

    /**
     * Reports whether {@code value} is currently in this set.
     *
     * @param value the element to check
     * @return {@code true} if the element is present
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    boolean contains(T value);

    /**
     * Removes every element, leaving this set empty.
     */
    void clear();

    /**
     * Returns how many elements are currently in this set.
     *
     * @return the element count, never negative
     */
    int size();

    /**
     * Reports whether this set holds no elements.
     *
     * @return {@code true} if {@link #size()} is {@code 0}
     */
    boolean isEmpty();
}
