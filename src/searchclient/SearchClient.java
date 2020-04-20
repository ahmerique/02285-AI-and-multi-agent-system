package searchclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;



// TODO UPDATE WITH STATE CHANGES
public class SearchClient {
    public State initialState;

    public SearchClient(BufferedReader serverMessages) throws Exception {
        
        int SAorMA = 0; //SA if 1 at the end of the file reading, else MA
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

                    State.MAX_COL = max_col;
                    State.MAX_ROW = serverMessageList.size();
                    this.initialState = new State(null); //Initial state

                    State.walls = new boolean[State.MAX_ROW][State.MAX_COL];
                    State.goals = new char[State.MAX_ROW][State.MAX_COL];
                    break;

                case 5://Goal state
                    //Iteration over the rows of the initial state while reading the final state in parallel
                    for(int i = 0; i < serverMessageList.size(); i++){
                        String rowline = serverMessageList.get(i);

                        for (int j = 0; j < rowline.length(); j++) {
                            char chr = rowline.charAt(j);
                            char chrGoal = line.charAt(j);
            
                            if (chr == '+') { // Wall.
                                State.walls[i][j] = true;
                            } else if ('0' <= chr && chr <= '9') { // Agent.
                                SAorMA += 1;
                                //this.initialState.agents[i][column] = chr; //TODO initialize as object with color
                                this.initialState.agentRow = i;
                                this.initialState.agentCol = j;
                            } else if ('A' <= chr && chr <= 'Z') { // Box.
                                this.initialState.boxes[i][j] = chr; //TODO initialize as object with color
                            }  else if (chr == ' ') {
                                // Free space.
                            } else {
                                System.err.println("Error, read invalid level character: " + (int) chr);
                                System.exit(1);
                            }
                            
                            //TODO remove if goals are added as Box objects parameters
                            if ('A' <= chrGoal && chrGoal <= 'Z') { // Goal.
                                State.goals[i][j] = Character.toLowerCase(chr);//TODO goal definition not needed if linked to boxes
                            }
                        }
                        
                        line = serverMessages.readLine();
                    }
                    break;
            }
            
        }
        
        System.err.println("----------- MAX_ROW = " + Integer.toString(State.MAX_ROW));
        System.err.println("----------- MAX_COL = " + Integer.toString(State.MAX_COL));
        System.err.println("----------- AGENT_ROW = " + Integer.toString(State.MAX_ROW));
        System.err.println("----------- AGENT_COL = " + Integer.toString(State.MAX_COL));
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

            if (leafState.isGoalState()) {
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
                    strategy = new Strategy.StrategyBestFirst(new Heuristic.AStar(client.initialState));
                    break;
                case "-wastar":
                    strategy = new Strategy.StrategyBestFirst(new Heuristic.WeightedAStar(client.initialState, 5));
                    break;
                case "-greedy":
                    strategy = new Strategy.StrategyBestFirst(new Heuristic.Greedy(client.initialState));
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
