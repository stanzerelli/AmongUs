package solutions;

import data.Amongus;
import data.Image;

import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelFinderSubtotals implements AmongiFinder {
    final int p; // The number of cores used by the Fork/Join Framework.
    final int T; // The sequential threshold for y-axis parallelization
    private final ForkJoinPool pool;

    // Global atomic counter to count the number of tasks
    public static final AtomicInteger globalAtomicCC = new AtomicInteger(0);

    public ParallelFinderSubtotals(int p, int T) {
        this.p = p;
        this.T = T;
        this.pool = new ForkJoinPool(p);
    }

    public HashMap<Integer, Integer> countAmongiByColour(Image img) {
        globalAtomicCC.set(0); // Reset the counter before starting

        XFinderTask task = new XFinderTask(img, 0, img.width);
        return pool.invoke(task);
    }

    /**
     * Task to find amongi in a range of x coordinates
     */
    private class XFinderTask extends RecursiveTask<HashMap<Integer, Integer>> {
        private static final int SEQUENTIAL_THRESHOLD = 100; // Threshold for sequential processing on x-axis
        private final Image img;
        private final int startX, endX;

        public XFinderTask(Image img, int startX, int endX) {
            this.img = img;
            this.startX = startX;
            this.endX = endX;
            globalAtomicCC.incrementAndGet(); // Increment the counter when a task is created
        }

        @Override
        protected HashMap<Integer, Integer> compute() {
            int rangeSize = endX - startX;

            // If the range is small enough, process sequentially
            if (rangeSize <= SEQUENTIAL_THRESHOLD) {
                return computeSequentially();
            }

            // Otherwise, divide the range and create subtasks
            int midX = startX + rangeSize / 2;

            XFinderTask leftTask = new XFinderTask(img, startX, midX);
            XFinderTask rightTask = new XFinderTask(img, midX, endX);

            // Execute the right task asynchronously
            rightTask.fork();
            // Execute the left task directly
            HashMap<Integer, Integer> leftResult = leftTask.compute();
            // Wait for the right task to complete
            HashMap<Integer, Integer> rightResult = rightTask.join();

            // Merge the results
            return mergeMaps(leftResult, rightResult);
        }

        private HashMap<Integer, Integer> computeSequentially() {
            HashMap<Integer, Integer> localColourCounts = new HashMap<>();

            for (int x = startX; x < endX; x++) {
                // For each x coordinate, start a task for y coordinates if T is not MAX_VALUE
                if (T < Integer.MAX_VALUE) {
                    YFinderTask yTask = new YFinderTask(img, x, 0, img.height);
                    HashMap<Integer, Integer> xResult = yTask.compute();
                    // Merge results for this x coordinate
                    mergeMapsInPlace(localColourCounts, xResult);
                } else {
                    // Process y axis sequentially
                    processYSequentially(x, localColourCounts);
                }
            }

            return localColourCounts;
        }

        private void processYSequentially(int x, HashMap<Integer, Integer> colourCounts) {
            for (int y = 0; y < img.height; y++) {
                if (Amongus.detect(x, y, img)) {
                    int colour = Amongus.bodyColor(x, y, img);
                    colourCounts.put(colour, colourCounts.getOrDefault(colour, 0) + 1);
                }
            }
        }
    }

    /**
     * Task to find amongi in a range of y coordinates for a specific x coordinate
     */
    private class YFinderTask extends RecursiveTask<HashMap<Integer, Integer>> {
        private final Image img;
        private final int x;
        private final int startY, endY;

        public YFinderTask(Image img, int x, int startY, int endY) {
            this.img = img;
            this.x = x;
            this.startY = startY;
            this.endY = endY;
            globalAtomicCC.incrementAndGet(); // Increment the counter when a task is created
        }

        @Override
        protected HashMap<Integer, Integer> compute() {
            int rangeSize = endY - startY;

            // If the range is small enough, process sequentially
            if (rangeSize <= T) {
                return computeSequentially();
            }

            // Otherwise, divide the range and create subtasks
            int midY = startY + rangeSize / 2;

            YFinderTask topTask = new YFinderTask(img, x, startY, midY);
            YFinderTask bottomTask = new YFinderTask(img, x, midY, endY);

            // Execute the bottom task asynchronously
            bottomTask.fork();
            // Execute the top task directly
            HashMap<Integer, Integer> topResult = topTask.compute();
            // Wait for the bottom task to complete
            HashMap<Integer, Integer> bottomResult = bottomTask.join();

            // Merge the results
            return mergeMaps(topResult, bottomResult);
        }

        private HashMap<Integer, Integer> computeSequentially() {
            HashMap<Integer, Integer> localColourCounts = new HashMap<>();

            for (int y = startY; y < endY; y++) {
                if (Amongus.detect(x, y, img)) {
                    int colour = Amongus.bodyColor(x, y, img);
                    localColourCounts.put(colour, localColourCounts.getOrDefault(colour, 0) + 1);
                }
            }

            return localColourCounts;
        }
    }

    /**
     * Merges two HashMaps and returns a new HashMap with combined results
     */
    private HashMap<Integer, Integer> mergeMaps(HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2) {
        HashMap<Integer, Integer> result = new HashMap<>(map1);

        for (Integer key : map2.keySet()) {
            result.put(key, result.getOrDefault(key, 0) + map2.get(key));
        }

        return result;
    }

    /**
     * Merges the source map into the target map (modifies the target)
     */
    private void mergeMapsInPlace(HashMap<Integer, Integer> target, HashMap<Integer, Integer> source) {
        for (Integer key : source.keySet()) {
            target.put(key, target.getOrDefault(key, 0) + source.get(key));
        }
    }
}