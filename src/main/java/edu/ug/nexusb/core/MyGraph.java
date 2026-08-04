package edu.ug.nexusb.core;

/**
 * A directed, weighted graph over facility vertices, identified by ID.
 *
 * <p>Implemented twice — once backed by an adjacency list, once by an
 * adjacency matrix — behind this one contract, so the two representations
 * can be benchmarked and cross-checked against each other rather than
 * trusted individually. Both implementations must agree on every query for
 * the same input: that agreement is this sub-team's free correctness
 * oracle, in addition to whatever unit tests exist.
 *
 * <p>An undirected road is modelled as two {@link Edge} instances, one in
 * each direction, both added through {@link #addEdge(Edge)}.
 */
public interface MyGraph extends Instrumented {

    /**
     * Identifies which representation this instance uses.
     *
     * <p>The returned value is written into {@code algorithm_run.structure_name}
     * for every experiment, which turns "does adjacency list or adjacency
     * matrix perform better at this graph size" from a manual comparison
     * into a database query.
     *
     * @return {@code "ADJACENCY_LIST"} or {@code "ADJACENCY_MATRIX"}
     */
    String representationName();

    /**
     * Adds a vertex with the given ID if it is not already present.
     * Adding a vertex that already exists is a no-op, not an error.
     *
     * @param vertexId the facility identifier to add
     * @throws IllegalArgumentException if {@code vertexId} is {@code null}
     */
    void addVertex(String vertexId);

    /**
     * Reports whether a vertex with the given ID has been added.
     *
     * @param vertexId the facility identifier to check
     * @return {@code true} if the vertex is present in this graph
     */
    boolean containsVertex(String vertexId);

    /**
     * Adds a directed edge to the graph, overwriting any existing edge
     * between the same ordered pair of vertices.
     *
     * @param edge the edge to add; its endpoints are added as vertices
     *     automatically if they are not already present
     * @throws IllegalArgumentException if {@code edge} is {@code null}
     */
    void addEdge(Edge edge);

    /**
     * Removes the directed edge from {@code fromId} to {@code toId}, if one
     * exists. Models a road closed by flooding or roadworks — the scenario
     * the BFS reachability demonstration depends on. Removing an edge that
     * does not exist is a no-op, not an error.
     *
     * @param fromId the edge's origin vertex
     * @param toId the edge's destination vertex
     */
    void removeEdge(String fromId, String toId);

    /**
     * Reports whether a directed edge exists from {@code fromId} to {@code toId}.
     *
     * @param fromId the edge's origin vertex
     * @param toId the edge's destination vertex
     * @return {@code true} if that directed edge is present
     */
    boolean containsEdge(String fromId, String toId);

    /**
     * Returns the weight of the directed edge from {@code fromId} to
     * {@code toId} — the effective travel time in minutes.
     *
     * @param fromId the edge's origin vertex
     * @param toId the edge's destination vertex
     * @return the edge weight
     * @throws KeyNotFoundException if no such edge exists
     */
    double weightOf(String fromId, String toId);

    /**
     * Returns every vertex currently in the graph.
     *
     * @return an iterable over all vertex IDs, in implementation-defined order
     */
    MyIterable<String> vertices();

    /**
     * Returns every outgoing edge from {@code vertexId}.
     *
     * @param vertexId the vertex whose outgoing edges are requested
     * @return an iterable over that vertex's outgoing edges; empty if the
     *     vertex has none
     * @throws KeyNotFoundException if {@code vertexId} is not in the graph
     */
    MyIterable<Edge> edgesFrom(String vertexId);

    /**
     * Returns how many vertices are currently in the graph.
     *
     * @return the vertex count, never negative
     */
    int vertexCount();

    /**
     * Returns how many directed edges are currently in the graph.
     *
     * @return the edge count, never negative
     */
    int edgeCount();
}
