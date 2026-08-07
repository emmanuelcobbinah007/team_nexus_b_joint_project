package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyIterable;

/**
 * {@link MyGraph} backed by an {@code n x n} weight matrix plus a parallel
 * presence matrix (rather than a sentinel weight value, since {@link Edge}
 * already allows a weight of {@code 0}). {@link #containsEdge}/{@link
 * #weightOf} are O(1) after vertex lookup, but {@link #edgesFrom(String)}
 * must scan a full row — O(V) regardless of degree — which is exactly the
 * tradeoff the comparison against {@link AdjacencyListGraph} is meant to
 * show.
 */
public final class AdjacencyMatrixGraph extends AbstractGraph {

    private double[][] weight;
    private boolean[][] present;
    private int edgeCount;

    /** Creates an empty graph with a small default starting capacity. */
    public AdjacencyMatrixGraph() {
        this(8);
    }

    /**
     * Creates an empty graph sized for {@code initialCapacity} vertices
     * before its first internal grow.
     *
     * @param initialCapacity starting vertex capacity; must be non-negative
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public AdjacencyMatrixGraph(int initialCapacity) {
        super(initialCapacity);
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        int capacity = Math.max(initialCapacity, 1);
        this.weight = new double[capacity][capacity];
        this.present = new boolean[capacity][capacity];
    }

    @Override
    protected void onVertexAdded(int newIndex) {
        if (newIndex >= weight.length) {
            int newCapacity = weight.length * 2;
            double[][] grownWeight = new double[newCapacity][newCapacity];
            boolean[][] grownPresent = new boolean[newCapacity][newCapacity];
            for (int i = 0; i < newIndex; i++) {
                System.arraycopy(weight[i], 0, grownWeight[i], 0, newIndex);
                System.arraycopy(present[i], 0, grownPresent[i], 0, newIndex);
            }
            weight = grownWeight;
            present = grownPresent;
        }
    }

    @Override
    public String representationName() {
        return "ADJACENCY_MATRIX";
    }

    @Override
    public void addEdge(Edge edge) {
        int[] endpoints = ensureEndpoints(edge);
        int fromIndex = endpoints[0];
        int toIndex = endpoints[1];
        countComparison();
        if (!present[fromIndex][toIndex]) {
            edgeCount++;
        }
        present[fromIndex][toIndex] = true;
        weight[fromIndex][toIndex] = edge.weight();
        countMovement();
    }

    @Override
    public void removeEdge(String fromId, String toId) {
        int fromIndex = indexOf(fromId);
        int toIndex = indexOf(toId);
        if (fromIndex < 0 || toIndex < 0) {
            return;
        }
        countComparison();
        if (!present[fromIndex][toIndex]) {
            return;
        }
        present[fromIndex][toIndex] = false;
        weight[fromIndex][toIndex] = 0;
        countMovement();
        edgeCount--;
    }

    @Override
    public boolean containsEdge(String fromId, String toId) {
        int fromIndex = indexOf(fromId);
        int toIndex = indexOf(toId);
        if (fromIndex < 0 || toIndex < 0) {
            return false;
        }
        return present[fromIndex][toIndex];
    }

    @Override
    public double weightOf(String fromId, String toId) {
        int fromIndex = requireVertexIndex(fromId);
        int toIndex = requireVertexIndex(toId);
        countComparison();
        if (!present[fromIndex][toIndex]) {
            throw new KeyNotFoundException("No such edge: " + fromId + " -> " + toId);
        }
        return weight[fromIndex][toIndex];
    }

    @Override
    public MyIterable<Edge> edgesFrom(String vertexId) {
        int fromIndex = requireVertexIndex(vertexId);

        int degree = 0;
        for (int j = 0; j < vertexSize; j++) {
            countComparison();
            if (present[fromIndex][j]) {
                degree++;
            }
        }

        Edge[] result = new Edge[degree];
        int k = 0;
        for (int j = 0; j < vertexSize; j++) {
            countComparison();
            if (present[fromIndex][j]) {
                result[k] = new Edge(vertexIds[fromIndex], vertexIds[j], weight[fromIndex][j]);
                k++;
                countMovement();
            }
        }
        return arrayIterable(result);
    }

    @Override
    public int edgeCount() {
        return edgeCount;
    }
}
