package edu.ug.nexusb.graphs;

/**
 * Immutable result produced by Prim's minimum spanning tree algorithm.
 */
public final class MstResult {

    private final String startId;
    private final Edge[] edges;
    private final double totalWeight;
    private final int vertexCount;

    /**
     * Creates a verified minimum spanning tree result.
     *
     * @param startId vertex from which Prim's algorithm started
     * @param edges selected minimum spanning tree edges
     * @param totalWeight sum of the selected edge weights
     * @param vertexCount number of vertices spanned
     */
    public MstResult(
            String startId,
            Edge[] edges,
            double totalWeight,
            int vertexCount) {

        if (startId == null) {
            throw new IllegalArgumentException("startId must not be null");
        }
        if (edges == null) {
            throw new IllegalArgumentException("edges must not be null");
        }
        if (vertexCount < 1) {
            throw new IllegalArgumentException(
                    "vertexCount must be positive");
        }
        if (edges.length != vertexCount - 1) {
            throw new IllegalArgumentException(
                    "an MST must contain vertexCount - 1 edges");
        }
        if (Double.isNaN(totalWeight)
                || Double.isInfinite(totalWeight)
                || totalWeight < 0) {
            throw new IllegalArgumentException(
                    "totalWeight must be finite and non-negative");
        }

        for (Edge edge : edges) {
            if (edge == null) {
                throw new IllegalArgumentException(
                        "MST edges must not contain null");
            }
        }

        this.startId = startId;
        this.edges = copyOf(edges);
        this.totalWeight = totalWeight;
        this.vertexCount = vertexCount;
    }

    /**
     * @return the vertex from which Prim's algorithm started
     */
    public String startId() {
        return startId;
    }

    /**
     * @return a defensive copy of the selected MST edges
     */
    public Edge[] edges() {
        return copyOf(edges);
    }

    /**
     * @return the number of selected MST edges
     */
    public int edgeCount() {
        return edges.length;
    }

    /**
     * @return the total weight of the minimum spanning tree
     */
    public double totalWeight() {
        return totalWeight;
    }

    /**
     * @return the number of vertices spanned
     */
    public int vertexCount() {
        return vertexCount;
    }

    private static Edge[] copyOf(Edge[] source) {
        Edge[] copy = new Edge[source.length];
        System.arraycopy(source, 0, copy, 0, source.length);
        return copy;
    }
}