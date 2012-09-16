package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.ca.view.TiledBitmapView;
import static net.nologin.meep.ca.util.Utils.log;

import java.util.*;

public class WolframTileProvider implements TiledBitmapView.TileProvider {

    private HashMap<Integer,WolframTile> tileCache;
    private int ruleNo = 0;

    private Queue<WolframTile> renderQueue;

    int renderOrderCnt = 1;

    private int PIXEL_ON, PIXEL_OFF;

    private Paint paint_tileDebugTxt;


    public WolframTileProvider(Context ctx, int ruleNo){

        tileCache = new HashMap<Integer,WolframTile>();
        renderQueue = new PriorityQueue<WolframTile>();

        this.ruleNo = ruleNo;

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);

        paint_tileDebugTxt = new Paint();
        paint_tileDebugTxt.setColor(Color.WHITE);
        paint_tileDebugTxt.setTextSize(20);
        paint_tileDebugTxt.setAntiAlias(true);
        paint_tileDebugTxt.setTextAlign(Paint.Align.CENTER);

    }


    public void changeRule(int newRule) {

        if(newRule < 0 || newRule > 255){
            Utils.log("Rule not in range 0-255: " + newRule);
            return;
        }

        ruleNo  = newRule;
        tileCache = new HashMap<Integer,WolframTile>();
        renderQueue = new PriorityQueue<WolframTile>();
        renderOrderCnt = 1;

    }

    @Override
    public void onSurfaceChange(int width, int height) {
            // TODO: needed?
    }


    @Override
    public int getTileSize(){
        return WolframTile.TILE_SIZE;
    }


    @Override
    public Tile getTile(int xId, int yId){

        int cacheKey = getCacheKey(xId, yId);

        // cache check
        WolframTile t = tileCache.get(cacheKey);
        if(t != null){
            return t;
        }

        // cache miss, new tile
        t = new WolframTile(xId,yId);
        tileCache.put(cacheKey,t);

        // to be rendered later in another thread
        renderQueue.add(t);

        return t;

    }

    private int getCacheKey(int x, int y){

        return x << 16 ^ y;

    }


    @Override
    public void renderNext() {

        if(renderQueue.isEmpty()){
            return;
        }

        // take next off queue.  If that has non-calculated prereqs, add them to the model
        //WolframTile t = renderQueue.poll();

        WolframTile t;
        do {
            t = renderQueue.peek();
        }
        while(addPrerequisites(t));

        renderQueue.remove();


        log("*** Rendering " + t + ", (queue size=" + renderQueue.size()+")");

        int tsize = getTileSize();

        renderRuleData(t);

        // do some debug
        Canvas c = new Canvas(t.bitmap);
        c.drawText("(" + t.xId + "," + t.yId + ",r=" + t.renderOrder + ")",tsize/2,tsize/2,paint_tileDebugTxt);

    }


    /*
     * As well as needing the tile above to be completed, each row of a tile is dependent on its neighbour's
     * touching boundary to be complete too.  If we required left and right to be complete before calculating,
     * we'd run into an infinite loop (left needs left needs left.. etc).  Instead, given we know the top of
     * the model is a row of blanks, we just require that the above left/above/right tiles be completed (resulting
     * in an inverse triangle of dependencies).  We then calculate the left and right boundaries on the fly (we don't
     * need to calculate the whole neighbour tile, just enough to populate the touching boundary - which only requires
     * the top row of the neighbour - avoiding the infinite loop).
     */
    private boolean addPrerequisites(WolframTile t){

        if(t.yId <= 0){  // top row doesn't have prerequisites
            return false;
        }

        int curY = t.yId-1;   // start one row up
        int curXMin = t.xId-1, curXMax = t.xId+1;  // scan from y-1 to y+1 of that parent row

        // keep looping up to the top row (y=0) of our model until we're satisfied all dependencies are met
        while(curY >= 0){

            boolean foundMissing = false;

            for(int x=curXMin; x <= curXMax; x++){

                // add all missing prereqs to the queue
                Tile preReq = getTile(x,curY); // a 'get' adds any non-existing tile to the model & render queue
                if(!preReq.renderFinished()){
                    foundMissing = true;
                }
            }

            if(!foundMissing){ // no missing prereq tiles were found, we can stop looking
                return false;
            }

            // move up a row, expand left and right by one
            curXMin--;
            curXMax++;
            curY--;

        }

        return true; // t had prerequisites which were added

    }

    private void renderRuleData(WolframTile t){

        int TSIZE = getTileSize();

        boolean[] stateTL, stateT, stateTR;
        if(t.yId == 0){
            stateTL = WolframTile.CA_EMPTY_STATE;
            stateT = WolframTile.CA_EMPTY_STATE;
            stateTR = WolframTile.CA_EMPTY_STATE;
        }
        else{
            stateTL = getRequiredBottomState(t.xId-1,t.yId-1);
            stateT = getRequiredBottomState(t.xId, t.yId - 1);
            stateTR = getRequiredBottomState(t.xId + 1, t.yId - 1);
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
                 System.arraycopy(newState,TSIZE,t.bot,0,TSIZE);
            }

            System.arraycopy(newState,0,prevState,0,prevState.length);
            //prevState = newState;

        }

        t.bitmap = Bitmap.createBitmap(TSIZE,TSIZE, Bitmap.Config.RGB_565);
        t.bitmap.setPixels(bmpData,0,TSIZE,0,0,TSIZE,TSIZE);

        t.renderOrder = renderOrderCnt++;


    }

    private boolean[] getRequiredBottomState(int xId, int yId){
        WolframTile tile = tileCache.get(getCacheKey(xId,yId));
        if(tile == null){
            throw new IllegalStateException("Required tile (" + xId + "," + yId + ") not in cache");
        }
        if(!tile.renderFinished()){ // TODO (maybe not render required, but state)
            throw new IllegalStateException("Required tile (" + xId + "," + yId + ") not rendered yet");
        }
        return tile.bot;
    }





    @Override
    public String toString(){
        return String.format("[rule=%d,c=%d,q=%d]",ruleNo,tileCache.size(),renderQueue.size());
    }


}
