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

    /** One case as actually served: its place in service order and how long it waited. */
    public record ServedCase(String caseId, int arrivalTime, int severityPriority, int waitTime) {
    }

    /** Full per-case breakdown of both dispatch modes, for visualizing the queue rather than just its average. */
    public static class DetailedResult {
        public final ServedCase[] fcfsOrder;
        public final ServedCase[] priorityOrder;
        public final double fcfsAverageWait;
        public final double priorityAverageWait;

        public DetailedResult(ServedCase[] fcfsOrder, ServedCase[] priorityOrder) {
            this.fcfsOrder = fcfsOrder;
            this.priorityOrder = priorityOrder;
            this.fcfsAverageWait = averageWait(fcfsOrder);
            this.priorityAverageWait = averageWait(priorityOrder);
        }

        private static double averageWait(ServedCase[] served) {
            long total = 0;
            for (ServedCase s : served) {
                total += s.waitTime();
            }
            return served.length == 0 ? 0.0 : (double) total / served.length;
        }
    }

    /**
     * Same simulation as {@link #compare}, but returns each case's actual
     * service order and individual wait time under both modes instead of
     * only the aggregate average -- what a queue visualization needs.
     *
     * @throws IllegalArgumentException if {@code cases} is null, empty, or contains a null element
     */
    public static DetailedResult compareDetailed(List<TriageCase> cases) {
        requireCases(cases);
        return new DetailedResult(runFcfsDetailed(cases), runPriorityDetailed(cases));
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
        ServedCase[] served = runFcfsDetailed(cases);
        double totalWait = 0;
        for (ServedCase s : served) {
            if (verbose) {
                System.out.println("Processed Case: " + s.caseId() + " | Arrival: " + s.arrivalTime() + " | Wait Time: " + s.waitTime());
            }
            totalWait += s.waitTime();
        }
        return totalWait / cases.size();
    }

    private static double runPriority(List<TriageCase> cases, boolean verbose) {
        ServedCase[] served = runPriorityDetailed(cases);
        double totalWait = 0;
        for (ServedCase s : served) {
            if (verbose) {
                System.out.println("Processed Case: " + s.caseId() + " | Priority: " + s.severityPriority() + " | Wait Time: " + s.waitTime());
            }
            totalWait += s.waitTime();
        }
        return totalWait / cases.size();
    }

    private static ServedCase[] runFcfsDetailed(List<TriageCase> cases) {
        List<TriageCase> ordered = new ArrayList<>(cases);
        ordered.sort(Comparator.comparingInt(TriageCase::getArrivalTime));

        ServedCase[] served = new ServedCase[ordered.size()];
        int currentTime = 0;
        for (int i = 0; i < ordered.size(); i++) {
            TriageCase c = ordered.get(i);
            int waitTime = Math.max(0, currentTime - c.getArrivalTime());
            served[i] = new ServedCase(c.getCaseId(), c.getArrivalTime(), c.getSeverityPriority(), waitTime);
            currentTime += 2; // Assume 2 units of processing time per case
        }
        return served;
    }

    private static ServedCase[] runPriorityDetailed(List<TriageCase> cases) {
        PriorityQueue<TriageCase> queue = new PriorityQueue<>(cases);
        ServedCase[] served = new ServedCase[cases.size()];
        int currentTime = 0;
        int i = 0;
        while (!queue.isEmpty()) {
            TriageCase c = queue.poll();
            int waitTime = Math.max(0, currentTime - c.getArrivalTime());
            served[i++] = new ServedCase(c.getCaseId(), c.getArrivalTime(), c.getSeverityPriority(), waitTime);
            currentTime += 2;
        }
        return served;
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