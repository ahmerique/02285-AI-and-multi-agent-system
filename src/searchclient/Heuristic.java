package src.searchclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public abstract class Heuristic implements Comparator<State> {

    private HashMap<String, Box> boxById = new HashMap<>();
    private HashMap<Goal, Coordinate> coordinateByGoal = new HashMap<>();


    /**
     * Instantiate a new Heuristic
     * Using static lists goalWithCoordinate and realBoardObjectsById
     */
    public Heuristic() {


    }
}


