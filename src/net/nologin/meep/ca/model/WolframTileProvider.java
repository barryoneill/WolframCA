package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.tbv.GridAnchor;
import net.nologin.meep.tbv.Tile;
import net.nologin.meep.tbv.TileProvider;
import net.nologin.meep.tbv.TileRange;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WolframTileProvider implements TileProvider {

    private static final int DEFAULT_RULE = 110;
    private static final int DEFAULT_ZOOMLEVEL = 4;

    private int ruleNo;
    private int pixelsPerCell;
    int renderOrderCnt = 1;
    private int PIXEL_ON, PIXEL_OFF;

    private final ConcurrentMap<Long,WolframTile> tileCache;
    private final List<WolframTile> renderQueue;

    public WolframTileProvider(Context ctx){
        this(ctx, DEFAULT_RULE, DEFAULT_ZOOMLEVEL);
    }

    public WolframTileProvider(Context ctx, int ruleNo, int zoomLevel){

        // sanitize inputs
        this.ruleNo = ruleNo < 1 || ruleNo > 255 ? DEFAULT_RULE : ruleNo;  // default to rule 110
        this.pixelsPerCell = zoomLevel < 1 ? DEFAULT_ZOOMLEVEL : Utils.roundZoomLevel(zoomLevel); // default to 4px per cell

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);

        // doc
        tileCache = new ConcurrentHashMap<Long,WolframTile>();

        // this will be emptied and rebuilt each time the set on on-screen tiles changes
        // Ensure only synchronized access at critical points!
        // renderQueue = Collections.synchronizedList(new LinkedList<WolframTile>());
        renderQueue = Collections.synchronizedList(new LinkedList<WolframTile>());

    }

    public int getRule() {
        return ruleNo;
    }

    public void setRule(int newRule) {

        // sanity checking
        if(newRule < 0){
            Log.w(Utils.LOG_TAG,"Rule " + newRule + " smaller than range 0-255, defaulting to 0");
            newRule = 0;
        }
        if(newRule > 255){
            Log.w(Utils.LOG_TAG,"Rule " + newRule + " larger than range 0-255, defaulting to 255");
            newRule = 255;
        }

        ruleNo  = newRule;

        tileCache.clear();

        synchronized (renderQueue){
            renderQueue.clear();
        }

        renderOrderCnt = 1;

    }

    public int getPixelsPerCell() {
        return pixelsPerCell;
    }

    public void setPixelsPerCell(int newZoom) {

        // sanity checking
        newZoom = Utils.roundZoomLevel(newZoom);

        pixelsPerCell = newZoom;

        tileCache.clear();

        synchronized (renderQueue){
            renderQueue.clear();
        }

        renderOrderCnt = 1;
    }


    @Override
    public Integer[] getTileIndexBounds(){

        return new Integer[]{null,0,null,null}; //  { left, top, right, bottom }
    }

    @Override
    public int getTileWidthPixels() {
        return WolframTile.TILE_WIDTH_PX;
    }


    @Override
    public WolframTile getTile(int xId, int yId){


        WolframTile t = tileCache.get(Tile.getCacheKey(xId,yId));
        if(t != null){
            return t;
        }

        // cache miss, new tile
        t = new WolframTile(xId,yId);

        // Log.w(Utils.LOG_TAG, " - Adding " + t + " to cache");
        tileCache.put(t.cacheKey,t);

        return t;

    }

    @Override
    public GridAnchor getGridAnchor() {
        return GridAnchor.TopCenter;
    }

    @Override
    public boolean processQueue(TileRange visible) {

        WolframTile t;

        synchronized (renderQueue){
            if(renderQueue.isEmpty()){
                return false;
            }
            t = renderQueue.remove(0);
        }

        // Log.w(Utils.LOG_TAG," ---> " + ((LinkedList)renderQueue).toString());


        //log("*** Rendering " + t + ", (queue size=" + renderQueue.size()+")");


        if(t == null){
            return false;
        }

        processTileState(t, visible.contains(t));

        return true;
    }


    /*
     * As well as needing the tile above to be completed, each row of a tile is dependent on its neighbour's
     * touching boundary to be complete too.  If we required left and right to be complete before calculating,
     * we'd run into an infinite loop (left needs left needs left.. etc).  Instead, given we know the top of
     * the model is a row of blanks, we just require that the above left/above/right tiles be completed (resulting
     * in an inverted triangle of dependent tiles).  We then calculate the left and right boundaries on the fly (we don't
     * need to calculate the whole neighbour tile, just enough to populate the touching boundary - which only requires
     * the top row of the neighbour - avoiding the infinite loop).
     */
    private void addPrerequisites(WolframTile t){


        List<WolframTile> deps = new LinkedList<WolframTile>();

        if(t.yId <= 0){  // top row doesn't have prerequisites
            return;
        }

        int curY = t.yId-1;   // start one row up
        int curXMin = t.xId-1, curXMax = t.xId+1;  // scan from y-1 to y+1 of that parent row

        // keep looping up to the top row (y=0) of our model until we're satisfied all dependencies are met
        while(curY >= 0){

            boolean foundMissing = false;

            for(int x=curXMin; x <= curXMax; x++){

                // the impl of getTile() above puts the tile in the cache if it wasn't already there
                WolframTile preReq = getTile(x, curY);
                if(!renderQueue.contains(preReq) && preReq.bottomState == null){
                    foundMissing = true;
                    deps.add(preReq);
                }
            }

            if(!foundMissing){ // no missing prereq tiles were found, we can stop looking
                break;
            }

            // move up a row, expand left and right by one
            curXMin--;
            curXMax++;
            curY--;

        }

        // reverse the found dependencies, so higher up rows are before lower in the queue!
        Collections.reverse(deps);
        renderQueue.addAll(deps);

    }


    private void processTileState(WolframTile t, boolean fillBitmap){

        //Log.w(Utils.LOG_TAG, "Rendering tile: " + t);


        /**
         *  While each tile has a fixed size, the data contained within is defined by the
         *  'pixelsPerCell' zoom level.
         *
         *  TODO: At the moment, the zoom level is set by a slider in the menu. But it would be way nicer
         *  to have pinch-to-zoom support (see the notifyZoomFactorChange method).  For now we'll just
         *  do it this way.
         *
         */
        int cellsPerTile = WolframTile.TILE_WIDTH_PX / pixelsPerCell;


        int[] bmpData = null;
        if(fillBitmap){
            bmpData = new int[cellsPerTile*cellsPerTile];
        }

        // hold the current state 'row', across three adjacent tiles (tile t in middle)
        boolean[] curState = new boolean[cellsPerTile * 3];

        // hold the state of the row above curState
        boolean[] prevState = new boolean[curState.length];


        // for any tile not on the 'top' row, we need the state of the bottom
        // of each of the three tiles above (above left, directly above, above right)
        if(t.yId != 0){
            int yAbove = t.yId - 1;
            try {
                boolean[] stateAL = getBottomStateFromPrereqTile(t.xId - 1, yAbove);
                boolean[] stateA = getBottomStateFromPrereqTile(t.xId, yAbove);
                boolean[] stateAR = getBottomStateFromPrereqTile(t.xId + 1, yAbove);

                System.arraycopy(stateAL,0,prevState,0,cellsPerTile);
                System.arraycopy(stateA,0,prevState,cellsPerTile,cellsPerTile);
                System.arraycopy(stateAR,0,prevState,cellsPerTile*2,cellsPerTile);

            }
            catch(IllegalStateException e){
                Log.w(Utils.LOG_TAG, "Cannot process tile " + t + ", error:" + e.getMessage());
                return;
            }
        }


        int leftPtr = 1, rightPtr = prevState.length-2;


        for (int row = 0; row < cellsPerTile; row++) {

            // update 'newState' for the current row
            if(row == 0 && t.yId == 0){
                // don't do anything...
                // .. unless you're tile -1<=x<=1, in which case we need to fix your references to the start state
                // (-1 will see it on the right of curState, 0 will see it in the middle, 1 over to the left )
                if((t.xId == -1 || t.xId == 0 || t.xId == 1)){
                    curState[curState.length/2 - (t.xId * cellsPerTile)] = true;
                }
            }
            else {
                // all other rows in all other tiles, we simply rule check on the previous state
                for(int col = leftPtr; col <= rightPtr; col++){
                    curState[col] = WolframRuleTable.checkRule(ruleNo,prevState[col-1],prevState[col],prevState[col+1]);
                }
            }

            if(fillBitmap){
                // populate the 'bmpData' segment for this tile
                int rowOffset = row * cellsPerTile;
                for(int col=cellsPerTile;col<cellsPerTile*2;col++){
                    int val = curState[col] ? PIXEL_ON : PIXEL_OFF;
                    bmpData[rowOffset+col-cellsPerTile] = val;
                }
            }

            // save the state of the bottom row
            if(row == cellsPerTile-1){
                t.bottomState = new boolean[cellsPerTile];
                System.arraycopy(curState,cellsPerTile,t.bottomState,0,cellsPerTile);
            }

            System.arraycopy(curState,0,prevState,0,prevState.length);


        }

        if(fillBitmap){

            Bitmap bmp = Bitmap.createBitmap(cellsPerTile,cellsPerTile, Bitmap.Config.RGB_565);
            bmp.setPixels(bmpData,0,cellsPerTile,0,0,cellsPerTile,cellsPerTile);
            t.setBmpData(Bitmap.createScaledBitmap(bmp,WolframTile.TILE_WIDTH_PX, WolframTile.TILE_WIDTH_PX, false));


        }

        t.renderOrder = renderOrderCnt++;

    }


    private boolean[] getBottomStateFromPrereqTile(int xId, int yId){
        WolframTile tile = tileCache.get(Tile.getCacheKey(xId,yId));
        if(tile == null){
            throw new IllegalStateException("Prerequisite tile (" + xId + "," + yId + ") not in cache");
        }
        if(tile.bottomState == null){
            throw new IllegalStateException("Prerequisite tile (" + xId + "," + yId + ") not yet processed");
        }
        return tile.bottomState;
    }

    public void notifyZoomFactorChange(float newZoom) {

        // TODO: pixelsPerCell is currently set by the slider in the menu.  Perhaps this could be used for
        // more fine grained pinch-to-zoom functionality.

    }

    public void notifyTileIDRangeChange(TileRange newRange) {

        // wipe any bmp content that's not currently in view
        Collection<WolframTile> entries = tileCache.values();
        for(WolframTile t : entries){
            if(t.bottomState != null && t.getBmpData() != null && !newRange.contains(t)){
                // Log.e(Utils.LOG_TAG, "clearing out tile (" + t.xId + "," + t.yId + ")");
                t.clearBmpData();
            }
        }


        synchronized (renderQueue){
            // wipe the render queue
            renderQueue.clear();


            // these values used in inner 'x' loop
            int num_tiles_horizontal = newRange.numTilesHorizontal();
            int half_tiles_horizontal = num_tiles_horizontal/2;

            // work out what tiles need renderin'
            for(int y = newRange.top; y <= newRange.bottom; y++){

                /* rather than just loop x from x=visble.left to x<=visible.right, it looks a lot better
                 * to add the middle element first, then add alternate each side adding one element at
                 * a time, so that the tiles flow out from the center as they are rendered, rather than
                 * left to right
                 */
                for (int i=0; i<num_tiles_horizontal; i++) {

                    // an even i results in the next tile to the right, an odd to the left
                    int offset = half_tiles_horizontal + ( i% 2 == 0 ? i/2 : -(i/2+1));
                    int x = newRange.left + offset;

                    // the impl of getTile adds the tile to the cache!
                    WolframTile t = getTile(x, y);
                    if(t.getBmpData() != null){
                        continue;
                    }

                    addPrerequisites(t);
                    renderQueue.add(t);
                }
            }


            Log.d(Utils.LOG_TAG,"RenderQ, size=" + renderQueue.size());

        }

    }


    @Override
    public int getGridBufferSize() {
        return 0;  // better to have temporarily empty tiles than excessive generation
    }

    @Override
    public String getDebugSummary(){
        return String.format("WTP[ru=%d,ca=%d,q=%d]", ruleNo, tileCache.size(), renderQueue.size());
    }



}
