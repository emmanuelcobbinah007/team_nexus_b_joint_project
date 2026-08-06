package edu.ug.nexusb.interfaces;

/**
 * Interface representing the empirical benchmarking framework for measuring 
 * algorithm execution time (nanoseconds) and memory usage (kilobytes).
 * Feeds into Task T042 (Benchmark framework implementation and database recording).
 * 
 * @author Johnson Kuzagbe (Sub-group E Leader)
 */
public interface Benchmark {

    /**
     * Executes a given algorithm task across a specified input size, runs multiple trials,
     * and records performance metrics to be exported to CSV or stored in the database.
     * 
     * @param algorithmName the name of the algorithm being tested (e.g., "MergeSort", "Dijkstra")
     * @param inputSize the size of the dataset or input structure ($n$)
     * @param trialCount the number of repeated trials to average out JVM warm-up anomalies
     * @return a BenchmarkResult object containing average runtime in nanoseconds and memory consumed in KB
     */
    BenchmarkResult measure(String algorithmName, int inputSize, int trialCount);

    /**
     * Nested record or data holder class representing the performance metrics of a single benchmark run.
     */
    class BenchmarkResult {
        private final String algorithmName;
        private final int inputSize;
        private final long timeNs;
        private final long memoryKb;
        private final long timestamp;

        public BenchmarkResult(String algorithmName, int inputSize, long timeNs, long memoryKb) {
            this.algorithmName = algorithmName;
            this.inputSize = inputSize;
            this.timeNs = timeNs;
            this.memoryKb = memoryKb;
            this.timestamp = System.currentTimeMillis();
        }

        public String getAlgorithmName() { return algorithmName; }
        public int getInputSize() { return inputSize; }
        public long getTimeNs() { return timeNs; }
        public long getMemoryKb() { return memoryKb; }
        public long getTimestamp() { return timestamp; }
    }
}