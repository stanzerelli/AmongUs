import data.Image;
import solutions.ParallelFinderGlobalHashMap;
import solutions.ParallelFinderSubtotals;
import solutions.SequentialFinder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Main class to run benchmarks for the different implementations
 */
public class BenchmarkMain {
    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASUREMENT_ITERATIONS = 5;
    private static final String RESULT_FILE = "benchmark_results.txt";  // Output file for results
    private static final long RUN_TIME_MS = 3 * 60 * 60 * 1000L;  // 3 hours in milliseconds

    public static void main(String[] args) {
        // List of image files to benchmark
        String[] imageFiles = {
                "images/place_2k_2k.png",
                "images/place_20k_2k.png",
                "images/place_20k_20k.png",
                "images/place_20k_20k_discoloured.png",
                "images/place_23k_23k.png"
        };

        // Try with resources for file writing
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESULT_FILE))) {
            writer.write("Image, Cores, Avg. Time (ms), Max Time (ms), Min Time (ms), Std Dev (ms), Speedup\n");

            // Start time of the benchmark
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;

            // Repeat the benchmark until the total time exceeds 3 hours
            while (elapsedTime < RUN_TIME_MS) {
                // Benchmark each image file
                for (String imageFile : imageFiles) {
                    Image img = new Image(imageFile);
                    System.out.println("Image: " + imageFile);
                    System.out.println("Image dimensions: " + img.width + "x" + img.height);

                    // Benchmark sequential implementation
                    System.out.println("\n=== Sequential Implementation ===");
                    BenchmarkResult seqResult = benchmarkSequential(img);
                    printBenchmarkResult(seqResult);

                    // Benchmark parallel implementation with global hashmap for different numbers of cores
                    System.out.println("\n=== Parallel Implementation with Global HashMap ===");
                    int[] coreCounts = {1, 2, 4, 8, 16, 32, 64, 128};
                    for (int cores : coreCounts) {
                        System.out.println("\nCores: " + cores);
                        BenchmarkResult parallelGlobalResult = benchmarkParallelGlobal(img, cores);
                        printBenchmarkResult(parallelGlobalResult);

                        // Calculate speedup
                        double speedup = seqResult.avgTime / parallelGlobalResult.avgTime;

                        // Write to file in CSV format
                        writer.write(String.format("%s, %d, %.2f, %.2f, %.2f, %.2f, %.2f\n",
                                imageFile, cores,
                                parallelGlobalResult.avgTime / 1_000_000.0,
                                parallelGlobalResult.maxTime / 1_000_000.0,
                                parallelGlobalResult.minTime / 1_000_000.0,
                                parallelGlobalResult.stdDev / 1_000_000.0,
                                speedup));
                    }

                    // Benchmark parallel implementation with subtotals for different numbers of cores
                    // First with sequential Y processing (T = Integer.MAX_VALUE)
                    System.out.println("\n=== Parallel Implementation with Subtotals (Sequential Y) ===");
                    for (int cores : coreCounts) {
                        System.out.println("\nCores: " + cores);
                        BenchmarkResult parallelSubtotalsResult = benchmarkParallelSubtotals(img, cores, Integer.MAX_VALUE);
                        printBenchmarkResult(parallelSubtotalsResult);

                        // Calculate speedup
                        double speedup = seqResult.avgTime / parallelSubtotalsResult.avgTime;

                        // Write to file in CSV format
                        writer.write(String.format("%s, %d, %.2f, %.2f, %.2f, %.2f, %.2f\n",
                                imageFile, cores,
                                parallelSubtotalsResult.avgTime / 1_000_000.0,
                                parallelSubtotalsResult.maxTime / 1_000_000.0,
                                parallelSubtotalsResult.minTime / 1_000_000.0,
                                parallelSubtotalsResult.stdDev / 1_000_000.0,
                                speedup));
                    }

                    // Then benchmark with different thresholds for Y-axis parallelization (Phase 2)
                    System.out.println("\n=== Parallel Implementation with Subtotals (Parallel X and Y) ===");
                    int[] thresholds = {1, 10, 20, 50, 100, 200, 500};
                    for (int threshold : thresholds) {
                        System.out.println("\nCores: " + Runtime.getRuntime().availableProcessors() + ", Threshold: " + threshold);
                        BenchmarkResult parallelSubtotalsResult = benchmarkParallelSubtotals(img, Runtime.getRuntime().availableProcessors(), threshold);
                        printBenchmarkResult(parallelSubtotalsResult);

                        // Calculate speedup
                        double speedup = seqResult.avgTime / parallelSubtotalsResult.avgTime;

                        // Write to file in CSV format
                        writer.write(String.format("%s, %d, %.2f, %.2f, %.2f, %.2f, %.2f\n",
                                imageFile, Runtime.getRuntime().availableProcessors(),
                                parallelSubtotalsResult.avgTime / 1_000_000.0,
                                parallelSubtotalsResult.maxTime / 1_000_000.0,
                                parallelSubtotalsResult.minTime / 1_000_000.0,
                                parallelSubtotalsResult.stdDev / 1_000_000.0,
                                speedup));
                    }
                }

                // Calculate elapsed time
                elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println("Elapsed time: " + (elapsedTime / 1000) + " seconds");

                // Optional: Print status every 10 minutes
                if (elapsedTime % (10 * 60 * 1000) < 1000) {
                    System.out.println("Benchmark still running... " + (elapsedTime / 1000) + " seconds elapsed.");
                }
            }

            System.out.println("\nBenchmark results written to: " + RESULT_FILE);

        } catch (IOException e) {
            System.err.println("Error writing benchmark results to file: " + e.getMessage());
        }
    }

    /**
     * Benchmark the sequential implementation
     */
    private static BenchmarkResult benchmarkSequential(Image img) {
        SequentialFinder finder = new SequentialFinder();

        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            finder.countAmongiByColour(img);
        }

        // Measurement
        long[] times = new long[MEASUREMENT_ITERATIONS];
        AbstractMap<Integer, Integer> result = null;

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            result = finder.countAmongiByColour(img);
            long endTime = System.nanoTime();
            times[i] = endTime - startTime;
        }

        return new BenchmarkResult(times, result);
    }

    /**
     * Benchmark the parallel implementation with global hashmap
     */
    private static BenchmarkResult benchmarkParallelGlobal(Image img, int cores) {
        ParallelFinderGlobalHashMap finder = new ParallelFinderGlobalHashMap(cores);

        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            finder.countAmongiByColour(img);
        }

        // Measurement
        long[] times = new long[MEASUREMENT_ITERATIONS];
        AbstractMap<Integer, Integer> result = null;

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            result = finder.countAmongiByColour(img);
            long endTime = System.nanoTime();
            times[i] = endTime - startTime;
        }

        return new BenchmarkResult(times, result);
    }

    /**
     * Benchmark the parallel implementation with subtotals
     */
    private static BenchmarkResult benchmarkParallelSubtotals(Image img, int cores, int threshold) {
        ParallelFinderSubtotals finder = new ParallelFinderSubtotals(cores, threshold);

        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            finder.countAmongiByColour(img);
        }

        // Measurement
        long[] times = new long[MEASUREMENT_ITERATIONS];
        AbstractMap<Integer, Integer> result = null;

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            result = finder.countAmongiByColour(img);
            long endTime = System.nanoTime();
            times[i] = endTime - startTime;
        }

        return new BenchmarkResult(times, result);
    }

    /**
     * Print the benchmark results
     */
    private static void printBenchmarkResult(BenchmarkResult result) {
        System.out.println("Results: " + result.result);
        System.out.printf("Avg. Time: %.2f ms\n", result.avgTime / 1_000_000.0);
        System.out.printf("Max Time: %.2f ms\n", result.maxTime / 1_000_000.0);
        System.out.printf("Min Time: %.2f ms\n", result.minTime / 1_000_000.0);
        System.out.printf("Std Dev: %.2f ms\n", result.stdDev / 1_000_000.0);
    }

    /**
     * Class to hold benchmark results
     */
    private static class BenchmarkResult {
        final long[] times;
        final double avgTime;
        final long minTime;
        final long maxTime;
        final double stdDev;
        final AbstractMap<Integer, Integer> result;

        public BenchmarkResult(long[] times, AbstractMap<Integer, Integer> result) {
            this.times = times;
            this.result = result;

            // Calculate statistics
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for (long time : times) {
                sum += time;
                min = Math.min(min, time);
                max = Math.max(max, time);
            }

            this.avgTime = (double) sum / times.length;
            this.minTime = min;
            this.maxTime = max;

            // Calculate standard deviation
            double sumDiffSquared = 0;
            for (long time : times) {
                double diff = time - avgTime;
                sumDiffSquared += diff * diff;
            }

            this.stdDev = Math.sqrt(sumDiffSquared / times.length);
        }
    }
}
