package edu.ug.nexusb.graphs;


public class Kruskal {

    /** Represents a weighted, undirected edge between two vertices. */
    public static class Edge {
        public final int src;
        public final int dest;
        public final int weight;

        public Edge(int src, int dest, int weight) {
            this.src = src;
            this.dest = dest;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return src + " -- " + dest + " == " + weight;
        }
    }

    /** Result of running Kruskal's algorithm. */
    public static class Result {
        public final Edge[] mstEdges;
        public final long totalWeight;
        public final boolean isSpanning; // false if the graph was disconnected

        Result(Edge[] mstEdges, long totalWeight, boolean isSpanning) {
            this.mstEdges = mstEdges;
            this.totalWeight = totalWeight;
            this.isSpanning = isSpanning;
        }
    }


    public static Result run(int numVertices, Edge[] edges) {
        validateInput(numVertices, edges);

        // Copy so we never mutate the caller's array.
        Edge[] sorted = new Edge[edges.length];
        System.arraycopy(edges, 0, sorted, 0, edges.length);
        mergeSort(sorted, 0, sorted.length - 1);

        DisjointSet dsu = new DisjointSet(numVertices);
        Edge[] mst = new Edge[Math.max(numVertices - 1, 0)];
        long totalWeight = 0;
        int edgesUsed = 0;

        for (int i = 0; i < sorted.length && edgesUsed < mst.length; i++) {
            Edge edge = sorted[i];
            if (dsu.union(edge.src, edge.dest)) {
                mst[edgesUsed] = edge;
                totalWeight += edge.weight;
                edgesUsed++;
            }
        }

        // Trim to actual size (fewer edges than numVertices - 1 if disconnected).
        Edge[] trimmed = new Edge[edgesUsed];
        System.arraycopy(mst, 0, trimmed, 0, edgesUsed);

        boolean isSpanning = (dsu.setCount() == 1);
        return new Result(trimmed, totalWeight, isSpanning);
    }

    private static void validateInput(int numVertices, Edge[] edges) {
        if (numVertices < 0) {
            throw new IllegalArgumentException("numVertices must be >= 0, was " + numVertices);
        }
        if (edges == null) {
            throw new IllegalArgumentException("edges array must not be null");
        }
        for (int i = 0; i < edges.length; i++) {
            Edge e = edges[i];
            if (e == null) {
                throw new IllegalArgumentException("edges[" + i + "] is null");
            }
            if (e.src < 0 || e.src >= numVertices) {
                throw new IllegalArgumentException(
                        "edges[" + i + "].src = " + e.src + " out of range [0, " + numVertices + ")");
            }
            if (e.dest < 0 || e.dest >= numVertices) {
                throw new IllegalArgumentException(
                        "edges[" + i + "].dest = " + e.dest + " out of range [0, " + numVertices + ")");
            }
        }
    }

    // ---- Hand-rolled merge sort (ascending by weight), no java.util ----

    private static void mergeSort(Edge[] arr, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid);
        mergeSort(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }

    private static void merge(Edge[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        Edge[] leftArr = new Edge[n1];
        Edge[] rightArr = new Edge[n2];
        System.arraycopy(arr, left, leftArr, 0, n1);
        System.arraycopy(arr, mid + 1, rightArr, 0, n2);

        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (leftArr[i].weight <= rightArr[j].weight) {
                arr[k++] = leftArr[i++];
            } else {
                arr[k++] = rightArr[j++];
            }
        }
        while (i < n1) arr[k++] = leftArr[i++];
        while (j < n2) arr[k++] = rightArr[j++];
    }

    public static void main(String[] args) {
        int numVertices = 6;

        Edge[] edges = new Edge[] {
                new Edge(0, 1, 4),
                new Edge(0, 2, 4),
                new Edge(1, 2, 2),
                new Edge(2, 3, 3),
                new Edge(2, 5, 2),
                new Edge(2, 4, 4),
                new Edge(3, 4, 3),
                new Edge(5, 4, 3),
        };

        Result result = run(numVertices, edges);

        System.out.println("Edges in the Minimum Spanning Tree:");
        for (Edge e : result.mstEdges) {
            System.out.println(e);
        }
        System.out.println("Total weight: " + result.totalWeight);
        System.out.println("Graph fully connected (single MST): " + result.isSpanning);
    }
}