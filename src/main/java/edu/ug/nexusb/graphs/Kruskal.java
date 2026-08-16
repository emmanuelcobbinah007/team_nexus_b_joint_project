package edu.ug.nexusb.graphs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Kruskal {

    /** Represents a weighted, undirected edge between two vertices. */
    public static class Edge implements Comparable<Edge> {
        public final int src;
        public final int dest;
        public final int weight;

        public Edge(int src, int dest, int weight) {
            this.src = src;
            this.dest = dest;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge other) {
            return Integer.compare(this.weight, other.weight);
        }

        @Override
        public String toString() {
            return src + " -- " + dest + " == " + weight;
        }
    }

    /** Result of running Kruskal's algorithm. */
    public static class Result {
        public final List<Edge> mstEdges;
        public final long totalWeight;
        public final boolean isSpanning; // false if the graph was disconnected

        Result(List<Edge> mstEdges, long totalWeight, boolean isSpanning) {
            this.mstEdges = mstEdges;
            this.totalWeight = totalWeight;
            this.isSpanning = isSpanning;
        }
    }

    public static Result run(int numVertices, List<Edge> edges) {
        List<Edge> sorted = new ArrayList<>(edges);
        Collections.sort(sorted); // ascending by weight

        DisjointSet dsu = new DisjointSet(numVertices);
        List<Edge> mst = new ArrayList<>();
        long totalWeight = 0;
        int edgesUsed = 0;

        for (Edge edge : sorted) {
            if (edgesUsed == numVertices - 1) break; // MST already complete

            if (dsu.union(edge.src, edge.dest)) {
                mst.add(edge);
                totalWeight += edge.weight;
                edgesUsed++;
            }
        }

        boolean isSpanning = (dsu.setCount() == 1);
        return new Result(mst, totalWeight, isSpanning);
    }

    public static void main(String[] args) {
        int numVertices = 6;

        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(0, 1, 4));
        edges.add(new Edge(0, 2, 4));
        edges.add(new Edge(1, 2, 2));
        edges.add(new Edge(2, 3, 3));
        edges.add(new Edge(2, 5, 2));
        edges.add(new Edge(2, 4, 4));
        edges.add(new Edge(3, 4, 3));
        edges.add(new Edge(5, 4, 3));

        Result result = run(numVertices, edges);

        System.out.println("Edges in the Minimum Spanning Tree:");
        for (Edge e : result.mstEdges) {
            System.out.println(e);
        }
        System.out.println("Total weight: " + result.totalWeight);
        System.out.println("Graph fully connected (single MST): " + result.isSpanning);
    }

    }
