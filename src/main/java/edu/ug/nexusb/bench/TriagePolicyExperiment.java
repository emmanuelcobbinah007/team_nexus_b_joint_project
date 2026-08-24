package edu.ug.nexusb.bench;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.linear.ArrayQueue;
import edu.ug.nexusb.linear.BinaryHeapPriorityQueue;
import edu.ug.nexusb.linear.MyPriorityQueue;
import edu.ug.nexusb.linear.MyQueue;

/**
 * T072: Experiment comparing FCFS vs triage-priority dispatch.
 *
 * Real time-ordered discrete-event simulation: a case only becomes visible
 * to either policy once it has actually arrived. Priority dispatch only
 * reorders among cases that are CURRENTLY waiting - never future ones.
 * (An earlier version of this file did full-batch lookahead, which let
 * priority dispatch "wait" for future emergencies while ignoring cases
 * already sitting in the room - that produced physically nonsensical
 * multi-day average waits. Fixed here.)
 *
 * Built on the team's own structures - BinaryHeapPriorityQueue for
 * triage-priority, ArrayQueue for FCFS - not java.util's built-in versions.
 *
 * Runs several request volumes, 3 times each with different random seeds,
 * averages the results, and writes:
 *   results/csv/triage_policy_comparison.csv   (every individual run)
 *   results/csv/triage_policy_comparison_avg.csv (averaged per volume+policy)
 *   results/graphs/triage_policy_comparison.svg (hand-drawn line chart, no
 *   charting library - just math and text)
 *
 * NOTE: uses java.util.Random for generating synthetic test cases only.
 * This is not one of the graded data structures - it's the same category
 * as DBLoader.java's existing use of java.util.HashMap for plumbing.
 */
public class TriagePolicyExperiment {

    private static final int[] VOLUMES = {50, 100, 200, 500};
    private static final int RUNS_PER_VOLUME = 3;

    // 5 dispatch servers working in parallel (ambulances / response teams),
    // matching the rough scale of resource.csv. Flag this assumption to the
    // team if 5 isn't realistic for the report.
    private static final int NUM_SERVERS = 5;

    static class SimCase {
        final int id;
        final int triageLevel;
        final int arrivalMin;
        final int responseWindowMin;
        final int serviceTimeMin;

        SimCase(int id, int triageLevel, int arrivalMin, int responseWindowMin, int serviceTimeMin) {
            this.id = id;
            this.triageLevel = triageLevel;
            this.arrivalMin = arrivalMin;
            this.responseWindowMin = responseWindowMin;
            this.serviceTimeMin = serviceTimeMin;
        }
    }

    static class SimCaseComparator implements MyComparator<SimCase> {
        @Override
        public int compare(SimCase a, SimCase b) {
            if (a.triageLevel != b.triageLevel) {
                return a.triageLevel - b.triageLevel;
            }
            return a.arrivalMin - b.arrivalMin;
        }
    }

    static class RunResult {
        int missedCount;
        double missedRate;
        double avgWaitCriticalMin;
        double avgWaitOverallMin;
    }

    // A "waiting room" abstraction so the same event-driven loop below can
    // drive either policy's structure without caring which one it is.
    private interface WaitingRoom {
        void add(SimCase c);
        SimCase takeNext();
        boolean isEmpty();
    }

    public static void main(String[] args) throws IOException {
        run();
    }

    public static void run() throws IOException {
        System.out.println("=== T072: FCFS vs Triage-Priority Dispatch Experiment ===");

        StringBuilder rawCsv = new StringBuilder();
        rawCsv.append("volume,policy,run,missed_count,missed_rate,avg_wait_critical_min,avg_wait_overall_min\n");

        RunResult[][] averaged = new RunResult[VOLUMES.length][2]; // [0]=FCFS [1]=PRIORITY

        for (int v = 0; v < VOLUMES.length; v++) {
            int volume = VOLUMES[v];
            RunResult[] fcfsRuns = new RunResult[RUNS_PER_VOLUME];
            RunResult[] priorityRuns = new RunResult[RUNS_PER_VOLUME];

            for (int run = 0; run < RUNS_PER_VOLUME; run++) {
                long seed = (long) volume * 1000 + run;
                SimCase[] casesForFcfs = generateCases(volume, seed);
                SimCase[] casesForPriority = generateCases(volume, seed);

                fcfsRuns[run] = simulate(casesForFcfs, fcfsWaitingRoom());
                priorityRuns[run] = simulate(casesForPriority, priorityWaitingRoom());

                appendRunRow(rawCsv, volume, "FCFS", run + 1, fcfsRuns[run]);
                appendRunRow(rawCsv, volume, "TRIAGE_PRIORITY", run + 1, priorityRuns[run]);

                System.out.println("volume=" + volume + " run=" + (run + 1)
                        + " FCFS missed=" + fcfsRuns[run].missedCount
                        + " PRIORITY missed=" + priorityRuns[run].missedCount);
            }

            averaged[v][0] = average(fcfsRuns);
            averaged[v][1] = average(priorityRuns);
        }

        writeCsv("results/csv/triage_policy_comparison.csv", rawCsv.toString());
        writeAveragedCsv(averaged);
        writeSvgChart(averaged);

        System.out.println("Done. See results/csv/ and results/graphs/.");
    }

    // ---- case generation (arrival-time sorted by construction) ----

    private static SimCase[] generateCases(int volume, long seed) {
        Random random = new Random(seed);
        SimCase[] cases = new SimCase[volume];
        // Tuned so the system runs at roughly 80% capacity (5 servers,
        // ~35 min average service time -> capacity is roughly one case
        // every 7 minutes; arriving every 9 minutes keeps it busy enough
        // for real queueing to happen without total overload). At the
        // earlier avgGapMin=12 (~58% utilization), a server was almost
        // always free the instant a case arrived, so FCFS and
        // TRIAGE_PRIORITY never actually had to choose between two
        // waiting cases - which is why their results came out identical.
        int avgGapMin = 9;

        for (int i = 0; i < volume; i++) {
            int triageLevel = 1 + random.nextInt(5);
            int arrivalMin = i * avgGapMin + random.nextInt(avgGapMin);

            int responseWindowMin;
            int serviceTimeMin;
            switch (triageLevel) {
                case 1 -> { responseWindowMin = 15; serviceTimeMin = 40 + random.nextInt(50); }
                case 2 -> { responseWindowMin = 30; serviceTimeMin = 20 + random.nextInt(40); }
                case 3 -> { responseWindowMin = 90; serviceTimeMin = 15 + random.nextInt(30); }
                default -> { responseWindowMin = 240; serviceTimeMin = 10 + random.nextInt(20); }
            }

            cases[i] = new SimCase(i, triageLevel, arrivalMin, responseWindowMin, serviceTimeMin);
        }
        return cases;
    }

    // ---- waiting room adapters ----

    private static WaitingRoom fcfsWaitingRoom() {
        MyQueue<SimCase> queue = new ArrayQueue<>();
        return new WaitingRoom() {
            @Override public void add(SimCase c) { queue.enqueue(c); }
            @Override public SimCase takeNext() { return queue.dequeue(); }
            @Override public boolean isEmpty() { return queue.isEmpty(); }
        };
    }

    private static WaitingRoom priorityWaitingRoom() {
        MyPriorityQueue<SimCase> heap = new BinaryHeapPriorityQueue<>(new SimCaseComparator());
        return new WaitingRoom() {
            @Override public void add(SimCase c) { heap.insert(c); }
            @Override public SimCase takeNext() { return heap.extractTop(); }
            @Override public boolean isEmpty() { return heap.isEmpty(); }
        };
    }

    // ---- the real event-driven simulation ----

    // cases[] must already be sorted by arrivalMin ascending (true by
    // construction in generateCases). At each step: if a server is free and
    // someone is already waiting, dispatch the highest-priority (or
    // earliest, for FCFS) case from the waiting room. Otherwise, admit the
    // next case that has actually arrived. A case is NEVER visible to the
    // dispatcher before its own arrivalMin - this is what makes it a fair,
    // realistic comparison instead of the earlier full-lookahead bug.
    private static RunResult simulate(SimCase[] cases, WaitingRoom room) {
        long[] serverFreeAt = new long[NUM_SERVERS];
        int nextArrivalIndex = 0;

        int missedCount = 0;
        long totalWaitAll = 0;
        long totalWaitCritical = 0;
        int criticalCount = 0;

        while (nextArrivalIndex < cases.length || !room.isEmpty()) {
            long nextArrivalTime = nextArrivalIndex < cases.length
                    ? cases[nextArrivalIndex].arrivalMin : Long.MAX_VALUE;
            long earliestServerFree = min(serverFreeAt);

            boolean canDispatchNow = !room.isEmpty() && earliestServerFree <= nextArrivalTime;

            if (canDispatchNow) {
                int serverIndex = indexOfMin(serverFreeAt);
                SimCase c = room.takeNext();

                long startTime = Math.max(serverFreeAt[serverIndex], c.arrivalMin);
                int waitMin = (int) Math.max(0, startTime - c.arrivalMin);

                if (waitMin > c.responseWindowMin) {
                    missedCount++;
                }
                totalWaitAll += waitMin;
                if (c.triageLevel == 1) {
                    totalWaitCritical += waitMin;
                    criticalCount++;
                }

                serverFreeAt[serverIndex] = startTime + c.serviceTimeMin;
            } else if (nextArrivalIndex < cases.length) {
                room.add(cases[nextArrivalIndex]);
                nextArrivalIndex++;
            } else {
                break; // nothing left to admit and nothing dispatchable - shouldn't happen
            }
        }

        RunResult result = new RunResult();
        result.missedCount = missedCount;
        result.missedRate = (double) missedCount / cases.length;
        result.avgWaitOverallMin = (double) totalWaitAll / cases.length;
        result.avgWaitCriticalMin = criticalCount == 0 ? 0.0 : (double) totalWaitCritical / criticalCount;
        return result;
    }

    private static long min(long[] values) {
        long m = values[0];
        for (long v : values) {
            if (v < m) m = v;
        }
        return m;
    }

    private static int indexOfMin(long[] values) {
        int idx = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[idx]) idx = i;
        }
        return idx;
    }

    // ---- averaging ----

    private static RunResult average(RunResult[] runs) {
        RunResult avg = new RunResult();
        double missedCountSum = 0, missedRateSum = 0, waitCriticalSum = 0, waitOverallSum = 0;
        for (RunResult r : runs) {
            missedCountSum += r.missedCount;
            missedRateSum += r.missedRate;
            waitCriticalSum += r.avgWaitCriticalMin;
            waitOverallSum += r.avgWaitOverallMin;
        }
        int n = runs.length;
        avg.missedCount = (int) Math.round(missedCountSum / n);
        avg.missedRate = missedRateSum / n;
        avg.avgWaitCriticalMin = waitCriticalSum / n;
        avg.avgWaitOverallMin = waitOverallSum / n;
        return avg;
    }

    // ---- output ----

    private static void appendRunRow(StringBuilder sb, int volume, String policy, int run, RunResult r) {
        sb.append(volume).append(',')
          .append(policy).append(',')
          .append(run).append(',')
          .append(r.missedCount).append(',')
          .append(String.format("%.4f", r.missedRate)).append(',')
          .append(String.format("%.2f", r.avgWaitCriticalMin)).append(',')
          .append(String.format("%.2f", r.avgWaitOverallMin)).append('\n');
    }

    private static void writeAveragedCsv(RunResult[][] averaged) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("volume,policy,avg_missed_rate,avg_wait_critical_min,avg_wait_overall_min\n");
        for (int v = 0; v < VOLUMES.length; v++) {
            appendAvgRow(sb, VOLUMES[v], "FCFS", averaged[v][0]);
            appendAvgRow(sb, VOLUMES[v], "TRIAGE_PRIORITY", averaged[v][1]);
        }
        writeCsv("results/csv/triage_policy_comparison_avg.csv", sb.toString());
    }

    private static void appendAvgRow(StringBuilder sb, int volume, String policy, RunResult r) {
        sb.append(volume).append(',')
          .append(policy).append(',')
          .append(String.format("%.4f", r.missedRate)).append(',')
          .append(String.format("%.2f", r.avgWaitCriticalMin)).append(',')
          .append(String.format("%.2f", r.avgWaitOverallMin)).append('\n');
    }

    private static void writeCsv(String path, String content) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
        }
        System.out.println("Wrote " + path);
    }

    private static void writeSvgChart(RunResult[][] averaged) throws IOException {
        int width = 640, height = 400, padding = 60;
        double maxWait = 0;
        for (RunResult[] pair : averaged) {
            maxWait = Math.max(maxWait, Math.max(pair[0].avgWaitCriticalMin, pair[1].avgWaitCriticalMin));
        }
        if (maxWait == 0) maxWait = 1;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
           .append(width).append(' ').append(height).append("\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n");
        svg.append("<text x=\"").append(width / 2).append("\" y=\"20\" text-anchor=\"middle\" font-size=\"14\">")
           .append("Average wait for critical cases (triage level 1) vs request volume</text>\n");

        svg.append("<line x1=\"").append(padding).append("\" y1=\"").append(height - padding)
           .append("\" x2=\"").append(width - padding).append("\" y2=\"").append(height - padding)
           .append("\" stroke=\"black\"/>\n");
        svg.append("<line x1=\"").append(padding).append("\" y1=\"").append(padding)
           .append("\" x2=\"").append(padding).append("\" y2=\"").append(height - padding)
           .append("\" stroke=\"black\"/>\n");

        appendSvgLine(svg, averaged, 0, "red", maxWait, width, height, padding);
        appendSvgLine(svg, averaged, 1, "blue", maxWait, width, height, padding);

        for (int v = 0; v < VOLUMES.length; v++) {
            int x = xForIndex(v, width, padding);
            svg.append("<text x=\"").append(x).append("\" y=\"").append(height - padding + 20)
               .append("\" text-anchor=\"middle\" font-size=\"11\">").append(VOLUMES[v]).append("</text>\n");
        }

        svg.append("<circle cx=\"").append(width - 150).append("\" cy=\"40\" r=\"5\" fill=\"red\"/>\n");
        svg.append("<text x=\"").append(width - 140).append("\" y=\"44\" font-size=\"12\">FCFS</text>\n");
        svg.append("<circle cx=\"").append(width - 150).append("\" cy=\"58\" r=\"5\" fill=\"blue\"/>\n");
        svg.append("<text x=\"").append(width - 140).append("\" y=\"62\" font-size=\"12\">Triage-Priority</text>\n");

        svg.append("</svg>\n");

        try (FileWriter writer = new FileWriter("results/graphs/triage_policy_comparison.svg")) {
            writer.write(svg.toString());
        }
        System.out.println("Wrote results/graphs/triage_policy_comparison.svg");
    }

    private static void appendSvgLine(StringBuilder svg, RunResult[][] averaged, int policyIndex,
                                       String color, double maxWait, int width, int height, int padding) {
        svg.append("<polyline fill=\"none\" stroke=\"").append(color).append("\" stroke-width=\"2\" points=\"");
        for (int v = 0; v < VOLUMES.length; v++) {
            int x = xForIndex(v, width, padding);
            double wait = averaged[v][policyIndex].avgWaitCriticalMin;
            int y = (int) (height - padding - (wait / maxWait) * (height - 2 * padding));
            svg.append(x).append(',').append(y).append(' ');
        }
        svg.append("\"/>\n");
    }

    private static int xForIndex(int index, int width, int padding) {
        int usableWidth = width - 2 * padding;
        return padding + (usableWidth * index) / (VOLUMES.length - 1);
    }
}