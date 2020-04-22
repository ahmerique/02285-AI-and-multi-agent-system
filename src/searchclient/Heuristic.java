package searchclient;

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

        for( Hashmap.Entry<String, BoardObject> box : State.realBoardObjectsById.entrySet()) {
            if(box.getValue() instanceof Box){
                this.boxById.put(box.getKey(), box.getValue());
            }
        }

        this.coordinateByGoal = State.goalWithCoordinate;
 
    }


    /**
     * Get the Coordinate of a Box at a given State n
     * @param n the State
     * @param id the id of the Box
     * @return the coordinates of the given Box at the given State
     */
    private Coordinate getBoxCoordinate(State n, String id) {
        if(this.boxById.containsKey(id)) {
            return n.getLocalCoordinateById().get(id);
        } else {
            return null;
        }
    }


    /**
     * Get a State and compute coordinates of all Boxes at this given State
     * @param n the State
     * @return a Hashmap containing all boxes object as keys and their coordinates as value
     */
    private Coordinate getAllBoxesCoordinate(State n) {
        HashMap<Box, Coordinate> coordinateByBox = new HashMap<>();

        for(HashMap.Entry<String, Box> box : this.boxById.entrySet()) {
            coordinateByBox.put(box.getValue(), this.getBoxCoordinate(n, box.getKey()));
        }

        return coordinateByBox;

    }

    /**
	 * Gets two coordinates as variables and compute the manhattan distance
	 * @param c1 coodinate 1
	 * @param c2 coordinate 2
	 * @return manhattan distance from c1 to c2
	 */
	private double manhattan(Coordinate c1, Coordinate c2) {
		return Math.abs(c1.getRow()-c2.getRow()) + Math.abs(c1.getColumn()-c2.getColumn());
    }
    

    /**
	 * Receives the state and the heuristic choice as parameters and calculates the distance for each box and player
	 * @param n state
	 * @param method heuristics choice
	 * @return sum of distances for player and boxes
	 */
	public double calculate_distance(State n, String method) {
        double sum = 0;
        HashMap<Box, Coordinate> coordinateByBox = this.getAllBoxesCoordinate(n);
		
        /*
            Room for improvement:
            Also take into accoutn the distance between boxes and agents
        
        */
		
		//Get the distance from boxes to goal and add the minimum distance to the Sum
        for (HashMap<Box, Coordinate> box : coordinateByBox.entrySet()) {
			double distanceMinimum = getMinimumDistanceFromBoxToGoals(box.getKey(), box.getValue(), method);
			sum += distanceMinimum;
		}
		
		return sum;
    }
    

    /**
	 * Get a box, its coordinate and a method as inputs
     * Calculates minimum distance from the given Box to the coordinate of each goals using the input method
	 * @param box
     * @param box_coordinates
	 * @param method
	 * @return distance from obj to closest coordinate in the hashset
	 */
	private double getMinimumDistanceFromBoxToGoals(Box box, Coordinate box_coordinates, String method) {
		double minimumDistance = 9999999; // Initialize the minimum distance at a very high value
		
		//For each goal whose color match with the box color, calculate the distance according to given heuristic choice
		for (HashMap.Entry<Goal, Coordinate> goal : this.coordinateByGoal.entrySet()) {
			double distance;
			if (method.equals("manhattan")){
                // Check if colors match
                if (box.getColor().equals(goal.getKey().getColor())) { 
                    distance = manhattan(box_coordinates, goal.getValue());
                }
            }
            /* 
                We can add other methods here    
            else
                distance = other_methode()
                */
			if (distance < minimumDistance) {
                minimumDistance = distance;
            }
		}
		return minDist;
	}



    public int h(State n) {
        //return stupid_h(n);
        return manhattan_h(n);
    }

    public abstract int f(State n);

    @Override
    public int compare(State n1, State n2) {
        return this.f(n1) - this.f(n2);
    }

    public static class AStar extends Heuristic {
        public AStar(State initialState) {
            super(initialState);
        }

        @Override
        public int f(State n) {
            return n.g() + this.h(n);
        }

        @Override
        public String toString() {
            return "A* evaluation";
        }
    }

    public static class WeightedAStar extends Heuristic {
        private int W;

        public WeightedAStar(State initialState, int W) {
            super(initialState);
            this.W = W;
        }

        @Override
        public int f(State n) {
            return n.g() + this.W * this.h(n);
        }

        @Override
        public String toString() {
            return String.format("WA*(%d) evaluation", this.W);
        }
    }

    public static class Greedy extends Heuristic {
        public Greedy(State initialState) {
            super(initialState);
        }

        @Override
        public int f(State n) {
            return this.h(n);
        }

        @Override
        public String toString() {
            return "Greedy evaluation";
        }
    }
}
