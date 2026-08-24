package edu.ug.nexusb.bench;

import edu.ug.nexusb.graphs.AdjacencyListGraph;
import edu.ug.nexusb.graphs.Dfs;
import edu.ug.nexusb.graphs.Dijkstra;
import edu.ug.nexusb.graphs.Edge;
import edu.ug.nexusb.graphs.Kruskal;
import edu.ug.nexusb.graphs.MyGraph;
import edu.ug.nexusb.graphs.Prim;
import edu.ug.nexusb.graphs.Reachability;
import edu.ug.nexusb.linear.ArrayQueue;
import edu.ug.nexusb.trees.HashSet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * T073: graph algorithms (BFS reachability, DFS, Dijkstra, Kruskal, Prim)
 * measured against graph size (|V|, fixed sparse density) and separately
 * against density (|E|, fixed |V|) — two sweeps over the same five
 * algorithms, so the report can show which ones scale with size, which
 * scale with density, and which (BFS/DFS, {@code Theta(V+E)}) scale with
 * both roughly equally.
 *
 * <p>Uses {@link DatabaseBenchmark} (T042's real measurement methodology --
 * untimed warm-up, {@code System.nanoTime()}, every repetition recorded)
 * rather than a bespoke timer. Graphs are generated with {@code
 * GENERATION_SEED} from {@code docs/parameters.md} (Parameter B) so every
 * run against the same (vertices, edges) pair produces an identical graph.
 */
public final class GraphExperiments {

    /** docs/parameters.md, Parameter B — this team's index-derived generation seed. */
    private static final long GENERATION_SEED = 79731L;

    private static final int TRIALS = 5;

    private GraphExperiments() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Graph Experiments (T073)...");

        runSizeSweep();
        runDensitySweep();

        System.out.println("Experiments completed. CSV written to results/csv/.");
    }

    // Fixed average degree ~6 (E ~= 3V, since each "average degree" edge is
    // counted from both endpoints) while |V| grows -- isolates size.
    private static void runSizeSweep() throws IOException {
        int[] vertexCounts = {50, 100, 200, 400, 800};

        try (FileWriter csv = new FileWriter("results/csv/graph_experiments_size.csv")) {
            csv.write("Algorithm,Vertices,Edges,AverageTimeNs\n");
            for (int vertexCount : vertexCounts) {
                int edgeCount = vertexCount * 3;
                System.out.println("Size sweep: |V|=" + vertexCount + " |E|=" + edgeCount);
                GraphInstance instance = generateConnectedGraph(vertexCount, edgeCount,
                        GENERATION_SEED + vertexCount);
                measureAll(csv, instance);
            }
        }
    }

    // Fixed |V|=300 while |E| grows from a bare spanning tree (299) up
    // toward a much denser graph -- isolates density.
    private static void runDensitySweep() throws IOException {
        int vertexCount = 300;
        int[] edgeCounts = {vertexCount - 1, vertexCount * 3, vertexCount * 6, vertexCount * 12, vertexCount * 24};

        try (FileWriter csv = new FileWriter("results/csv/graph_experiments_density.csv")) {
            csv.write("Algorithm,Vertices,Edges,AverageTimeNs\n");
            for (int edgeCount : edgeCounts) {
                System.out.println("Density sweep: |V|=" + vertexCount + " |E|=" + edgeCount);
                GraphInstance instance = generateConnectedGraph(vertexCount, edgeCount,
                        GENERATION_SEED + edgeCount);
                measureAll(csv, instance);
            }
        }
    }

    private static void measureAll(FileWriter csv, GraphInstance instance) throws IOException {
        measureOne(csv, "BFS_Reachability", instance, () -> {
            Reachability.bfsReachable(instance.graph, "0", new HashSet<>(), new ArrayQueue<>(), new HashSet<>());
        });
        measureOne(csv, "DFS", instance, () -> Dfs.traverse(instance.graph));
        measureOne(csv, "Dijkstra", instance, () -> Dijkstra.shortestPaths(instance.graph, "0"));
        measureOne(csv, "Kruskal", instance, () -> Kruskal.run(instance.vertexCount, instance.kruskalEdges));
        measureOne(csv, "Prim", instance, () -> Prim.minimumSpanningTree(instance.graph, "0"));
    }

    private static void measureOne(FileWriter csv, String name, GraphInstance instance, Runnable algorithm)
            throws IOException {
        DatabaseBenchmark benchmark = new DatabaseBenchmark();
        benchmark.setAlgorithm(algorithm);
        Benchmark.BenchmarkResult result = benchmark.measure(name, instance.vertexCount, TRIALS);
        csv.write(name + "," + instance.vertexCount + "," + instance.edgeCount + "," + result.getTimeNs() + "\n");
    }

    // ------------------------------------------------------------------
    // Deterministic connected random graph generation
    // ------------------------------------------------------------------

    private static final class GraphInstance {
        final MyGraph graph;
        final Kruskal.Edge[] kruskalEdges;
        final int vertexCount;
        final int edgeCount;

        GraphInstance(MyGraph graph, Kruskal.Edge[] kruskalEdges, int vertexCount, int edgeCount) {
            this.graph = graph;
            this.kruskalEdges = kruskalEdges;
            this.vertexCount = vertexCount;
            this.edgeCount = edgeCount;
        }
    }

    /**
     * Builds a connected undirected graph with exactly {@code vertexCount}
     * vertices and {@code edgeCount} distinct edges (or {@code vertexCount - 1}
     * if {@code edgeCount} is smaller than that, since a spanning tree is
     * the floor for connectivity -- required by {@link Prim}, which throws
     * on a disconnected graph).
     */
    private static GraphInstance generateConnectedGraph(int vertexCount, int edgeCount, long seed) {
        Random rng = new Random(seed);
        MyGraph graph = new AdjacencyListGraph();
        Kruskal.Edge[] tempEdges = new Kruskal.Edge[Math.max(edgeCount, vertexCount - 1)];
        int edgesAdded = 0;

        String[] ids = new String[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            ids[i] = String.valueOf(i);
            graph.addVertex(ids[i]);
        }

        // Random spanning tree first, so the graph is always connected: each
        // new vertex (in a random order) attaches to a uniformly random
        // already-placed vertex.
        int[] order = shuffledIndices(vertexCount, rng);
        for (int i = 1; i < vertexCount; i++) {
            int a = order[i];
            int b = order[rng.nextInt(i)];
            edgesAdded = addUndirectedEdge(graph, tempEdges, edgesAdded, ids[a], ids[b], 1 + rng.nextInt(50));
        }

        // Extra random edges up to the requested density.
        int additional = Math.max(0, edgeCount - (vertexCount - 1));
        for (int i = 0; i < additional && vertexCount > 1; i++) {
            int a = rng.nextInt(vertexCount);
            int b = rng.nextInt(vertexCount);
            if (a == b) {
                continue;
            }
            edgesAdded = addUndirectedEdge(graph, tempEdges, edgesAdded, ids[a], ids[b], 1 + rng.nextInt(50));
        }

        Kruskal.Edge[] kruskalEdges = new Kruskal.Edge[edgesAdded];
        System.arraycopy(tempEdges, 0, kruskalEdges, 0, edgesAdded);
        return new GraphInstance(graph, kruskalEdges, vertexCount, edgesAdded);
    }

    private static int addUndirectedEdge(
            MyGraph graph, Kruskal.Edge[] kruskalEdges, int count, String fromId, String toId, int weight) {
        graph.addEdge(new Edge(fromId, toId, weight));
        graph.addEdge(new Edge(toId, fromId, weight));
        kruskalEdges[count] = new Kruskal.Edge(Integer.parseInt(fromId), Integer.parseInt(toId), weight);
        return count + 1;
    }

    private static int[] shuffledIndices(int n, Random rng) {
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int temp = indices[i];
            indices[i] = indices[j];
            indices[j] = temp;
        }
        return indices;
    }
}
