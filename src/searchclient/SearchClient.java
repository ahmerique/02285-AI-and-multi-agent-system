package src.searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Array;
import java.util.*;

import static java.lang.Character.toLowerCase;

// TODO UPDATE WITH STATE CHANGES
public class SearchClient {
    public State initialState;
    public PriorityQueue<Goal> goalQueue;
    public ArrayList<Agent> agentList;
    public HashMap<Agent, ArrayList<State>> planByAgent = new HashMap<>();
    public HashMap<Agent, State> latestStateMap = new HashMap<>();
    public String[] latestServerOutput;

    public SearchClient(BufferedReader serverMessages) throws Exception {

        System.err.println("Begin reading from server");
        readMapFromServer(serverMessages);

        // Preprocess data
        System.err.println("Begin preprocessing of the map");
        goalQueue = preprocessMap();

        ////////// TO GET ORDERED GOAL USE .poll() function and not iteration

        while (!goalQueue.isEmpty()) {
            System.out.println(goalQueue.poll());
        }

        //Match Boxes and Goals
        matchGoalsAndBoxes();

        // TODO REPLACE BY CHOSEN STRATEGY
        Strategy strategy = new Strategy.StrategyBFS();

        /*
        ArrayList<State> solution;
        try {
            solution = Search(strategy, agentList.get(0), problemType.COMPLETE); // TODO REPLACE BY REAL
        } catch (OutOfMemoryError ex) {
            System.err.println("Maximum memory usage exceeded.");
            solution = null;
        }

        if (solution == null) {
            System.err.println(strategy.searchStatus());
            System.err.println("Unable to solve level.");
            System.exit(0);
        } else {
            System.err.println("\nSummary for " + strategy.toString());
            System.err.println("Found solution of length " + solution.size());
            System.err.println(strategy.searchStatus());

            for (State n : solution) {
                String act = n.action.toString();
                System.out.println(act);
                String response = serverMessages.readLine();
                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                    System.err.format("%s was attempted in \n%s\n", act, n.toString());
                    break;
                }
            }
        }
        */

        //  Example of working algorithm
        boolean finished = false;
        while (!finished) {
            for (Agent agent : agentList) {
                agent.updateGoal(initialState, goalQueue);
                if (agent.getCurrentGoal() != null) {
                    System.err.println("Agent " + agent.getId() + " finding solution for goal " + (agent.getCurrentGoal().getId()));
                    ArrayList<State> plan = Search(strategy, agent, problemType.COMPLETE);
                    if (plan != null) {
                        // TODO maybe if change plan to help someone, should do something with it
                        ArrayList<State> previousPlan = planByAgent.replace(agent, plan);
                    } else {
                        System.err.println("Solution could not be found");
                    }
                }
            }
            //Execute solutions as long as possible
            boolean cont = true;
            while (cont) {
                cont = sendNextStepToServer(serverMessages);
                boolean status = State.updateStaticMap(latestStateMap, latestServerOutput);
                /*
                currentState.printState();
                if (!status) {
                    currentState.printState();
                    System.exit(0);
                }
                */
            }

            /** TODO Error handling
            boolean error = false;
            for (int i = 0; i < latestServerOutput.length; i++) {
                if (latestServerOutput[i] != null && latestServerOutput[i].equals("false")) {
                    error = true;
                    agentErrorState[i] = true;
                    Agent failAgent = currentState.getAgentById(Integer.toString(i).charAt(0));
                    if (!currentState.agents.get(i).isQuarantined()) {
                        System.err.println("Agent number " + failAgent.getId() + " is requesting clear");
                        failAgent.requestClear(currentState);
                    }
                } else {
                    agentErrorState[i] = false;
                    System.err.println("Agent number " + currentState.agents.get(i).getId() + " is done with no error");
                    for (Agent a : currentState.getAgents()) {
                        if (a.isQuarantined() && a.getQuarantinedBy().getId() == currentState.agents.get(i).getId()) {
                            a.setQuarantined(false);
                        }
                    }
                }
            }
            if (error) {
                while (update()) {
                    boolean status = currentState.changeState(latestActionArray, latestServerOutput, this);
                    if (!status) {
                        currentState.printState();
                        System.exit(0);
                    }
                }
            }
             */

            boolean agentsDone = true;
            for (Agent a : agentList) {
                if (a.getCurrentGoal() != null) {
                    agentsDone = false;
                    break;
                }
            }

            if (agentsDone && goalQueue.isEmpty()) {
                finished = true;
            }
        }

    }

    private void readMapFromServer(BufferedReader serverMessages) throws Exception {

        boolean isMA; //SA if 1 at the end of the file reading, else MA
        List<String> serverMessageList = new ArrayList<>(); //All lines of the Initial level
        int max_col = 0; //Maximum column reached
        int row = 0; //Iteration variable
        int countpart = 0; //Part of the initial file read
        HashMap<String, String> colors = new HashMap<>(); //All colors

        String line = serverMessages.readLine();

        while (!line.equals("#end")) {

            //System.err.println(line);/////////////////////////////////////////////////////////////

            if (line.charAt(0) == '#') {
                countpart += 1;
            }

            line = serverMessages.readLine();

            switch (countpart) {
                case 1://Domain
                    //No action needed. Name of the domain can be saved here.
                    break;

                case 2://Level Name
                    isMA = toLowerCase(line.charAt(0)) != 's';
                    //No action needed. Name of the level can be saved here.
                    break;

                case 3://Colors
                    while (line.charAt(0) != '#') {
                        String[] str = line.split(": ");
                        String[] objects = str[1].split(", ");
                        //We add each color to the colors dictionnary
                        for (String object : objects) {
                            colors.put(object, str[0]); //Considering that objects are only initialized once
                        }
                        line = serverMessages.readLine();
                    }
                    break;

                case 4://Initial state
                    //Features all the information regarding the level
                    while (line.charAt(0) != '#') {
                        serverMessageList.add(line);
                        max_col = Math.max(max_col, line.length());
                        line = serverMessages.readLine();
                    }

                    //Initialize static attributes of State
                    State.MAX_COL = max_col;
                    State.MAX_ROW = serverMessageList.size();
                    break;

                case 5://Goal state
                    //Iteration over the rows of the initial state while reading the final state in parallel
                    for (int i = 0; i < serverMessageList.size(); i++) {
                        String rowline = serverMessageList.get(i);

                        for (int j = 0; j < rowline.length(); j++) {

                            char chr = rowline.charAt(j);
                            if (chr == '+') { // Wall
                                State.wallByCoordinate.put(new Coordinate(i, j), true);
                            } else if ('0' <= chr && chr <= '9') { // Agent
                                Agent newAgent = (Agent) State.setNewStateObject(i, j, "AGENT", chr, colors.get(Character.toString(chr)));
                                agentList.add(newAgent);
                                planByAgent.put(newAgent, new ArrayList<>());
                            } else if ('A' <= chr && chr <= 'Z') { // Box
                                State.setNewStateObject(i, j, "BOX", chr, colors.get(Character.toString(chr)));
                            } else if (chr == ' ') { // Free space
                                // Nothing
                            } else {
                                System.exit(1);
                            }

                            char chrGoal = line.charAt(j);
                            //TODO remove if goals are added as Box objects parameters
                            if ('A' <= chrGoal && chrGoal <= 'Z') { // Goal
                                State.setNewStateObject(i, j, "GOAL", chrGoal, colors.get(Character.toString(chrGoal)));
                            }
                        }
                        line = serverMessages.readLine();
                    }
                    break;
            }

        }

        agentList.sort(Comparator.comparingInt(a -> Integer.parseInt(a.getId())));
        System.err.println("----------- MAX_ROW = " + Integer.toString(State.MAX_ROW));
        System.err.println("----------- MAX_COL = " + Integer.toString(State.MAX_COL));
        System.err.println("Done initializing");
    }

    private PriorityQueue<Goal> preprocessMap() {
        int MAX_GOAL_PRIORITY = 1000; // for test 9;
        int NORMAL_GOAL_PRIORITY = 100; // for test 1
        int LOW_GOAL_PRIORITY = 0;

        HashSet<Coordinate> deadEndCaseSet = new HashSet<>();
        HashSet<Coordinate> corridorCaseSet = new HashSet<>();
        HashSet<Coordinate> cornerCaseSet = new HashSet<>();

        // create degree map (number of non-wall case in the vicinity)
        for (int i = 0; i < State.MAX_ROW; i++) {
            for (int j = 0; j < State.MAX_COL; j++) {
                Coordinate tempCoordinate = new Coordinate(i, j);
                if (State.wallByCoordinate.get(tempCoordinate) == null) {
                    int tempDegree = 0;
                    int cornerOrCorridor = 0; // if pair corridor else corner
                    int index = 0;
                    for (Coordinate neighbor : tempCoordinate.get4VicinityCoordinates()) {
                        if (State.wallByCoordinate.get(neighbor) == null) {
                            tempDegree++;
                            cornerOrCorridor += index;
                        }
                        index++;
                    }

                    // We want to fill goals that are in dead end first
                    if (tempDegree == 1) {
                        deadEndCaseSet.add(tempCoordinate);
                        State.degreeMap.put(tempCoordinate, MAX_GOAL_PRIORITY);
                    } else if (tempDegree == 2) {
                        // we want to fill corridor last to prevent clogs
                        if (cornerOrCorridor % 2 == 0) {
                            corridorCaseSet.add(tempCoordinate);
                            State.degreeMap.put(tempCoordinate, LOW_GOAL_PRIORITY);
                        }

                        // A corner is a corridor if linked to one but else it's a free cell
                        else {
                            cornerCaseSet.add(tempCoordinate);
                            State.degreeMap.put(tempCoordinate, NORMAL_GOAL_PRIORITY);
                        }
                    } else { // Normal free cell
                        State.degreeMap.put(tempCoordinate, NORMAL_GOAL_PRIORITY);
                    }
                }
            }
        }

        // find deadEnd
        for (Coordinate deadEnd : deadEndCaseSet) {

            boolean isCorridor = true;
            boolean hasGoal = State.goalByCoordinate.containsKey(deadEnd);

            Coordinate prevCoor = deadEnd;
            Coordinate currentCoor = deadEnd;
            int goalPriorityValue = MAX_GOAL_PRIORITY;
            ArrayList<Coordinate> tempList = new ArrayList<>();
            tempList.add(deadEnd);

            // find full deadend
            while (isCorridor) {
                goalPriorityValue--;

                for (Coordinate tempCoor : currentCoor.get4VicinityCoordinates()) { //beware if tempCoor is the same for all the modification

                    if (State.wallByCoordinate.get(tempCoor) == null
                            && !tempCoor.equals(prevCoor)) {

                        if (State.goalByCoordinate.containsKey(tempCoor)) hasGoal = true;

                        if (corridorCaseSet.contains(tempCoor)) {
                            State.degreeMap.put(tempCoor, goalPriorityValue);
                            corridorCaseSet.remove(tempCoor);
                            tempList.add(tempCoor);

                        } else if (cornerCaseSet.contains(tempCoor)) {
                            State.degreeMap.put(tempCoor, goalPriorityValue);
                            cornerCaseSet.remove(tempCoor);
                            tempList.add(tempCoor);

                        } else {// if not a corridor anymore, the end of a dead end is a low priority cell that shouldn't be closed too quickly
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            isCorridor = false;
                        }

                        prevCoor = new Coordinate(currentCoor);
                        currentCoor = new Coordinate(tempCoor);
                        break;
                    }
                }
            }

            // Action on full deadend
            if (hasGoal) {
                State.busyDeadEndList.add(tempList);
            } else {
                State.emptyDeadEndList.add(tempList);

                /* // If don't have a goal in the deadend we consider it as normal cells?
                for (Coordinate tempCoor : tempList) {
                    State.degreeMap.put(tempCoor, NORMAL_GOAL_PRIORITY);
                }
                */
            }
        }

        // link corridor to corner, add extremities, fill corridorList

        while (!corridorCaseSet.isEmpty()) {

            Coordinate corridor = corridorCaseSet.iterator().next();
            corridorCaseSet.remove(corridor);

            ArrayList<Coordinate> tempCorridorList = new ArrayList<>();
            tempCorridorList.add(corridor);

            boolean isCorridorRight = true;
            boolean isCorridorLeft = true;

            Coordinate leftCoor = null;
            Coordinate rightCoor = null;

            // first iteration to get left and right of corridor
            for (Coordinate tempCoor : corridor.get4VicinityCoordinates()) {
                if (State.wallByCoordinate.get(tempCoor) == null) {
                    if (leftCoor == null) {
                        leftCoor = tempCoor;
                        if (corridorCaseSet.contains(tempCoor)) {
                            corridorCaseSet.remove(tempCoor);
                        } else if (cornerCaseSet.contains(tempCoor)) { // a corner is a normal cell except if part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            isCorridorLeft = false;
                        }
                        //add to list
                        tempCorridorList.add(0, tempCoor);

                    } else {
                        rightCoor = tempCoor;
                        if (corridorCaseSet.contains(tempCoor)) {
                            corridorCaseSet.remove(tempCoor);
                        } else if (cornerCaseSet.contains(tempCoor)) { // a corner is a normal cell except if part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            isCorridorRight = false;
                        }
                        tempCorridorList.add(tempCoor);
                    }
                }
            }

            // fill normally the right and left size of the list

            //LEFT
            Coordinate prevCoor = corridor;
            Coordinate currentCoor = leftCoor;

            while (isCorridorLeft) {

                for (Coordinate tempCoor : currentCoor.get4VicinityCoordinates()) {
                    if (!tempCoor.equals(prevCoor) && State.wallByCoordinate.get(tempCoor) == null) {

                        if (corridorCaseSet.contains(tempCoor)) {
                            corridorCaseSet.remove(tempCoor);
                        } else if (cornerCaseSet.contains(tempCoor)) { // a corner is a normal cell except if part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor anymore, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY); // don't break so that one case corridor can modify its entrance and exit
                            isCorridorLeft = false;
                        }

                        //add to list
                        tempCorridorList.add(0, tempCoor);

                        prevCoor = currentCoor;
                        currentCoor = tempCoor;
                        break; //Only one way out in a corridor
                    }
                }
            }

            //RIGHT
            prevCoor = corridor;
            currentCoor = rightCoor;

            while (isCorridorRight) {

                for (Coordinate tempCoor : currentCoor.get4VicinityCoordinates()) {
                    if (!tempCoor.equals(prevCoor) && State.wallByCoordinate.get(tempCoor) == null) {

                        if (corridorCaseSet.contains(tempCoor)) {
                            corridorCaseSet.remove(tempCoor);
                        } else if (cornerCaseSet.contains(tempCoor)) { // a corner is a normal cell except if part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor anymore, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, LOW_GOAL_PRIORITY);
                            isCorridorRight = false;
                        }

                        //add to list
                        tempCorridorList.add(0, tempCoor);

                        prevCoor = currentCoor;
                        currentCoor = tempCoor;
                        break;
                    }
                }
            }

            // Add to list of corridorList
            State.corridorList.add(tempCorridorList);
        }

        // TODO classify normal goals between them (for instance goals that are stuck between each other)

        /*
        // Test things up
        HashMap<Coordinate, String> testMap = new HashMap<>();
        State.degreeMap.forEach((k, v) -> testMap.put(k, Integer.toString(v)));
        State testState = new State(testMap);
        System.err.println(testState);
        System.err.println(corridorList);
        */

        PriorityQueue<Goal> goalPriorityQueue = new PriorityQueue<>(State.goalWithCoordinate.keySet().size(), goalComparator);
        for (Goal tempGoal : State.goalWithCoordinate.keySet()) {
            tempGoal.setPriority(State.degreeMap.get(tempGoal.getCoordinate()));
            goalPriorityQueue.add(tempGoal);
        }

        System.err.println("Finished preprocessing of the map");

        return goalPriorityQueue;
    }

    private void matchGoalsAndBoxes() {
		/* TODO
		Inefficient but temporary method.
		Needs to be made better to match goals and boxes according to heuristics.

		for goal in setGoals:
			for object in setBoxes:
				if object is Box and object.letter == goal.letter:

		*/

        Set<Goal> setGoals = State.goalWithCoordinate.keySet();
        Collection<BoardObject> setBoxes = State.realBoardObjectsById.values();
        String colorToMatch;

        for (Goal goal : setGoals) {
            char letterToMatch = goal.getLetter();

            for (BoardObject object : setBoxes) {
                if (object instanceof Box) {
                    // NOt a good way to call a child class method
                    if (((Box) object).getLetter() == letterToMatch) {
                        ((Box) object).setBoxGoal(goal);
                        setBoxes.remove(object);
                        break;
                    }
                }
            }
        }
    }

    public static Comparator<Goal> goalComparator = new Comparator<Goal>() {
        @Override
        public int compare(Goal o1, Goal o2) {
            return o2.getPriority() - o1.getPriority();
        }
    };

    public ArrayList<State> Search(Strategy strategy, Agent agent, problemType typeOfProblem) {
        System.err.format("Search starting with strategy %s.\n", strategy.toString());

        // TODO DEFINE WHAT IS INITIAL STATE, it's here we can choose to use easier problem or full problem, for now it takes the board as it is.

        // TODO Decide if the box should store a goal or the goal store a box.
        State firstState = new State(State.realCoordinateById, State.realIdByCoordinate, agent.getId(), agent.getCurrentGoal().getAttachedBox().getId());

        strategy.addToFrontier(firstState);

        int iterations = 0;
        while (true) {
            if (iterations == 1000) {
                System.err.println(strategy.searchStatus());
                iterations = 0;
            }

            if (strategy.frontierIsEmpty()) {
                return null;
            }

            State leafState = strategy.getAndRemoveLeaf();

            if (leafState.isSubGoalState()) {
                return leafState.extractPlan();
            }

            strategy.addToExplored(leafState);
            for (State n : leafState.getExpandedStates()) { // The list of expanded states is shuffled randomly; see State.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }
            iterations++;
        }
    }

    public boolean sendNextStepToServer(BufferedReader serverMessages) throws IOException {

        int noAct = 0; // if no more action to do, get new goals
        StringBuilder jointAction = new StringBuilder("[");

        // Concatenate action for each agent (agentList is ordered by name)
        for (Agent agent : agentList) {

            State next = null;
            String actionString;

            // get next State and remove it from plan
            if (planByAgent.get(agent).size() > 0) {
                next = planByAgent.get(agent).get(0);
                this.latestStateMap.put(agent, next);
                planByAgent.get(agent).remove(0);

                if (next.action == null) { // Deliberate NoOp
                    actionString = "NoOp";
                } else {
                    actionString = next.action.toString();
                }
            } else {
                noAct++;
                actionString = "NoOp";
            }

            jointAction.append(actionString);
            if (!agent.equals(agentList.get(agentList.size() - 1))) {
                jointAction.append(",");
            }
        }
        jointAction.append("]");

        if (noAct == agentList.size()) return false;

        System.err.println("Sending command: " + jointAction + "\n");

        // Place message in buffer
        System.out.println(jointAction);

        // Flush buffer
        System.out.flush();

        // Disregard these for now, but read or the server stalls when its output buffer gets filled!
        String serverAnswer = serverMessages.readLine();
        System.err.println(serverAnswer);

        if (serverAnswer == null) return false;

        String strip = serverAnswer.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("    \\s", "");
        String[] returnVals = strip.split(",");
        this.latestServerOutput = returnVals;
        for (String returnVal : returnVals) {
            if (returnVal.equals("false")) {
                return false;
            }
        }

        return true;
    }

    public static void main(String[] args) throws Exception {
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing.");
        System.out.println("Baguettes' engine");

        // Read level and create the initial state of the problem
        SearchClient client = new SearchClient(serverMessages);
    }
}

enum problemType {
    COMPLETE,
    RELAXED
}