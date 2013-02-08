package net.nologin.meep.tbv;

import android.content.Context;
import android.graphics.Rect;

public interface TileProvider {

    /**
     * @return Return the dimension of the tiles generated by this providers (Tiles are
     * square, so for 128*128, return 128
     */
    public int getTileSize();

    /**
     * Get the specified tile
     * @param x The x-attribute of the id
     * @param y The y-attribute of the id
     * @return The tile.
     */
    public Tile getTile(int x, int y);

    /**
     * Called from a background thread, this gives the provider a chance to render
     * the bitmap contents of next tile in its queue, if any.
     */
    public void generateNextTile();

    /**
     * @return A rect containing the bounds of the tile ids that are valid in this view.
     * The viewport will not be scrollable past these bounds.
     */
    public Rect getTileIndexBounds();

    /**
     * Tell the provider which tiles are currently visible in the view, giving the provider
     * an opportunity to add new tiles to its rendering queue, or to remove stale tiles
     * from any caches.
     * @param tileIdRange The boundaries of the tile ids visible in the viewport
     */
    public void notifyTileIDRangeChange(Rect tileIdRange, Context ctx);

}