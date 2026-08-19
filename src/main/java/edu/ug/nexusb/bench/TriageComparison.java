package edu.ug.nexusb.bench;

import java.util.*;

public class TriageComparison {

    // Simple Case representation for the dispatch simulation
    public static class TriageCase implements Comparable<TriageCase> {
        private final String caseId;
        private final int arrivalTime;
        private final int severityPriority; // Lower number = higher priority (e.g., 1 = critical)

        public TriageCase(String caseId, int arrivalTime, int severityPriority) {
            this.caseId = caseId;
            this.arrivalTime = arrivalTime;
            this.severityPriority = severityPriority;
        }

        public String getCaseId() { return caseId; }
        public int getArrivalTime() { return arrivalTime; }
        public int getSeverityPriority() { return severityPriority; }

        @Override
        public int compareTo(TriageCase other) {
            // For Priority Queue: sort by severity first, then arrival time
            if (this.severityPriority != other.severityPriority) {
                return Integer.compare(this.severityPriority, other.severityPriority);
            }
            return Integer.compare(this.arrivalTime, other.arrivalTime);
        }
    }

    /** The two dispatch modes' average wait times over the same case list. */
    public static class ComparisonResult {
        public final double fcfsAverageWait;
        public final double priorityAverageWait;

        public ComparisonResult(double fcfsAverageWait, double priorityAverageWait) {
            this.fcfsAverageWait = fcfsAverageWait;
            this.priorityAverageWait = priorityAverageWait;
        }
    }

    /**
     * Simulates dispatching {@code cases} two ways over a fixed 2-time-unit
     * processing cost per case -- first-come-first-served (arrival-time
     * order) and triage-priority (severity, then arrival time, via {@link
     * TriageCase#compareTo}) -- and returns the average wait time under each.
     *
     * @throws IllegalArgumentException if {@code cases} is null, empty, or contains a null element
     */
    public static ComparisonResult compare(List<TriageCase> cases) {
        requireCases(cases);
        return new ComparisonResult(runFcfs(cases, false), runPriority(cases, false));
    }

    public static void runComparison() {
        System.out.println("=== FCFS vs. Triage-Priority Dispatch Comparison ===");

        // Sample test cases simulating incoming healthcare dispatch events
        List<TriageCase> cases = Arrays.asList(
            new TriageCase("C001", 0, 3),
            new TriageCase("C002", 1, 1), // High priority arrives shortly after
            new TriageCase("C003", 2, 2),
            new TriageCase("C004", 4, 1)  // Another high priority
        );

        System.out.println("\n--- Running FCFS (First-Come-First-Served) ---");
        double fcfsAverageWait = runFcfs(cases, true);
        System.out.println("Average FCFS Wait Time: " + fcfsAverageWait + " units");

        System.out.println("\n--- Running Triage-Priority Mode ---");
        double priorityAverageWait = runPriority(cases, true);
        System.out.println("Average Triage-Priority Wait Time: " + priorityAverageWait + " units");

        System.out.println("\n=== Comparison Complete ===");
    }

    // FCFS means arrival-time order, not "whatever order the caller's list
    // happens to be in" -- explicitly sorting a copy is what makes this
    // correct for a cases list that isn't already arrival-ordered.
    private static double runFcfs(List<TriageCase> cases, boolean verbose) {
        List<TriageCase> ordered = new ArrayList<>(cases);
        ordered.sort(Comparator.comparingInt(TriageCase::getArrivalTime));

        int currentTime = 0;
        double totalWait = 0;
        for (TriageCase c : ordered) {
            int waitTime = Math.max(0, currentTime - c.getArrivalTime());
            totalWait += waitTime;
            if (verbose) {
                System.out.println("Processed Case: " + c.getCaseId() + " | Arrival: " + c.getArrivalTime() + " | Wait Time: " + waitTime);
            }
            currentTime += 2; // Assume 2 units of processing time per case
        }
        return totalWait / cases.size();
    }

    private static double runPriority(List<TriageCase> cases, boolean verbose) {
        PriorityQueue<TriageCase> queue = new PriorityQueue<>(cases);
        int currentTime = 0;
        double totalWait = 0;
        while (!queue.isEmpty()) {
            TriageCase c = queue.poll();
            int waitTime = Math.max(0, currentTime - c.getArrivalTime());
            totalWait += waitTime;
            if (verbose) {
                System.out.println("Processed Case: " + c.getCaseId() + " | Priority: " + c.getSeverityPriority() + " | Wait Time: " + waitTime);
            }
            currentTime += 2;
        }
        return totalWait / cases.size();
    }

    private static void requireCases(List<TriageCase> cases) {
        if (cases == null || cases.isEmpty()) {
            throw new IllegalArgumentException("cases must not be null or empty");
        }
        for (TriageCase c : cases) {
            if (c == null) {
                throw new IllegalArgumentException("cases must not contain a null element");
            }
        }
    }

    public static void main(String[] args) {
        runComparison();
    }
}