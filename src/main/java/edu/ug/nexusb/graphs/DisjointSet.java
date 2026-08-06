package edu.ug.nexusb.graphs;


public class DisjointSet {

    private final int[] parent;
    private final int[] rank;
    private int count;

    /** Initialize n elements, each in its own set, labeled 0..n-1. */
    public DisjointSet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must greater than 0");
        }
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        count = n;
    }

    /** Find the representative (root) of the set containing x, with path compression. */
    public int find(int x) {
        validate(x);
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
        }

        // Path compression: point every node on the path directly to root
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }

        return root;
    }

    /**
     * Merge the sets containing x and y using union by rank.
     * Returns true if a merge happened, false if x and y were already in the same set.
     */
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX == rootY) {
            return false;
        }

        // Union by rank: attach smaller rank tree under larger rank tree
        if (rank[rootX] < rank[rootY]) {
            int tmp = rootX;
            rootX = rootY;
            rootY = tmp;
        }
        parent[rootY] = rootX;
        if (rank[rootX] == rank[rootY]) {
            rank[rootX]++;
        }

        count--;
        return true;
    }

    /** Return true if x and y are in the same set. */
    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }

    /** Return the current number of disjoint sets. */
    public int setCount() {
        return count;
    }

    /** Package-private accessor used by tests to inspect internal rank array. */
    int rankOf(int root) {
        return rank[root];
    }

    /** Package-private accessor used by tests to inspect internal parent array. */
    int parentOf(int x) {
        return parent[x];
    }

    private void validate(int x) {
        if (x < 0 || x >= parent.length) {
            throw new IndexOutOfBoundsException(
                    "element " + x + " out of range [0, " + parent.length + ")");
        }
    }
}

