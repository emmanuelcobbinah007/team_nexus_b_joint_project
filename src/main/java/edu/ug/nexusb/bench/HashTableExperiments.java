package edu.ug.nexusb.bench;

import edu.ug.nexusb.trees.ChainedHashTable;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * T070: hash table load factor vs. collisions. Inserts a growing number of
 * keys into one {@link ChainedHashTable} (default constructor, real
 * auto-resize behavior -- capacity {@code INITIAL_TABLE_SIZE} doubling
 * whenever load factor crosses 0.75), recording how collision count,
 * longest-bucket length, and average lookup time change as the table grows
 * through several resize events.
 *
 * <p>Keys are random (seeded), not sequential {@code 0..N-1}: sequential
 * integer keys are collision-free against any table size by the pigeonhole
 * principle as long as {@code N <= capacity} (which the resize policy
 * always keeps true), so they'd measure nothing about hash quality --
 * random keys are also the more realistic case (real identifiers aren't
 * guaranteed contiguous).
 */
public final class HashTableExperiments {

    /** docs/parameters.md, Parameter B -- this team's index-derived generation seed. */
    private static final long GENERATION_SEED = 79731L;

    private static final int TRIALS = 7;

    private HashTableExperiments() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Hash Table Experiments (T070)...");

        // 100 to 20,000, per the brief's minimum for this experiment
        // (Section 9): "Hash table load factor: 100 to 20,000 keys with
        // different table sizes". The auto-resize policy means the table's
        // capacity genuinely differs across this range (53 at the low end,
        // tens of thousands by the top), rather than needing separate runs
        // at separately-fixed capacities.
        int[] keyCounts = {100, 300, 800, 1500, 3000, 5000, 8000, 12000, 16000, 20000};

        try (FileWriter csv = new FileWriter("results/csv/hashtable_experiments.csv")) {
            csv.write("Series,N,LoadFactor,Capacity,CollisionCount,LongestBucket,ResizeCount,AverageGetTimeNs\n");

            ChainedHashTable<Integer, String> table = new ChainedHashTable<>();
            Random rng = new Random(GENERATION_SEED);
            int inserted = 0;
            int lastKey = 0;

            for (int target : keyCounts) {
                while (inserted < target) {
                    lastKey = rng.nextInt();
                    table.put(lastKey, "value-" + inserted);
                    inserted++;
                }

                long getTimeNs = measureAverageGet(table, lastKey, target);

                System.out.println("N=" + target + " loadFactor=" + table.loadFactor()
                        + " capacity=" + table.capacity() + " collisions=" + table.collisionCount()
                        + " longestBucket=" + table.longestBucket() + " resizes=" + table.resizeCount());

                csv.write("ChainedHashTable," + target + "," + table.loadFactor() + "," + table.capacity() + ","
                        + table.collisionCount() + "," + table.longestBucket() + "," + table.resizeCount() + ","
                        + getTimeNs + "\n");
            }
        }

        Charts.render(new Charts.Config(
                "results/csv/hashtable_experiments.csv", 0, 1, 4, false, false,
                "results/graphs/hashtable_collisions.svg",
                "T070: Hash Table Collision Count vs. Number of Keys Inserted",
                "Keys inserted (N)", "Collision count"));

        Charts.render(new Charts.Config(
                "results/csv/hashtable_experiments.csv", 0, 1, 7, false, true,
                "results/graphs/hashtable_get_time.svg",
                "T070: Hash Table Average get() Time vs. Number of Keys Inserted",
                "Keys inserted (N)", "Average get() time"));

        System.out.println("Experiments completed. CSV + charts written.");
    }

    private static long measureAverageGet(ChainedHashTable<Integer, String> table, int probeKey, int currentSize) {
        DatabaseBenchmark benchmark = new DatabaseBenchmark();
        benchmark.setAlgorithm(() -> table.get(probeKey));
        Benchmark.BenchmarkResult result = benchmark.measure("ChainedHashTable_get", currentSize, TRIALS);
        return result.getTimeNs();
    }
}
