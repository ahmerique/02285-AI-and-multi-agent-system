package src.searchclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Goal extends BoardObject {


    private final char letter;
    private final Coordinate coordinate;
    private int priority;

    private Box attachedBox;

    /**
     * Instantiate a new Goal.
     *
     * @param id    the id
     * @param color the color
     */
    public Goal(String id, String color, Coordinate coordinate, char letter) {
        super(id, color);
        this.coordinate = coordinate;
        this.letter = letter;
    }

    /**
     * GETTERS AND SETTERS
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

    public Box getAttachedBox() {
        return attachedBox;
    }

    public void setAttachedBox(Box attachedBox) {
        this.attachedBox = attachedBox;
    }


    static public void insertInOrderedGoalList(ArrayList<Goal> list, Goal element) {
        int index = Collections.binarySearch(list, element, Comparator.comparing(Goal::getPriority));
        if (index < 0) {
            index = -index - 1;
        }
        list.add(index, element);
    }



    @Override
    public String toString() {
        return "Goal{" +
                "letter=" + letter +
                ", coordinate=" + coordinate +
                ", priority=" + priority +
                ", box=" + attachedBox +
                '}';
    }
}
