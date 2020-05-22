package src.searchclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Agent extends BoardObject {

    private int counter = 0;

    private Goal currentGoal;
    public Coordinate destinationGoal;
    public Boolean moveToCornerCaseGoal = false;

    // Communication in corridors
    public Boolean isWaiting = false;
    public ArrayList<State> plan = new ArrayList<>();// Communication between agents
   
   // Communication between agents
    private Boolean isWaiting2; // An agent with isWaiting2 = true wait for one turn to don't bother another agent
    private Agent askedToWaitBy; 
    public Boolean isClearing = false; // To know if agent has order to clear path for another agent
    public ArrayList<Coordinate> clearCoordinates; // Cells to clear when it is the case
    public State lastStateHelp = null; // Variable to store lastState when removing action from a conflictual plan
    public State stateToSearchStrategy = null; // If a new plan need to be computed, it is starting from this State

    /**
     * Instantiate a new Agent
     *
     * @param id    the id
     * @param color the color
     */
    public Agent(String id, String color) {
        super(id, color);
        this.isWaiting2 = false;
        this.askedToWaitBy = null;
        counter++;
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }
    public Boolean getAgentOrder() {
        return this.isWaiting2;
    }

    public Agent getAgentWhoOrdered() {
        return this.askedToWaitBy;
    }
    public void setCurrentGoal(Goal currentGoal) {
        this.currentGoal = currentGoal;
    }
    public void setOrder(boolean mustWait) {
        this.isWaiting2 = mustWait;
    }  

    public void giveOrderToAgent(Agent agent) {
        agent.isWaiting2 = true;
        agent.askedToWaitBy = this;
    }   
     public void requeueCurrentGoal(ArrayList<Goal> goalQueue) {
        Goal.insertInOrderedGoalList(goalQueue, this.currentGoal);
        this.currentGoal = null;
    }
}