package searchclient;


public class Box extends BoardObject {

    /**
     * Additional attributes
     */
    private Goal currentGoal;
    private boolean isOnGoal;


    /**
     * Instantiate a new Box
     *
     * @param id    the id
     * @param color the color
     */
    public Box(String id, String color) {
        super(id, color);
    }

    /**
     * GETTER AND SETTER
     */

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(Goal currentGoal) {
        this.currentGoal = currentGoal;
    }

    public boolean isOnGoal() {
        return isOnGoal;
    }

    public void setOnGoal(boolean onGoal) {
        isOnGoal = onGoal;
    }

}