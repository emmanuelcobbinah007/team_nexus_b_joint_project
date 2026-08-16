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

    public static void runComparison() {
        System.out.println("=== FCFS vs. Triage-Priority Dispatch Comparison ===");

        // Sample test cases simulating incoming healthcare dispatch events
        List<TriageCase> cases = Arrays.asList(
            new TriageCase("C001", 0, 3),
            new TriageCase("C002", 1, 1), // High priority arrives shortly after
            new TriageCase("C003", 2, 2),
            new TriageCase("C004", 4, 1)  // Another high priority
        );

        // 1. Evaluate First-Come-First-Served (FCFS)
        System.out.println("\n--- Running FCFS (First-Come-First-Served) ---");
        Queue<TriageCase> fcfsQueue = new LinkedList<>(cases);
        int currentTime = 0;
        double totalFcfsWait = 0;

        while (!fcfsQueue.isEmpty()) {
            TriageCase c = fcfsQueue.poll();
            int waitTime = Math.max(0, currentTime - c.getArrivalTime());
            totalFcfsWait += waitTime;
            System.out.println("Processed Case: " + c.getCaseId() + " | Arrival: " + c.getArrivalTime() + " | Wait Time: " + waitTime);
            currentTime += 2; // Assume 2 units of processing time per case
        }
        System.out.println("Average FCFS Wait Time: " + (totalFcfsWait / cases.size()) + " units");

        // 2. Evaluate Triage-Priority
        System.out.println("\n--- Running Triage-Priority Mode ---");
        PriorityQueue<TriageCase> priorityQueue = new PriorityQueue<>(cases);
        currentTime = 0;
        double totalPriorityWait = 0;

        while (!priorityQueue.isEmpty()) {
            TriageCase c = priorityQueue.poll();
            int waitTime = Math.max(0, currentTime - c.getArrivalTime());
            totalPriorityWait += waitTime;
            System.out.println("Processed Case: " + c.getCaseId() + " | Priority: " + c.getSeverityPriority() + " | Wait Time: " + waitTime);
            currentTime += 2;
        }
        System.out.println("Average Triage-Priority Wait Time: " + (totalPriorityWait / cases.size()) + " units");
        System.out.println("\n=== Comparison Complete ===");
    }

    public static void main(String[] args) {
        runComparison();
    }
}