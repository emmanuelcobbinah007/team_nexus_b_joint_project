package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.linear.ArrayQueue;
import edu.ug.nexusb.linear.MyQueue;
import edu.ug.nexusb.trees.MySet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReachabilityTest {

    @Test
    void allConnectedFacilitiesReachableWhenNoRoadsClosed() {
        MyGraph graph = GraphBuilder.buildFromRows(
                List.of(1, 2, 3),
                List.of(
                        new GraphBuilder.RoadRow(1, 2, 5.0, false),
                        new GraphBuilder.RoadRow(2, 3, 5.0, false)
                ));

        MyQueue<String> queue = new ArrayQueue<>();
        MySet<String> visited = new TestOnlySet<>();
        MySet<String> closedRoads = new TestOnlySet<>();

        MySet<String> reachable = Reachability.bfsReachable(graph, "1", closedRoads, queue, visited);

        assertTrue(reachable.contains("1"));
        assertTrue(reachable.contains("2"));
        assertTrue(reachable.contains("3"));
    }

    @Test
    void closedRoadBlocksReachability() {
        MyGraph graph = GraphBuilder.buildFromRows(
                List.of(1, 2, 3),
                List.of(
                        new GraphBuilder.RoadRow(1, 2, 5.0, false),
                        new GraphBuilder.RoadRow(2, 3, 5.0, false)
                ));

        MyQueue<String> queue = new ArrayQueue<>();
        MySet<String> visited = new TestOnlySet<>();
        MySet<String> closedRoads = new TestOnlySet<>();
        closedRoads.add("2->3");

        MySet<String> reachable = Reachability.bfsReachable(graph, "1", closedRoads, queue, visited);

        assertTrue(reachable.contains("1"));
        assertTrue(reachable.contains("2"));
        assertFalse(reachable.contains("3"));
    }
}