package edu.ug.nexusb.optimization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.graphs.AdjacencyListGraph;
import edu.ug.nexusb.graphs.Edge;
import edu.ug.nexusb.graphs.MyGraph;
import edu.ug.nexusb.optimization.GreedyDispatch.CaseRequest;
import org.junit.jupiter.api.Test;

class GreedyDispatchTest {

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void greedyDispatchOrdersByDistanceAloneNearestFirst() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addEdge(new Edge("STATION", "F024", 3.0));
        graph.addEdge(new Edge("STATION", "F053", 5.0));
        CaseRequest[] requests = {
            new CaseRequest("REQ_URGENT", "F053", 1, 15),
            new CaseRequest("REQ_ROUTINE", "F024", 4, 30),
        };

        String[] order = GreedyDispatch.runGreedyDispatch("STATION", requests, graph);

        assertArrayEquals(new String[] {"REQ_ROUTINE", "REQ_URGENT"}, order, "3km is nearer than 5km, urgency ignored");
    }

    @Test
    void optimalDispatchOrdersByDistanceTimesTriageLevel() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addEdge(new Edge("STATION", "F024", 3.0));
        graph.addEdge(new Edge("STATION", "F053", 5.0));
        CaseRequest[] requests = {
            new CaseRequest("REQ_URGENT", "F053", 1, 15),   // score 5*1=5
            new CaseRequest("REQ_ROUTINE", "F024", 4, 30),  // score 3*4=12
        };

        String[] order = GreedyDispatch.runOptimalDispatch("STATION", requests, graph);

        assertArrayEquals(new String[] {"REQ_URGENT", "REQ_ROUTINE"}, order, "lower distance*triageLevel score goes first");
    }

    // ------------------------------------------------------------------
    // Counterexample: greedy's own dispatch order costs more than optimal's,
    // under the urgency-weighted penalty both orders are actually judged by.
    // See docs/counterexamples/counterexample_greedy_dispatch.md for the
    // worked write-up this test backs.
    // ------------------------------------------------------------------

    @Test
    void greedyProducesAStrictlyWorsePenaltyThanOptimalOnAnEngineeredCase() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addEdge(new Edge("STATION", "F024", 3.0)); // nearer, routine
        graph.addEdge(new Edge("STATION", "F053", 5.0)); // farther, critical
        CaseRequest[] requests = {
            new CaseRequest("REQ_ROUTINE", "F024", 4, 30),
            new CaseRequest("REQ_URGENT", "F053", 1, 15),
        };

        String[] greedyOrder = GreedyDispatch.runGreedyDispatch("STATION", requests, graph);
        String[] optimalOrder = GreedyDispatch.runOptimalDispatch("STATION", requests, graph);

        assertArrayEquals(new String[] {"REQ_ROUTINE", "REQ_URGENT"}, greedyOrder);
        assertArrayEquals(new String[] {"REQ_URGENT", "REQ_ROUTINE"}, optimalOrder);

        double greedyPenalty = GreedyDispatch.totalWeightedPenalty("STATION", greedyOrder, requests, graph);
        double optimalPenalty = GreedyDispatch.totalWeightedPenalty("STATION", optimalOrder, requests, graph);

        // greedy: 3/4 + (3+5)/1 = 0.75 + 8    = 8.75
        // optimal: 5/1 + (5+3)/4 = 5.0 + 2    = 7.0
        assertEquals(8.75, greedyPenalty, 1e-9);
        assertEquals(7.0, optimalPenalty, 1e-9);
        assertTrue(optimalPenalty < greedyPenalty,
            "serving the farther but critical case first must cost less overall than serving the nearer routine case first");
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void emptyRequestsProducesEmptyOrderAndZeroPenalty() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addVertex("STATION");
        CaseRequest[] requests = {};

        assertEquals(0, GreedyDispatch.runGreedyDispatch("STATION", requests, graph).length);
        assertEquals(0, GreedyDispatch.runOptimalDispatch("STATION", requests, graph).length);
        assertEquals(0.0, GreedyDispatch.totalWeightedPenalty("STATION", new String[0], requests, graph));
    }

    @Test
    void singleRequestIsTrivialForBothOrderings() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addEdge(new Edge("STATION", "F024", 3.0));
        CaseRequest[] requests = { new CaseRequest("REQ0001", "F024", 2, 30) };

        assertArrayEquals(new String[] {"REQ0001"}, GreedyDispatch.runGreedyDispatch("STATION", requests, graph));
        assertArrayEquals(new String[] {"REQ0001"}, GreedyDispatch.runOptimalDispatch("STATION", requests, graph));
        assertEquals(1.5, GreedyDispatch.totalWeightedPenalty("STATION", new String[] {"REQ0001"}, requests, graph), 1e-9);
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void nullRequestsThrows() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addVertex("STATION");
        assertThrows(IllegalArgumentException.class, () -> GreedyDispatch.runGreedyDispatch("STATION", null, graph));
        assertThrows(IllegalArgumentException.class, () -> GreedyDispatch.runOptimalDispatch("STATION", null, graph));
    }

    @Test
    void nullElementInRequestsThrows() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addVertex("STATION");
        CaseRequest[] requests = { new CaseRequest("REQ0001", "STATION", 1, 30), null };
        assertThrows(IllegalArgumentException.class, () -> GreedyDispatch.runGreedyDispatch("STATION", requests, graph));
    }

    @Test
    void nullGraphThrows() {
        CaseRequest[] requests = { new CaseRequest("REQ0001", "STATION", 1, 30) };
        assertThrows(IllegalArgumentException.class, () -> GreedyDispatch.runGreedyDispatch("STATION", requests, null));
    }

    @Test
    void unknownResourceStationThrows() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addVertex("F024");
        CaseRequest[] requests = { new CaseRequest("REQ0001", "F024", 1, 30) };
        assertThrows(RuntimeException.class, () -> GreedyDispatch.runGreedyDispatch("NOWHERE", requests, graph));
    }

    @Test
    void totalWeightedPenaltyRejectsUnknownCaseRefInDispatchOrder() {
        MyGraph graph = new AdjacencyListGraph();
        graph.addEdge(new Edge("STATION", "F024", 3.0));
        CaseRequest[] requests = { new CaseRequest("REQ0001", "F024", 2, 30) };

        assertThrows(IllegalArgumentException.class,
            () -> GreedyDispatch.totalWeightedPenalty("STATION", new String[] {"REQ_GHOST"}, requests, graph));
    }
}
