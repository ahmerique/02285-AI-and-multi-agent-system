package src.searchclient;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class Agent extends BoardObject {

    private int counter = 0;

    private Goal currentGoal;

    /**
     * Instantiate a new Agent
     *
     * @param id    the id
     * @param color the color
     */
    public Agent(String id, String color) {
        super(id, color);
        counter++;
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(Goal currentGoal) {
        this.currentGoal = currentGoal;
    }

    public void updateGoal(ArrayList<Goal> goalQueue) {
        if (currentGoal == null) {
            for (Goal tempGoal : goalQueue) {
                System.err.println(tempGoal.getColor());
                if (tempGoal.getColor().equals(this.getColor())) {
                    currentGoal = tempGoal;
                    break;
                }
            }
            goalQueue.remove(currentGoal);
        } else {
            // TODO when there is a conflict for instance
        }
    }
}