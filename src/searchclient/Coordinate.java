package searchclient;

import java.util.ArrayList;
import java.util.LinkedList;

public class Coordinate {

    private int row;
    private int column;

    public Coordinate(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public Coordinate(Coordinate cord){
        this.row = cord.getRow();
        this.column = cord.getColumn();
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String toString() {
        return this.row + ", " + this.column;
    }

    public LinkedList<Coordinate> get4VicinityCoordinates() {
        LinkedList<Coordinate> neighbours = new LinkedList<>();
        neighbours.add(new Coordinate(this.row-1, this.column));
        neighbours.add(new Coordinate(this.row+1, this.column));
        neighbours.add(new Coordinate(this.row, this.column-1));
        neighbours.add(new Coordinate(this.row, this.column+1));
        return neighbours;
    }

    public static ArrayList<Coordinate> cloneCordList(ArrayList<Coordinate> list){
        ArrayList<Coordinate> clone = new ArrayList<>(list.size());
        for(Coordinate cord : list){
            clone.add(new Coordinate(cord));
        }
        return clone;
    }

    @Override
    public int hashCode() {
        return 3571 + this.row + this.column;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;
        Coordinate other = (Coordinate) obj;
        return this.row == other.row && this.column == other.column;
    }


}