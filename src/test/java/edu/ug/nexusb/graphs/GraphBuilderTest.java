package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link GraphBuilder#buildFromRows} — the pure-logic half of
 * T038, with no database involved. That split exists (see {@link
 * GraphBuilder}'s own javadoc) specifically so the assembly logic can be
 * unit-tested with plain in-memory data; these tests previously drove
 * {@code buildFromDatabase} instead, against a hand-rolled schema
 * (a {@code road_link} table with different columns than the real one, and
 * no {@code v_weighted_edge} view at all) that didn't match what
 * {@code buildFromDatabase} actually queries, and failed in CI as soon as
 * no pre-existing {@code campus.db} happened to be lying around locally.
 *
 * <p>{@code buildFromDatabase} itself is covered separately, against the
 * real schema, by {@link GraphBuilderDatabaseIntegrationTest}.
 */
class GraphBuilderTest {

    @Test
    void vertexCountMatchesFacilityIdCount() {
        List<Integer> facilityIds = idsFrom(1, 60);

        MyGraph graph = GraphBuilder.buildFromRows(facilityIds, List.of());

        assertEquals(60, graph.vertexCount());
        assertTrue(graph.vertexCount() >= 50, "expected at least 50 facilities per the brief");
    }

    @Test
    void edgeCountIsAtLeastRoadRowCountAndOneWayIsRespected() {
        List<Integer> facilityIds = idsFrom(1, 5);
        List<GraphBuilder.RoadRow> roads = List.of(
            new GraphBuilder.RoadRow(1, 2, 4.0, true),   // one-way: 1 edge
            new GraphBuilder.RoadRow(2, 3, 6.0, false),  // two-way: 2 edges
            new GraphBuilder.RoadRow(3, 4, 2.5, false)); // two-way: 2 edges

        MyGraph graph = GraphBuilder.buildFromRows(facilityIds, roads);

        assertTrue(graph.edgeCount() >= roads.size());
        assertEquals(5, graph.edgeCount()); // 1 + 2 + 2
        assertTrue(graph.containsEdge("1", "2"));
        assertTrue(graph.containsEdge("2", "3"));
        assertTrue(graph.containsEdge("3", "2")); // two-way reverse edge exists
        assertTrue(graph.containsEdge("3", "4"));
        assertTrue(graph.containsEdge("4", "3"));
        assertEquals(6.0, graph.weightOf("2", "3"));
    }

    @Test
    void emptyInputProducesAnEmptyGraph() {
        MyGraph graph = GraphBuilder.buildFromRows(List.of(), List.of());

        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void roadRowReferencingAnUnlistedFacilityStillAddsItAsAVertex() {
        // GraphBuilder auto-adds edge endpoints (MyGraph.addEdge's own
        // contract), so a road row is not required to reference only
        // facility IDs already passed in facilityIds.
        List<Integer> facilityIds = idsFrom(1, 1);
        List<GraphBuilder.RoadRow> roads = List.of(new GraphBuilder.RoadRow(1, 99, 3.0, true));

        MyGraph graph = GraphBuilder.buildFromRows(facilityIds, roads);

        assertEquals(2, graph.vertexCount());
        assertTrue(graph.containsVertex("99"));
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void nullFacilityIdsThrows() {
        assertThrows(IllegalArgumentException.class, () -> GraphBuilder.buildFromRows(null, List.of()));
    }

    @Test
    void nullRoadsThrows() {
        assertThrows(IllegalArgumentException.class, () -> GraphBuilder.buildFromRows(List.of(), null));
    }

    private static List<Integer> idsFrom(int startInclusive, int endInclusive) {
        List<Integer> ids = new ArrayList<>();
        for (int i = startInclusive; i <= endInclusive; i++) {
            ids.add(i);
        }
        return ids;
    }
}
