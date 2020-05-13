package src.searchclient;

public class Goal extends BoardObject {


    private final char letter;
    private final Coordinate coordinate;
    private int priority;
    /**
     * Instantiate a new Goal.
     *
     * @param id    the id
     * @param color the color
     */
    public Goal(String id, String color, Coordinate coordinate, char letter) {
        super(id, color);
        this.coordinate=coordinate;
        this.letter= letter;
    }

    /**
     * GETTERS
     */

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public char getLetter() {
        return letter;
    }


    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Goal{" +
                "letter=" + letter +
                ", coordinate=" + coordinate +
                ", priority=" + priority +
                '}';
    }
}
