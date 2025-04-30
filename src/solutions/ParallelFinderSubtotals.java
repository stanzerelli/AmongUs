package solutions;

import data.Image;

import java.util.HashMap;

public class ParallelFinderSubtotals implements AmongiFinder {
    final int p; // The number of cores used by the Fork/Join Framework.
    final int T; // The sequential threshold.

    public ParallelFinderSubtotals(int p, int T) {
        this.p = p;
        this.T = T;
        // Hint: initialise the Fork/Join framework here as well.
    }

    public HashMap<Integer, Integer> countAmongiByColour(Image img) {
        // TODO: Implement this using Java Fork/Join.
        return new HashMap<>();
    }
}
