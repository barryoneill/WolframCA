package net.nologin.meep.ca.model;


import android.graphics.Bitmap;
import net.nologin.meep.tbv.Tile;

public class WolframTile extends Tile {

    /* From playing around with tile/bitmap size, it seems somewhere around 256 is
     * a good balance for tile size.  If the tile size is too big, the longer processing time
     * for each tile ruins the feeling of responsiveness (it also can gobble the heap).
     * If it's too small, there's more heap/queue object processing and while 'responsive',
     * it'll be slow. */
    public static final int TILE_WIDTH_PX = 256;

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
        super(xId, yId, TILE_WIDTH_PX);
    }

    @Override
    public Bitmap getBmpData(){
        return bmpData;
    }

    public String toString(){
        return "Wolf" + super.toString();
    }

}
