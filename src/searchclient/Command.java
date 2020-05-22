package src.searchclient;

import java.util.ArrayList;
import java.util.LinkedList;

public class Command {
    // Order of enum important for determining opposites.
    public enum Dir {
        N, W, E, S
    }

    public enum Type {
        Move, Push, Pull
    }

    public static final Command[] EVERY;

    static {
        ArrayList<Command> cmds = new ArrayList<>();  // LinkedList<Command> cmds = new LinkedList<Command>();
        for (Dir d1 : Dir.values()) {
            for (Dir d2 : Dir.values()) {
                if (!Command.isOpposite(d1, d2)) {
                    cmds.add(new Command(Type.Push, d1, d2));
                }
            }
        }
        for (Dir d1 : Dir.values()) {
            for (Dir d2 : Dir.values()) {
                if (d1 != d2) {
                    cmds.add(new Command(Type.Pull, d1, d2));
                }
            }
        }
        for (Dir d : Dir.values()) {
            cmds.add(new Command(d));
        }

        EVERY = cmds.toArray(new Command[0]);
    }

    public static boolean isOpposite(Dir d1, Dir d2) {
        return d1.ordinal() + d2.ordinal() == 3;
    }

    public static int dirToRowChange(Dir d) {
        // South is down one row (1), north is up one row (-1).
        switch (d) {
            case S:
                return 1;
            case N:
                return -1;
            default:
                return 0;
        }
    }

    public static int dirToColChange(Dir d) {
        // East is right one column (1), west is left one column (-1).
        switch (d) {
            case E:
                return 1;
            case W:
                return -1;
            default:
                return 0;
        }
    }

    public static Coordinate movedBoxPosition(Command c, Coordinate agentCoor) {
        if (c.actionType == Command.Type.Push) { // Agent takes the place of the box and box move toward dir2
            return new Coordinate(
                    agentCoor.getRow() + dirToRowChange(c.dir2),
                    agentCoor.getColumn() + dirToColChange(c.dir2));
        } else if (c.actionType == Command.Type.Pull) { // Box takes the place of the agent and agent move toward dir1
            // Cell is free where agent is going
            return new Coordinate(
                    agentCoor.getRow() - dirToRowChange(c.dir1),
                    agentCoor.getColumn() - dirToColChange(c.dir1));
        } else {
            return null;
        }
    }

// Return an array with the new coordinates of the Agent, and the coordinate of the box if it is a Push/Pull action
public ArrayList<Coordinate> commandToCoordinates(Coordinate startPosition, Command c){
    ArrayList<Coordinate> retArr = new ArrayList<>();

    Coordinate newAgentPosition = new Coordinate(startPosition.getRow() + dirToRowChange(c.dir1), startPosition.getColumn() + dirToColChange(c.dir1));
    retArr.add(newAgentPosition);

    if(c.actionType == Command.Type.Push){
        Coordinate newBoxPosition = new Coordinate(newAgentPosition.getRow() + dirToRowChange(c.dir2), newAgentPosition.getColumn() + dirToColChange(c.dir2));
        retArr.add(newBoxPosition);
    } else if(c.actionType == Command.Type.Pull){
        Coordinate newBoxPosition = new Coordinate(startPosition.getRow() + dirToRowChange(c.dir2), startPosition.getColumn() + dirToColChange(c.dir2));
        retArr.add(newBoxPosition);
    }
    return retArr;
}

    public final Type actionType;
    public final Dir dir1;
    public final Dir dir2;

    public Command(Dir d) {
        this.actionType = Type.Move;
        this.dir1 = d;
        this.dir2 = null;
    }

    public Command(Type t, Dir d1, Dir d2) {
        this.actionType = t;
        this.dir1 = d1;
        this.dir2 = d2;
    }

    @Override
    public String toString() {
        if (this.actionType == Type.Move)

            return String.format("%s(%s)", this.actionType.toString(), this.dir1.toString());
        else {
            return String.format("%s(%s,%s)", this.actionType.toString(), this.dir1.toString(), this.dir2 != null ? this.dir2.toString() : null);
        }
    }
}