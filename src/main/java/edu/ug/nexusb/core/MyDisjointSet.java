package edu.ug.nexusb.core;

/**
 * A union-find structure over facility identifiers: which facilities form
 * one connected service cluster, and — via {@link #connected(String, String)}
 * — whether adding a given road would close a cycle.
 *
 * <p>This is what makes Kruskal's minimum-spanning-tree construction
 * efficient: an edge is accepted only when its endpoints are found to be in
 * different sets, which this structure answers without walking the graph.
 * Both union by rank (or size) and path compression are required by the
 * brief, and {@link #maxDepth()} exists specifically to provide the
 * measurement behind the proof sketch that path compression keeps the
 * structure flat.
 */
public interface MyDisjointSet extends Instrumented {

    /**
     * Creates a new singleton set containing just {@code elementId}, if it
     * does not already belong to a set in this structure. Calling this
     * again for an element already present is a no-op, not an error.
     *
     * @param elementId the facility identifier to register
     * @throws IllegalArgumentException if {@code elementId} is {@code null}
     */
    void makeSet(String elementId);

    /**
     * Returns the representative (root) identifier of the set containing
     * {@code elementId}, applying path compression along the way.
     *
     * @param elementId the element to look up
     * @return the identifier of that element's set representative
     * @throws KeyNotFoundException if {@code elementId} was never passed to
     *     {@link #makeSet(String)}
     */
    String find(String elementId);

    /**
     * Merges the sets containing {@code elementIdA} and {@code elementIdB}
     * using union by rank (or size), attaching the shorter/smaller tree
     * under the taller/larger one to keep the structure flat.
     *
     * @param elementIdA an element in the first set
     * @param elementIdB an element in the second set
     * @return {@code true} if the two elements were in different sets and a
     *     merge happened; {@code false} if they were already in the same set
     *     (this is Kruskal's cycle-detection signal — a returned
     *     {@code false} means the candidate edge would close a cycle)
     * @throws KeyNotFoundException if either element was never registered
     *     via {@link #makeSet(String)}
     */
    boolean union(String elementIdA, String elementIdB);

    /**
     * Reports whether two elements currently belong to the same set, without
     * performing a union.
     *
     * @param elementIdA the first element
     * @param elementIdB the second element
     * @return {@code true} if both elements resolve to the same representative
     * @throws KeyNotFoundException if either element was never registered
     *     via {@link #makeSet(String)}
     */
    boolean connected(String elementIdA, String elementIdB);

    /**
     * Returns how many disjoint sets currently exist — the number of
     * distinct connected service clusters.
     *
     * @return the current set count, never negative
     */
    int setCount();

    /**
     * Returns the depth of the deepest element-to-root chain currently in
     * the structure. Recorded as evidence that path compression is actually
     * flattening the structure: after enough {@link #find(String)} calls,
     * this should trend toward small constants regardless of input size.
     *
     * @return the maximum find-chain depth across all sets
     */
    int maxDepth();
}
