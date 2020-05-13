package searchclient;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.primitives.Ints;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;

import static java.lang.Character.toLowerCase;

// TODO UPDATE WITH STATE CHANGES
public class SearchClient {
    public State initialState;

    public SearchClient(BufferedReader serverMessages) throws Exception {
        
        boolean isMA; //SA if 1 at the end of the file reading, else MA
        List<String> serverMessageList = new ArrayList<String>(); //All lines of the Initial level
        int max_col = 0; //Maximum column reached
        int row = 0; //Iteration variable
        int countpart = 0; //Part of the initial file read
        HashMap<String, String> colors = new HashMap<String, String>(); //All colors

        String line = serverMessages.readLine();

        while (!line.equals("#end")) {
            System.err.println(line);/////////////////////////////////////////////////////////////

            if (line.charAt(0) == '#'){
                countpart += 1;
            } 
            
            line = serverMessages.readLine();
            
            switch(countpart){
                case 1://Domain
                    //No action needed. Name of the domain can be saved here.
                    break;

                case 2://Level Name
                    isMA = toLowerCase(line.charAt(0)) != 's';
                    //No action needed. Name of the level can be saved here.
                    break;

                case 3://Colors
                    while(line.charAt(0) != '#'){
                        String[] str = line.split(": ");
                        String[] objects = str[1].split(", ");
                        //We add each color to the colors dictionnary
                        for (String object : objects){
                            colors.put(object, str[0]); //Considering that objects are only initialized once
                        }
                        line = serverMessages.readLine();
                    }
                    break;

                case 4://Initial state
                    //Features all the information regarding the level
                    while(line.charAt(0) != '#'){
                        serverMessageList.add(line);
                        max_col = Math.max(max_col, line.length());
                        line = serverMessages.readLine();
                    }

                    //Initialize static attributes of State
                    this.initialState = new State(null); //Initial state
                    State.MAX_COL = max_col;
                    State.MAX_ROW = serverMessageList.size();
                    State.wallByCoordinate = new HashMap<Coordinate, Boolean>();
                    State.goalWithCoordinate = new HashMap<Goal, Coordinate>();
                    State.realBoardObjectsById = new HashMap<String, BoardObject>();
                    State.goalByCoordinate = new HashMap<Coordinate, Goal>();
                    State.realBoardObjectByCoordinate = new HashMap<Coordinate, BoardObject>();

                    break;

                case 5://Goal state
                    //Iteration over the rows of the initial state while reading the final state in parallel
                    for(int i = 0; i < serverMessageList.size(); i++){
                        String rowline = serverMessageList.get(i);

                        for (int j = 0; j < rowline.length(); j++) {

                            char chr = rowline.charAt(j);
                            if (chr == '+') { // Wall
                                State.wallByCoordinate.put(new Coordinate(i, j), true);
                            } else if ('0' <= chr && chr <= '9') { // Agent
                                State.setNewStateObject(i,j,0,chr,colors.get(Character.toString(chr)));
                            } else if ('A' <= chr && chr <= 'Z') { // Box
                                State.setNewStateObject(i,j,1,chr,colors.get(Character.toString(chr)));
                            }  else if (chr == ' ') { // Free space
                                // Nothing
                            } else {
                                System.err.println("Error, read invalid level character: " + (int) chr);
                                System.exit(1);
                            }

                            char chrGoal = line.charAt(j);
                            //TODO remove if goals are added as Box objects parameters
                            if ('A' <= chrGoal && chrGoal <= 'Z') { // Goal
                                State.setNewStateObject(i,j,2,chrGoal,colors.get(Character.toString(chrGoal)));
                            }
                        }
                        
                        line = serverMessages.readLine();
                    }
                    break;
            }
            
        }


        // Preprocess data

        int MAX_GOAL_PRIORITY = State.MAX_COL * State.MAX_ROW;
        HashMap<Coordinate, Integer> degreeMap = new HashMap<>();
        HashSet<Coordinate> deadEndCaseSet = new HashSet<>();
        HashSet<Coordinate> corridorCaseSet = new HashSet<>();
        HashSet<Coordinate> cornerCaseSet = new HashSet<>();
        ArrayList<ArrayList<Coordinate>> deadEndlist = new ArrayList<>();

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
                        degreeMap.put(tempCoordinate, MAX_GOAL_PRIORITY);
                    } else if (tempDegree == 2) {

                        // we want to fill corridor last to prevent clogs
                        if (cornerOrCorridor % 2 == 0) {
                            corridorCaseSet.add(tempCoordinate);
                            degreeMap.put(tempCoordinate, 0);
                        }

                        // A corner is a corridor if linked to one but else it's a free cell
                        else {
                            cornerCaseSet.add(tempCoordinate);
                            degreeMap.put(tempCoordinate, 1);
                        }
                    } else { // Normal free cell
                        degreeMap.put(tempCoordinate, 1);
                    }
                }
            }
        }

        // find deadEnd
        for (Coordinate deadEnd : deadEndCaseSet) {

            boolean isCorridor = true;
            Coordinate prevCoor = deadEnd;
            Coordinate currentCoor = deadEnd;
            int goalPriorityValue = MAX_GOAL_PRIORITY;
            ArrayList<Coordinate> tempList = new ArrayList<>();
            tempList.add(deadEnd);

            while (isCorridor) {
                isCorridor = false;
                goalPriorityValue--;

                for (Coordinate tempCoor : currentCoor.get4VicinityCoordinates()) { //beware if tempCoor is the same for all the modification
                    if (tempCoor != prevCoor && State.wallByCoordinate.get(tempCoor) == null) {
                        prevCoor = currentCoor;
                        currentCoor = tempCoor;

                        if (corridorCaseSet.contains(tempCoor)) {
                            degreeMap.put(tempCoor, goalPriorityValue);
                            corridorCaseSet.remove(tempCoor);
                            tempList.add(tempCoor);

                            isCorridor = true;
                            break;

                        } else if (cornerCaseSet.contains(tempCoor)) {
                            degreeMap.put(tempCoor, goalPriorityValue);
                            cornerCaseSet.remove(tempCoor);
                            tempList.add(tempCoor);

                            isCorridor = true;
                            break;
                        } // if not a corridor anymore, the end of a dead end is a normal cell with a low priority of 1
                    }
                }
            }

            deadEndlist.add(tempList);
        }

        // link corridor to corner and add extremities
        for (Coordinate corridor : corridorCaseSet) {

            boolean isCorridor = true;
            Coordinate prevCoor = corridor;
            Coordinate currentCoor = corridor;

            while (isCorridor) {
                isCorridor = false;

                for (Coordinate tempCoor : currentCoor.get4VicinityCoordinates()) { //beware if tempCoor is the same for all the modification
                    if (tempCoor != prevCoor && State.wallByCoordinate.get(tempCoor) == null) {
                        prevCoor = currentCoor;
                        currentCoor = tempCoor;

                        if (corridorCaseSet.contains(tempCoor)) {
                            corridorCaseSet.remove(tempCoor);
                            isCorridor = true;
                            break;

                        } else if (cornerCaseSet.contains(tempCoor)) {
                            degreeMap.put(tempCoor, 0);
                            cornerCaseSet.remove(tempCoor);
                            isCorridor = true;
                            break;
                        } else { // if not a corridor anymore, the end of a corridor is part of a corridor
                            degreeMap.put(tempCoor, 0); // don't break so that one case corridor can modify its entrance and exit
                        }
                    }
                }
            }
        }

        // TODO process "free cells" with deadend which don't have goals.

        // TODO classify normal goals between them (for instance goals that are stuck between each other)

        //Match Boxes and Goals
        State.matchGoalsAndBoxes();

        System.err.println("----------- MAX_ROW = " + Integer.toString(State.MAX_ROW));
        System.err.println("----------- MAX_COL = " + Integer.toString(State.MAX_COL));
        System.err.println("Done initializing");
    }

    public ArrayList<State> Search(Strategy strategy) {
        System.err.format("Search starting with strategy %s.\n", strategy.toString());
        strategy.addToFrontier(this.initialState);

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

    public static void main(String[] args) throws Exception {
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing.");
        System.out.println("Baguettes' engine");

        // Read level and create the initial state of the problem
        SearchClient client = new SearchClient(serverMessages);

        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                    strategy = new Strategy.StrategyBFS();
                    break;
                case "-dfs":
                    strategy = new Strategy.StrategyDFS();
                    break;
                case "-astar":
                    //strategy = new Strategy.StrategyBestFirst(new Heuristic.AStar(client.initialState));
                    strategy = new Strategy.StrategyBFS();
                    break;
                case "-wastar":
                    //strategy = new Strategy.StrategyBestFirst(new Heuristic.WeightedAStar(client.initialState, 5));
                    strategy = new Strategy.StrategyBFS();
                    break;
                case "-greedy":
                    //strategy = new Strategy.StrategyBestFirst(new Heuristic.Greedy(client.initialState));
                    strategy = new Strategy.StrategyBFS();
                    break;
                default:
                    strategy = new Strategy.StrategyBFS();
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
            strategy = new Strategy.StrategyBFS();
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }

        ArrayList<State> solution;
        try {
            solution = client.Search(strategy);
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
    }
}
