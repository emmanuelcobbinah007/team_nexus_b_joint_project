package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * Vertex bookkeeping shared by {@link AdjacencyListGraph} and
 * {@link AdjacencyMatrixGraph}: the ID-to-index table, growth, and the
 * {@code Instrumented} counters. Deliberately package-private — callers
 * outside {@code graphs} depend on {@link MyGraph}, not this.
 *
 * <p>Only vertex storage lives here. Edge storage is exactly what
 * distinguishes the two representations, so it stays in the subclasses.
 *
 * <p>{@link #vertices()} and each subclass's {@code edgesFrom} return a
 * snapshot array, not a live view: simpler than modification-count-based
 * fail-fast tracking, and sufficient since nothing in this codebase
 * mutates a graph while iterating it.
 */
abstract class AbstractGraph implements MyGraph {

    protected String[] vertexIds;
    protected int vertexSize;

    private long comparisonCount;
    private long movementCount;

    protected AbstractGraph(int initialCapacity) {
        int capacity = Math.max(initialCapacity, 1);
        this.vertexIds = new String[capacity];
        this.vertexSize = 0;
    }

    @Override
    public void addVertex(String vertexId) {
        if (vertexId == null) {
            throw new IllegalArgumentException("vertexId must not be null");
        }
        ensureVertex(vertexId);
    }

    @Override
    public boolean containsVertex(String vertexId) {
        return indexOf(vertexId) >= 0;
    }

    @Override
    public MyIterable<String> vertices() {
        String[] snapshot = new String[vertexSize];
        System.arraycopy(vertexIds, 0, snapshot, 0, vertexSize);
        return arrayIterable(snapshot);
    }

    @Override
    public int vertexCount() {
        return vertexSize;
    }

    @Override
    public long comparisonCount() {
        return comparisonCount;
    }

    @Override
    public long movementCount() {
        return movementCount;
    }

    @Override
    public void resetCounters() {
        comparisonCount = 0;
        movementCount = 0;
    }

    protected void countComparison() {
        comparisonCount++;
    }

    protected void countMovement() {
        movementCount++;
    }

    /**
     * Linear scan for {@code vertexId}'s index. Both representations pay
     * this same O(V) cost to resolve an ID before doing anything
     * representation-specific — the interesting difference between them is
     * what happens next, not this lookup.
     */
    protected int indexOf(String vertexId) {
        for (int i = 0; i < vertexSize; i++) {
            countComparison();
            if (vertexIds[i].equals(vertexId)) {
                return i;
            }
        }
        return -1;
    }

    protected int requireVertexIndex(String vertexId) {
        int index = indexOf(vertexId);
        if (index < 0) {
            throw new KeyNotFoundException("No such vertex: " + vertexId);
        }
        return index;
    }

    /**
     * Ensures {@code vertexId} is present, growing {@link #vertexIds} if
     * needed. Calls {@link #onVertexAdded(int)} exactly when a new vertex
     * was actually added, so subclasses can grow their own edge storage in
     * lockstep. Returns the vertex's index either way.
     */
    protected int ensureVertex(String vertexId) {
        int existing = indexOf(vertexId);
        if (existing >= 0) {
            return existing;
        }
        if (vertexSize == vertexIds.length) {
            String[] grown = new String[vertexIds.length * 2];
            System.arraycopy(vertexIds, 0, grown, 0, vertexSize);
            vertexIds = grown;
        }
        int newIndex = vertexSize;
        vertexIds[newIndex] = vertexId;
        vertexSize++;
        countMovement();
        onVertexAdded(newIndex);
        return newIndex;
    }

    /** Adds both endpoints of {@code edge}, returning {fromIndex, toIndex}. */
    protected int[] ensureEndpoints(Edge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("edge must not be null");
        }
        int fromIndex = ensureVertex(edge.fromId());
        int toIndex = ensureVertex(edge.toId());
        return new int[] {fromIndex, toIndex};
    }

    /**
     * Called immediately after a new vertex is added at {@code newIndex},
     * so the subclass can grow its own edge storage to match.
     */
    protected abstract void onVertexAdded(int newIndex);

    protected static <T> MyIterable<T> arrayIterable(T[] items) {
        return () -> new MyIterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < items.length;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new StructureException("iterator has no more elements");
                }
                return items[index++];
            }
        };
    }
}
