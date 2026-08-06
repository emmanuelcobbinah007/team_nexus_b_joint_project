package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterable;

/**
 * A {@link MyMap} whose keys are kept in order, shared by the BST, the
 * balanced tree, and the B-tree implementations built in this package.
 *
 * <p>The BST-versus-balanced-tree experiment is only a fair comparison if
 * both run through the same interface and the same harness — that is what
 * this contract is for. {@link #height()} is the measurement itself;
 * {@link #isBalanced()} doubles as a test oracle after randomised
 * insertion, confirming a balanced implementation actually stayed balanced
 * rather than degrading toward a linked list. {@link #rangeKeys(Object,
 * Object)} is the operation a hash table cannot perform — it is the reason
 * the indexing engine keeps a tree alongside {@code MyHashTable} rather
 * than relying on the table alone.
 *
 * <p>Ordering is pluggable via {@link #comparator()} rather than requiring
 * {@code K} to implement {@code Comparable}, the same choice already made
 * for {@code MyPriorityQueue}: a tree needs to be able to order case
 * records by triage level, or facilities by ID, without forcing every key
 * type used anywhere in this project to implement {@code Comparable}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface MyTree<K, V> extends MyMap<K, V> {

    /**
     * Returns the ordering strategy this tree uses to arrange its keys.
     *
     * @return the comparator used by this tree
     */
    MyComparator<? super K> comparator();

    /**
     * Returns this tree's height: the number of edges on the longest path
     * from the root to a leaf.
     *
     * @return {@code -1} if this tree is empty, {@code 0} if it holds only
     *     a single node, otherwise the longest root-to-leaf edge count
     */
    int height();

    /**
     * Reports whether this tree currently satisfies its own balance
     * invariant.
     *
     * <p>What "balanced" means depends on the implementation: an AVL-style
     * balanced tree checks its height-difference invariant at every node;
     * a B-tree is balanced by construction and this trivially returns
     * {@code true}; a plain BST has no invariant to maintain and this
     * reports whatever its actual shape happens to be after the inserts
     * performed so far.
     *
     * @return {@code true} if this tree currently satisfies its balance
     *     invariant
     */
    boolean isBalanced();

    /**
     * Returns every key currently in this tree that falls within
     * {@code [from, to]}, inclusive of both bounds.
     *
     * @param from the lower bound of the range, inclusive
     * @param to the upper bound of the range, inclusive
     * @return an iterable over the matching keys, in ascending order; empty
     *     if no keys fall in the range or if {@code from} is greater than
     *     {@code to}
     * @throws IllegalArgumentException if {@code from} or {@code to} is
     *     {@code null}
     */
    MyIterable<K> rangeKeys(K from, K to);
}
