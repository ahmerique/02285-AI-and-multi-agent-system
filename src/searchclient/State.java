package src.searchclient;

import java.util.*;

import static src.searchclient.Command.dirToColChange;
import static src.searchclient.Command.dirToRowChange;

public class State {

    public static Integer objectCounter = 0;

    /**
     * STATIC ATTRIBUTES
     **/

    // From direct reading
    //Final
    public volatile static HashMap<String, BoardObject> realBoardObjectsById = new HashMap<>(); // Get Agent and boxes instances from their id
    public static HashMap<Goal, Coordinate> goalWithCoordinate = new HashMap<>();
    public static HashMap<Coordinate, Goal> goalByCoordinate = new HashMap<>();
    public static HashMap<Coordinate, Boolean> wallByCoordinate = new HashMap<>();
    public static HashMap<String, Coordinate> agentGoalWithCoordinate = new HashMap<>();
    public static int MAX_ROW;
    public static int MAX_COL;

    //Map
    public static HashMap<Coordinate, String> realIdByCoordinate = new HashMap<>(); // Agent and Boxes
    public static HashMap<String, Coordinate> realCoordinateById = new HashMap<>(); // Agent and Boxes


    // From preprocessing
    public static HashMap<Coordinate, Integer> degreeMap = new HashMap<>();

    public static ArrayList<ArrayList<Coordinate>> busyDeadEndList = new ArrayList<>();
    public static ArrayList<ArrayList<Coordinate>> emptyDeadEndList = new ArrayList<>();
    public static ArrayList<ArrayList<Coordinate>> corridorList = new ArrayList<>();

    public static HashMap<Coordinate, Integer> corridorIndexByCoordinate = new HashMap<>();
    public static HashMap<Coordinate, Integer> busyDeadEndIndexByCoordinate = new HashMap<>();

    public static ArrayList<ArrayList<String>> busyDeadEndOccupancy = new ArrayList<>();
    public static ArrayList<ArrayList<String>> corridorOccupancy = new ArrayList<>();

    public final static int MAX_GOAL_PRIORITY = 1000; // for test 9;
    public final static int NORMAL_GOAL_PRIORITY = 100; // for test 1
    public final static int LOW_GOAL_PRIORITY = 0;

    /**
     * LOCAL ATTRIBUTES
     **/

    private HashMap<String, Coordinate> localCoordinateById;
    private HashMap<Coordinate, String> localIdByCoordinate;

    // TODO On peut peut être remplacer par de vraies objet car ils ne seront pas copiés, uniquement référencés.
    public String agentId;
    public String boxId;
    public Coordinate destination;
    //

    private State parent;
    public Command action;

    // Heuristic g
    private int g;
    private static final Random RNG = new Random(1);
    private int _hash = 0;


    /**
     * CONSTRUCTORS
     **/
    public State(State parent) {
        this.parent = parent;
        this.agentId = parent.agentId;
        this.boxId = parent.boxId;
        this.destination = parent.destination;
        this.g = parent.g() + 1;

        this.localCoordinateById = new HashMap<>();
        this.localIdByCoordinate = new HashMap<>();
    }

    // firstStateCreator move box to goal
    public State(HashMap<String, Coordinate> localCoordinateById, HashMap<Coordinate, String> localIdByCoordinate, String agentId, String boxId) throws AssertionError {

        this.agentId = agentId;
        this.boxId = boxId;
        this.g = 0;

        // copy hashmap
        this.localCoordinateById = new HashMap<>();
        this.localIdByCoordinate = new HashMap<>();
        for (Coordinate key : localIdByCoordinate.keySet()) {
            this.localIdByCoordinate.put(key, localIdByCoordinate.get(key));
        }
        for (String key : localCoordinateById.keySet()) {
            this.localCoordinateById.put(key, localCoordinateById.get(key));
        }

        if (agentId == null) throw new AssertionError("MUST have an agentId");
    }

    // firstStateCreator move agent to destination
    public State(HashMap<String, Coordinate> localCoordinateById, HashMap<Coordinate, String> localIdByCoordinate, String agentId, Coordinate destination) throws AssertionError {

        this.agentId = agentId;
        this.destination = destination;
        this.g = 0;

        // copy hashmap
        this.localCoordinateById = new HashMap<>();
        this.localIdByCoordinate = new HashMap<>();
        for (Coordinate key : localIdByCoordinate.keySet()) {
            this.localIdByCoordinate.put(key, localIdByCoordinate.get(key));
        }
        for (String key : localCoordinateById.keySet()) {
            this.localCoordinateById.put(key, localCoordinateById.get(key));
        }

        if (agentId == null) throw new AssertionError("MUST have an agentId");
    }

    // firstStateCreator move agent to free cell
    public State(HashMap<String, Coordinate> localCoordinateById, HashMap<Coordinate, String> localIdByCoordinate, String agentId) throws AssertionError {

        this.agentId = agentId;
        this.g = 0;

        // copy hashmap
        this.localCoordinateById = new HashMap<>();
        this.localIdByCoordinate = new HashMap<>();
        for (Coordinate key : localIdByCoordinate.keySet()) {
            this.localIdByCoordinate.put(key, localIdByCoordinate.get(key));
        }
        for (String key : localCoordinateById.keySet()) {
            this.localCoordinateById.put(key, localCoordinateById.get(key));
        }

        if (agentId == null) throw new AssertionError("MUST have an agentId");
    }

    // FOR testing purpose only
    public State(HashMap<Coordinate, String> localIdByCoordinate) throws AssertionError {
        this.localIdByCoordinate = localIdByCoordinate;
    }

    public State getCopy() {
        return new State(this.localCoordinateById, this.localIdByCoordinate, this.agentId, this.boxId);
    }


    /**
     * GETTER AND SETTER
     **/

    public String getAgentId(){
        return this.agentId;
    }

    public String getBoxId(){
        return this.boxId;
    }

    public HashMap<String, Coordinate> getLocalCoordinateById() {
        return this.localCoordinateById;
    }

    public int g() {
        return this.g;
    }

    public boolean isInitialState() {
        return this.parent == null;
    }

    //TODO : Readapt this function to actual use case
    public boolean isSubGoalState() {
        if (boxId != null) {
            Box boxObject = (Box) realBoardObjectsById.get(boxId);
            return (boxObject.getBoxGoal().getCoordinate().equals(localCoordinateById.get(boxId)));
        } else if (destination != null) {
            return destination.equals(localCoordinateById.get(agentId));
        } else {
            return degreeMap.get(localCoordinateById.get(agentId)) == NORMAL_GOAL_PRIORITY;
        }
    }

    //Method to set a new object (agent:0, box:1, goal:2) in the State
    public static BoardObject setNewStateObject(int row, int col, String type, char id, String color) {

        Coordinate coord = new Coordinate(row, col);
        String finalId;

        switch (type) {
            case "AGENT"://Agent
                finalId = Character.toString(id);
                Agent newAgent = new Agent(finalId, color);
                State.realBoardObjectsById.put(finalId, newAgent);
                State.realIdByCoordinate.put(coord, finalId);
                State.realCoordinateById.put(finalId, coord);
                return newAgent;

            case "BOX"://Box
                finalId = generateUniqueId(id);
                Box newBox = new Box(finalId, color, id);
                State.realBoardObjectsById.put(finalId, newBox);
                State.realIdByCoordinate.put(coord, finalId);
                State.realCoordinateById.put(finalId, coord);
                return newBox;

            case "GOAL"://Goal
                finalId = 'g' + generateUniqueId(id);
                Goal newGoal = new Goal(finalId, color, coord, id);
                State.goalWithCoordinate.put(newGoal, coord);
                State.goalByCoordinate.put(coord, newGoal);
                return newGoal;

            default:
                System.err.println("INVALID setNewStateObject value");
                return null;
        }
    }

    private static String generateUniqueId(char id) {
        objectCounter++;
        String stringId = Character.toString(id);
/*        int iterator = 0;

        //Process until id is new
        while (realBoardObjectsById.containsKey(stringId + iterator)) {
            iterator += 1;
        }
*/
        return (stringId + objectCounter);
    }

    // TODO update map from agent action
    public static boolean updateStaticMap(State[] latestStateArray, String[] latestServerOutput) {

        boolean hasError = false;

        for (int i = 0; i < latestStateArray.length; i++) {
            if (latestServerOutput[i].equals("true")) {
                if (latestStateArray[i] != null && latestStateArray[i].action != null) {
                    Command c = latestStateArray[i].action;
                    String agentId = latestStateArray[i].agentId;
                    Coordinate currentAgentCoordinate = realCoordinateById.get(agentId);
                    Coordinate nextAgentCoordinate = new Coordinate(
                            currentAgentCoordinate.getRow() + dirToRowChange(c.dir1),
                            currentAgentCoordinate.getColumn() + dirToColChange(c.dir1));

                    // Check deadEnd and modify occupancy
                    checkDeadEnd(agentId, currentAgentCoordinate, nextAgentCoordinate);

                    if (c.actionType == Command.Type.Move) {
                        moveRealObject(agentId, currentAgentCoordinate, nextAgentCoordinate);
                    } else if (c.actionType == Command.Type.Push) { // Agent takes the place of the box and box move toward dir2
                        String boxToMoveId = realBoxAt(nextAgentCoordinate, realBoardObjectsById.get(agentId).getColor());
                        if (boxToMoveId != null) {
                            Coordinate nextBoxCoordinate = new Coordinate(
                                    nextAgentCoordinate.getRow() + dirToRowChange(c.dir2),
                                    nextAgentCoordinate.getColumn() + dirToColChange(c.dir2));
                            moveRealObject(boxToMoveId, nextAgentCoordinate, nextBoxCoordinate);
                            moveRealObject(agentId, currentAgentCoordinate, nextAgentCoordinate);
                        }
                    } else if (c.actionType == Command.Type.Pull) { // Box takes the place of the agent and agent move toward dir1
                        Coordinate expectedBoxCoordinate = new Coordinate(
                                currentAgentCoordinate.getRow() + dirToRowChange(c.dir2),
                                currentAgentCoordinate.getColumn() + dirToColChange(c.dir2));
                        String boxToMoveId = realBoxAt(expectedBoxCoordinate, realBoardObjectsById.get(agentId).getColor());
                        if (boxToMoveId != null) {
                            moveRealObject(agentId, currentAgentCoordinate, nextAgentCoordinate);
                            moveRealObject(boxToMoveId, expectedBoxCoordinate, currentAgentCoordinate);
                        }
                    }
                }
            } else {
                // TODO Conflict handling or not possible move handling
                hasError = true;
            }

        }
        return hasError;
    }

    public static void checkDeadEnd(String agentId, Coordinate currentAgentCoor, Coordinate nextCoor){

        // Move In
        if (State.busyDeadEndIndexByCoordinate.containsKey(nextCoor)) {
            Integer index = State.busyDeadEndIndexByCoordinate.get(nextCoor);
            if (State.busyDeadEndOccupancy.get(index).isEmpty() || !State.busyDeadEndOccupancy.get(index).contains(agentId)) {
                State.busyDeadEndOccupancy.get(index).add(agentId);
            }
        }

        // Move Out
        else if (State.busyDeadEndIndexByCoordinate.containsKey(currentAgentCoor)) { // if previous state was in dead end and next one isn't
            Integer index = State.busyDeadEndIndexByCoordinate.get(currentAgentCoor);
            State.busyDeadEndOccupancy.get(index).remove(agentId);
            Agent agent = (Agent) State.realBoardObjectsById.get(agentId);
            if(agent.moveToCornerCaseGoal) agent.moveToCornerCaseGoal = false;
        }
    }

    /**
     * METHODS
     **/

    public ArrayList<State> getExpandedStates() {
        ArrayList<State> expandedStates = new ArrayList<>(Command.EVERY.length);
        for (Command c : Command.EVERY) {
            // Determine applicability of action
            Coordinate currentAgentCoordinate = localCoordinateById.get(agentId);
            Coordinate nextAgentCoordinate = new Coordinate(
                    currentAgentCoordinate.getRow() + dirToRowChange(c.dir1),
                    currentAgentCoordinate.getColumn() + dirToColChange(c.dir1));

            if (c.actionType == Command.Type.Move) {
                // Check if there's a wall or box on the cell to which the agent is moving
                if (cellIsFree(nextAgentCoordinate)) {
                    State n = this.childState();
                    n.action = c;
                    moveLocalObject(n, agentId, currentAgentCoordinate, nextAgentCoordinate);
                    expandedStates.add(n);
                }

            } else if (c.actionType == Command.Type.Push) { // Agent takes the place of the box and box move toward dir2
                String boxToMoveId = boxAt(nextAgentCoordinate, realBoardObjectsById.get(agentId).getColor());
                if (boxToMoveId != null) {
                    Coordinate nextBoxCoordinate = new Coordinate(
                            nextAgentCoordinate.getRow() + dirToRowChange(c.dir2),
                            nextAgentCoordinate.getColumn() + dirToColChange(c.dir2));
                    // .. and that new cell of box is free
                    if (cellIsFree(nextBoxCoordinate)) {
                        State n = this.childState();
                        n.action = c;
                        moveLocalObject(n, boxToMoveId, nextAgentCoordinate, nextBoxCoordinate);
                        moveLocalObject(n, agentId, currentAgentCoordinate, nextAgentCoordinate);
                        expandedStates.add(n);
                    }
                }
            } else if (c.actionType == Command.Type.Pull) { // Box takes the place of the agent and agent move toward dir1
                // Cell is free where agent is going
                if (cellIsFree(nextAgentCoordinate)) {
                    Coordinate expectedBoxCoordinate = new Coordinate(
                            currentAgentCoordinate.getRow() + dirToRowChange(c.dir2),
                            currentAgentCoordinate.getColumn() + dirToColChange(c.dir2));
                    // .. and there's a box in "dir2" of the agent
                    String boxToMoveId = boxAt(expectedBoxCoordinate, realBoardObjectsById.get(agentId).getColor());
                    if (boxToMoveId != null) {
                        State n = this.childState();
                        n.action = c;
                        moveLocalObject(n, agentId, currentAgentCoordinate, nextAgentCoordinate);
                        moveLocalObject(n, boxToMoveId, expectedBoxCoordinate, currentAgentCoordinate);
                        expandedStates.add(n);
                    }
                }
            }
        }

        return expandedStates;
    }

    private String boxAt(Coordinate expectedBoxCoordinate, String agentcolor) {
        String objectId = localIdByCoordinate.get(expectedBoxCoordinate);
        if (objectId != null && 'A' <= objectId.charAt(0) && objectId.charAt(0) <= 'Z') {
            if (realBoardObjectsById.get(objectId).getColor().equals(agentcolor)) {
                return objectId;
            }
        }
        return null;
    }

    private boolean cellIsFree(Coordinate coordinate) {
        return State.wallByCoordinate.get(coordinate) == null
                && localIdByCoordinate.get(coordinate) == null;
    }

    public static boolean cellIsFreeFromWall(Coordinate coordinate) {
        return State.wallByCoordinate.get(coordinate) == null
                && coordinate.getColumn() <= MAX_COL
                && coordinate.getRow() <= MAX_ROW;
    }

    private State childState() {
        State copy = new State(this);
        for (Coordinate key : this.localIdByCoordinate.keySet()) {
            copy.localIdByCoordinate.put(key, this.localIdByCoordinate.get(key));
        }
        for (String key : this.localCoordinateById.keySet()) {
            copy.localCoordinateById.put(key, this.localCoordinateById.get(key));
        }
        return copy;
    }

    private void moveLocalObject(State nextState, String objectId, Coordinate currentCoordinate, Coordinate nextCoordinate) {
        nextState.localIdByCoordinate.put(nextCoordinate, objectId);
        nextState.localIdByCoordinate.remove(currentCoordinate, objectId);
        nextState.localCoordinateById.replace(objectId, currentCoordinate, nextCoordinate);
    }

    private static void moveRealObject(String objectId, Coordinate currentCoordinate, Coordinate nextCoordinate) {
        realIdByCoordinate.put(nextCoordinate, objectId);
        realIdByCoordinate.remove(currentCoordinate, objectId);
        realCoordinateById.replace(objectId, currentCoordinate, nextCoordinate);
    }

    private static String realBoxAt(Coordinate expectedBoxCoordinate, String agentcolor) {
        String objectId = realIdByCoordinate.get(expectedBoxCoordinate);
        if (objectId != null && 'A' <= objectId.charAt(0) && objectId.charAt(0) <= 'Z') {
            if (realBoardObjectsById.get(objectId).getColor().equals(agentcolor)) {
                return objectId;
            }
        }
        return null;
    }

    public ArrayList<State> extractPlan() {
        ArrayList<State> plan = new ArrayList<>();
        State n = this;
        while (!n.isInitialState()) {
            plan.add(n);
            n = n.parent;
        }
        Collections.reverse(plan);
        return plan;
    }


    /**
     * OVERRIDES
     */
    @Override
    public int hashCode() {
        Coordinate currentAgentCoordinate = localCoordinateById.get(agentId);

        if (this._hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + currentAgentCoordinate.getColumn();
            result = prime * result + currentAgentCoordinate.getRow();
            result = (prime * result) + Arrays.deepHashCode(localIdByCoordinate.keySet().toArray());
            result = prime * result + Arrays.deepHashCode(localCoordinateById.keySet().toArray());
            result = prime * result + Arrays.deepHashCode(State.wallByCoordinate.keySet().toArray());
            this._hash = result;
        }
        return this._hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;

        State other = (State) obj;
        Coordinate agentCoordinate = this.localCoordinateById.get(this.agentId);
        Coordinate otherAgentCoordinate = other.localCoordinateById.get(other.agentId);

        if (!agentCoordinate.equals(otherAgentCoordinate))
            return false;
        if (!(this.localCoordinateById.equals(other.localCoordinateById)))
            return false;
        if (!(this.localIdByCoordinate.equals(other.localIdByCoordinate)))
            return false;
        if (!this.agentId.equals(other.agentId))
            return false;
        if (this.boxId != null && !this.boxId.equals(other.boxId))
            return false;
        if (this.destination != null && !this.destination.equals(other.destination))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return getStringMap(localIdByCoordinate);
    }

    public static String getRealMapString() {
        return getStringMap(realIdByCoordinate);
    }

    private static String getStringMap(HashMap<Coordinate, String> idByCoordinateMap) {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < MAX_ROW; row++) {
            for (int column = 0; column < MAX_COL; column++) {
                Coordinate coord = new Coordinate(row, column);
                String objectId = idByCoordinateMap.get(coord);
                if (State.wallByCoordinate.get(coord) != null) {
                    s.append("+");
                } else if (objectId != null) {
                    s.append(objectId.charAt(0));
                } else {
                    s.append(" ");
                }
            }
            s.append("\n");
        }
        return s.toString();
    }

}