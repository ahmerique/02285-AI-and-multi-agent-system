package src.searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static java.lang.Character.toLowerCase;

// TODO UPDATE WITH STATE CHANGES
public class SearchClient {
    public State initialState;
    public ArrayList<Goal> highGoalQueue = new ArrayList<>();
    public ArrayList<Goal> normalGoalQueue = new ArrayList<>();
    public ArrayList<Goal> lowGoalQueue = new ArrayList<>();
    public ArrayList<Agent> agentList = new ArrayList<>();
    public ArrayList<Agent> agentListByPriority = new ArrayList<>();
    public HashMap<Agent, ArrayList<State>> planByAgent = new HashMap<>();
    public State[] latestStateArray;
    public String[] latestServerOutput;
    public HashMap<String, String> colors = new HashMap<>(); //All colors
    public ArrayList<Box> setBoxes = new ArrayList<>(); //All boxes

    public SearchClient(BufferedReader serverMessages) throws Exception {

        System.err.println("Begin reading from server");
        readMapFromServer(serverMessages);
        latestStateArray = new State[agentList.size()];

        for (int i = 0; i < latestStateArray.length; i++) {
            latestStateArray[i] = new State(State.realCoordinateById, State.realIdByCoordinate, agentList.get(i).getId());
        }

        latestServerOutput = new String[agentList.size()];

        // Preprocess data
        System.err.println("Begin preprocessing of the map");
        preprocessMap();

        ArrayList<BoardObject> setObjects = new ArrayList<>(State.realBoardObjectsById.values());
        Iterator<BoardObject> itr = setObjects.iterator();
        while (itr.hasNext()) {
            BoardObject b = (BoardObject) itr.next();
            if (b instanceof Box) {
                setBoxes.add(((Box) b));
            }
        }

        //Match Boxes and Goals
        matchGoalsAndBoxes();

        System.err.println("Done initializing");

        //----------- BEGIN MAIN LOOP -------------

        boolean finished = false;
        while (!finished) {

            //Bidding on goals
            matchAgentsAndGoals();

            // Check if all goals priority are inferior to the first element of goalQueue (For instance if
            // only one agent can fill the first case of a deadEnd)
            // Questions the goal ordering, should we first fill every first case of a deadend or fill a deadend at a time
            for (Agent agent : agentList) {
                if (!agent.isWaiting && agent.getCurrentGoal() != null) {

                    // High priority goals has to be finished in order
                    if (agent.getCurrentGoal().getPriority() > State.NORMAL_GOAL_PRIORITY
                            && !highGoalQueue.isEmpty()
                            && agent.getCurrentGoal().getPriority() < highGoalQueue.get(0).getPriority()) {
                        System.err.println("--- Goal removed : " + agent.getCurrentGoal());
                        agent.requeueCurrentGoal(highGoalQueue);
                        continue;
                    }

                    // Low priority has to wait for all the other before beginning
                    if (agent.getCurrentGoal().getPriority() == State.LOW_GOAL_PRIORITY
                            && !normalGoalQueue.isEmpty()
                            && !highGoalQueue.isEmpty()) {
                        agent.requeueCurrentGoal(lowGoalQueue);
                        continue;
                    }
                }
            }

            // Sort by goal priority
            agentListByPriority.sort(AgentGoalComparator);

            int minLength = 0;
            for (Agent agent : agentListByPriority) {
                if (agent.getCurrentGoal() != null || agent.moveToCornerCaseGoal || agent.destinationGoal != null) {

                    // Strategy choice
                    //ArrayList<State> plan = Search(new Strategy.StrategyBFS(), agent, problemType.COMPLETE);
                    ArrayList<State> plan;
                    if (!agent.isWaiting) { // if is waiting, already has a plan
                        plan = Search(new Strategy.StrategyBestFirst(new Heuristic.AStar()), agent, problemType.COMPLETE);
                        //plan = Search(new Strategy.StrategyDFS(), agent, problemType.COMPLETE);
                    } else {
                        //System.err.println(agent.getId() + " already have a plan");
                        plan = planByAgent.get(agent);
                    }

                    if (plan != null && !agent.isWaiting) {

                        // add NoOP to let the agent who has priority finish first
                        int planSize = plan.size();
                        if (planSize <= minLength && planSize > 0) {
                            for (int i = 0; i < minLength - planSize + 1; i++) {
                                State noOpState = plan.get(0).getCopy();
                                noOpState.action = null;
                                plan.add(0, noOpState);
                            }
                        }

                        // TODO maybe if change plan to help someone, should do something with it
                        ArrayList<State> previousPlan = planByAgent.replace(agent, plan);
                        minLength = plan.size();
                    } else if (!agent.isWaiting) {
                        System.err.println("Solution could not be found");
                    }
                }
            }

            // Keep track of previous path of agent. TODO If there is a conflict, trackback  until no conflict or do something else
            for(Map.Entry<Agent,ArrayList<State>> pair : planByAgent.entrySet()){
                pair.getKey().plan = pair.getValue();
            }

            // Execute as long as possible
            boolean cont = true;
            while (cont) {
                // TODO When a box is removed from it's goal it has to go back into the queue list
                cont = sendNextStepToServer(serverMessages);
                boolean status = State.updateStaticMap(latestStateArray, latestServerOutput);
                /*
                currentState.printState();
                if (!status) {
                    currentState.printState();
                    System.exit(0);
                }
                

                if (!status) {
                    System.err.println("-----------------------");
                    for(Map.Entry<String,Coordinate> tuple : State.realCoordinateById.entrySet()) {
                        if(State.realBoardObjectsById.get(tuple.getKey()) instanceof Box){
                            System.err.println("--- Coordinates for " +  tuple.getKey() +  " = " + tuple.getValue() + " to go to goal at " + ((Box) State.realBoardObjectsById.get(tuple.getKey())).getBoxGoal().getCoordinate());
                        }
                    }
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
                Coordinate destinationGoal = State.agentGoalWithCoordinate.get(a.getId());
                if (a.getCurrentGoal() != null
                        || (destinationGoal != null && !State.realCoordinateById.get(a.getId()).equals(destinationGoal))) {
                    agentsDone = false;
                    break;
                }
            }

            //System.err.println("Agent Done" + agentsDone);

            if (agentsDone && highGoalQueue.isEmpty() && normalGoalQueue.isEmpty() && lowGoalQueue.isEmpty()) {

                //Verify that no goal is missing. This occurs if a box that was on its goal has been pushed.
                for (Box box : setBoxes) {
                    Goal goal = box.getBoxGoal();
                    if(!(State.realCoordinateById.get(box.getId()).equals(State.goalWithCoordinate.get(goal))) && goal != null){
                        if(!(highGoalQueue.contains(goal) || normalGoalQueue.contains(goal) || lowGoalQueue.contains(goal))){
                            
                            //System.err.println("--- Append goal : " + goal);
                            int priority = goal.getPriority();
                            if (priority < 150 && priority > 50) {
                                normalGoalQueue.add(goal);
                            } else if (priority <= 50) {
                                lowGoalQueue.add(goal);
                            } else {
                                highGoalQueue.add(goal);
                            }
                        }
                    }
                }

                if (highGoalQueue.isEmpty() && normalGoalQueue.isEmpty() && lowGoalQueue.isEmpty()) {
                    finished = true;
                }
            }
        }

    }

    private void readMapFromServer(BufferedReader serverMessages) throws Exception {

        boolean isMA; //SA if 1 at the end of the file reading, else MA
        List<String> serverMessageList = new ArrayList<>(); //All lines of the Initial level
        int max_col = 0; //Maximum column reached
        int row = 0; //Iteration variable
        int countpart = 0; //Part of the initial file read

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
                        String[] str = line.replaceAll("\\s+", "").split(":");
                        String[] objects = str[1].split(",");
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
                                agentListByPriority.add(newAgent);
                                planByAgent.put(newAgent, new ArrayList<>());
                            } else if ('A' <= chr && chr <= 'Z') { // Box
                                State.setNewStateObject(i, j, "BOX", chr, colors.get(Character.toString(chr)));
                            } else if (chr == ' ') { // Free space
                                // Nothing
                            } else {
                                System.exit(1);
                            }

                            char chrGoal = line.charAt(j);
                            if ('A' <= chrGoal && chrGoal <= 'Z') { // Goal
                                State.setNewStateObject(i, j, "GOAL", chrGoal, colors.get(Character.toString(chrGoal)));
                            } else if ('0' <= chrGoal && chrGoal <= '9') { // AgentGoal
                                State.agentGoalWithCoordinate.put(Character.toString(chrGoal), new Coordinate(i, j));
                                //System.err.println("----------- Destination = " + Character.toString(chrGoal) + " to " + new Coordinate(i,j));
                            }
                        }
                        line = serverMessages.readLine();
                    }
                    break;
            }

        }

        agentList.sort(Comparator.comparingInt(a -> Integer.parseInt(a.getId())));
        //System.err.println("----------- MAX_ROW = " + State.MAX_ROW);
        //System.err.println("----------- MAX_COL = " + State.MAX_COL);
    }

    private void preprocessMap() {

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
                        State.degreeMap.put(tempCoordinate, State.MAX_GOAL_PRIORITY);
                    } else if (tempDegree == 2) {
                        // we want to fill corridor last to prevent clogs
                        if (cornerOrCorridor % 2 == 0) {
                            corridorCaseSet.add(tempCoordinate);
                            State.degreeMap.put(tempCoordinate, State.LOW_GOAL_PRIORITY);
                        }

                        // A corner is a corridor if linked to one but else it's a free cell
                        else {
                            cornerCaseSet.add(tempCoordinate);
                            State.degreeMap.put(tempCoordinate, State.NORMAL_GOAL_PRIORITY);
                        }
                    } else { // Normal free cell
                        State.degreeMap.put(tempCoordinate, State.NORMAL_GOAL_PRIORITY);
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
            int goalPriorityValue = State.MAX_GOAL_PRIORITY;
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

                        } else if (cornerCaseSet.contains(tempCoor)) {
                            State.degreeMap.put(tempCoor, goalPriorityValue);
                            cornerCaseSet.remove(tempCoor);

                        } else {// if not a corridor anymore, the end of a dead end is a low priority cell that shouldn't be closed too quickly
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
                            isCorridor = false;
                        }

                        tempList.add(tempCoor); // exit is also part of the deadend
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
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
                            isCorridorLeft = false;
                        }
                        //add to list
                        tempCorridorList.add(0, tempCoor);

                    } else {
                        rightCoor = tempCoor;
                        if (corridorCaseSet.contains(tempCoor)) {
                            corridorCaseSet.remove(tempCoor);
                        } else if (cornerCaseSet.contains(tempCoor)) { // a corner is a normal cell except if part of a corridor
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
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
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor anymore, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY); // don't break so that one case corridor can modify its entrance and exit
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
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
                            cornerCaseSet.remove(tempCoor);
                        } else { // if not a corridor anymore, the end of a corridor is part of a corridor
                            State.degreeMap.put(tempCoor, State.LOW_GOAL_PRIORITY);
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

        // Goal ordering
        for (Goal tempGoal : State.goalWithCoordinate.keySet()) {
            tempGoal.setPriority(State.degreeMap.get(tempGoal.getCoordinate()));
            // check if box on goal
            String objectId = State.realIdByCoordinate.get(tempGoal.getCoordinate());
            if (objectId == null || objectId.charAt(0) != tempGoal.getLetter()) {
                if (tempGoal.getPriority() == State.NORMAL_GOAL_PRIORITY) {
                    normalGoalQueue.add(tempGoal);
                } else if (tempGoal.getPriority() == State.LOW_GOAL_PRIORITY) {
                    lowGoalQueue.add(tempGoal);
                } else {
                    highGoalQueue.add(tempGoal);
                }
            }
        }

        highGoalQueue.sort(goalComparator);

        // Create accessor and occupancy for corridor and deadEnd
        for (int i = 0; i < State.corridorList.size(); i++) {
            State.corridorOccupancy.add(new ArrayList<>());
            for (Coordinate tempCoor : State.corridorList.get(i)) {
                State.corridorIndexByCoordinate.put(tempCoor, i);
            }
        }

        for (int i = 0; i < State.busyDeadEndList.size(); i++) {
            State.busyDeadEndOccupancy.add(new ArrayList<>());
            for (Coordinate tempCoor : State.busyDeadEndList.get(i)) {
                State.busyDeadEndIndexByCoordinate.put(tempCoor, i);
            }
        }

        // Verify if any agent is inside a corridor or a deadEnd
        for (Agent agent : agentList) {
            Integer agentCorridor = State.corridorIndexByCoordinate.get(State.realCoordinateById.get(agent.getId()));
            Integer agentDeadEnd = State.busyDeadEndIndexByCoordinate.get(State.realCoordinateById.get(agent.getId()));
            if (agentCorridor != null) {
                State.corridorOccupancy.get(agentCorridor).add(agent.getId());
            } else if (agentDeadEnd != null) {
                State.busyDeadEndOccupancy.get(agentDeadEnd).add(agent.getId());
            }
        }
    }

    /**
     * Assigns to each box the best goal, depending on the chosen distance metric
     */
    private void matchGoalsAndBoxes() {

        Goal[] setGoals = State.goalWithCoordinate.keySet().toArray(new Goal[State.goalWithCoordinate.size()]);
        int i = 0;
        int j = 0;
        Coordinate coord1;
        Coordinate coord2;
        double score;

        //System.err.println("----------- LEN SETBOXES = " + setBoxes.size());
        //System.err.println("----------- LEN SETGOALS = " + setGoals.length);
        double[][] scores = new double[setBoxes.size()][setGoals.length];

        for (Goal goal : setGoals) {
            char letterToMatch = goal.getLetter();

            for (BoardObject object : setBoxes) {

                if (((Box) object).getLetter() == letterToMatch) {
                    coord1 = State.goalWithCoordinate.get(goal);
                    coord2 = State.realCoordinateById.get(object.getId());
                    //System.err.println("----------- Coord1 = " + coord1 + ", Coord2 = " + coord2);
                    score = Heuristic.pullDistance(coord1, coord2);
                    //System.err.println("----------- Pull Distance = " + object.getId() + " with " + goal.getId()+ " (" + (int)score + ")");
                    scores[j][i] = score;
                    //scores[j][i] = j;

                    //If the box is on the goal, we consider it as the best box ; all remaining boxes are set with a distance of 9998
                    if (score == 0 && j < (setBoxes.size() - 1)) {
                        for (int l = j + 1; l < setBoxes.size(); l++) {
                            scores[l][i] = 9998;
                        }

                        break;
                    }

                } else {
                    scores[j][i] = 10000;
                }

                j += 1;
            }

            i += 1;
            j = 0;
        }

        HungarianAlgorithm ha = new HungarianAlgorithm(scores);
        int[] jobs = ha.execute();
        Box box;
        Goal goal;

        for (int k = 0; k < jobs.length; k++) {
            if (jobs[k] != -1) {
                box = ((Box) setBoxes.get(k));
                goal = setGoals[jobs[k]];
                box.setBoxGoal(goal);
                box.setBoxGoalDistance(scores[k][jobs[k]]);
                goal.setAttachedBox(box);
                //System.err.println("----------- Pair = " + box.getId() + " with " + goal.getId()+ " (" + scores[k][jobs[k]] + ")");
            }
        }

    }

    /**
     * Gives a goal to every agent, by minimizing the pullDistance total cost and priorizing high priority goals
     */
    private void matchAgentsAndGoals() {

        int i = 0;
        int j = 0;
        Coordinate coord1;
        Coordinate coord2;
        double score;
        int totalGoals = highGoalQueue.size() + normalGoalQueue.size() + lowGoalQueue.size();
        double[][] scores = new double[totalGoals][agentList.size()];

        if(totalGoals > 0) {
            for (Agent agent : agentList) {
                String colorToMatch = agent.getColor();

                for (Goal tempGoal : highGoalQueue) {

                    if (tempGoal.getColor() == colorToMatch) {
                        Box boxOfGoal = tempGoal.getAttachedBox();
                        coord1 = State.realCoordinateById.get(boxOfGoal.getId());
                        coord2 = State.realCoordinateById.get(agent.getId());
                        //Original distance from agent to box position + original distance from box to its goal
                        double metric = Heuristic.pullDistance(coord1, coord2, colorToMatch) + boxOfGoal.getBoxGoalDistance();
                        // !!! ONLY WORKS IF MAX DEPTH OF DEAD-END IS <~25
                        if(metric<5000){scores[j][i] = metric + ((1000 - tempGoal.getPriority()) * 200);}
                        else{scores[j][i] = 10000;}

                    } else {
                        scores[j][i] = 10000;
                    }

                    j += 1;
                }

                for (Goal tempGoal : normalGoalQueue) {

                    if (tempGoal.getColor() == colorToMatch) {
                        Box boxOfGoal = tempGoal.getAttachedBox();
                        coord1 = State.realCoordinateById.get(boxOfGoal.getId());
                        coord2 = State.realCoordinateById.get(agent.getId());
                        //Original distance from agent to box position + original distance from box to its goal + 5000
                        double metric = Heuristic.pullDistance(coord1, coord2, colorToMatch) + boxOfGoal.getBoxGoalDistance();
                        if(metric<5000){scores[j][i] = metric + 5000;}
                        else{scores[j][i] = 10000;}
                        

                    } else {
                        scores[j][i] = 10000;
                    }

                    j += 1;
                }

                for (Goal tempGoal : lowGoalQueue) {

                    if (tempGoal.getColor() == colorToMatch) {
                        Box boxOfGoal = tempGoal.getAttachedBox();
                        coord1 = State.realCoordinateById.get(boxOfGoal.getId());
                        coord2 = State.realCoordinateById.get(agent.getId());
                        //Original distance from agent to box position + original distance from box to its goal + 7500
                        double metric = Heuristic.pullDistance(coord1, coord2, colorToMatch) + boxOfGoal.getBoxGoalDistance();
                        if(metric<5000){scores[j][i] = metric + 7500;}
                        else{scores[j][i] = 10000;}

                    } else {
                        scores[j][i] = 10000;
                    }

                    j += 1;
                }

                i += 1;
                j = 0;
            }

            HungarianAlgorithm ha = new HungarianAlgorithm(scores);
            int[] jobs = ha.execute();
            Goal bestGoal;
            Agent agent;

            /*
            System.err.println("----- Len of jobs = " + jobs.length);
            System.err.println("----- Jobs = " + Arrays.toString(jobs));

            
            for(int x = 0; x < totalGoals; x++){
                for(int y = 0; y < agentList.size(); y++){
                    System.err.println(scores[x][y]);
                }
            }
            
            
            System.err.println("---- highGoalQueue.size() = " + highGoalQueue.size());
            System.err.println("---- normalGoalQueue.size() = " + normalGoalQueue.size());
            System.err.println("---- lowGoalQueue.size() = " + lowGoalQueue.size());
            
            

            for(Goal goal : highGoalQueue){
                System.err.println("---- highGoal = " + goal);
            }
            for(Goal goal : normalGoalQueue){
                System.err.println("---- normalGoal = " + goal);
            }
            for(Goal goal : lowGoalQueue){
                System.err.println("---- lowGoal = " + goal);
            }
            */

            ArrayList<Goal> highGoalsToRemove = new ArrayList<Goal>();
            ArrayList<Goal> normalGoalsToRemove = new ArrayList<Goal>();
            ArrayList<Goal> lowGoalsToRemove = new ArrayList<Goal>();

            for (int k = 0; k < jobs.length; k++) {

                if (jobs[k] != -1) {
                    agent = agentList.get(jobs[k]);

                    if (scores[k][jobs[k]] < 9998) {

                        if (scores[k][jobs[k]] < 5000) { //Agent has been assigned a high priority goal

                            bestGoal = highGoalQueue.get(k);
                            agent.setCurrentGoal(bestGoal);
                            highGoalsToRemove.add(bestGoal);
                        
                        } else if (scores[k][jobs[k]] >= 5000 && scores[k][jobs[k]] < 7500) { //Agent has been assigned a normal priority goal
                            bestGoal = normalGoalQueue.get(k - highGoalQueue.size());
                            agent.setCurrentGoal(bestGoal);
                            normalGoalsToRemove.add(bestGoal);
                        
                        } else { //Agent has been assigned a low priority goal

                            bestGoal = lowGoalQueue.get(k - highGoalQueue.size() - normalGoalQueue.size());
                            agent.setCurrentGoal(bestGoal);
                            lowGoalsToRemove.add(bestGoal);

                        }

                        System.err.println("----------- Pair = " + agent.getId() + " with " + bestGoal+ " (" + scores[k][jobs[k]] + ")");
                    
                    } else if (scores[k][jobs[k]] >= 9998){

                        //Agent has no goal or an impossible goal
                        agent = agentList.get(jobs[k]);
                        Coordinate destinationGoal = State.agentGoalWithCoordinate.get(agent.getId());
                        if (destinationGoal != null && !State.realCoordinateById.get(agent.getId()).equals(destinationGoal)) {
                            agent.destinationGoal = destinationGoal;
                        }
                    }
                }
            }

            for(Goal goal : highGoalsToRemove){
                highGoalQueue.remove(goal);
            }
            for(Goal goal : normalGoalsToRemove){
                normalGoalQueue.remove(goal);
            }
            for(Goal goal : lowGoalsToRemove){
                lowGoalQueue.remove(goal);
            }


        } else {
            for (Agent agent : agentList){
                Coordinate destinationGoal = State.agentGoalWithCoordinate.get(agent.getId());
                    if (destinationGoal != null && !State.realCoordinateById.get(agent.getId()).equals(destinationGoal)) {
                        agent.destinationGoal = destinationGoal;
                    }
            }
        }
    }

    public static Comparator<Goal> goalComparator = (o1, o2) -> o2.getPriority() - o1.getPriority();
    public static Comparator<Agent> AgentGoalComparator = (a1, a2) -> { // if has to exit to a corner case has priority
        int priority2 = a2.getCurrentGoal() == null ? 0 : a2.getCurrentGoal().getPriority();
        int priority1 = a1.getCurrentGoal() == null ? 0 : a1.getCurrentGoal().getPriority();
        int moveToCorner2 = a2.moveToCornerCaseGoal ? State.MAX_GOAL_PRIORITY + 1 : 0;
        int moveToCorner1 = a1.moveToCornerCaseGoal ? State.MAX_GOAL_PRIORITY + 1 : 0;
        return priority2 + moveToCorner2 - priority1 - moveToCorner1;
    };

    /* TO REMOVE IF GOAL BIDS WORK
    public void updateGoal(Agent agent) {
        Goal bestGoal = null;

        if (!agent.isWaiting && agent.getCurrentGoal() == null) {

            // Get high goal
            bestGoal = findBestGoal(highGoalQueue, agent);

            if(bestGoal != null){
                agent.setCurrentGoal(bestGoal);
                highGoalQueue.remove(bestGoal);
            }

            // or get normal goal // TODO get the closest goal of agent ---------------------------------------------------------------------------------------------------------------------
            if (agent.getCurrentGoal() == null) {

                bestGoal = findBestGoal(normalGoalQueue, agent);

                if(bestGoal != null){
                    agent.setCurrentGoal(bestGoal);
                    normalGoalQueue.remove(bestGoal);
                }
                
            }

            // or get low goal queue
            if (agent.getCurrentGoal() == null) {

                bestGoal = findBestGoal(lowGoalQueue, agent);

                if(bestGoal != null){
                    agent.setCurrentGoal(bestGoal);
                    lowGoalQueue.remove(bestGoal);
                }
            }

            if (agent.getCurrentGoal() == null) {
                Coordinate destinationGoal = State.agentGoalWithCoordinate.get(agent.getId());
                if (destinationGoal != null && !State.realCoordinateById.get(agent.getId()).equals(destinationGoal)) {
                    agent.destinationGoal = destinationGoal;
                }
            }

            // TODO check if there is a corner case available
            // TODO put the agent back to their goal position
            // if no goal left for him
            //agent.moveToCornerCaseGoal = agent.getCurrentGoal() == null;
        } else {
            // TODO when there is a conflict for instance
        }
    }

    
    public Goal findBestGoal(ArrayList<Goal> goalQueue, Agent agent) {

        Goal bestGoal = null;
        double bestMetric = 9998;

        for (Goal tempGoal : goalQueue) {

            if (tempGoal.getColor().equals(agent.getColor())) {
                Box boxOfGoal = tempGoal.getAttachedBox();
                Coordinate coord1 = State.realCoordinateById.get(boxOfGoal.getId());
                Coordinate coord2 = State.realCoordinateById.get(agent.getId());
                //Original distance from agent to box position + original distance from box to its goal
                double metric = Heuristic.pullDistance(coord1, coord2) + boxOfGoal.getBoxGoalDistance();

                System.err.println("----- Agent " + agent.getId() + " is analyzing goal " + tempGoal.getId() + " at distance " + metric);

                if(metric < bestMetric) {
                    bestGoal = tempGoal;
                    bestMetric = metric;
                }
            }
        }

        return(bestGoal);
    }
    */

    public ArrayList<State> Search(Strategy strategy, Agent agent, problemType typeOfProblem) {

        // TODO DEFINE WHAT IS INITIAL STATE, it's here we can choose to use easier problem or full problem, for now it takes the board as it is.

        // TODO Decide if the box should store a goal or the goal store a box.
        State firstState;
        if (agent.getCurrentGoal() != null) {
            System.err.println("Agent " + agent.getId() + " finding solution for goal " + (agent.getCurrentGoal()));
            firstState = new State(State.realCoordinateById, State.realIdByCoordinate, agent.getId(), agent.getCurrentGoal().getAttachedBox().getId());
        } else if (agent.destinationGoal != null) {
            System.err.println("Agent " + agent.getId() + " finding solution for moving to " + agent.destinationGoal);
            firstState = new State(State.realCoordinateById, State.realIdByCoordinate, agent.getId(), agent.destinationGoal);
        } else { //} else if (agent.moveToCornerCaseGoal){
            System.err.println("Agent " + agent.getId() + " finding solution for moving to corner case");
            firstState = new State(State.realCoordinateById, State.realIdByCoordinate, agent.getId());
        }

        //System.err.println(firstState);

        strategy.addToFrontier(firstState);

        int iterations = 0;
        while (true) {
            if (iterations == 1000) {
                System.err.println(strategy.searchStatus());
                iterations = 0;
            }

            if (strategy.frontierIsEmpty()) {
                System.err.println("frontier is empty");
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
        String[] jointActionList = new String[agentList.size()];

        // Concatenate action for each agent (agentList is ordered by name)
        for (Agent agent : agentListByPriority) {
            int agentNumber = agentList.indexOf(agent);
            State next;
            String actionString;

            // get next State and remove it from plan if valid move
            if (planByAgent.get(agent).size() > 0) {
                next = planByAgent.get(agent).get(0);

                if (next.action == null) {
                    this.latestStateArray[agentNumber] = next;
                    planByAgent.get(agent).remove(0);
                    actionString = "NoOp";

                } else if (checkNextStep(next)) { // check possible issues corridor/deadEnd TODO for now if not valid move put agent to wait
                    this.latestStateArray[agentNumber] = next;
                    planByAgent.get(agent).remove(0);
                    actionString = next.action.toString();
                } else { // isWaiting == true
                    noAct++;
                    this.latestStateArray[agentNumber].action = null; // Stay at the same state except that there is no action
                    //System.err.println(agent.getId() + " is waiting");
                    actionString = "NoOp";
                }

            } else { // If plan has been fully executed
                //System.err.println("Agent " + agent.getId() + " reached its goal " + agent.getCurrentGoal());
                noAct++;
                actionString = "NoOp";
                //System.err.println(agent.getId() + " no step left");
                State latestState = this.latestStateArray[agentNumber];
                latestState.action = null; // Stay at the same state except that there is no action

                // put goals to null for next iteration
                agent.setCurrentGoal(null); // TODO What happens if execution fails in your last action? must update current goal to null somewhere
                Coordinate destinationGoal = State.agentGoalWithCoordinate.get(agent.getId());
                if (destinationGoal != null && State.realCoordinateById.get(agent.getId()).equals(destinationGoal)) {
                    agent.destinationGoal = null;
                }

                if (agent.moveToCornerCaseGoal && State.degreeMap.get(latestState.getLocalCoordinateById().get(agent.getId())) == State.NORMAL_GOAL_PRIORITY) {
                    agent.moveToCornerCaseGoal = false;
                }
            }

            jointActionList[agentNumber] = actionString;
        }


        String jointAction = String.join(";", jointActionList);

        //System.err.println("Command: " + jointAction);

        if (noAct == agentList.size()) return false;

        // Place message in buffer
        System.out.println(jointAction);
        // Flush buffer
        System.out.flush();

        String serverAnswer = serverMessages.readLine();

        //System.err.println("Answer: " + serverAnswer + "\n");

        if (serverAnswer == null) return false;

        String strip = serverAnswer.replaceAll("\\s", "");
        String[] returnVals = strip.split(";");
        this.latestServerOutput = returnVals;
        for (String returnVal : returnVals) {
            if (returnVal.equals("false")) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param next NOT NULL, Next state of the plan to send to the server
     * @return true if next step is valid, false if next step is deliberate NoOP (waiting)
     */
    public boolean checkNextStep(State next) {

        Agent nextAgent = (Agent) State.realBoardObjectsById.get(next.agentId);

        // ---- CHECK DEADEND ----
        if (next.action.actionType == Command.Type.Move) {
            //check agent is not in deadend
            Coordinate nextAgentCoor = next.getLocalCoordinateById().get(next.agentId);
            if (State.busyDeadEndIndexByCoordinate.containsKey(nextAgentCoor)) {
                Integer index = State.busyDeadEndIndexByCoordinate.get(nextAgentCoor);
                if (deadEndOcuppied(nextAgent, index)) return false;
            }
        } else { // PULL OR PUSH
            // check box and agent is not in deadend
            Coordinate nextAgentCoor = next.getLocalCoordinateById().get(next.agentId);
            Coordinate nextBoxCoor = Command.movedBoxPosition(next.action, next.getLocalCoordinateById().get(next.agentId));
            if (State.busyDeadEndIndexByCoordinate.containsKey(nextBoxCoor) || State.busyDeadEndIndexByCoordinate.containsKey(nextAgentCoor)) {
                Integer index = State.busyDeadEndIndexByCoordinate.get(nextBoxCoor) != null ?
                        State.busyDeadEndIndexByCoordinate.get(nextBoxCoor) :
                        State.busyDeadEndIndexByCoordinate.get(nextAgentCoor);
                if (deadEndOcuppied(nextAgent, index)) return false;
            }
        }


        // ---- TODO CHECK CORRIDOR ----

        return true;
    }

    private boolean deadEndOcuppied(Agent nextAgent, Integer index) {
        if (!State.busyDeadEndOccupancy.get(index).isEmpty() && !State.busyDeadEndOccupancy.get(index).contains(nextAgent.getId())) {
            // Put agent to wait
            nextAgent.isWaiting = true;
            // ask for him to go out
            for (String agentId : State.busyDeadEndOccupancy.get(index)) {
                Agent tempAgent = (Agent) State.realBoardObjectsById.get(agentId);
                if (tempAgent.getCurrentGoal() == null && tempAgent.destinationGoal == null) {
                    tempAgent.moveToCornerCaseGoal = true;
                }
            }
            return true;
        }
        nextAgent.isWaiting = false;
        return false;
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