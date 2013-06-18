/*
 *    WolframCA - an android application to view 1-dimensional cellular automata (CA)
 *    Copyright 2013 Barry O'Neill (http://meep.nologin.net/)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.WolframUtils;
import net.nologin.meep.tbv.GridAnchor;
import net.nologin.meep.tbv.Tile;
import net.nologin.meep.tbv.TileProvider;
import net.nologin.meep.tbv.TileRange;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The WolframCA provider is a {@link TileProvider} implementation for the {@link net.nologin.meep.ca.view.WolframCAView}.
 * The grid is made up of tiles: Each tile contains many rows of 'cells', where each successive row (across all tiles in
 * which it appears) represents successive generations of a
 * <a href="http://mathworld.wolfram.com/ElementaryCellularAutomaton.html">Wolfram CA Rule</a>.  The number of cell
 * rows (generations) per tile is configured by the view.
 * <br/><br/>
 * The implementation of this gets complicated by the tiled nature of the provider.  Since each cell's state is dependent
 * on the previous generations's state and that of its two neighbours, cells that are at the edges of tiles are dependent
 * on cells that exist in other tiles.
 * <br/><br/>
 * For example, consider the following view of a tile, t (populated with <i>a-e</i>):
 * <pre>
 *      | 54321 | 11111 | 12345 |
 *     -+-------+-------+-------+-
 *      |  5432 | aaaaa | 2345  |
 *      |   543 | bbbbb | 345   |
 *      |    54 | ccccc | 45    |
 *      |     5 | ddddd | 5     |
 *      |       | eeeee |       |
 *     -+-------+-------+-------+-
 *      |       |       |       |
 *
 * </pre>
 * For cells in row 'a', we need all the cells marked '1'.  For row 'b', we need all cells in area 'a', and those
 * marked '2', and so on.  As we go down the rows in the tile, our list of prerequisite cells grows as well,
 * and results in an inverted triangle of prerequisite cells (and by extension, tiles).
 * <br/><br/>
 * Note: For the first row of cells in the first row of tiles, there are no dependencies - We manually set this row/
 * generation as all off/false, but leave the center cell in tile x=0 to be true/on).  This is our starting generation.
 * All other cell rows follow the 3-cell dependency.
 * <br/><br/>
 * <b>Implementation</b>:<br/>
 * When the view requests a set of tiles, all prerequisite tiles are added to a processing queue, and a background
 * task processess these in such an order that the dependencies are met for each tile.  In an ideal world, we'd
 * just store the bitmap, and move on.  Unfortnately, this will quickly lead to the exhaustion of heap space.
 * Rather than resort to local storage, we wipe the bitmaps of tiles that have gone far enough out of view.  However,
 * we maintain a boolean array in each tile containing the state of the last cell row/generation.  That way, when a
 * tile is re-requested, rather than build all the dependent tiles all over again, we only need go one tile row up,
 * and we have the state required to regenerate the cell data we need.
 * <br><br/>
 */
public class WolframTileProvider implements TileProvider {

    /* Improvements that can be made in future versions:
     * - Increase heap usage (see OFFSCREEN_TILE_BUFFER) - possibly to the exclusions of older devices
     * - Add a flag to tiles that result in common patterns (eg all off/on), and when requests for bitmap data
     *   occur, reuse a common copy.
     * - Many rules result in repeating patters, perhaps add detection for these, cutting off a lot of calculation
     * - Add hints to many of the rules that don't generate anything on one half of the space (eg rule 110), so we
     *   can shortcircuit processing/prerequisites for those.
     */

    /**
     * Default rule (110), should a rule not be specified during construction or via {@link #setPixelsPerCell(int)}
     */
    public static final int DEFAULT_RULE = 110;

    /**
     * Default zoom level (size of cell width in pixels).  If {@link WolframUtils#getNumCores()} reports a single
     * core device, this will have value 6 (less processor intensive), otherwise value 2 (finer detail).
     */
    public static final int DEFAULT_ZOOMLEVEL = WolframUtils.getNumCores() == 1 ? 6 : 2;

    // how many tiles wide a tile can be scrolled offscreen before getting cleared (caching performance v memory)
    private static final int OFFSCREEN_TILE_BUFFER = 3;

    private int ruleNo;
    private int pixelsPerCell;
    private int colorPixelOn, colorPixelOff;

    // background tasks here will set it, view's rendering thread (via hasFreshData()) will poll it
    private AtomicBoolean hasFreshData = new AtomicBoolean(false);

    /* All referenced tiles get cached here, even though their bitmap content will be cleared as necessary (see
     * OFFSCREEN_TILE_BUFFER). Must be mulithread friendly as it'll be accessed indirectly by view's rendering thread
     * via getTile(), as well as by tile generation stuff here. */
    private final ConcurrentMap<Long, WolframTile> tileCache;

    private ExecutorService executorService;
    private Future lastSubmittedTask;

    /**
     * Constructor, defaulting the rule number to {@link #DEFAULT_RULE} and zoom level to {@link #DEFAULT_ZOOMLEVEL}
     *
     * @param ctx the context
     */
    public WolframTileProvider(Context ctx) {
        this(ctx, DEFAULT_RULE, DEFAULT_ZOOMLEVEL);
    }

    /**
     * Constructor
     *
     * @param ctx       The context
     * @param ruleNo    The rule number (0-255). Invalid rule numbers will result in {@link #DEFAULT_RULE}.
     * @param zoomLevel The zoomLevel (1-16, step of 2). Invalid levels will be adjusted to the nearest valid value.
     */
    public WolframTileProvider(Context ctx, int ruleNo, int zoomLevel) {

        this.ruleNo = ruleNo < 1 || ruleNo > 255 ? DEFAULT_RULE : ruleNo;
        this.pixelsPerCell = zoomLevel < 1 ? DEFAULT_ZOOMLEVEL : WolframUtils.sanitizeZoom(zoomLevel);

        // an easy future feature would be to make this configurable
        colorPixelOn = ctx.getResources().getColor(R.color.CAView_PixelOn);
        colorPixelOff = ctx.getResources().getColor(R.color.CAView_PixelOff);

        // as mentioned in field comment, this should be multi-thread friendly
        tileCache = new ConcurrentHashMap<Long, WolframTile>();

        Log.i(WolframUtils.LOG_TAG, "WolframTileProvider created, rule=" + ruleNo + ", pixelsPerCell=" + pixelsPerCell);
    }

    /**
     * @return The currently set rule number
     */
    public int getRule() {
        return ruleNo;
    }

    /**
     * Set a new rule number
     *
     * @param newRule The new rule number.  If not in range 0-255, {@link #DEFAULT_RULE} will be used
     */
    public void setRule(int newRule) {

        if (newRule < 1 || newRule > 255) {
            Log.w(WolframUtils.LOG_TAG, "Rule " + newRule + " not in range 0-255, defaulting to " + DEFAULT_RULE);
            newRule = DEFAULT_RULE;
        }

        ruleNo = newRule;
        tileCache.clear();
    }

    /**
     * Get the number of pixels wide that each cell will be rendered
     *
     * @return The number of pixels
     */
    public int getPixelsPerCell() {
        return pixelsPerCell;
    }

    /**
     * Set the number of pixels wide that each cell should be rendered
     *
     * @param newZoom The number of pixels (1-16, step of 2). Invalid values will be adjusted to the nearest valid one.
     */
    public void setPixelsPerCell(int newZoom) {

        newZoom = WolframUtils.sanitizeZoom(newZoom);

        pixelsPerCell = newZoom;
        tileCache.clear();
    }


    @Override
    public Integer[] getConfigTileIDLimits() {

        /* Each CA generation is rendered below the previous, so there's no sense in letting the user scroll upwards
         * beyond y=0 - there's nothing to generate there. (All other scrolling is unlimited) */
        return new Integer[]{null, 0, null, null};
    }

    @Override
    public GridAnchor getConfigGridAnchor() {

        // have the (0,0) tile (where our first generation is drawn) be anchored to the top of the screen
        return GridAnchor.TopCenter;
    }

    @Override
    public int getConfigTileSize() {
        return Tile.DEFAULT_TILE_SIZE; // default is fine
    }

    @Override
    public WolframTile getTile(int xId, int yId) {

        // Return cache hits, otherwise create, cache and return. Async processing triggered by onTileIDRangeChange
        WolframTile t = tileCache.get(Tile.createCacheKey(xId, yId));
        if (t != null) {
            return t;
        }

        t = new WolframTile(xId, yId);
        tileCache.put(t.cacheKey, t);
        return t;

    }

    @Override
    public boolean hasFreshData() {

        // Set by WolframQueueProcessorTask on new data. Reset value on poll to prevent pointless re-rendering.
        return hasFreshData.getAndSet(false);
    }


    @Override
    public void onZoomFactorChange(float newZoom) {
        // NOP - At the moment, pixelsPerCell is set by a menu item, but pinch-to-zoom is a possibile future feature
    }

    @Override
    public void onTileIDRangeChange(TileRange newRange) {

        // free up bitmap data of non-rendered tiles (observing OFFSCREEN_TILE_BUFFER). Tile lastCellRow stays.
        Collection<WolframTile> entries = tileCache.values();
        for (WolframTile t : entries) {
            if (t.lastCellRow != null && t.getBmpData() != null && !newRange.contains(t, OFFSCREEN_TILE_BUFFER)) {
                t.clearBmpData();
            }
        }

        List<WolframTile> renderQueue = new LinkedList<WolframTile>();

        /* Adding the tiles to the renderQueue in a row-by-row, cell-by-cell manner is not very optimal, given that
         * the chain of prerequisites (calculated by addPrerequisites) is an inverted triangle.  It's better to add
         * the tiles that are vertically center in newRange first, then work outwards.  This results in these more
         * central tiles appearing first, before boundary tiles that may require a lot of off-screen tile processing.
         * I.e. - The user sees a constant trickle of tiles, rather than nothing for a while, then all tiles at once. */
        int num_tiles_horizontal = newRange.numTilesHorizontal();
        int half_tiles_horizontal = num_tiles_horizontal / 2;

        for (int y = newRange.top; y <= newRange.bottom; y++) {

            for (int i = 0; i < num_tiles_horizontal; i++) {

                // an even i results in the next tile to the right, an odd to the left
                int offset = half_tiles_horizontal + (i % 2 == 0 ? i / 2 : -(i / 2 + 1));
                int x = newRange.left + offset;

                // the impl of getTile adds the tile to the cache!
                WolframTile t = getTile(x, y);
                if (t.getBmpData() != null) {
                    continue;
                }

                addPrerequisites(t, renderQueue);
                renderQueue.add(t);
            }
        }

        // init the executor if not ready, and try to kill any previous task, should one exist
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }
        if (lastSubmittedTask != null) {
            lastSubmittedTask.cancel(true);
        }

        // fire off the async job
        Log.d(WolframUtils.LOG_TAG, "Starting async queue processing, queue size:" + renderQueue.size());
        lastSubmittedTask = executorService.submit(new WolframQueueProcessorTask(renderQueue, newRange));
    }


    /**
     * Add any (unprocessed) preqrequisite tiles for the specified tile to the queue.  To generate any given tile,
     * we need access to the last generation of cells in the above left, above, and above right tiles.  If those
     * tiles haven't been processed, we keep iterating up the inverse triangle of dependencies (adding all those
     * found to the queue), until we hit the top row (y=0).
     *
     * @param t           The tile whose prerequisite tiles we are searching for
     * @param renderQueue The queue to add any unprocessed prerequisite tiles to
     */
    private void addPrerequisites(WolframTile t, List<WolframTile> renderQueue) {

        List<WolframTile> deps = new LinkedList<WolframTile>();

        if (t.yId <= 0) {  // top tile doesn't have prerequisite tiles
            return;
        }

        int curY = t.yId - 1;   // start one tile row up
        int curXMin = t.xId - 1, curXMax = t.xId + 1;  // scan from y-1 to y+1 of that parent tile row

        // keep looping up to the top tile row (y=0) unless we hit a set of already processed prerequisite tiles first
        while (curY >= 0) {

            boolean foundMissing = false;

            for (int x = curXMin; x <= curXMax; x++) {

                // the impl of getTile() above puts the tile in the cache if it wasn't already there
                WolframTile preReq = getTile(x, curY);
                if (!renderQueue.contains(preReq) && preReq.lastCellRow == null) {
                    foundMissing = true;
                    deps.add(preReq);
                }
            }

            // if the current level of prerequisite tiles are already processed, we're done
            if (!foundMissing) {
                break;
            }

            // move up a tile row, expand left and right by one tile
            curXMin--;
            curXMax++;
            curY--;

        }

        // we added deps while working upwards - reverse this so higher up tiles get processed first!
        Collections.reverse(deps);
        renderQueue.addAll(deps);

    }

    /**
     * An instance of this task is started every time {@link #onTileIDRangeChange(net.nologin.meep.tbv.TileRange)}
     * generates a new list of required tiles.  This task processes each tile in order, and when done, toggles the
     * flag that {@link #hasFreshData()} checks when polled.
     */
    class WolframQueueProcessorTask implements Runnable {

        private List<WolframTile> renderQueue;
        private TileRange visibleRange;

        public WolframQueueProcessorTask(List<WolframTile> renderQueue, TileRange visibleRange) {

            this.renderQueue = renderQueue;
            this.visibleRange = visibleRange;

        }

        @Override
        public void run() {

            Log.d(WolframUtils.LOG_TAG, "WolframQueueProcessorTask task starting");

            /* this task ends as soon as the queue is finished.  If the provider detects a change in the
             * list of required tiles, it'll start a new task (requesting that this one stop) */
            while (!renderQueue.isEmpty()) {

                // check for interruption every time, we need to stop quickly if requested
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(WolframUtils.LOG_TAG, "WolframQueueProcessorTask interrupted (new task incoming?)");
                    return;
                }

                WolframTile t = renderQueue.remove(0);

                // sanity check for null or already processed tiles
                if (t == null || t.getBmpData() != null) {
                    continue;
                }

                Log.d(WolframUtils.LOG_TAG, "WolframQueueProcessorTask processing tile " + t);

                processTileState(t, visibleRange.contains(t));

                // allow the hasFreshData() interface method to report that there's new data available
                hasFreshData.set(true);
            }

            Log.d(WolframUtils.LOG_TAG, "WolframQueueProcessorTask task finished normally");
        }

        /**
         * Calculate the state of all the cells in the tile. When done, we record the state of the last row of cells
         * in the tile. If requested, the bitmap data to be rendered for this tile is also generated and stored.
         *
         * @param t          The tile to process
         * @param fillBitmap If <code>true</code>, the bitmap data for this cell is generated and stored in the tile,
         *                   otherwise just the last cell row will be stored.
         */
        private void processTileState(WolframTile t, boolean fillBitmap) {

            // how many cells high/wide our square tile measures
            int cellsPerEdge = Tile.DEFAULT_TILE_SIZE / pixelsPerCell;

            // bitmap data is stored as an int array updated row by row, then converted to bmp at the end
            int[] bmpData = null;
            if (fillBitmap) {
                bmpData = new int[cellsPerEdge * cellsPerEdge]; // rows * cols
            }

            // 'current' generation, across 3 tiles (tile t in center)
            boolean[] curGenCells = new boolean[cellsPerEdge * 3];

            // 'next' generation (same length)
            boolean[] nextGenCells = new boolean[curGenCells.length];

            /* Copy the last cell row in the top-left, top and top-right tiles into each third of the 'curGenCells'
             * array respectively - this is our 'starting state' (first tile row has no starting state) */
            if (t.yId != 0) {
                int yAbove = t.yId - 1;
                try {
                    boolean[] stateAL = getLastCellRowState(t.xId - 1, yAbove);
                    boolean[] stateA = getLastCellRowState(t.xId, yAbove);
                    boolean[] stateAR = getLastCellRowState(t.xId + 1, yAbove);

                    System.arraycopy(stateAL, 0, curGenCells, 0, cellsPerEdge);
                    System.arraycopy(stateA, 0, curGenCells, cellsPerEdge, cellsPerEdge);
                    System.arraycopy(stateAR, 0, curGenCells, cellsPerEdge * 2, cellsPerEdge);

                } catch (IllegalStateException e) {
                    Log.w(WolframUtils.LOG_TAG, "Cannot process tile " + t + ", error:" + e.getMessage());
                    return;
                }
            }

            // set end pointers to one element in from each side of nextGenCells (see class doc for logic)
            int leftPtr = 1, rightPtr = curGenCells.length - 2;

            for (int row = 0; row < cellsPerEdge; row++) { // for each row of cells in tile t


                if (row == 0 && t.yId == 0) {

                   /* For all our rules, we start our very first row of cells, in every tile on the first row (y=0)
                    * to be false(off), except for one cell right in the middle of tile x=0.  This is our CA starting
                    * data.
                    *
                    * If the xId==0, we set the first cell row's middle cell to true.  However, we shouldn't forget
                    * that neighbouring tiles x=-1 and x=1 need to see this value (in the right and left segments of
                    * nextGenCells respectively) */
                    if ((t.xId == -1 || t.xId == 0 || t.xId == 1)) {
                        nextGenCells[nextGenCells.length / 2 - (t.xId * cellsPerEdge)] = true;
                    }

                } else {
                    // for all other cell rows in all other tiles, simply do a rule lookup
                    for (int col = leftPtr; col <= rightPtr; col++) {
                        nextGenCells[col] = WolframRuleTable.getNextState(ruleNo,
                                curGenCells[col - 1], curGenCells[col], curGenCells[col + 1]);
                    }
                }

                // mid segment of nextGenCells holds the data for the respective row of the bitmap content (if needed)
                if (fillBitmap) {
                    int rowOffset = row * cellsPerEdge;
                    for (int col = cellsPerEdge; col < cellsPerEdge * 2; col++) {
                        int val = nextGenCells[col] ? colorPixelOn : colorPixelOff;
                        bmpData[rowOffset + col - cellsPerEdge] = val;
                    }
                }

                /* finally, regardless of whether we want a bitmap or not, we keep a copy of the last cell row
                 * (the mid segment of nextGenCells) */
                if (row == cellsPerEdge - 1) {
                    t.lastCellRow = new boolean[cellsPerEdge];
                    System.arraycopy(nextGenCells, cellsPerEdge, t.lastCellRow, 0, cellsPerEdge);
                }

                // the 'next' generation becomes the current, and we loop
                System.arraycopy(nextGenCells, 0, curGenCells, 0, curGenCells.length);

            }

            // finally, if needed, we convert the int array of colours into the desired bitmap
            if (fillBitmap) {
                Bitmap bmp = Bitmap.createBitmap(cellsPerEdge, cellsPerEdge, Bitmap.Config.RGB_565);
                bmp.setPixels(bmpData, 0, cellsPerEdge, 0, 0, cellsPerEdge, cellsPerEdge);
                t.setBmpData(Bitmap.createScaledBitmap(bmp, Tile.DEFAULT_TILE_SIZE, Tile.DEFAULT_TILE_SIZE, false));
            }

        }


        // Convenience method to get the last row of cells from the desired tile with some sanity checking
        private boolean[] getLastCellRowState(int xId, int yId) {
            WolframTile tile = tileCache.get(Tile.createCacheKey(xId, yId));
            if (tile == null) {
                throw new IllegalStateException("Prerequisite tile (" + xId + "," + yId + ") not in cache");
            }
            if (tile.lastCellRow == null) {
                throw new IllegalStateException("Prerequisite tile (" + xId + "," + yId + ") not yet processed");
            }
            return tile.lastCellRow;
        }
    }

    @Override
    public void onSurfaceDestroyed() {

        // ensure we don't leave any hanging threads
        if (executorService != null) {
            executorService.shutdownNow();
        }

    }

    @Override
    public String getDebugSummary() {
        return String.format("WolframProv[r=%d,cache=%d]", ruleNo, tileCache.size());
    }

}
