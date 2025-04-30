package solutions;

import data.Amongus;
import data.Image;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelFinderGlobalHashMap implements AmongiFinder {
    final int p; // The number of cores used by the Fork/Join Framework.
    private final ForkJoinPool pool;

    // Global atomic counter to count the number of tasks
    public static final AtomicInteger globalAtomicCC = new AtomicInteger(0);

    public ParallelFinderGlobalHashMap(int p) {
        this.p = p;
        this.pool = new ForkJoinPool(p);
    }

    public ConcurrentHashMap<Integer, Integer> countAmongiByColour(Image img) {
        ConcurrentHashMap<Integer, Integer> colourCounts = new ConcurrentHashMap<>();
        globalAtomicCC.set(0); // Reset the counter before starting

        FinderTask task = new FinderTask(img, 0, img.width, colourCounts);
        pool.invoke(task);

        return colourCounts;
    }

    /**
     * Task to find amongi in a range of x coordinates
     */
    private static class FinderTask extends RecursiveAction {
        private static final int SEQUENTIAL_THRESHOLD = 100; // Threshold for sequential processing
        private final Image img;
        private final int startX, endX;
        private final ConcurrentHashMap<Integer, Integer> colourCounts;

        public FinderTask(Image img, int startX, int endX, ConcurrentHashMap<Integer, Integer> colourCounts) {
            this.img = img;
            this.startX = startX;
            this.endX = endX;
            this.colourCounts = colourCounts;
            globalAtomicCC.incrementAndGet(); // Increment the counter when a task is created
        }

        @Override
        protected void compute() {
            int rangeSize = endX - startX;

            // If the range is small enough, process sequentially
            if (rangeSize <= SEQUENTIAL_THRESHOLD) {
                computeSequentially();
                return;
            }

            // Otherwise, divide the range and create subtasks
            int midX = startX + rangeSize / 2;

            FinderTask leftTask = new FinderTask(img, startX, midX, colourCounts);
            FinderTask rightTask = new FinderTask(img, midX, endX, colourCounts);

            // Execute the right task asynchronously
            rightTask.fork();
            // Execute the left task directly
            leftTask.compute();
            // Wait for the right task to complete
            rightTask.join();
        }

        private void computeSequentially() {
            for (int x = startX; x < endX; x++) {
                for (int y = 0; y < img.height; y++) {
                    if (Amongus.detect(x, y, img)) {
                        int colour = Amongus.bodyColor(x, y, img);
                        colourCounts.compute(colour, (key, count) -> (count == null) ? 1 : count + 1);
                    }
                }
            }
        }
    }
}