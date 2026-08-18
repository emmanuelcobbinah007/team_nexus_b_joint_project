package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;
import edu.ug.nexusb.linear.ArrayStack;
import edu.ug.nexusb.linear.MyStack;
import edu.ug.nexusb.trees.HashSet;
import edu.ug.nexusb.trees.MySet;

/**
 * Iterative depth-first traversal with cycle detection (T048) over the
 * whole graph — every vertex, not just those reachable from one start
 * point, since a referral loop (A refers to B, B to C, C back to A) can
 * exist anywhere in the network regardless of where a caller happens to
 * start looking.
 *
 * <p>Backed by {@link ArrayStack} rather than recursion, per {@code
 * docs/interfaces.md}'s "Stack backs the audit-trail undo and the
 * iterative DFS" — the same structure T026 built, used here for its
 * second documented purpose.
 *
 * <p>Cycle detection in a <em>directed</em> graph needs three vertex
 * states, not just visited/unvisited: an edge to a vertex that's already
 * fully explored (finished) is perfectly normal in a DAG (e.g. two
 * branches that both reference a shared downstream facility) and is not a
 * cycle, but an edge to a vertex that's still an open ancestor on the
 * current path (on the stack, not yet finished) is a genuine loop. Using
 * a plain visited-set here would report false cycles on any diamond-shaped
 * DAG.
 */
public final class Dfs {

    private Dfs() {
        // utility class
    }

    /** One stack frame: either "start processing vertexId" or "finish it". */
    private static final class Frame {
        final String vertexId;
        final boolean isExit;

        Frame(String vertexId, boolean isExit) {
            this.vertexId = vertexId;
            this.isExit = isExit;
        }
    }

    /**
     * The outcome of a full-graph DFS: the order vertices were finished in,
     * and — if a referral loop exists anywhere in the network — the back
     * edge that closed it.
     */
    public static final class Result {

        private final String[] visitOrder;
        private final boolean hasCycle;
        private final String cycleFromId;
        private final String cycleToId;

        private Result(String[] visitOrder, boolean hasCycle, String cycleFromId, String cycleToId) {
            this.visitOrder = visitOrder;
            this.hasCycle = hasCycle;
            this.cycleFromId = cycleFromId;
            this.cycleToId = cycleToId;
        }

        /**
         * @return every vertex in the graph, in the order DFS finished
         *     exploring it (post-order) — the traversal-order evidence the
         *     brief asks for
         */
        public MyIterable<String> visitOrder() {
            String[] order = visitOrder;
            return () -> new MyIterator<String>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < order.length;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new StructureException("visitOrder() iterator has no more elements");
                    }
                    return order[index++];
                }
            };
        }

        /** @return {@code true} if a referral loop exists anywhere in the graph */
        public boolean hasCycle() {
            return hasCycle;
        }

        /**
         * @return the origin of the back edge that closed the (first)
         *     cycle found, or {@code null} if {@link #hasCycle()} is
         *     {@code false}
         */
        public String cycleFromId() {
            return cycleFromId;
        }

        /**
         * @return the destination of the back edge that closed the (first)
         *     cycle found — an ancestor of {@link #cycleFromId()} on the
         *     path DFS was following — or {@code null} if {@link
         *     #hasCycle()} is {@code false}
         */
        public String cycleToId() {
            return cycleToId;
        }
    }

    /**
     * Runs DFS over every vertex in {@code graph}, detecting whether any
     * referral loop exists.
     *
     * @param graph the graph to traverse; not mutated
     * @return the traversal order and cycle-detection outcome
     * @throws IllegalArgumentException if {@code graph} is {@code null}
     */
    public static Result traverse(MyGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }

        MyStack<Frame> stack = new ArrayStack<>();
        MySet<String> onStack = new HashSet<>();
        MySet<String> finished = new HashSet<>();
        String[] visitOrder = new String[graph.vertexCount()];
        int visitCount = 0;

        String foundCycleFrom = null;
        String foundCycleTo = null;

        MyIterator<String> vertices = graph.vertices().iterator();
        while (vertices.hasNext()) {
            String root = vertices.next();
            if (finished.contains(root)) {
                continue;
            }
            stack.push(new Frame(root, false));

            while (!stack.isEmpty()) {
                Frame frame = stack.pop();

                if (frame.isExit) {
                    onStack.remove(frame.vertexId);
                    finished.add(frame.vertexId);
                    visitOrder[visitCount] = frame.vertexId;
                    visitCount++;
                    continue;
                }
                if (finished.contains(frame.vertexId) || onStack.contains(frame.vertexId)) {
                    continue; // reached via another path already; nothing to redo
                }

                onStack.add(frame.vertexId);
                stack.push(new Frame(frame.vertexId, true));

                MyIterator<Edge> edges = graph.edgesFrom(frame.vertexId).iterator();
                while (edges.hasNext()) {
                    Edge edge = edges.next();
                    if (onStack.contains(edge.toId())) {
                        // Back edge to a still-open ancestor: a genuine cycle.
                        if (foundCycleFrom == null) {
                            foundCycleFrom = edge.fromId();
                            foundCycleTo = edge.toId();
                        }
                    } else if (!finished.contains(edge.toId())) {
                        stack.push(new Frame(edge.toId(), false));
                    }
                }
            }
        }

        boolean hasCycle = foundCycleFrom != null;
        return new Result(visitOrder, hasCycle, foundCycleFrom, foundCycleTo);
    }
}
