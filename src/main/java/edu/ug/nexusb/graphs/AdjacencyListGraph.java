package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyIterable;

/**
 * {@link MyGraph} backed by a growable array of per-vertex outgoing-edge
 * arrays — the classic adjacency list. {@link #edgesFrom(String)} costs
 * O(out-degree); enumerating a vertex's edges never touches vertices it
 * isn't connected to, which is the whole point of comparing this against
 * {@link AdjacencyMatrixGraph}.
 */
public final class AdjacencyListGraph extends AbstractGraph {

    private Edge[][] outEdges;
    private int[] outDegree;
    private int edgeCount;

    /** Creates an empty graph with a small default starting capacity. */
    public AdjacencyListGraph() {
        this(8);
    }

    /**
     * Creates an empty graph sized for {@code initialCapacity} vertices
     * before its first internal grow.
     *
     * @param initialCapacity starting vertex capacity; must be non-negative
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public AdjacencyListGraph(int initialCapacity) {
        super(initialCapacity);
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        int capacity = Math.max(initialCapacity, 1);
        this.outEdges = new Edge[capacity][];
        this.outDegree = new int[capacity];
    }

    @Override
    protected void onVertexAdded(int newIndex) {
        if (newIndex >= outEdges.length) {
            int newCapacity = outEdges.length * 2;
            Edge[][] grownEdges = new Edge[newCapacity][];
            int[] grownDegree = new int[newCapacity];
            System.arraycopy(outEdges, 0, grownEdges, 0, newIndex);
            System.arraycopy(outDegree, 0, grownDegree, 0, newIndex);
            outEdges = grownEdges;
            outDegree = grownDegree;
        }
        outEdges[newIndex] = new Edge[4];
        outDegree[newIndex] = 0;
    }

    @Override
    public String representationName() {
        return "ADJACENCY_LIST";
    }

    @Override
    public void addEdge(Edge edge) {
        int[] endpoints = ensureEndpoints(edge);
        int fromIndex = endpoints[0];
        String toId = edge.toId();

        int existing = findEdgeIndex(fromIndex, toId);
        if (existing >= 0) {
            outEdges[fromIndex][existing] = edge;
            countMovement();
            return;
        }
        appendEdge(fromIndex, edge);
        edgeCount++;
    }

    @Override
    public void removeEdge(String fromId, String toId) {
        int fromIndex = indexOf(fromId);
        if (fromIndex < 0) {
            return;
        }
        int edgeIndex = findEdgeIndex(fromIndex, toId);
        if (edgeIndex < 0) {
            return;
        }
        int lastIndex = outDegree[fromIndex] - 1;
        outEdges[fromIndex][edgeIndex] = outEdges[fromIndex][lastIndex];
        outEdges[fromIndex][lastIndex] = null;
        outDegree[fromIndex]--;
        countMovement();
        edgeCount--;
    }

    @Override
    public boolean containsEdge(String fromId, String toId) {
        int fromIndex = indexOf(fromId);
        if (fromIndex < 0) {
            return false;
        }
        return findEdgeIndex(fromIndex, toId) >= 0;
    }

    @Override
    public double weightOf(String fromId, String toId) {
        int fromIndex = requireVertexIndex(fromId);
        int edgeIndex = findEdgeIndex(fromIndex, toId);
        if (edgeIndex < 0) {
            throw new KeyNotFoundException("No such edge: " + fromId + " -> " + toId);
        }
        return outEdges[fromIndex][edgeIndex].weight();
    }

    @Override
    public MyIterable<Edge> edgesFrom(String vertexId) {
        int index = requireVertexIndex(vertexId);
        Edge[] snapshot = new Edge[outDegree[index]];
        System.arraycopy(outEdges[index], 0, snapshot, 0, outDegree[index]);
        return arrayIterable(snapshot);
    }

    @Override
    public int edgeCount() {
        return edgeCount;
    }

    private int findEdgeIndex(int fromIndex, String toId) {
        Edge[] bucket = outEdges[fromIndex];
        int degree = outDegree[fromIndex];
        for (int i = 0; i < degree; i++) {
            countComparison();
            if (bucket[i].toId().equals(toId)) {
                return i;
            }
        }
        return -1;
    }

    private void appendEdge(int fromIndex, Edge edge) {
        if (outDegree[fromIndex] == outEdges[fromIndex].length) {
            Edge[] grown = new Edge[outEdges[fromIndex].length * 2];
            System.arraycopy(outEdges[fromIndex], 0, grown, 0, outDegree[fromIndex]);
            outEdges[fromIndex] = grown;
        }
        outEdges[fromIndex][outDegree[fromIndex]] = edge;
        outDegree[fromIndex]++;
        countMovement();
    }
}
