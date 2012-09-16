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

        int curY = t.yId-1;   // start one row up
        int curXMin = t.xId-1, curXMax = t.xId+1;  // scan from y-1 to y+1 of that parent row


        if(curY <= 0){  // top row doesn't have prerequisites
            return false;
        }

        // keep looping up to the top row (x=0) of our model until we're satisfied all dependencies are met
        while(curY > 0){

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

        int tsize = getTileSize();



        WolframTile above = tileCache.get(getCacheKey(t.xId,t.yId-1));
        WolframTile left = tileCache.get(getCacheKey(t.xId-1,t.yId));
        WolframTile right = tileCache.get(getCacheKey(t.xId+1,t.yId));

        boolean[] aboveState = above != null ? above.bot : WolframTile.CA_EMPTY_STATE;
        boolean[] leftState = left != null ? left.right : WolframTile.CA_EMPTY_STATE;
        boolean[] rightState = right != null ? right.left : WolframTile.CA_EMPTY_STATE;

        // we need to calculate the state of the first row (t.top) before calling renderPreppedTile
        if(t.isOrigin()){
            // origin starts off with our hardcoded one-pixel-in-the-middle state
            t.top = WolframTile.CA_START_STATE;
        }
        else {
            // otherwise, we base it on the neighbours
            WolframTile aboveLeft = tileCache.get(getCacheKey(t.xId-1,t.yId-1));
            WolframTile aboveRight = tileCache.get(getCacheKey(t.xId-1,t.yId-1));

            boolean prevLeft = aboveLeft != null && aboveLeft.bot[tsize - 1];
            boolean prevRight = aboveRight != null && aboveRight.bot[0];

            t.top = calculateNextRowState(prevLeft, aboveState, prevRight);

        }

        // init the bitmap, and pixel data array
        t.bitmap = Bitmap.createBitmap(tsize,tsize, Bitmap.Config.RGB_565);

        int[] data = new int[tsize*tsize];

        paintRow(data,0,t.top);
        t.left[0] = t.top[0];
        t.right[0] = t.top[tsize-1];

        boolean[] prevRowState = t.top;

        for (int row = 1; row < tsize; row++) {

            boolean[] rowState = calculateNextRowState(leftState[row-1],prevRowState, rightState[row-1]);
            paintRow(data,row,rowState);

            if(row == tsize-1){
                t.bot = rowState;
            }

            t.left[row] = rowState[0];
            t.right[row] = rowState[tsize-1];

            prevRowState = rowState;

        }

        t.bitmap.setPixels(data,0,tsize,0,0,tsize,tsize);

        t.renderOrder = renderOrderCnt++;


    }


    private boolean[] calculateNextRowState(boolean prevLeft, boolean[] prev, boolean prevRight){

        boolean[] next = new boolean[prev.length];

        for(int col = 0; col < prev.length; col++){

            if(col == 0){
                // leftmost elem, include 'prevLeft'
                next[col] = WolframRuleTable.checkRule(ruleNo,prevLeft,prev[col],prev[col+1]);
            }
            else if(col == prev.length-1){
                // rightmost elem, include 'prevRight'
                next[col] = WolframRuleTable.checkRule(ruleNo,prev[col-1],prev[col],prevRight);
            }
            else{
                // non-perimeter elem, prev has all we need
                next[col] = WolframRuleTable.checkRule(ruleNo,prev[col-1],prev[col],prev[col+1]);
            }

        }


        return next;
    }

    private void paintRow(int data[], int row, boolean[] state){

        int rowOffset = row * state.length;
        for(int col=0;col<state.length;col++){
            data[rowOffset+col] = state[col] ? PIXEL_ON : PIXEL_OFF;
        }
    }


    @Override
    public String toString(){
        return String.format("prov[r=%d,c=%d,q=%d]",ruleNo,tileCache.size(),renderQueue.size());
    }


}
