// TS000068
package benchmark;

import data.Image;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import solutions.ParallelFinderGlobalHashMap;
import solutions.ParallelFinderSubtotals;
import solutions.SequentialFinder;

import java.util.AbstractMap;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for comparing sequential and parallel implementations
 * This one isn't used, but it is a good example of how to use JMH.
 * why is this not used? Because firefly used java 14, and I couldn't find the right version of JMH for it.
 * Possibility would be using maven or gradle to manage dependencies, but the project provided wasn't using it.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS) // Reduced warmup for faster startup
@Measurement(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS) // Reduced measurement iterations
@Fork(1)
@State(Scope.Benchmark)
public class JMHBenchmark {

    // Using all the images from the previous example
    @Param({
            "images/place_2k_2k.png",
            "images/place_20k_2k.png",
            "images/place_20k_20k.png",
            "images/place_20k_20k_discoloured.png",
            "images/place_23k_23k.png"
    })
    private String imageFile;

    // Allowing up to 128 threads
    @Param({"1", "2", "4", "8", "16", "32", "64", "128"})
    private int cores;

    // Various thresholds for the subtotals implementation
    @Param({"1", "10", "50", "100", "500", "1000"}) // Limited threshold values for faster results
    private int threshold;

    private Image img;
    private SequentialFinder sequentialFinder;
    private ParallelFinderGlobalHashMap parallelGlobalFinder;
    private ParallelFinderSubtotals parallelSubtotalsFinder;

    @Setup
    public void setup() {
        img = new Image(imageFile);
        sequentialFinder = new SequentialFinder();
        parallelGlobalFinder = new ParallelFinderGlobalHashMap(cores);
        parallelSubtotalsFinder = new ParallelFinderSubtotals(cores, threshold);
    }

    @Benchmark
    public AbstractMap<Integer, Integer> sequential() {
        return sequentialFinder.countAmongiByColour(img);
    }

    @Benchmark
    public AbstractMap<Integer, Integer> parallelGlobal() {
        return parallelGlobalFinder.countAmongiByColour(img);
    }

    @Benchmark
    public AbstractMap<Integer, Integer> parallelSubtotals() {
        return parallelSubtotalsFinder.countAmongiByColour(img);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(JMHBenchmark.class.getSimpleName())
                .threads(128)  // Use all 128 threads available on machine
                .warmupIterations(1) // Reduced warmup iterations
                .measurementIterations(2) // Reduced measurement iterations
                .forks(1)
                .jvmArgs("-Xms4g", "-Xmx64g") // JVM memory arguments for large image processing
                // .timeout(TimeValue.seconds(14400))
                .build();

        new Runner(options).run();
    }
}
