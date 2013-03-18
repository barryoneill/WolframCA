package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.tbv.TileProvider;
import net.nologin.meep.tbv.TileRange;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WolframTileProvider implements TileProvider {

    private int ruleNo;
    int renderOrderCnt = 1;

    int zoomFactor = 2;

    private int PIXEL_ON, PIXEL_OFF;

    private final Map<Integer,WolframTile> tileCache;
    private final List<WolframTile> renderQueue;

    private Context ctx;

    public WolframTileProvider(Context ctx, int ruleNo){

        this.ctx = ctx;

        this.ruleNo = ruleNo;

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);

        // doc
        tileCache = new ConcurrentHashMap<Integer,WolframTile>();

        // this will be emptied and rebuilt each time the set on on-screen tiles changes
        // Ensure only synchronized access at critical points!
        // renderQueue = Collections.synchronizedList(new LinkedList<WolframTile>());
        renderQueue = Collections.synchronizedList(new LinkedList<WolframTile>(){
            @Override
            public boolean add(WolframTile object) {
                Log.w(Utils.LOG_TAG,"RQ += " + object);
                return super.add(object);
            }
        });

    }

    public void changeRule(int newRule) {

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

    public int getRule(){
        return ruleNo;
    }

    @Override
    public TileRange getTileIndexBounds() {

        return new TileRange(-20,0,20,20);

    }

    @Override
    public int getTileWidthPixels() {
        return WolframTile.TILE_WIDTH_PX;
    }

    @Override
    public WolframTile getTileWithCache(int xId, int yId){

        int cacheKey = getCacheKey(xId, yId);

        WolframTile t = tileCache.get(cacheKey);
        if(t != null){
            return t;
        }

        // cache miss, new tile
        t = new WolframTile(xId,yId);

        // Log.w(Utils.LOG_TAG, " - Adding " + t + " to cache");
        tileCache.put(cacheKey,t);

        return t;

    }

    // TODO: revisit and properly justify/doc
    private int getCacheKey(int x, int y){

        return x << 16 ^ y;

    }


    @Override
    public void generateNextTile(TileRange visible) {

        WolframTile t;

        synchronized (renderQueue){
            if(renderQueue.isEmpty()){
                return;
            }
            t = renderQueue.remove(0);
        }

        // Log.w(Utils.LOG_TAG," ---> " + ((LinkedList)renderQueue).toString());


        //log("*** Rendering " + t + ", (queue size=" + renderQueue.size()+")");


        if(t == null){
            return;
        }

        processTileState(t, visible.contains(t));

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

                WolframTile preReq = getTileWithCache(x, curY);
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
        renderQueue.addAll(deps); // TODO: belongs here?

    }

    // TODO: **************************************************************************************************
    // TODO: **************************************************************************************************
    // TODO: calculate per zoom level!
    static final int DATASIZE_FOR_ZOOM = WolframTile.TILE_WIDTH_PX / 64;
    // TODO: **************************************************************************************************
    // TODO: **************************************************************************************************
    static final boolean[] EMPTY = new boolean[DATASIZE_FOR_ZOOM];

    private String TMP(boolean[] in){
        String r = "[";
        for(int i=0;i<in.length;i++){
            r += in[i] ? "1" : "0";
            if(i != in.length-1){
                r+=",";
            }
        }


        r += "]";
        return r;
    }

    private void processTileState(WolframTile t, boolean fillBitmap){

        Log.w(Utils.LOG_TAG, "Rendering tile: " + t);

        int[] bmpData = null;
        if(fillBitmap){
            bmpData = new int[DATASIZE_FOR_ZOOM*DATASIZE_FOR_ZOOM];
        }

        // hold the current state 'row', across three adjacent tiles (tile t in middle)
        boolean[] curState = new boolean[DATASIZE_FOR_ZOOM * 3];

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

                System.arraycopy(stateAL,0,prevState,0,DATASIZE_FOR_ZOOM);
                System.arraycopy(stateA,0,prevState,DATASIZE_FOR_ZOOM,DATASIZE_FOR_ZOOM);
                System.arraycopy(stateAR,0,prevState,DATASIZE_FOR_ZOOM*2,DATASIZE_FOR_ZOOM);

            }
            catch(IllegalStateException e){
                Log.w(Utils.LOG_TAG, "Cannot process tile " + t + ", error:" + e.getMessage());
                return;
            }
        }


        int leftPtr = 1, rightPtr = prevState.length-2;


        for (int row = 0; row < DATASIZE_FOR_ZOOM; row++) {

            // update 'newState' for the current row
            if(row == 0 && t.yId == 0){
                // don't do anything...
                // .. unless you're tile -1<=x<=1, in which case we need to fix your references to the start state
                // (-1 will see it on the right of curState, 0 will see it in the middle, 1 over to the left )
                if((t.xId == -1 || t.xId == 0 || t.xId == 1)){
                    curState[curState.length/2 - (t.xId * DATASIZE_FOR_ZOOM)] = true;
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
                int rowOffset = row * DATASIZE_FOR_ZOOM;
                for(int col=DATASIZE_FOR_ZOOM;col<DATASIZE_FOR_ZOOM*2;col++){
                    int val = curState[col] ? PIXEL_ON : PIXEL_OFF;
                    bmpData[rowOffset+col-DATASIZE_FOR_ZOOM] = val;
                }
            }

            // save the state of the bottom row
            if(row == DATASIZE_FOR_ZOOM-1){
                t.bottomState = new boolean[DATASIZE_FOR_ZOOM];
                System.arraycopy(curState,DATASIZE_FOR_ZOOM,t.bottomState,0,DATASIZE_FOR_ZOOM);
            }

            System.arraycopy(curState,0,prevState,0,prevState.length);


        }

        if(fillBitmap){

            // orig (roughly)
//            Bitmap bmp = Bitmap.createBitmap(WolframTile.TILE_WIDTH_PX,WolframTile.TILE_WIDTH_PX, Bitmap.Config.RGB_565);
//            bmp.setPixels(bmpData,0,DATASIZE_FOR_ZOOM,0,0,DATASIZE_FOR_ZOOM,DATASIZE_FOR_ZOOM);
//            t.bmpData = bmp;

            Bitmap bmp = Bitmap.createBitmap(DATASIZE_FOR_ZOOM,DATASIZE_FOR_ZOOM, Bitmap.Config.RGB_565);
            bmp.setPixels(bmpData,0,DATASIZE_FOR_ZOOM,0,0,DATASIZE_FOR_ZOOM,DATASIZE_FOR_ZOOM);

            // disable filters, or we'll lose edge sharpness
            t.bmpData = Bitmap.createScaledBitmap(bmp,WolframTile.TILE_WIDTH_PX,WolframTile.TILE_WIDTH_PX, false); // = bmp;


//
//            Bitmap.createBitmap()


        }

        t.renderOrder = renderOrderCnt++;

    }

    private boolean[] getBottomStateFromPrereqTile(int xId, int yId){
        WolframTile tile = tileCache.get(getCacheKey(xId,yId));
        if(tile == null){
            throw new IllegalStateException("Prerequisite tile (" + xId + "," + yId + ") not in cache");
        }
        if(tile.bottomState == null){
            throw new IllegalStateException("Prerequisite tile (" + xId + "," + yId + ") not yet processed");
        }
        return tile.bottomState;
    }

    public void notifyZoomFactorChangeTEMP(float newZoom) {



    }

    public void notifyTileIDRangeChange(TileRange newRange) {

        // wipe any bmp content that's not currently in view
        Collection<WolframTile> entries = tileCache.values();
        for(WolframTile t : entries){
            if(t.bottomState != null && t.bmpData != null && !newRange.contains(t)){
                // Log.e(Utils.LOG_TAG, "clearing out tile (" + t.xId + "," + t.yId + ")");
                t.bmpData = null;
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

                    WolframTile t = getTileWithCache(x, y);
                    if(t.bmpData != null){
                        continue;
                    }

                    addPrerequisites(t);
                    renderQueue.add(t);
                }
            }


            String q = "";
            for(WolframTile t : renderQueue){
                q += "(" + t.xId + "," + t.yId + "),";
            }
            Log.d(Utils.LOG_TAG,"RenderQ, size=" + renderQueue.size() + ", elems=" + q);

        }

    }

    @Override
    public String toString(){
        return String.format("[rule=%d,c=%d,q=%d]", ruleNo, tileCache.size(), renderQueue.size());
    }



}
