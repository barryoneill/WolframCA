package net.nologin.meep.ca.model;


import android.graphics.Bitmap;
import net.nologin.meep.tbv.Tile;

public class WolframTile extends Tile {

    public static final int TILE_SIZE = 256;

    public static final boolean[] CA_START_STATE;
    public static final boolean[] CA_EMPTY_STATE;
    static {
        CA_EMPTY_STATE = new boolean[TILE_SIZE];
        CA_START_STATE = new boolean[TILE_SIZE];
        CA_START_STATE[TILE_SIZE/2] = true;
    }

    // the actual contents to be rendered to the screen, may be null
    public Bitmap bmpData = null;

    /* We can't really keep the bmpData data for every tile generated in memory, otherwise we'd quickly
     * run out of heap space (as the user scrolls further away from the start position).
     *
     * When this tile goes out of view, the caller will clear the bmpData data from this Tile instance.
     * To save it having to recalculate the contents (should this tile be scrolled back into view), we
     * keep a boolean array of the bottom row/state of this tile (doesn't get cleared with the bmpData data).
     *
     * This way, any Tile's bmpData contents can quickly be recalculated using the bottom state
     * from the three tiles above.
     *
     */
    boolean[] bottomState = null;

    public int renderOrder = -1; // debug for determining when a cell was rendered

    public WolframTile(int xId, int yId) {
        super(xId, yId, TILE_SIZE);
    }

    @Override
    public Bitmap getBmpData(){
        return bmpData;
    }

    public String toString(){
        return "Wolf" + super.toString();
    }

    // TODO: strip out into comparator!
    /*
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
    */

}
