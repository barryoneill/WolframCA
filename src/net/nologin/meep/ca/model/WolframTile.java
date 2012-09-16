package net.nologin.meep.ca.model;

import net.nologin.meep.ca.util.Utils;

public class WolframTile extends Tile {

    public static final int TILE_SIZE = 256;

    public static final boolean[] CA_START_STATE;
    public static final boolean[] CA_EMPTY_STATE;
    static {
        CA_EMPTY_STATE = new boolean[TILE_SIZE];
        CA_START_STATE = new boolean[TILE_SIZE];
        CA_START_STATE[TILE_SIZE/2] = true;

    }

    /* We could just use the bitmap data in each cell to determine state, but I'm going to add the additional
     * cost of 4 boolean arrays (cell state for each boundary of this cell). The problem is when the user
     * starts scrolling _waay_ offcenter, holding the bitmap data for each cell will gobble RAM.  The plan is
     * to clear the bitmap data of cells not being displayed, and recalculate it from these boolean arrays
     * if/when the cells are scrolled back into view.
     *
     * if memory starts being a problem, consider using BitSet instead (at the cost of CPU overhead) */
    boolean[] top,bot; // left,right

    public WolframTile(int xId, int yId) {
        super(xId, yId, TILE_SIZE);

        top = new boolean[size];
//        left = new boolean[size];
//        right = new boolean[size];
        bot = new boolean[size];

    }

    public String toString(){

        return "Wolf" + super.toString();

    }

    @Override
    public int compareTo(Tile tile) {

        // render queue is a PriorityQueue, - 'least' gets priority

        // The state of a wolfram cell depends on the row above - upper rows (lower y value) get priority
        if(yId < tile.yId){
            return -1;
        }
        if(yId > tile.yId){
            return 1;
        }

        // same level - so we want the tile closest to the vertical axis
        int thisX = Math.abs(xId);
        int thatX = Math.abs(tile.xId);

        if(thisX < thatX){
            return -1;
        }
        if(thisX > thatX){
            return 1;
        }

        return 0;
    }

}
