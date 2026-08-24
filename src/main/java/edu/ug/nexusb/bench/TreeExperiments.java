package edu.ug.nexusb.bench;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.trees.BinarySearchTree;
import edu.ug.nexusb.trees.MyTree;
import edu.ug.nexusb.trees.RedBlackTree;

import java.io.FileWriter;
import java.io.IOException;

/**
 * T071: plain BST vs. Red-Black tree on sorted input -- the exact case
 * {@code docs/proofs/proof_bst_height.md} argues degenerates a BST to
 * {@code height = n-1} while the RB tree stays {@code O(log n)}. This
 * measures both the structural claim (actual {@code height()} after
 * inserting {@code 0..n-1} in order) and its practical consequence
 * (insertion time, which degrades with height since every insert walks
 * root-to-leaf).
 */
public final class TreeExperiments {

    private static final MyComparator<Integer> NATURAL_ORDER = Integer::compareTo;
    private static final int TRIALS = 5;

    private TreeExperiments() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Tree Experiments (T071)...");

        int[] sizes = {50, 100, 200, 400, 800, 1600, 3200};

        try (FileWriter csv = new FileWriter("results/csv/tree_experiments.csv")) {
            csv.write("Series,N,Height,AverageInsertAllTimeNs\n");

            for (int n : sizes) {
                System.out.println("N=" + n);
                measureOne(csv, "BinarySearchTree", n, BinarySearchTree::new);
                measureOne(csv, "RedBlackTree", n, RedBlackTree::new);
            }
        }

        Charts.render(new Charts.Config(
                "results/csv/tree_experiments.csv", 0, 1, 2, true, false,
                "results/graphs/tree_height.svg",
                "T071: BST vs. Red-Black Tree Height on Sorted Input",
                "N (sorted keys inserted)", "Tree height"));

        Charts.render(new Charts.Config(
                "results/csv/tree_experiments.csv", 0, 1, 3, true, true,
                "results/graphs/tree_insert_time.svg",
                "T071: BST vs. Red-Black Tree Insertion Time on Sorted Input",
                "N (sorted keys inserted)", "Total insertion time"));

        System.out.println("Experiments completed. CSV + charts written.");
    }

    private interface TreeFactory {
        MyTree<Integer, String> create(MyComparator<? super Integer> comparator);
    }

    private static void measureOne(FileWriter csv, String seriesName, int n, TreeFactory factory)
            throws IOException {
        // height() is read from one real build, not the benchmark's own
        // internal state -- DatabaseBenchmark's Runnable returns nothing.
        MyTree<Integer, String> reference = factory.create(NATURAL_ORDER);
        for (int i = 0; i < n; i++) {
            reference.put(i, "v" + i);
        }
        int height = reference.height();

        DatabaseBenchmark benchmark = new DatabaseBenchmark();
        benchmark.setAlgorithm(() -> {
            MyTree<Integer, String> tree = factory.create(NATURAL_ORDER);
            for (int i = 0; i < n; i++) {
                tree.put(i, "v" + i);
            }
        });
        Benchmark.BenchmarkResult result = benchmark.measure(seriesName, n, TRIALS);

        csv.write(seriesName + "," + n + "," + height + "," + result.getTimeNs() + "\n");
    }
}
