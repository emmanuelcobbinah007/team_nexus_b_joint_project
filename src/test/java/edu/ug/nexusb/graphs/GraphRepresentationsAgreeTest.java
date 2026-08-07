package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ug.nexusb.core.MyIterator;
import org.junit.jupiter.api.Test;

/**
 * The free correctness oracle {@code docs/interfaces.md} promises: builds
 * the identical sequence of operations on both representations and asserts
 * every query agrees. If this ever fails, one of the two implementations
 * has a bug — not which one, but that they disagree at all is the signal.
 */
class GraphRepresentationsAgreeTest {

    private static final String[][] EDGES = {
        {"F001", "F002", "4.0"},
        {"F001", "F003", "9.0"},
        {"F002", "F003", "2.0"},
        {"F003", "F004", "6.0"},
        {"F004", "F001", "1.0"},
        {"F005", "F005", "0.0"}, // self-loop
    };

    @Test
    void bothRepresentationsAgreeOnEveryQuery() {
        MyGraph list = new AdjacencyListGraph();
        MyGraph matrix = new AdjacencyMatrixGraph();

        for (String[] e : EDGES) {
            Edge edge = new Edge(e[0], e[1], Double.parseDouble(e[2]));
            list.addEdge(edge);
            matrix.addEdge(edge);
        }
        list.addVertex("F999"); // isolated vertex, no edges either direction
        matrix.addVertex("F999");
        list.removeEdge("F002", "F003");
        matrix.removeEdge("F002", "F003");

        assertEquals(list.vertexCount(), matrix.vertexCount());
        assertEquals(list.edgeCount(), matrix.edgeCount());

        String[] probeIds = {"F001", "F002", "F003", "F004", "F005", "F999"};
        for (String from : probeIds) {
            for (String to : probeIds) {
                assertEquals(
                    list.containsEdge(from, to), matrix.containsEdge(from, to),
                    "containsEdge(" + from + "," + to + ") disagreed");
                if (list.containsEdge(from, to)) {
                    assertEquals(
                        list.weightOf(from, to), matrix.weightOf(from, to), 1e-9,
                        "weightOf(" + from + "," + to + ") disagreed");
                }
            }
        }

        for (String vertexId : probeIds) {
            assertEquals(
                countEdges(list, vertexId), countEdges(matrix, vertexId),
                "out-degree of " + vertexId + " disagreed");
        }
    }

    private static int countEdges(MyGraph graph, String vertexId) {
        int count = 0;
        MyIterator<Edge> it = graph.edgesFrom(vertexId).iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }
}
