package benchmark;

import data.Image;
import org.openjdk.jmh.annotations.*;
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
 * To use this, you need to add JMH dependencies to your project
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
@State(Scope.Benchmark)
public class JMHBenchmark {

    @Param({"images/place_2k_2k.png"})
    private String imageFile;

    @Param({"1", "2", "4", "8"})
    private int cores;

    @Param({"1", "10", "50", "100", "500", "2147483647"}) // Integer.MAX_VALUE as the last value
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
                .build();
        new Runner(options).run();
    }
}