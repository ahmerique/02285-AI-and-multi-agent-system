package src.searchclient;
import java.util.*;


/*
Utility class to execute a simple BFS for the Pull Distance in Heuristic
*/

public class Node_PullDist {

    // Coordinates of a cell in matrix
    Coordinate coord;

    // Distance of current Node from the source Node
    int number_actions;

    public Node_PullDist(Coordinate coord, int number_actions) {
        this.coord = coord;
        this.number_actions = number_actions;

    }
    
    
}