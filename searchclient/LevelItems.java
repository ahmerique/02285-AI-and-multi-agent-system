package searchclient;

import java.awt.*;
import java.util.*;

// Some features of a level doesn't change from State 0 to final State
// Theses are locations of walls and goals
// They are computed and defined once in this class and used by all States

public class LevelItems {
    private static LevelItems instance;
    private boolean[][] walls;  
    private char[][] goals;  
    private HashMap<Character, HashSet<Point>> goalMap;
    private int MAX_ROW;
    private int MAX_COL;
    private int NUM_GOALS;

    private LevelItems(int MAX_ROW, int MAX_COL){
        walls = new boolean[MAX_ROW][MAX_COL];
        goals = new char[MAX_ROW][MAX_COL];
        this.MAX_ROW = MAX_ROW;
        this.MAX_COL = MAX_COL;
        NUM_GOALS = 0;
        goalMap = new HashMap<>();
    }


    public static LevelItems createInstance(int MAX_ROW, int MAX_COL) {
        if(instance == null) {
            instance = new LevelItems(MAX_ROW, MAX_COL);
        }
        return instance;
    }


    public static LevelItems getInstance(){
        return instance;
    }


    public void setWall(boolean wall, int row, int col){
        walls[row][col] = wall;
    }


    public void setGoal(char goal, int row, int col){
        goals[row][col] = goal;
        if (goalMap.containsKey(goal)){
            HashSet<Point> charSet = goalMap.get(goal);
            charSet.add(new Point(col, row));
        }else{
            HashSet<Point> charSet = new HashSet<>();
            charSet.add(new Point(col, row));
            goalMap.put(goal, charSet);
        }
        NUM_GOALS++;
    }


    public char[][] getGoals(){
        return goals;
    }


    public int getNUM_GOALS(){
        return NUM_GOALS;
    }


    public HashMap<Character, HashSet<Point>> getGoalMap(){
        return goalMap;
    }


    public boolean[][] getWalls(){
        return walls;
    }


    public int getMAX_ROW(){
        return MAX_ROW;
    }


    public int getMAX_COL() {
        return MAX_COL;
    }
    
}