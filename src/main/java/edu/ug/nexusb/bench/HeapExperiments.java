package edu.ug.nexusb.bench;

import edu.ug.nexusb.linear.BinaryHeapPriorityQueue;
import edu.ug.nexusb.linear.MyPriorityQueue;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Heap priority dispatch experiment: {@code BinaryHeapPriorityQueue}
 * insert and extract operation time as request volume grows from 100 to
 * 20,000 -- the brief's Section 9 minimum for this specific experiment
 * ("Heap priority dispatch: 100 to 20,000 requests, insert/extract
 * operation time"), which the team's own Week 4 task breakdown (T070-T073)
 * substituted a different comparison for (T072, triage-priority vs. FCFS
 * outcomes) rather than dropping outright -- this fills that gap directly,
 * measuring the raw structure rather than the policy built on top of it.
 *
 * <p>Uses {@link DatabaseBenchmark} (T042's real measurement methodology),
 * matching every other experiment in this project.
 */
public final class HeapExperiments {

    /** docs/parameters.md, Parameter B -- this team's index-derived generation seed. */
    private static final long GENERATION_SEED = 79731L;

    private static final int TRIALS = 5;

    private HeapExperiments() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Heap Experiments (heap priority dispatch)...");

        int[] sizes = {100, 500, 1000, 5000, 10000, 15000, 20000};

        try (FileWriter csv = new FileWriter("results/csv/heap_experiments.csv")) {
            csv.write("Series,N,AverageTimeNs\n");

            for (int n : sizes) {
                System.out.println("N=" + n);
                measureInsert(csv, n);
                measureExtract(csv, n);
            }
        }

        Charts.render(new Charts.Config(
                "results/csv/heap_experiments.csv", 0, 1, 2, true, true,
                "results/graphs/heap_insert_extract.svg",
                "Heap Priority Dispatch: Insert vs. Extract Time vs. N",
                "Requests (N)", "Time"));

        System.out.println("Experiments completed. CSV + chart written.");
    }

    private static void measureInsert(FileWriter csv, int n) throws IOException {
        long[] data = randomData(n, GENERATION_SEED + n);
        DatabaseBenchmark benchmark = new DatabaseBenchmark();
        benchmark.setAlgorithm(() -> {
            MyPriorityQueue<Long> heap = new BinaryHeapPriorityQueue<>(Long::compareTo);
            for (long value : data) {
                heap.insert(value);
            }
        });
        Benchmark.BenchmarkResult result = benchmark.measure("Insert", n, TRIALS);
        csv.write("Insert," + n + "," + result.getTimeNs() + "\n");
    }

    // heapify() builds a fresh heap in O(n) each repetition (the "generate
    // fresh input every invocation" rule DatabaseBenchmark's Javadoc
    // requires for order-sensitive operations -- a drained heap can't be
    // drained a second time), so this measures heapify-then-fully-drain,
    // dominated by the O(n log n) extraction cost for any N worth reporting.
    private static void measureExtract(FileWriter csv, int n) throws IOException {
        Long[] data = boxed(randomData(n, GENERATION_SEED + n + 1));
        DatabaseBenchmark benchmark = new DatabaseBenchmark();
        benchmark.setAlgorithm(() -> {
            MyPriorityQueue<Long> heap = new BinaryHeapPriorityQueue<>(Long::compareTo);
            heap.heapify(data.clone());
            while (!heap.isEmpty()) {
                heap.extractTop();
            }
        });
        Benchmark.BenchmarkResult result = benchmark.measure("Extract", n, TRIALS);
        csv.write("Extract," + n + "," + result.getTimeNs() + "\n");
    }

    private static long[] randomData(int n, long seed) {
        Random rng = new Random(seed);
        long[] data = new long[n];
        for (int i = 0; i < n; i++) {
            data[i] = rng.nextLong();
        }
        return data;
    }

    private static Long[] boxed(long[] values) {
        Long[] boxed = new Long[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return boxed;
    }
}
