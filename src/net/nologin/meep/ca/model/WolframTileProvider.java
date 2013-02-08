package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.tbv.TileProvider;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WolframTileProvider implements TileProvider {

    private int ruleNo;
    int renderOrderCnt = 1;

    private int PIXEL_ON, PIXEL_OFF;

    private Paint paint_tileDebugTxt;

    private final Map<Integer,WolframTile> tileCache;
    private final List<WolframTile> renderQueue;

    private boolean displayDebug;

    public WolframTileProvider(Context ctx, int ruleNo, boolean renderDebug){

        this.ruleNo = ruleNo;

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);

        paint_tileDebugTxt = new Paint();
        paint_tileDebugTxt.setColor(Color.WHITE);
        paint_tileDebugTxt.setTextSize(20);
        paint_tileDebugTxt.setAntiAlias(true);
        paint_tileDebugTxt.setTextAlign(Paint.Align.CENTER);

        // doc
        tileCache = new ConcurrentHashMap<Integer,WolframTile>();

        // this will be emptied and rebuilt each time the set on on-screen tiles changes
        // Ensure only synchronized access at critical points!
        renderQueue = Collections.synchronizedList(new LinkedList<WolframTile>());

        this.displayDebug = renderDebug;
    }


    public void changeRule(int newRule) {

        if(newRule < 0 || newRule > 255){
            Utils.log("Rule not in range 0-255: " + newRule);
            return;
        }

        ruleNo  = newRule;

        tileCache.clear();

        synchronized (renderQueue){
            renderQueue.clear();
        }

        renderOrderCnt = 1;

    }

    @Override
    public int getTileSize(){
        return WolframTile.TILE_SIZE;
    }

    @Override
    public Rect getTileIndexBounds() {

        return new Rect(-20,0,20,20);

    }

    @Override
    public WolframTile getTile(int xId, int yId){

        int cacheKey = getCacheKey(xId, yId);

        WolframTile t = tileCache.get(cacheKey);
        if(t != null){
            return t;
        }

        // cache miss, new tile
        t = new WolframTile(xId,yId);

        Log.w(Utils.LOG_TAG, " - Adding " + t + " to cache");
        tileCache.put(cacheKey,t);

        return t;

    }

    // TODO: revisit and properly justify/doc
    private int getCacheKey(int x, int y){

        return x << 16 ^ y;

    }


    @Override
    public void generateNextTile() {

        WolframTile t;

        synchronized (renderQueue){
            if(renderQueue.isEmpty()){
                return;
            }
            t = renderQueue.remove(0);
        }

        // Log.w(Utils.LOG_TAG," ---> " + ((LinkedList)renderQueue).toString());


        //log("*** Rendering " + t + ", (queue size=" + renderQueue.size()+")");

        int tsize = getTileSize();

        if(t == null){
            return;
        }

        generateBmpForTile(t);

        if(t.bmpData == null){
            return;
        }


        if(displayDebug){
            Canvas c = new Canvas(t.bmpData);
            c.drawText("(" + t.xId + "," + t.yId + ",r=" + t.renderOrder + ")",tsize/2,tsize/2,paint_tileDebugTxt);
        }

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

        if(t.yId <= 0){  // top row doesn't have prerequisites
            return;
        }

        int curY = t.yId-1;   // start one row up
        int curXMin = t.xId-1, curXMax = t.xId+1;  // scan from y-1 to y+1 of that parent row

        // keep looping up to the top row (y=0) of our model until we're satisfied all dependencies are met
        while(curY >= 0){

            boolean foundMissing = false;

            for(int x=curXMin; x <= curXMax; x++){

                WolframTile preReq = getTile(x,curY);
                if(!renderQueue.contains(preReq) && preReq.bmpData == null){
                    foundMissing = true;
                    renderQueue.add(preReq); // TODO: belongs here?
                }
            }

            if(!foundMissing){ // no missing prereq tiles were found, we can stop looking
                return;
            }

            // move up a row, expand left and right by one
            curXMin--;
            curXMax++;
            curY--;

        }


    }

    private void generateBmpForTile(WolframTile t){

        int TSIZE = getTileSize();

        // to build a tile, we need to know the state of the bottom row of the three tiles above
        boolean[] stateTL, stateT, stateTR;
        if(t.yId == 0){
            stateTL = WolframTile.CA_EMPTY_STATE;
            stateT = WolframTile.CA_EMPTY_STATE;
            stateTR = WolframTile.CA_EMPTY_STATE;
        }
        else{
            int yAbove = t.yId - 1;
            try {
                stateTL = getBottomStateFromPrereqTile(t.xId - 1, yAbove);
                stateT = getBottomStateFromPrereqTile(t.xId, yAbove);
                stateTR = getBottomStateFromPrereqTile(t.xId + 1, yAbove);
            }
            catch(IllegalStateException e){
                Log.w(Utils.LOG_TAG, "Generating bmpData for " + t + ", error:" + e.getMessage());
                return;
            }
        }

        int[] bmpData = new int[TSIZE*TSIZE];

        boolean[] prevState = new boolean[TSIZE * 3];
        boolean[] newState = new boolean[prevState.length];
        System.arraycopy(stateTL,0,prevState,0,TSIZE);
        System.arraycopy(stateT,0,prevState,TSIZE,TSIZE);
        System.arraycopy(stateTR,0,prevState,TSIZE*2,TSIZE);

        int leftPtr = 1, rightPtr = prevState.length-2;

        for (int row = 0; row < TSIZE; row++) {

            // update 'newState' for the current row
            if(row == 0 && t.yId == 0 && (t.xId == -1 || t.xId == 0 || t.xId == 1)){
                newState[newState.length/2 - (t.xId * TSIZE)] = true;
            }
            else {
                for(int col = leftPtr; col < rightPtr; col++){
                    newState[col] = WolframRuleTable.checkRule(ruleNo,prevState[col-1],prevState[col],prevState[col+1]);
                }
            }

            // populate the 'bmpData' segment for this tile
            int rowOffset = row * TSIZE;
            for(int col=TSIZE;col<TSIZE*2;col++){
                int val = newState[col] ? PIXEL_ON : PIXEL_OFF;
                bmpData[rowOffset+col-TSIZE] = val;
            }

            // save the state of the bottom row
            if(row == TSIZE-1){
                t.bottomState = new boolean[WolframTile.TILE_SIZE];
                System.arraycopy(newState,TSIZE,t.bottomState,0,TSIZE);
            }

            System.arraycopy(newState,0,prevState,0,prevState.length);
        }

        Bitmap bmp = Bitmap.createBitmap(TSIZE,TSIZE, Bitmap.Config.RGB_565);
        bmp.setPixels(bmpData,0,TSIZE,0,0,TSIZE,TSIZE);
        t.bmpData = bmp;

        t.renderOrder = renderOrderCnt++;

    }

    private boolean[] getBottomStateFromPrereqTile(int xId, int yId){
        WolframTile tile = tileCache.get(getCacheKey(xId,yId));
        if(tile == null){
            throw new IllegalStateException("Required tile (" + xId + "," + yId + ") not in cache");
        }
        if(tile.bottomState == null){
            throw new IllegalStateException("Required tile (" + xId + "," + yId + ") not rendered");
        }
        return tile.bottomState;
    }


    public void notifyTileIDRangeChange(Rect currentViewportIDRange, Context ctx) {

        Log.w(Utils.LOG_TAG,"TILE ID RANGE CHANGE - " + currentViewportIDRange);

        // wipe any bmp content that's not currently in view
        Collection<WolframTile> entries = tileCache.values();
        for(WolframTile t : entries){
            if(t.bottomState != null && t.bmpData != null && !currentViewportIDRange.contains(t.xId,t.yId)){
                Log.e(Utils.LOG_TAG, "clearing out tile (" + t.xId + "," + t.yId + ")");
                t.bmpData = null;
            }
        }


        synchronized (renderQueue){
            // wipe the render queue
            renderQueue.clear();

            String q = "";

            // work out what tiles need renderin'
            for(int y = currentViewportIDRange.top; y <= currentViewportIDRange.bottom; y++){
                for(int x = currentViewportIDRange.left; x <= currentViewportIDRange.right; x++){

                    WolframTile t = getTile(x,y);
                    addPrerequisites(t);
                    renderQueue.add(t);
                    q += "(" + t.xId + "," + t.yId + ")";

                }
            }

            Log.e(Utils.LOG_TAG,"RenderQ = " + q);

        }

    }

    @Override
    public String toString(){
        return String.format("[rule=%d,c=%d,q=%d]", ruleNo, tileCache.size(), renderQueue.size());
    }



}
