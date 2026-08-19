package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.linear.BinaryHeapPriorityQueue;
import edu.ug.nexusb.linear.MyPriorityQueue;

/**
 * Prim's minimum spanning tree algorithm for the facility road network.
 *
 * <p>An MST is defined for an undirected graph, so every stored road link
 * is treated as a connection between its two endpoints. Route-finding
 * algorithms such as Dijkstra still respect road direction.
 */
public final class Prim {

    private Prim() {
        // Utility class: it should not be instantiated.
    }

    /**
     * Grows a minimum spanning tree from the specified starting vertex.
     *
     * @param graph connected weighted graph; it is not modified
     * @param startId vertex from which the tree should grow
     * @return selected MST edges and their total weight
     * @throws IllegalArgumentException if graph or startId is null
     * @throws KeyNotFoundException if startId is not in the graph
     * @throws IllegalStateException if the graph is disconnected
     */
    public static MstResult minimumSpanningTree(
            MyGraph graph,
            String startId) {

        if (graph == null) {
            throw new IllegalArgumentException(
                    "graph must not be null");
        }
        if (startId == null) {
            throw new IllegalArgumentException(
                    "startId must not be null");
        }
        if (!graph.containsVertex(startId)) {
            throw new KeyNotFoundException(
                    "No such vertex in graph: " + startId);
        }

        String[] vertexIds = collectVertexIds(graph);
        Edge[][] incidentEdges =
                buildIncidentEdges(graph, vertexIds);

        boolean[] inTree = new boolean[vertexIds.length];
        Edge[] selected =
                new Edge[Math.max(0, vertexIds.length - 1)];

        int selectedCount = 0;
        int visitedCount = 1;
        double totalWeight = 0.0;

        int startIndex = indexOf(vertexIds, startId);
        inTree[startIndex] = true;

        MyPriorityQueue<Edge> frontier =
                new BinaryHeapPriorityQueue<>(EDGE_ORDER);

        addFrontierEdges(
                startIndex,
                vertexIds,
                incidentEdges,
                inTree,
                frontier);

        while (!frontier.isEmpty()
                && selectedCount < selected.length) {

            Edge lightest = frontier.extractTop();
            int destinationIndex =
                    indexOf(vertexIds, lightest.toId());

            if (inTree[destinationIndex]) {
                continue;
            }

            selected[selectedCount] = lightest;
            selectedCount++;
            totalWeight += lightest.weight();

            inTree[destinationIndex] = true;
            visitedCount++;

            addFrontierEdges(
                    destinationIndex,
                    vertexIds,
                    incidentEdges,
                    inTree,
                    frontier);
        }

        if (visitedCount != vertexIds.length) {
            throw new IllegalStateException(
                    "graph is disconnected from start vertex "
                            + startId
                            + "; reached "
                            + visitedCount
                            + " of "
                            + vertexIds.length
                            + " vertices");
        }

        return new MstResult(
                startId,
                selected,
                totalWeight,
                vertexIds.length);
    }

    private static void addFrontierEdges(
            int vertexIndex,
            String[] vertexIds,
            Edge[][] incidentEdges,
            boolean[] inTree,
            MyPriorityQueue<Edge> frontier) {

        String fromId = vertexIds[vertexIndex];

        for (Edge edge : incidentEdges[vertexIndex]) {
            String destinationId;

            if (edge.fromId().equals(fromId)) {
                destinationId = edge.toId();
            } else {
                destinationId = edge.fromId();
            }

            int destinationIndex =
                    indexOf(vertexIds, destinationId);

            if (!inTree[destinationIndex]) {
                frontier.insert(
                        new Edge(
                                fromId,
                                destinationId,
                                edge.weight()));
            }
        }
    }

    private static Edge[][] buildIncidentEdges(
            MyGraph graph,
            String[] vertexIds) {

        int[] degrees = new int[vertexIds.length];
        countIncidentEdges(graph, vertexIds, degrees);

        Edge[][] incident = new Edge[vertexIds.length][];

        for (int i = 0; i < incident.length; i++) {
            incident[i] = new Edge[degrees[i]];
        }

        int[] next = new int[vertexIds.length];
        MyIterator<String> vertices =
                graph.vertices().iterator();

        while (vertices.hasNext()) {
            String vertexId = vertices.next();
            MyIterator<Edge> edges =
                    graph.edgesFrom(vertexId).iterator();

            while (edges.hasNext()) {
                Edge edge = edges.next();
                int from =
                        indexOf(vertexIds, edge.fromId());
                int to =
                        indexOf(vertexIds, edge.toId());

                if (from != to) {
                    incident[from][next[from]] = edge;
                    next[from]++;

                    incident[to][next[to]] = edge;
                    next[to]++;
                }
            }
        }

        return incident;
    }

    private static void countIncidentEdges(
            MyGraph graph,
            String[] vertexIds,
            int[] degrees) {

        MyIterator<String> vertices =
                graph.vertices().iterator();

        while (vertices.hasNext()) {
            String vertexId = vertices.next();
            MyIterator<Edge> edges =
                    graph.edgesFrom(vertexId).iterator();

            while (edges.hasNext()) {
                Edge edge = edges.next();
                int from =
                        indexOf(vertexIds, edge.fromId());
                int to =
                        indexOf(vertexIds, edge.toId());

                if (from != to) {
                    degrees[from]++;
                    degrees[to]++;
                }
            }
        }
    }

    private static String[] collectVertexIds(MyGraph graph) {
        String[] vertexIds =
                new String[graph.vertexCount()];

        MyIterator<String> vertices =
                graph.vertices().iterator();

        int index = 0;

        while (vertices.hasNext()) {
            vertexIds[index] = vertices.next();
            index++;
        }

        return vertexIds;
    }

    private static int indexOf(
            String[] vertexIds,
            String targetId) {

        for (int i = 0; i < vertexIds.length; i++) {
            if (vertexIds[i].equals(targetId)) {
                return i;
            }
        }

        throw new KeyNotFoundException(
                "No such vertex in this Prim run: "
                        + targetId);
    }

    private static final MyComparator<Edge> EDGE_ORDER =
            (first, second) -> {

                int byWeight =
                        Double.compare(
                                first.weight(),
                                second.weight());

                if (byWeight != 0) {
                    return byWeight;
                }

                int byFrom =
                        first.fromId()
                                .compareTo(second.fromId());

                if (byFrom != 0) {
                    return byFrom;
                }

                return first.toId()
                        .compareTo(second.toId());
            };
}