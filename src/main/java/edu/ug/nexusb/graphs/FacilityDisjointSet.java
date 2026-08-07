package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;

/**
 * Facility-ID-keyed union-find implementing the frozen {@link MyDisjointSet}
 * contract, so Kruskal and the rest of the graph layer can code against
 * {@code String} facility IDs the way {@link Edge} and {@link MyGraph}
 * already do.
 *
 * <p>This is a separate implementation from {@link DisjointSet} rather than
 * a wrapper around it: {@code DisjointSet} is fixed-size and int-indexed
 * (its own tests exercise it directly that way), while facilities need to
 * be registered one at a time as they're loaded, under their real IDs. Same
 * algorithm — union by rank plus path compression — adapted for that.
 *
 * <p>IDs are resolved by linear scan rather than a hash table, since
 * {@code MyHashTable} has no implementation yet (T019 is still
 * interface-only) and this project's facility counts (~150) make that a
 * non-issue for now; {@link #comparisonCount()} makes the cost visible if
 * that ever changes.
 */
public class FacilityDisjointSet implements MyDisjointSet {

    private String[] ids;
    private int[] parent;
    private int[] rank;
    private int size;
    private int setCount;
    private long comparisonCount;
    private long movementCount;

    /** Creates an empty disjoint set with a small default starting capacity. */
    public FacilityDisjointSet() {
        this(16);
    }

    /**
     * Creates an empty disjoint set sized for {@code initialCapacity}
     * elements before its first internal grow.
     *
     * @param initialCapacity starting array capacity; must be non-negative
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public FacilityDisjointSet(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        int capacity = Math.max(initialCapacity, 1);
        this.ids = new String[capacity];
        this.parent = new int[capacity];
        this.rank = new int[capacity];
        this.size = 0;
        this.setCount = 0;
    }

    @Override
    public void makeSet(String elementId) {
        if (elementId == null) {
            throw new IllegalArgumentException("elementId must not be null");
        }
        if (indexOf(elementId) >= 0) {
            return;
        }
        ensureCapacity(size + 1);
        ids[size] = elementId;
        parent[size] = size;
        rank[size] = 0;
        size++;
        movementCount++;
        setCount++;
    }

    @Override
    public String find(String elementId) {
        int index = requireIndex(elementId);
        return ids[findRoot(index)];
    }

    @Override
    public boolean union(String elementIdA, String elementIdB) {
        int rootA = findRoot(requireIndex(elementIdA));
        int rootB = findRoot(requireIndex(elementIdB));
        comparisonCount++;
        if (rootA == rootB) {
            return false;
        }
        comparisonCount++;
        if (rank[rootA] < rank[rootB]) {
            int tmp = rootA;
            rootA = rootB;
            rootB = tmp;
        }
        parent[rootB] = rootA;
        movementCount++;
        if (rank[rootA] == rank[rootB]) {
            rank[rootA]++;
        }
        setCount--;
        return true;
    }

    @Override
    public boolean connected(String elementIdA, String elementIdB) {
        int rootA = findRoot(requireIndex(elementIdA));
        int rootB = findRoot(requireIndex(elementIdB));
        comparisonCount++;
        return rootA == rootB;
    }

    @Override
    public int setCount() {
        return setCount;
    }

    @Override
    public int maxDepth() {
        int max = 0;
        for (int i = 0; i < size; i++) {
            int depth = 0;
            int cur = i;
            while (parent[cur] != cur) {
                cur = parent[cur];
                depth++;
            }
            if (depth > max) {
                max = depth;
            }
        }
        return max;
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

    private int findRoot(int index) {
        int root = index;
        while (parent[root] != root) {
            comparisonCount++;
            root = parent[root];
        }
        while (parent[index] != root) {
            int next = parent[index];
            parent[index] = root;
            movementCount++;
            index = next;
        }
        return root;
    }

    private int indexOf(String elementId) {
        for (int i = 0; i < size; i++) {
            comparisonCount++;
            if (ids[i].equals(elementId)) {
                return i;
            }
        }
        return -1;
    }

    private int requireIndex(String elementId) {
        int index = indexOf(elementId);
        if (index < 0) {
            throw new KeyNotFoundException("makeSet() was never called for: " + elementId);
        }
        return index;
    }

    private void ensureCapacity(int required) {
        if (required <= ids.length) {
            return;
        }
        int newCapacity = ids.length * 2;
        String[] newIds = new String[newCapacity];
        int[] newParent = new int[newCapacity];
        int[] newRank = new int[newCapacity];
        for (int i = 0; i < size; i++) {
            newIds[i] = ids[i];
            newParent[i] = parent[i];
            newRank[i] = rank[i];
        }
        ids = newIds;
        parent = newParent;
        rank = newRank;
    }
}
