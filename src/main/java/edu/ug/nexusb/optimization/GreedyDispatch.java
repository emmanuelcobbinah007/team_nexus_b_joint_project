package edu.ug.nexusb.optimization;

import edu.ug.nexusb.graphs.MyGraph;
import edu.ug.nexusb.graphs.PathResult;
import edu.ug.nexusb.graphs.Dijkstra;

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
     */
    public static String[] runGreedyDispatch(String resourceStationId, CaseRequest[] requests, MyGraph roadNetwork) {
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
     */
    public static String[] runOptimalDispatch(String resourceStationId, CaseRequest[] requests, MyGraph roadNetwork) {
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
}
