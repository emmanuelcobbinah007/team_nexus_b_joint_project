package edu.ug.nexusb.bench;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.ug.nexusb.bench.Benchmark.BenchmarkResult;

/**
 * Early experiments for benchmarking search and sorting algorithms.
 * Generates CSV files with performance metrics for analysis.
 * 
 * Note: This class provides basic sorting implementations inline.
 * Production implementations should use dedicated Sorter interface classes.
 */
public class EarlyExperiments {

    public static void main(String[] args) {
        int[] sizes = {100, 500, 1000, 5000, 10000};
        int trials = 3; // Minimum runs per size required by the brief
        SimpleBenchmark bench = new SimpleBenchmark();

        System.out.println("Starting Early Experiments (T064)...");

        try (FileWriter searchWriter = new FileWriter("search_comparison.csv");
             FileWriter sortWriter = new FileWriter("sorting_comparison.csv")) {

            searchWriter.write("Algorithm,InputSize,AverageTimeNs\n");
            sortWriter.write("Algorithm,InputSize,AverageTimeNs\n");

            for (int size : sizes) {
                System.out.println("Processing size n=" + size);

                Integer[] arr = generateRandomArray(size);
                Integer[] sortedArr = generateSortedArray(size);
                Integer target = sortedArr[size / 2]; // Target in the middle

                // --- 1. SEARCH EXPERIMENTS ---
                BenchmarkResult lsResult = bench.measureSearch("LinearSearch", sortedArr, target, trials);
                searchWriter.write("LinearSearch," + size + "," + lsResult.getTimeNs() + "\n");

                BenchmarkResult bsResult = bench.measureSearch("BinarySearch", sortedArr, target, trials);
                searchWriter.write("BinarySearch," + size + "," + bsResult.getTimeNs() + "\n");

                // --- 2. SORT EXPERIMENTS ---
                Integer[] arrClone1 = arr.clone();
                BenchmarkResult ssResult = bench.measureSort("SelectionSort", arrClone1, trials);
                sortWriter.write("SelectionSort," + size + "," + ssResult.getTimeNs() + "\n");

                Integer[] arrClone2 = arr.clone();
                BenchmarkResult isResult = bench.measureSort("InsertionSort", arrClone2, trials);
                sortWriter.write("InsertionSort," + size + "," + isResult.getTimeNs() + "\n");
            }

            System.out.println("Experiments completed. CSVs generated in the project root.");

        } catch (IOException e) {
            System.err.println("File write error: " + e.getMessage());
        }
    }

    private static Integer[] generateRandomArray(int size) {
        Integer[] arr = new Integer[size];
        // Custom random generation to avoid java.util.Random import issues if strict
        long seed = System.currentTimeMillis(); 
        for (int i = 0; i < size; i++) {
            seed = (seed * 25214903917L + 11) & ((1L << 48) - 1);
            arr[i] = (int) (seed % 100000);
        }
        return arr;
    }

    private static Integer[] generateSortedArray(int size) {
        Integer[] arr = new Integer[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i * 2;
        }
        return arr;
    }

    private static int linearSearch(Integer[] arr, Integer target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(target)) return i;
        }
        return -1;
    }

    private static int binarySearch(Integer[] arr, Integer target) {
        int left = 0, right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int comparison = arr[mid].compareTo(target);
            if (comparison == 0) return mid;
            if (comparison < 0) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }

    /**
     * Simple in-place selection sort implementation for benchmarking.
     */
    private static void selectionSort(Integer[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < array.length; j++) {
                if (array[j] < array[minIdx]) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                int temp = array[i];
                array[i] = array[minIdx];
                array[minIdx] = temp;
            }
        }
    }

    /**
     * Simple in-place insertion sort implementation for benchmarking.
     */
    private static void insertionSort(Integer[] array) {
        for (int i = 1; i < array.length; i++) {
            int key = array[i];
            int j = i - 1;
            while (j >= 0 && array[j] > key) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
    }

    /**
     * Simple benchmark implementation for search and sort algorithms.
     */
    static class SimpleBenchmark implements Benchmark {
        private final List<Long> measurements;

        public SimpleBenchmark() {
            this.measurements = new ArrayList<>();
        }

        @Override
        public BenchmarkResult measure(String algorithmName, int inputSize, int trialCount) {
            // This is not used in the new design
            throw new UnsupportedOperationException("Use measureSort or measureSearch instead");
        }

        public BenchmarkResult measureSearch(String algorithmName, Integer[] array, Integer target, int trials) {
            measurements.clear();
            long totalTimeNs = 0;

            for (int t = 0; t < trials; t++) {
                long startTime = System.nanoTime();
                if ("LinearSearch".equals(algorithmName)) {
                    linearSearch(array, target);
                } else if ("BinarySearch".equals(algorithmName)) {
                    binarySearch(array, target);
                }
                long endTime = System.nanoTime();
                long elapsed = endTime - startTime;
                measurements.add(elapsed);
                totalTimeNs += elapsed;
            }

            long avgTime = totalTimeNs / trials;
            return new BenchmarkResult(algorithmName, array.length, avgTime, 0);
        }

        public BenchmarkResult measureSort(String algorithmName, Integer[] array, int trials) {
            measurements.clear();
            long totalTimeNs = 0;

            for (int t = 0; t < trials; t++) {
                Integer[] arrCopy = array.clone();
                long startTime = System.nanoTime();
                if ("SelectionSort".equals(algorithmName)) {
                    selectionSort(arrCopy);
                } else if ("InsertionSort".equals(algorithmName)) {
                    insertionSort(arrCopy);
                }
                long endTime = System.nanoTime();
                long elapsed = endTime - startTime;
                measurements.add(elapsed);
                totalTimeNs += elapsed;
            }

            long avgTime = totalTimeNs / trials;
            return new BenchmarkResult(algorithmName, array.length, avgTime, 0);
        }
    }
}