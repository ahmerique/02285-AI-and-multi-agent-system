package src.searchclient;


public class Box extends BoardObject {

    /**
     * Additional attributes
     */
    private double boxGoalDistance;
    private Goal boxGoal;
    private boolean isOnGoal;
    private final char letter;


    /**
     * Instantiate a new Box
     *
     * @param id    the id
     * @param color the color
     */
    public Box(String id, String color, char letter) {
        super(id, color);
        this.letter= letter;
    }

    /**
     * GETTER AND SETTER
     */

    public Goal getBoxGoal() {
        return boxGoal;
    }

    public void setBoxGoal(Goal boxGoal) {
        this.boxGoal = boxGoal;
    }

    public double getBoxGoalDistance() {
        return boxGoalDistance;
    }

    public void setBoxGoalDistance(double boxGoalDistance) {
        this.boxGoalDistance = boxGoalDistance;
    }

    public boolean isOnGoal() {
        return isOnGoal;
    }

    public void setOnGoal(boolean onGoal) {
        isOnGoal = onGoal;
    }

    public char getLetter() {
        return letter;
    }
}