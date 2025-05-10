package benchmark;

import data.Image;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import solutions.ParallelFinderGlobalHashMap;
import solutions.ParallelFinderSubtotals;
import solutions.SequentialFinder;

import java.util.AbstractMap;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for comparing sequential and parallel implementations
 * Configured specifically for AMD Ryzen Threadripper 3990X with 64 cores
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(2)
@State(Scope.Benchmark)
public class EnhancedJMHBenchmark {

    // Using all test images from the test class
    @Param({
            "images/place_2k_2k.png",
            "images/place_20k_2k.png",
            "images/place_20k_20k.png",
            "images/place_20k_20k_discoloured.png",
            "images/place_23k_23k.png"
    })
    private String imageFile;

    // Testing with 1, 4, 16, 32, 64, and 128 threads (Threadripper has 128 threads)
    @Param({"1", "4", "16", "32", "64", "128"})
    private int cores;

    // Various thresholds for the subtotals implementation
    @Param({"1", "10", "50", "100", "500", "1000", "2147483647"}) // Integer.MAX_VALUE as the last value
    private int threshold;

    private Image img;
    private SequentialFinder sequentialFinder;
    private ParallelFinderGlobalHashMap parallelGlobalFinder;
    private ParallelFinderSubtotals parallelSubtotalsFinder;

    // Task counter variables to analyze overhead
    private int globalHashMapTasks;
    private int subtotalsTasks;

    @Setup
    public void setup() {
        img = new Image(imageFile);
        sequentialFinder = new SequentialFinder();
        parallelGlobalFinder = new ParallelFinderGlobalHashMap(cores);
        parallelSubtotalsFinder = new ParallelFinderSubtotals(cores, threshold);

        // Reset task counters
        ParallelFinderGlobalHashMap.globalAtomicCC.set(0);
        ParallelFinderSubtotals.globalAtomicCC.set(0);
    }

    @Benchmark
    public AbstractMap<Integer, Integer> sequential() {
        return sequentialFinder.countAmongiByColour(img);
    }

    @Benchmark
    public void parallelGlobal(Blackhole bh) {
        AbstractMap<Integer, Integer> result = parallelGlobalFinder.countAmongiByColour(img);
        globalHashMapTasks = ParallelFinderGlobalHashMap.globalAtomicCC.get();
        bh.consume(result);
        bh.consume(globalHashMapTasks);
    }

    @Benchmark
    public void parallelSubtotals(Blackhole bh) {
        AbstractMap<Integer, Integer> result = parallelSubtotalsFinder.countAmongiByColour(img);
        subtotalsTasks = ParallelFinderSubtotals.globalAtomicCC.get();
        bh.consume(result);
        bh.consume(subtotalsTasks);
    }

    @TearDown
    public void tearDown() {
        System.out.printf("Image: %s, Cores: %d, Threshold: %d%n", imageFile, cores, threshold);
        System.out.printf("Global HashMap Tasks created: %d%n", globalHashMapTasks);
        System.out.printf("Subtotals Tasks created: %d%n", subtotalsTasks);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(EnhancedJMHBenchmark.class.getSimpleName())
                // Configure specific parameters if you want to run a subset of benchmarks
                // .param("imageFile", "images/place_2k_2k.png")
                // .param("cores", "64")
                // .param("threshold", "100")
                .threads(Runtime.getRuntime().availableProcessors())
                .jvmArgs("-Xms4g", "-Xmx64g") // Configure JVM heap for large images
                .build();

        new Runner(options).run();
    }
}