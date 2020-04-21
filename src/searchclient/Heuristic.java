package searchclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public abstract class Heuristic implements Comparator<State> {

    private HashMap<Character, Coordinate> goalsByID = new HashMap<>();
    private ArrayList<Character> goalList = new ArrayList<>();

    public Heuristic(State initialState) {
        // Here's a chance to pre-process the static parts of the level.

        // Get list of goals

        for (int row = 1; row < State.MAX_ROW - 1; row++) {
            for (int col = 1; col < State.MAX_COL - 1; col++) {
                char g = State.goals[row][col];
                if ('a' <= g && g <= 'z') {
                    goalsByID.put(g, new Coordinate(row, col));
                    goalList.add(g);
                }
            }
        }

    }

    private Coordinate getBoxCoordinate(State n, char ID) {
        for (int row = 1; row < State.MAX_ROW -1; row++) {
            for (int col = 1; col < State.MAX_COL -1; col++) {
                char b = Character.toLowerCase(n.boxes[row][col]);
                if (b == ID) {
                    return new Coordinate(row, col);
                }
            }
        }
        return new Coordinate(0, 0);
    }

    public int stupid_h(State n) {
        int uncleared_goals = 0;
        for (int row = 1; row < State.MAX_ROW - 1; row++) {
            for (int col = 1; col < State.MAX_COL - 1; col++) {
                char g = State.goals[row][col];
                char b = Character.toLowerCase(n.boxes[row][col]);
                if (g > 0 && b != g) {
                    uncleared_goals++;
                }
            }
        }

        return uncleared_goals;
    }

    public int manhattan_h(State n) {
        int sumH = 0;
        int minAgentBox =  State.MAX_COL+State.MAX_ROW;

        for (char ID : goalsByID.keySet()) {
            Coordinate goalCoordinate = goalsByID.get(ID);
            Coordinate boxCoordinate = getBoxCoordinate(n, ID);

            int newH = Math.abs(boxCoordinate.row - goalCoordinate.row)
                    + Math.abs(boxCoordinate.col - goalCoordinate.col);

            sumH += newH;

            int newAgentBox = Math.abs(n.agentRow - boxCoordinate.row) + Math.abs(n.agentCol - boxCoordinate.col) - 1;

            if (minAgentBox>newAgentBox){
                minAgentBox = newAgentBox;
            }

        }
        //System.err.println(sumH);
        return sumH+minAgentBox;
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
