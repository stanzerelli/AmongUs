// TS000068
import data.Image;
import solutions.SequentialFinder;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        Image img = new Image("images/place_2k_2k.png");
        HashMap<Integer, Integer> counts = new SequentialFinder().countAmongiByColour(img);
        System.out.println(counts);
    }
}