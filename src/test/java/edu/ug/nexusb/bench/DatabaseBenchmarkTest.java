// File Path: src/test/java/edu/ug/nexusb/bench/DatabaseBenchmarkTest.java

package edu.ug.nexusb.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import edu.ug.nexusb.bench.Benchmark.BenchmarkResult;

class DatabaseBenchmarkTest {

    @Test
    void testMeasureAndDatabaseWrite() {
        DatabaseBenchmark benchmark = new DatabaseBenchmark();
        
        // Simulating a dummy sorting workload for the test
        benchmark.setAlgorithm(() -> {
            int[] temp = new int[1000];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = temp.length - i;
            }
            java.util.Arrays.sort(temp);
        });

        // Test running 5 trials on an input size of 1000
        BenchmarkResult result = benchmark.measure("DummySortTest", 1000, 5);

        assertNotNull(result);
        assertEquals("DummySortTest", result.getAlgorithmName());
        assertEquals(1000, result.getInputSize());
        assertTrue(result.getTimeNs() > 0, "Average time should be greater than 0 nanoseconds");
    }
    
}