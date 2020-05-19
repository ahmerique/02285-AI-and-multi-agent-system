package src.searchclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Agent extends BoardObject {

    private int counter = 0;

    private Goal currentGoal;
    public Coordinate destinationGoal;
    public Boolean moveToCornerCaseGoal;
    public Boolean isWaiting = false;

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

    public void requeueCurrentGoal(ArrayList<Goal> goalQueue) {
        Goal.insertInOrderedGoalList(goalQueue, this.currentGoal);
        this.currentGoal = null;
    }

    public void updateGoal(ArrayList<Goal> goalQueue) {
        if (currentGoal == null) {
            for (Goal tempGoal : goalQueue) {
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