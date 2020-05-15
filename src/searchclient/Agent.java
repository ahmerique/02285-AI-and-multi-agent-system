package src.searchclient;

import java.util.PriorityQueue;

public class Agent extends BoardObject {

    private Goal currentGoal;

    /**
     * Instantiate a new Agent
     *
     * @param id    the id
     * @param color the color
     */
    public Agent(String id, String color) {
        super(id, color);
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(Goal currentGoal) {
        this.currentGoal = currentGoal;
    }

    // TODO
    public void updateGoal(State initialState, PriorityQueue<Goal> goalQueue) {
    }
}