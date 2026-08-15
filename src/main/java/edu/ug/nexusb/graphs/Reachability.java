package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.linear.MyQueue;
import edu.ug.nexusb.trees.MySet;

/**
 * BFS-based reachability (T047): starting from one facility, finds every
 * facility still reachable when a given set of roads is treated as closed
 * (e.g. flooding, roadworks) — a realistic campus/hospital-network scenario.
 */
public final class Reachability {

    private Reachability() {
        // utility class
    }

    /**
     * @param graph          the graph to search
     * @param startId        facility ID to start from
     * @param closedEdgeKeys set of closed edges, formatted as "fromId->toId"
     * @param queue          an empty MyQueue instance to use for traversal
     * @param visited        an empty MySet instance to record visited vertices
     * @return the set of facility IDs reachable from startId, honoring closed roads
     */
    public static MySet<String> bfsReachable(
            MyGraph graph,
            String startId,
            MySet<String> closedEdgeKeys,
            MyQueue<String> queue,
            MySet<String> visited) {

        queue.enqueue(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.dequeue();

            MyIterator<Edge> edges = graph.edgesFrom(current).iterator();
            while (edges.hasNext()) {
                Edge edge = edges.next();
                String edgeKey = edge.fromId() + "->" + edge.toId();
                boolean isClosed = closedEdgeKeys.contains(edgeKey);
                boolean alreadyVisited = visited.contains(edge.toId());

                if (!isClosed && !alreadyVisited) {
                    visited.add(edge.toId());
                    queue.enqueue(edge.toId());
                }
            }
        }
        return visited;
    }
}