package solutions;

import data.Image;

import java.util.concurrent.ConcurrentHashMap;

public class ParallelFinderGlobalHashMap implements AmongiFinder {
    final int p; // The number of cores used by the Fork/Join Framework.

    public ParallelFinderGlobalHashMap(int p) {
        this.p = p;
        // Hint: initialise the Fork/Join framework here as well.
    }

    public ConcurrentHashMap<Integer, Integer> countAmongiByColour(Image img) {
        // TODO: Implement this using Java Fork/Join.
        return new ConcurrentHashMap<>();
    }
}
