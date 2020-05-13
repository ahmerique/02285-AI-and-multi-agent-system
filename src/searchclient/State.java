package searchclient;

import java.util.*;

import static searchclient.Command.dirToColChange;
import static searchclient.Command.dirToRowChange;

public class State {

	// STATIC ATTRIBUTES
	public static HashMap<Goal, Coordinate> goalWithCoordinate;
	public static HashMap<String, BoardObject> realBoardObjectsById; // Agent and Boxes
	public static HashMap<Coordinate, Goal> goalByCoordinate; 
	public static HashMap<Coordinate, BoardObject> realBoardObjectByCoordinate; // Agent and Boxes
	public static HashMap<Coordinate, Boolean> wallByCoordinate;

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

	public HashMap<String, Coordinate> getLocalCoordinateById() {
		return this.localCoordinateById;
	}

	//Method to set a new object (agent:0, box:1, goal:2) in the State
	public static boolean setNewStateObject(int row, int column, int type, char id, String color) {
		
		Coordinate coord = new Coordinate(row,col);
		
		switch(type){
			case 0://Agent
				finalId = Character.toString(id);
				Agent newAgent = new Agent(finalId, color);
				State.realBoardObjectsById.put(newAgent, coord);
				State.realBoardObjectByCoordinate.put(coord, newAgent);

			case 1://Box
				finalId = generateUniqueId(id);
				Box newBox = new Box(finalId, color, id);
				State.realBoardObjectsById.put(newBox, coord);
				State.realBoardObjectByCoordinate.put(coord, newBox);

			case 2://Goal
				finalId = generateUniqueId(id);
				Goal newGoal = new Goal(finalId, color, coord, id);
				State.goalWithCoordinate.put(newGoal, coord);
				State.goalByCoordinate.put(coord, newGoal);

		}
	}

	public static boolean matchGoalsAndBoxes(){
		/* TODO
		Inefficient but temporary method.
		Needs to be made better to match goals and boxes according to heuristics.

		for goal in setGoals:
			for object in setBoxes:
				if object is Box and object.letter == goal.letter:
					
		*/

		Set setGoals = State.goalWithCoordinate.keySet();
		Set setBoxes = State.goalWithCoordinate.values();
		String colorToMatch = new String();
		
		for(Goal goal : setGoals){
		letterToMatch = goal.getLetter();
			for(BoardObject object : setBoxes){
				if((object.getClass().getSimpleName() == "Box") && (object.getLetter() == letterToMatch))  {
					object.setCurrentGoal(goal);
					setBoxes.remove(object);
					break;
				}   
			}
		}


	}

	private String generateUniqueId(char id){
		String stringId = Character.toString(id);
		int iterator = 0;

		//Process until id is new
		while(realBoardObjectsById.containsKey(stringId + Integer.toString(iterator))){
			iterator += 1;
		}

		return(stringId + Integer.toString(iterator));
	}


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
		return State.wallByCoordinate.get(coordinate) == null
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
		if (!this.boxId.equals(other.boxId) || !this.agentId.equals(other.agentId))
			return false;

		return true;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int row = 0; row < MAX_ROW; row++) {
			for (int column = 0; column < MAX_COL; column++) {
				Coordinate coord = new Coordinate(row, column);
				String objectId = localIdByCoordinate.get(coord);
				if (State.wallByCoordinate.get(coord) == null) {
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