package edu.ug.nexusb.core;

/**
 * An immutable, directed, weighted connection from one facility to another.
 *
 * <p>{@code weight} is the effective travel time in minutes, as computed by
 * the {@code v_weighted_edge} database view: base travel time multiplied by
 * the road's traffic weight and condition factor. Every algorithm that
 * consumes an {@code Edge} (Dijkstra, Prim, Kruskal, BFS) optimises this same
 * quantity, so there is exactly one definition of "cost" in the system.
 *
 * <p>To model an undirected road, callers add two {@code Edge} instances —
 * one in each direction — to a {@link MyGraph}.
 *
 * @param fromId the origin facility's identifier
 * @param toId the destination facility's identifier
 * @param weight the effective travel time in minutes; must be non-negative
 */
public record Edge(String fromId, String toId, double weight) {

    /**
     * Validates the record components. Negative weights are rejected here,
     * at construction, rather than left for an algorithm to discover later:
     * Dijkstra's correctness argument (a node, once finalised, is never
     * revisited) assumes every edge weight is non-negative.
     *
     * @throws IllegalArgumentException if either endpoint is {@code null},
     *     or if {@code weight} is negative or not a finite number
     */
    public Edge {
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("Edge endpoints must not be null");
        }
        if (Double.isNaN(weight) || Double.isInfinite(weight)) {
            throw new IllegalArgumentException("Edge weight must be a finite number, got " + weight);
        }
        if (weight < 0) {
            throw new IllegalArgumentException(
                "Edge weight must be non-negative (got " + weight + "); "
                    + "Dijkstra's correctness argument assumes no negative weights");
        }
    }
}
