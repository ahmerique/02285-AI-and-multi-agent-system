package src.searchclient;

import java.util.*;
import java.util.HashMap;


/**
 * Calculates the cost from state to the goal, and returns the optimal cost.
 */
public abstract class Heuristic implements Comparator<State> {

    private HashMap<String, BoardObject> boxById = new HashMap<>(); // List Ids and Box objects
    private HashMap<String, BoardObject> agentById = new HashMap<>(); // List Ids and agent objects
    private HashMap<Goal, Coordinate> coordinateByGoal = new HashMap<>();

    // Choice of method to compute the heuristic
    // Possible choices for now: "euclidian", "manhattan", "pythagorean", "pullDistance"
    String heuristic_method = "manhattan";


    /**
     * Instantiate a new Heuristic
     * Using static lists goalWithCoordinate and realBoardObjectsById
     */
    public Heuristic() {

        // Instantiate HashMaps with box and agent by IDs
        for (HashMap.Entry<String, BoardObject> boxOrAgent : State.realBoardObjectsById.entrySet()) {
            if (boxOrAgent.getValue() instanceof Box) {
                this.boxById.put(boxOrAgent.getKey(), boxOrAgent.getValue());
            }

            if (boxOrAgent.getValue() instanceof Agent) {
                this.agentById.put(boxOrAgent.getKey(), boxOrAgent.getValue());
            }

        }

        this.coordinateByGoal = State.goalWithCoordinate;
    }


    /**
     * Get the Coordinate of a Box or an Agent at a given State n
     *
     * @param n    the State
     * @param id   the id of the board object
     * @param type the type of the object: "Box" or "Agent"
     * @return the coordinates of the object from the given type at the given State
     */
    private Coordinate getCoordinate(State n, String id, String type) {
        if (type.equals("Agent")) {
            if (this.agentById.containsKey(id)) {
                return n.getLocalCoordinateById().get(id);
            } else {
                return null;
            }
        }

        if (type.equals("Box")) {
            if (this.boxById.containsKey(id)) {
                return n.getLocalCoordinateById().get(id);
            } else {
                return null;
            }
        }

        return null;
    }


    /**
     * Get a State and compute coordinates of all Boxes or Agent at this given State
     *
     * @param n    the State
     * @param type the type of the board object: "Box" or "Agent"
     * @return a Hashmap containing all boxes or agents object as keys and their coordinates as value
     */
    private HashMap<BoardObject, Coordinate> getAllCoordinate(State n, String type) {
        if (type.equals("Box")) {
            HashMap<BoardObject, Coordinate> coordinateByBox = new HashMap<>();

            for (HashMap.Entry<String, BoardObject> box : this.boxById.entrySet()) {
                coordinateByBox.put(box.getValue(), this.getCoordinate(n, box.getKey(), "Box"));
            }


            return coordinateByBox;
        }

        if (type.equals("Agent")) {
            HashMap<BoardObject, Coordinate> coordinateByAgent = new HashMap<>();

            for (HashMap.Entry<String, BoardObject> agent : this.agentById.entrySet()) {
                coordinateByAgent.put(agent.getValue(), this.getCoordinate(n, agent.getKey(), "Agent"));
            }


            return coordinateByAgent;
        }
        return null;

    }


    /**
     * Gets two coordinates as variables and compute the euclidian distance
     * Consists in a straight line distance between the two coordinates
     *
     * @param c1 coordinate 1
     * @param c2 coordinate 2
     * @return euclidian distance from c1 to c2
     */
    private double euclidian(Coordinate c1, Coordinate c2) {
        return Math.sqrt((double) ((c1.getRow() - c2.getRow()) * (c1.getRow() - c2.getRow()) + (c1.getColumn() - c2.getColumn()) * (c1.getColumn() - c2.getColumn())));
    }


    /**
     * Gets two coordinates as variables and compute the manhattan distance
     * Distance step along squares in a rectangular grid (A box can't be moved diagonally, better than euclidian)
     *
     * @param c1 coodinate 1
     * @param c2 coordinate 2
     * @return manhattan distance from c1 to c2
     */
    private double manhattan(Coordinate c1, Coordinate c2) {
        return Math.abs(c1.getRow() - c2.getRow()) + Math.abs(c1.getColumn() - c2.getColumn());
    }

    /**
     * Gets two coordinates as variables and compute the pythegorean distance
     * Distance step along squares in a rectangular grid.
     * Compared to manhattan distance, two squares are closer together if they are diagonal to each other
     *
     * @param c1 coodinate 1
     * @param c2 coordinate 2
     * @return pythagorean distance from c1 to c2
     */
    private double pythagorean(Coordinate c1, Coordinate c2) {
        return Math.sqrt((c1.getRow() - c2.getRow()) * (c1.getRow() - c2.getRow()) + (c1.getColumn() - c2.getColumn()) * (c1.getColumn() - c2.getColumn()));
    }


    /**
     * More complexe measure of distance (using the effective pull distance)
     * It takes the position of the walls into account by doing a BFS
     *
     * @param c1 coordinate 1
     * @param c2 coordinate 2
     * @return distance form position c1 to position c2 on the board
     */
    static public double pullDistance(Coordinate c1, Coordinate c2) {

        // Table with all possible directions of movements
        Command.Dir[] directions = Command.Dir.values();

        // Create a queue
        Queue<Node_PullDist> q = new ArrayDeque<>();

        // Enqueue first node = position of coordinate c1
        Node_PullDist start = new Node_PullDist(c1, 0);
        q.add(start);

        // Set to contain all cells already visited
        Set<Coordinate> visited = new HashSet<>();

        // Add coordinates c1
        visited.add(c1);

        // Stop to run when queue is empty
        while (!q.isEmpty()) {

            // Pop front Node from queue and process it
            Node_PullDist current = q.poll();
            Coordinate current_cord = current.coord;
            int current_level = current.number_actions;

            // Return if c2 is reached
            if (current_cord.equals(c2)) {
                return (double) current_level;
            }


            // Check and recurr on all possible movements from recurrent cell
            for (Command.Dir direction : directions) {

                Coordinate next_coord = new Coordinate(current_cord.getRow(), current_cord.getColumn());

                // Get next possible position coordinates
                next_coord.setColumn(current_cord.getColumn() + Command.dirToColChange(direction));
                next_coord.setRow(current_cord.getRow() + Command.dirToRowChange(direction));


                // Check if next cell is Free of wall or not
                if (State.cellIsFreeFromWall(next_coord)) {

                    // Add cell Node
                    Node_PullDist next = new Node_PullDist(next_coord, current.number_actions + 1);

                    // Check if coordinate not visited yet
                    if (!visited.contains(next_coord)) {

                        // Push Node in Queue and add the coordinate to visited list
                        q.add(next);
                        visited.add(next_coord);
                    }

                }

            }


        }
        // If coordinate c2 is not reachable from coordinate c1, return a high distance
        double minimumDistance = 9999; // Initialize the minimum distance at a very high value
        return minimumDistance;
    }


    /**
     * Receives the state and the heuristic choice as parameters and calculates the minimum distance for each box and agent
     *
     * @param n      state
     * @param method heuristic choice
     * @return sum of distances for player and boxes
     */
    public double calculate_distance(State n, String method) {
        double sum = 0;
        
        /* 
        // V1 - Don't take the assigned GOAL to BOX and Agent into account
        /*
        HashMap<BoardObject, Coordinate> coordinateByBox = this.getAllCoordinate(n, "Box");
        HashMap<BoardObject, Coordinate> coordinateByAgent = this.getAllCoordinate(n, "Agent");

        // Get the minimum distance from each agent to the nearest box, and add the minimum distance to the Sum
        for (HashMap.Entry<BoardObject, Coordinate> agent : coordinateByAgent.entrySet()) {
			double agentDistanceMinimum = getMinimumDistanceFromAgentToBoxes((Agent) agent.getKey(), agent.getValue(), coordinateByBox, method);
			sum += agentDistanceMinimum;
        }
        
		// Get the minimum distance from each box to the nearest goal and add the minimum distance to the Sum
        for (HashMap.Entry<BoardObject, Coordinate> box : coordinateByBox.entrySet()) {
			double distanceMinimum = getMinimumDistanceFromBoxToGoals((Box) box.getKey(), box.getValue(), method);
			sum += distanceMinimum;
		}
        */

        //V2
        /*
        HashMap<BoardObject, Coordinate> coordinateByBox = this.getAllCoordinate(n, "Box");
        HashMap<BoardObject, Coordinate> coordinateByAgent = this.getAllCoordinate(n, "Agent");
        // Get the minimum distance from each agent to its assigned Box, and add the minimum distance to the Sum
        for (HashMap.Entry<BoardObject, Coordinate> agent : coordinateByAgent.entrySet()) {
            if (((Agent) agent.getKey()).getCurrentGoal() != null){
                double agentDistanceMinimum = getMinimumDistanceFromAgentToAssignedBox((Agent) agent.getKey(), agent.getValue(), coordinateByBox, method);
                sum += agentDistanceMinimum;
            };
        }
        
        
        //Get the minimum distance from each box to its assigned goal and add the minimum distance to the Sum
        for (HashMap.Entry<BoardObject, Coordinate> box : coordinateByBox.entrySet()) {
            double distanceMinimum = getMinimumDistanceFromBoxToAssignedGoal((Box) box.getKey(), box.getValue(), method);
            sum += distanceMinimum;
           
        }
        */

        // V3: Heuristic evaluated only on Agent and Box from current State. Problem = Agent don't know when moving Box already placed on Goal
        /*
        if (n.getBoxId() != null) {
            // Get the minimum distance from the current agent to its assigned Box, and add the minimum distance to the Sum
            double agentDistanceMinimum = getMinimumDistanceFromAgentToAssignedBoxAtState(n.getLocalCoordinateById().get(n.getAgentId()), 
                                                                                            n.getLocalCoordinateById().get(n.getBoxId()), 
                                                                                            method);
            sum += agentDistanceMinimum;

            // Get the minimum distance from the current box to its assigned goal and add the minimum distance to the Sum
            double distanceMinimum = getMinimumDistanceFromBoxToAssignedGoal((Box) State.realBoardObjectsById.get(n.getBoxId()), n.getLocalCoordinateById().get(n.getBoxId()), method);
            sum += distanceMinimum; 
            
           } 
        /*
        else {
            // Case when Box is null
            // We are then simply interested in moving the agent to a specific location
            sum = manhattan(n.getLocalCoordinateById().get(n.getAgentId()), coordonnées de la destination)
            
        }
        */


        // V4: heuristic evaluated only on Agent form current State and its goal, and on all Boxes movable by the Agent compared to their assigned Goal
        HashMap<BoardObject, Coordinate> coordinateByBox = this.getAllCoordinate(n, "Box");

        if (n.getBoxId() != null) {
            // Get the minimum distance from the current agent to its assigned Box, and add the minimum distance to the Sum
            double agentDistanceMinimum = getMinimumDistanceFromAgentToAssignedBoxAtState(n.getLocalCoordinateById().get(n.getAgentId()),
                    n.getLocalCoordinateById().get(n.getBoxId()),
                    method);
            sum += agentDistanceMinimum;
        } else if (n.destination != null) {
            // Case when going to destination
            // We are then simply interested in moving the agent to a specific location
            //TODO FIND A GOOD HEURISTIC TO find way back MABaguettes -------------------------------------
            sum = manhattan(n.getLocalCoordinateById().get(n.agentId), n.destination);
        }

        //Get the minimum distance from each box movable by the agent to its assigned goal and add the minimum distance to the Sum
        for (HashMap.Entry<BoardObject, Coordinate> box : coordinateByBox.entrySet()) {
            if (State.realBoardObjectsById.get(n.getAgentId()).getColor().equals(box.getKey().getColor())) {
                double distanceMinimum = getMinimumDistanceFromBoxToAssignedGoal((Box) box.getKey(), box.getValue(), method);
                sum += distanceMinimum;
            }
        }
        return sum;
    }


    /**
     * Get a box, its coordinate and a method as inputs
     * Calculates minimum distance from the given Box to the coordinate of its assigned Goal
     *
     * @param box
     * @param box_coordinates
     * @param method
     * @return distance from Box to its assigned Goal
     */
    private double getMinimumDistanceFromBoxToAssignedGoal(Box box, Coordinate box_coordinates, String method) {
        double distance = 0; // Value by default for a box without any Goal
        //For each goal assigned to a box, calculate the distance according to given heuristic choice
        if (box.getBoxGoal() != null) {
            if (method.equals("manhattan")) {
                distance = manhattan(box_coordinates, this.coordinateByGoal.get(box.getBoxGoal()));
            }

            if (method.equals("euclidian")) {
                distance = euclidian(box_coordinates, this.coordinateByGoal.get(box.getBoxGoal()));
            }

            if (method.equals("pythagorean")) {
                distance = pythagorean(box_coordinates, this.coordinateByGoal.get(box.getBoxGoal()));
            }

            if (method.equals("pullDistance")) {
                distance = pullDistance(box_coordinates, this.coordinateByGoal.get(box.getBoxGoal()));
            }
            /* 
                We can add other methods here    
                */
        }
        return distance;
    }


    /**
     * Get a box, its coordinate and a method as inputs
     * Calculates minimum distance from the given Box to the coordinate of each goal of the same color using the input method
     *
     * @param box
     * @param box_coordinates
     * @param method
     * @return distance from Box to closest Goal
     */
    private double getMinimumDistanceFromBoxToGoals(Box box, Coordinate box_coordinates, String method) {
        double minimumDistance = 9999; // Initialize the minimum distance at a very high value

        //For each goal whose color match with the box color, calculate the distance according to given heuristic choice
        for (HashMap.Entry<Goal, Coordinate> goal : this.coordinateByGoal.entrySet()) {
            double distance = 99999;
            if (method.equals("manhattan")) {
                // Check if colors match
                if (box.getLetter() == goal.getKey().getLetter()) {
                    distance = manhattan(box_coordinates, goal.getValue());
                }
            }

            if (method.equals("euclidian")) {
                // Check if colors match
                if (box.getLetter() == goal.getKey().getLetter()) {
                    distance = euclidian(box_coordinates, goal.getValue());
                }
            }

            if (method.equals("pythagorean")) {
                // Check if colors match
                if (box.getLetter() == goal.getKey().getLetter()) {
                    distance = pythagorean(box_coordinates, goal.getValue());
                }
            }

            if (method.equals("pullDistance")) {
                // Check if colors match
                if (box.getLetter() == goal.getKey().getLetter()) {
                    distance = pullDistance(box_coordinates, goal.getValue());
                }
            }
            /* 
                We can add other methods here    
                */
            if (distance < minimumDistance) {
                minimumDistance = distance;
            }
        }
        return minimumDistance;
    }


    /**
     * Get an agent coordinates, its assigned box coordinates, and a method as inputs
     * Calculates minimum distance from the given Agent to the coordinate of the Box with the same assigned Goal using the input method
     *
     * @param agent_coordinates
     * @param box_coordinates
     * @param method
     * @return distance from Agent to assigned goal
     */
    private double getMinimumDistanceFromAgentToAssignedBoxAtState(Coordinate agent_coordinates, Coordinate box_coordinates, String method) {
        double distance = 0; // Value by default for an Agent without any Goal

        //For each box whose goal match with the agent's goal, calculate the distance according to given heuristic choice
        if (method.equals("manhattan")) {
            // Check if goal match
            distance = manhattan(agent_coordinates, box_coordinates);
            return distance;
        }

        if (method.equals("euclidian")) {
            // Check if goal match
            distance = euclidian(agent_coordinates, box_coordinates);
            return distance;
        }

        if (method.equals("pythagorean")) {
            // Check if goal match
            distance = pythagorean(agent_coordinates, box_coordinates);
            return distance;
        }

        if (method.equals("pullDistance")) {
            // Check if goal match
            distance = pullDistance(agent_coordinates, box_coordinates);
            return distance;
        }

            /* 
                We can add other methods here    
                */
        return distance;
    }


    /**
     * Get an agent, its coordinate and a method as inputs
     * Calculates minimum distance from the given Agent to the coordinate of the Box with the same assigned Goal using the input method
     *
     * @param agent
     * @param agent_coordinates
     * @param method
     * @return distance from Agent to assigned goal
     */
    private double getMinimumDistanceFromAgentToAssignedBox(Agent agent, Coordinate agent_coordinates, HashMap<BoardObject, Coordinate> coordinateByBox, String method) {
        double distance = 0; // Value by default for an Agent without any Goal

        //For each box whose goal match with the agent's goal, calculate the distance according to given heuristic choice
        if (method.equals("manhattan")) {
            // Check if goal match
            distance = manhattan(agent_coordinates, coordinateByBox.get(agent.getCurrentGoal().getAttachedBox()));
            return distance;
        }

        if (method.equals("euclidian")) {
            // Check if goal match
            distance = euclidian(agent_coordinates, coordinateByBox.get(agent.getCurrentGoal().getAttachedBox()));
            return distance;
        }

        if (method.equals("pythagorean")) {
            // Check if goal match
            distance = pythagorean(agent_coordinates, coordinateByBox.get(agent.getCurrentGoal().getAttachedBox()));
            return distance;
        }

        if (method.equals("pullDistance")) {
            // Check if goal match
            distance = pullDistance(agent_coordinates, coordinateByBox.get(agent.getCurrentGoal().getAttachedBox()));
            return distance;
        }

            /* 
                We can add other methods here    
                */
        return distance;
    }


    /**
     * Get an agent, its coordinate and a method as inputs
     * Calculates minimum distance from the given Agent to the coordinate of each boxes of the same color using the input method
     *
     * @param agent
     * @param agent_coordinates
     * @param method
     * @return distance from Agent to closest Box
     */
    private double getMinimumDistanceFromAgentToBoxes(BoardObject agent, Coordinate agent_coordinates, HashMap<BoardObject, Coordinate> coordinateByBox, String method) {
        double minimumDistance = 9999; // Initialize the minimum distance at a very high value

        //For each box whose color match with the agent color, calculate the distance according to given heuristic choice
        for (HashMap.Entry<BoardObject, Coordinate> box : coordinateByBox.entrySet()) {
            double distance = 99999;
            if (method.equals("manhattan")) {
                // Check if colors match
                if (agent.getColor().equals(box.getKey().getColor())) {
                    distance = manhattan(agent_coordinates, box.getValue());
                }
            }

            if (method.equals("euclidian")) {
                // Check if colors match
                if (agent.getColor().equals(box.getKey().getColor())) {
                    distance = euclidian(agent_coordinates, box.getValue());
                }
            }

            if (method.equals("pythagorean")) {
                // Check if colors match
                if (agent.getColor().equals(box.getKey().getColor())) {
                    distance = pythagorean(agent_coordinates, box.getValue());
                }
            }

            if (method.equals("pullDistance")) {
                // Check if colors match
                if (agent.getColor().equals(box.getKey().getColor())) {
                    distance = pullDistance(agent_coordinates, box.getValue());
                }
            }

            /* 
                We can add other methods here    
                */
            if (distance < minimumDistance) {
                minimumDistance = distance;
            }
        }
        return minimumDistance;
    }


    /**
     * Gets a State and calculates the cost from this state to the Goal state
     * using the chosen method defined in heuristic_method
     *
     * @param state
     * @return cost
     */
    public double getHeuristic(State state) {

        if (heuristic_method.equals("manhattan"))
            return calculate_distance(state, "manhattan");
        if (heuristic_method.equals("euclidian"))
            return calculate_distance(state, "euclidian");
        if (heuristic_method.equals("pythagorean"))
            return calculate_distance(state, "pythagorean");
        if (heuristic_method.equals("pullDistance"))
            return calculate_distance(state, "pullDistance");

        // For the moment, return manhattan distance by default
        return calculate_distance(state, "manhattan");

    }


    // Old code

    public int h(State n) {
        //throw new NotImplementedException();
        return (int) Math.round(getHeuristic(n));
    }

    public abstract int f(State n);

    @Override
    public int compare(State n1, State n2) {
        return this.f(n1) - this.f(n2);
    }

    public static class AStar extends Heuristic {
        public AStar() {  // public AStar(State initialState) {
            super();
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

        public WeightedAStar(int W) { // public WeightedAStar(State initialState, int W) {
            super();
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
        public Greedy() { // public Greedy(State initialState) {
            super();
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
