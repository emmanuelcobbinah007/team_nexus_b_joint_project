package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * The output of a single-source traversal or shortest-path computation
 * (Dijkstra, BFS) over a {@link MyGraph}: the distance to every vertex the
 * search reached, the predecessor chain needed to reconstruct an actual
 * route, and the order in which vertices were finalised.
 *
 * <p>Holding distances <em>and</em> predecessors together — rather than
 * returning a bare distance map — is what lets the console menu display a
 * real ambulance route rather than just a number. {@link #visitOrder()}
 * exists specifically to supply the Dijkstra trace table required as
 * correctness evidence: it is the sequence in which vertices left the
 * frontier, not the order they were first discovered.
 *
 * <p>Implemented as a plain immutable data holder rather than an interface:
 * unlike {@link MyGraph}, there is only ever one shape of "shortest path
 * result", regardless of which graph representation produced it.
 */
public final class PathResult {

    private final String sourceId;
    private final String[] vertexIds;
    private final double[] distances;
    private final String[] predecessorIds;
    private final String[] visitOrder;

    /**
     * Builds a result from parallel arrays produced by a traversal
     * algorithm.
     *
     * @param sourceId the vertex the search started from
     * @param vertexIds every vertex the search considered; the same array
     *     indexes {@code distances} and {@code predecessorIds}
     * @param distances {@code distances[i]} is the shortest known distance
     *     from {@code sourceId} to {@code vertexIds[i]}, or
     *     {@link Double#POSITIVE_INFINITY} if unreached
     * @param predecessorIds {@code predecessorIds[i]} is the vertex visited
     *     immediately before {@code vertexIds[i]} on the shortest path
     *     found, or {@code null} for the source itself and for unreached
     *     vertices
     * @param visitOrder the vertex IDs in the order the algorithm finalised
     *     them, for the trace table
     * @throws IllegalArgumentException if {@code sourceId} is {@code null},
     *     or if {@code vertexIds}, {@code distances} and
     *     {@code predecessorIds} are not the same length
     */
    public PathResult(String sourceId, String[] vertexIds, double[] distances,
            String[] predecessorIds, String[] visitOrder) {
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId must not be null");
        }
        if (vertexIds.length != distances.length || vertexIds.length != predecessorIds.length) {
            throw new IllegalArgumentException(
                "vertexIds, distances and predecessorIds must have the same length");
        }
        this.sourceId = sourceId;
        this.vertexIds = vertexIds;
        this.distances = distances;
        this.predecessorIds = predecessorIds;
        this.visitOrder = visitOrder;
    }

    /**
     * Returns the vertex this search started from.
     *
     * @return the source vertex ID
     */
    public String sourceId() {
        return sourceId;
    }

    /**
     * Reports whether the search reached the given vertex at all.
     *
     * @param vertexId the vertex to check
     * @return {@code true} if a finite distance was recorded for {@code vertexId}
     */
    public boolean isReachable(String vertexId) {
        int i = indexOf(vertexId);
        return i >= 0 && !Double.isInfinite(distances[i]);
    }

    /**
     * Returns the shortest known distance from the source to {@code vertexId}.
     *
     * @param vertexId the destination vertex
     * @return the distance, or {@link Double#POSITIVE_INFINITY} if unreached
     * @throws KeyNotFoundException if {@code vertexId} was not part of this result
     */
    public double distanceTo(String vertexId) {
        return distances[requireIndex(vertexId)];
    }

    /**
     * Returns the vertex visited immediately before {@code vertexId} on the
     * shortest path found from the source.
     *
     * @param vertexId the vertex whose predecessor is requested
     * @return the predecessor's ID, or {@code null} if {@code vertexId} is
     *     the source or was never reached
     * @throws KeyNotFoundException if {@code vertexId} was not part of this result
     */
    public String predecessorOf(String vertexId) {
        return predecessorIds[requireIndex(vertexId)];
    }

    /**
     * Reconstructs the full route from the source to {@code vertexId} by
     * walking the predecessor chain backwards.
     *
     * @param vertexId the destination vertex
     * @return the route as an ordered array from source to {@code vertexId}
     *     inclusive, or an empty array if {@code vertexId} is unreached
     * @throws KeyNotFoundException if {@code vertexId} was not part of this result
     */
    public String[] pathTo(String vertexId) {
        requireIndex(vertexId);
        if (!isReachable(vertexId)) {
            return new String[0];
        }
        int hops = 1;
        String current = vertexId;
        while (!current.equals(sourceId)) {
            current = predecessorOf(current);
            hops++;
        }
        String[] path = new String[hops];
        current = vertexId;
        for (int i = hops - 1; i >= 0; i--) {
            path[i] = current;
            if (i > 0) {
                current = predecessorOf(current);
            }
        }
        return path;
    }

    /**
     * Returns the vertices in the order the algorithm finalised them — the
     * data behind the required Dijkstra trace table.
     *
     * @return an iterable over the visit order, source first
     */
    public MyIterable<String> visitOrder() {
        String[] order = visitOrder;
        return () -> new MyIterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < order.length;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new StructureException("visitOrder() iterator has no more elements");
                }
                return order[index++];
            }
        };
    }

    private int indexOf(String vertexId) {
        for (int i = 0; i < vertexIds.length; i++) {
            if (vertexIds[i].equals(vertexId)) {
                return i;
            }
        }
        return -1;
    }

    private int requireIndex(String vertexId) {
        int i = indexOf(vertexId);
        if (i < 0) {
            throw new KeyNotFoundException("No such vertex in this path result: " + vertexId);
        }
        return i;
    }
}
