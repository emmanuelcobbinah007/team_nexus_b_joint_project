package edu.ug.nexusb.bench;

import edu.ug.nexusb.bench.TriageComparison.ComparisonResult;
import edu.ug.nexusb.bench.TriageComparison.TriageCase;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * T072: triage-priority vs. first-come-first-served outcomes, at scale.
 * Generates deterministic (seeded) synthetic case lists of growing size,
 * with random arrival times and random severity (1-4), and runs both
 * dispatch modes via {@link TriageComparison#compare} (the same method
 * fixed for T054's FCFS-ordering bug) to see how the gap between the two
 * policies' average wait time grows as case volume grows.
 */
public final class TriageExperiments {

    /** docs/parameters.md, Parameter B -- this team's index-derived generation seed. */
    private static final long GENERATION_SEED = 79731L;

    private TriageExperiments() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Triage Experiments (T072)...");

        int[] caseCounts = {10, 20, 50, 100, 200, 400, 800};

        try (FileWriter csv = new FileWriter("results/csv/triage_experiments.csv")) {
            csv.write("Series,N,AverageWait\n");

            for (int n : caseCounts) {
                List<TriageCase> cases = generateCases(n, GENERATION_SEED + n);
                ComparisonResult result = TriageComparison.compare(cases);

                System.out.println("N=" + n + " fcfs=" + result.fcfsAverageWait
                        + " priority=" + result.priorityAverageWait);

                csv.write("FCFS," + n + "," + result.fcfsAverageWait + "\n");
                csv.write("TriagePriority," + n + "," + result.priorityAverageWait + "\n");
            }
        }

        Charts.render(new Charts.Config(
                "results/csv/triage_experiments.csv", 0, 1, 2, true, false,
                "results/graphs/triage_average_wait.svg",
                "T072: FCFS vs. Triage-Priority Average Wait Time vs. Case Volume",
                "Number of cases (N)", "Average wait time (units)"));

        System.out.println("Experiments completed. CSV + chart written.");
    }

    private static List<TriageCase> generateCases(int n, long seed) {
        Random rng = new Random(seed);
        List<TriageCase> cases = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int arrivalTime = rng.nextInt(n * 2);
            int severity = 1 + rng.nextInt(4); // 1 (critical) .. 4 (routine)
            cases.add(new TriageCase("CASE" + i, arrivalTime, severity));
        }
        return cases;
    }
}
