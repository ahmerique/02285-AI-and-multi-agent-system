package searchclient;

import java.util.*;

import static searchclient.Command.dirToColChange;
import static searchclient.Command.dirToRowChange;

public class State {

    // STATIC ATTRIBUTES
    public static HashMap<String, BoardObject> realBoardObjectsById;
    public static HashMap<Coordinate, Character> goalByCoordinate;
    public static HashMap<Coordinate, BoardObject> realBoardObjectByCoordinate;

    public static boolean[][] walls;
    public static int MAX_ROW;
    public static int MAX_COL;

    // LOCAL ATTRIBUTES
    private HashMap<String, Coordinate> localCoordinateById;
    private HashMap<Coordinate, String> localIdByCoordinate;

    private String agentId;
    private String boxId;

    private State parent;
    public Command action;

    // Heuristic g
    private int g;
    private static final Random RNG = new Random(1);
    private int _hash = 0;


    public State(State parent) {
        this.parent = parent;
        if (parent == null) {
            this.g = 0;
        } else {
            this.g = parent.g() + 1;
        }
    }

    //
    public State(HashMap<String, Coordinate> localCoordinateById, HashMap<Coordinate, String> localIdByCoordinate, String agentId, String boxId) throws AssertionError {
        this.localCoordinateById = localCoordinateById;
        this.localIdByCoordinate = localIdByCoordinate;
        this.agentId = agentId;
        this.boxId = boxId;
        this.g = 0;

        if (agentId == null) throw new AssertionError("MUST have an agentId");
    }

    public int g() {
        return this.g;
    }

    public boolean isInitialState() {
        return this.parent == null;
    }

    public boolean isSubgGoalState() {
        Box boxObject = (Box) realBoardObjectsById.get(boxId);
        return boxObject.getCurrentGoal().getCoordinate() == localCoordinateById.get(boxId);
    }

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
                String boxToMoveId = boxAt(nextAgentCoordinate);
                if (boxId != null) {
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
                    String boxToMoveId = boxAt(expectedBoxCoordinate);
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

    /**
     * TODO Add color management
     * If not the same color return null. So it won't be able to move that way.
     * Might not be necessary if we are in a relaxed problem
     */
    private String boxAt(Coordinate expectedBoxCoordinate) {
        String objectId = localIdByCoordinate.get(expectedBoxCoordinate);
        if (objectId != null && 'A' <= objectId.charAt(0) && objectId.charAt(0) <= 'Z') {
            return objectId;
        } else {
            return null;
        }
    }

    private boolean cellIsFree(Coordinate coordinate) {
        return !State.walls[coordinate.getRow()][coordinate.getColumn()]
                && localIdByCoordinate.get(coordinate) == null;
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
            result = prime * result + Arrays.deepHashCode(State.walls);
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
        if (!this.boxId.equals(other.boxId) || !this.agentId.equals(other.agentId))
            return false;

        return Arrays.deepEquals(State.walls, State.walls);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < MAX_ROW; row++) {
            for (int column = 0; column < MAX_COL; column++) {
                Coordinate coord = new Coordinate(row, column);
                String objectId = localIdByCoordinate.get(coord);
                if (State.walls[row][column]) {
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