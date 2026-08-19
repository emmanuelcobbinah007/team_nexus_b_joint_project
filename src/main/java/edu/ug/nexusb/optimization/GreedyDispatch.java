package edu.ug.nexusb.optimization;

import edu.ug.nexusb.graphs.MyGraph;
import edu.ug.nexusb.graphs.PathResult;
import edu.ug.nexusb.graphs.Dijkstra;

/**
 * T051: greedy (nearest-facility-first) dispatch ordering, an urgency-weighted
 * alternative, and the counterexample metric that shows the two can disagree.
 * See {@code docs/counterexamples/counterexample_greedy_dispatch.md} for the
 * worked scenario and why {@link #runOptimalDispatch} sorts by
 * {@code distance * triageLevel} rather than distance alone.
 */
public class GreedyDispatch {

    // Represents a case request from case_request.csv
    public static class CaseRequest {
        public String caseRef;
        public String originFacilityId;
        public int triageLevel; // 1 (Highest/Urgent) to 4 (Lowest)
        public int responseWindowMin;

        public CaseRequest(String caseRef, String originFacilityId, int triageLevel, int responseWindowMin) {
            this.caseRef = caseRef;
            this.originFacilityId = originFacilityId;
            this.triageLevel = triageLevel;
            this.responseWindowMin = responseWindowMin;
        }
    }

    /**
     * GREEDY DISPATCH HEURISTIC (Nearest Facility First)
     * Uses T046 Dijkstra shortest paths to pick the closest facility first.
     *
     * @throws IllegalArgumentException if {@code requests} is null or contains a null element
     */
    public static String[] runGreedyDispatch(String resourceStationId, CaseRequest[] requests, MyGraph roadNetwork) {
        requireRequests(requests);
        PathResult pathResult = Dijkstra.shortestPaths(roadNetwork, resourceStationId);

        // Sort copy array by distance (Greedy Choice)
        CaseRequest[] sorted = requests.clone();
        for (int i = 0; i < sorted.length - 1; i++) {
            for (int j = 0; j < sorted.length - i - 1; j++) {
                double distA = pathResult.distanceTo(sorted[j].originFacilityId);
                double distB = pathResult.distanceTo(sorted[j + 1].originFacilityId);
                if (distA > distB) {
                    CaseRequest temp = sorted[j];
                    sorted[j] = sorted[j + 1];
                    sorted[j + 1] = temp;
                }
            }
        }

        String[] dispatchOrder = new String[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
            dispatchOrder[i] = sorted[i].caseRef;
        }
        return dispatchOrder;
    }

    /**
     * OPTIMAL DISPATCH BENCHMARK
     * Minimizes Total Triage Delay Penalty by combining distance and priority weight.
     *
     * @throws IllegalArgumentException if {@code requests} is null or contains a null element
     */
    public static String[] runOptimalDispatch(String resourceStationId, CaseRequest[] requests, MyGraph roadNetwork) {
        requireRequests(requests);
        PathResult pathResult = Dijkstra.shortestPaths(roadNetwork, resourceStationId);

        CaseRequest[] sorted = requests.clone();
        for (int i = 0; i < sorted.length - 1; i++) {
            for (int j = 0; j < sorted.length - i - 1; j++) {
                double scoreA = pathResult.distanceTo(sorted[j].originFacilityId) * sorted[j].triageLevel;
                double scoreB = pathResult.distanceTo(sorted[j + 1].originFacilityId) * sorted[j + 1].triageLevel;
                if (scoreA > scoreB) {
                    CaseRequest temp = sorted[j];
                    sorted[j] = sorted[j + 1];
                    sorted[j + 1] = temp;
                }
            }
        }

        String[] dispatchOrder = new String[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
            dispatchOrder[i] = sorted[i].caseRef;
        }
        return dispatchOrder;
    }

    /**
     * The objective a dispatch order is actually judged against: for a single
     * resource visiting requests sequentially in {@code dispatchOrder}, travel
     * time accumulates (the resource can't be in two places at once), so every
     * request after the first one waits at least as long as everything ahead
     * of it took to reach. This sums, for each request, its cumulative wait
     * (distance-so-far along the route) divided by its {@code triageLevel} —
     * an urgent case (triageLevel 1) has almost no wait tolerance, so its wait
     * counts against the total almost fully; a routine case (triageLevel 4)
     * tolerates the same wait four times as well, so it counts for a quarter
     * as much. {@link #runOptimalDispatch}'s sort key,
     * {@code distance * triageLevel} ascending, is exactly the ordering that
     * minimizes this sum (Smith's rule / weighted-shortest-processing-time,
     * with weight {@code 1/triageLevel}) — {@link #runGreedyDispatch}'s
     * distance-only order has no reason to agree with it. See
     * {@code docs/counterexamples/counterexample_greedy_dispatch.md} for a
     * worked case where it doesn't.
     *
     * @throws IllegalArgumentException if any argument is null, {@code dispatchOrder}
     *         contains a null or duplicate {@code caseRef}, or a {@code caseRef} in
     *         {@code dispatchOrder} has no matching entry in {@code requests}
     */
    public static double totalWeightedPenalty(
            String resourceStationId, String[] dispatchOrder, CaseRequest[] requests, MyGraph roadNetwork) {
        requireRequests(requests);
        if (dispatchOrder == null) {
            throw new IllegalArgumentException("dispatchOrder must not be null");
        }
        PathResult pathResult = Dijkstra.shortestPaths(roadNetwork, resourceStationId);

        double cumulativeDistance = 0.0;
        double penalty = 0.0;
        for (String caseRef : dispatchOrder) {
            CaseRequest request = findByCaseRef(requests, caseRef);
            cumulativeDistance += pathResult.distanceTo(request.originFacilityId);
            penalty += cumulativeDistance / request.triageLevel;
        }
        return penalty;
    }

    private static CaseRequest findByCaseRef(CaseRequest[] requests, String caseRef) {
        if (caseRef == null) {
            throw new IllegalArgumentException("dispatchOrder must not contain a null caseRef");
        }
        for (CaseRequest request : requests) {
            if (caseRef.equals(request.caseRef)) {
                return request;
            }
        }
        throw new IllegalArgumentException("no request in requests[] has caseRef " + caseRef);
    }

    private static void requireRequests(CaseRequest[] requests) {
        if (requests == null) {
            throw new IllegalArgumentException("requests must not be null");
        }
        for (CaseRequest request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("requests must not contain a null element");
            }
        }
    }
}
