package searchclient;


public class Box extends BoardObject {

    // Additional attributes
    private int row; // Coordinate on y-axis
    private int col; // Coordinate on x-axis


    /**
	 * Instantiate a new Box
	 * 
	 * @param id the id
	 * @param color the color
	 */
    public Box(String id, String color, int row, int col) {
        super(id, color);
        this.row = row;
        this.col = col;
    }


    /**
	 * Gets the Row.
	 * 
	 * @return the row
	 * @returns the position on y-axis of the box
	 */
    public int getRow(){
        return row;
    }

    /**
	 * Gets the Column.
	 * 
	 * @return the col
	 * @returns the position on x-axis of the box
	 */
    public int getCol(){
        return col;
    }

    /**
	 * Set the Row.
	 */
    public int setRow(int row){
        this.row = row;
    }


    /**
	 * Set the Col.
	 */
    public int setCol(int col){
        this.col = col;
    }


}