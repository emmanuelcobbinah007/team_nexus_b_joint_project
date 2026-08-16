package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.linear.BinaryHeapPriorityQueue;
import edu.ug.nexusb.linear.MyPriorityQueue;

/**
 * Dijkstra's shortest-path algorithm (T046): fastest ambulance route from
 * one facility to every other facility reachable from it, under the
 * traffic-weighted road network.
 *
 * <p>Correctness depends on every edge weight being non-negative, which
 * {@link Edge}'s compact constructor already guarantees at construction
 * time — there is no separate check here because there is nothing left to
 * check.
 *
 * <p>This is exactly the scenario {@link MyPriorityQueue#decreaseKey}
 * exists for: one mutable {@link Entry} object per discovered vertex is
 * inserted into the heap once, and every time a shorter route is found the
 * same object's distance is updated in place and {@code decreaseKey} is
 * called to re-sift it, rather than inserting a second, stale entry.
 */
public final class Dijkstra {

    private Dijkstra() {
        // utility class
    }

    /**
     * Runs Dijkstra's algorithm from {@code sourceId} over every vertex
     * {@code graph} currently contains.
     *
     * @param graph the graph to search; not mutated
     * @param sourceId the facility to route from
     * @return distances, predecessors and visit order for every vertex
     *     {@code graph} contains, source included (distance 0)
     * @throws IllegalArgumentException if {@code graph} or {@code sourceId} is {@code null}
     * @throws KeyNotFoundException if {@code sourceId} is not in {@code graph}
     */
    public static PathResult shortestPaths(MyGraph graph, String sourceId) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId must not be null");
        }
        if (!graph.containsVertex(sourceId)) {
            throw new KeyNotFoundException("No such vertex in graph: " + sourceId);
        }

        String[] ids = collectVertexIds(graph);
        int n = ids.length;
        double[] dist = new double[n];
        String[] pred = new String[n];
        boolean[] finalized = new boolean[n];
        Entry[] entries = new Entry[n];
        for (int i = 0; i < n; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
        }

        MyPriorityQueue<Entry> queue = new BinaryHeapPriorityQueue<>(DISTANCE_ORDER);

        int sourceIndex = indexOf(ids, sourceId);
        dist[sourceIndex] = 0.0;
        entries[sourceIndex] = new Entry(sourceId, 0.0);
        queue.insert(entries[sourceIndex]);

        String[] visitOrder = new String[n];
        int visitCount = 0;

        while (!queue.isEmpty()) {
            Entry current = queue.extractTop();
            int u = indexOf(ids, current.vertexId);
            finalized[u] = true;
            visitOrder[visitCount] = ids[u];
            visitCount++;

            MyIterator<Edge> edges = graph.edgesFrom(ids[u]).iterator();
            while (edges.hasNext()) {
                Edge edge = edges.next();
                int v = indexOf(ids, edge.toId());
                if (finalized[v]) {
                    continue;
                }
                double candidate = dist[u] + edge.weight();
                if (candidate < dist[v]) {
                    dist[v] = candidate;
                    pred[v] = ids[u];
                    if (entries[v] == null) {
                        entries[v] = new Entry(ids[v], candidate);
                        queue.insert(entries[v]);
                    } else {
                        entries[v].distance = candidate;
                        queue.decreaseKey(entries[v]);
                    }
                }
            }
        }

        String[] trimmedVisitOrder = new String[visitCount];
        System.arraycopy(visitOrder, 0, trimmedVisitOrder, 0, visitCount);
        return new PathResult(sourceId, ids, dist, pred, trimmedVisitOrder);
    }

    private static String[] collectVertexIds(MyGraph graph) {
        String[] ids = new String[graph.vertexCount()];
        int i = 0;
        MyIterator<String> it = graph.vertices().iterator();
        while (it.hasNext()) {
            ids[i] = it.next();
            i++;
        }
        return ids;
    }

    private static int indexOf(String[] ids, String vertexId) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(vertexId)) {
                return i;
            }
        }
        throw new KeyNotFoundException("No such vertex in this run: " + vertexId);
    }

    private static final MyComparator<Entry> DISTANCE_ORDER =
        (a, b) -> Double.compare(a.distance, b.distance);

    /**
     * One mutable heap entry per discovered vertex. {@code equals}/{@code
     * hashCode} are based on {@code vertexId} alone, deliberately ignoring
     * {@code distance} — {@link MyPriorityQueue#decreaseKey} looks an entry
     * up by equality and re-sifts whatever is already stored at that slot,
     * so identity-by-vertex is what lets the same entry be found again
     * after its distance has changed.
     */
    private static final class Entry {
        private final String vertexId;
        private double distance;

        Entry(String vertexId, double distance) {
            this.vertexId = vertexId;
            this.distance = distance;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry)) {
                return false;
            }
            return vertexId.equals(((Entry) other).vertexId);
        }

        @Override
        public int hashCode() {
            return vertexId.hashCode();
        }
    }
}
