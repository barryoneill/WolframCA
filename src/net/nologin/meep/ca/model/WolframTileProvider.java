package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.Bitmap;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.ca.view.TiledBitmapView;

import java.util.*;

public class WolframTileProvider implements TiledBitmapView.TileProvider {

    public static final int TILE_SIZE = 256;

    private Context ctx;
    HashMap<String,Tile> tileCache;
    int ruleNo = 0;

    Queue<Tile> renderQueue;

    private int PIXEL_ON, PIXEL_OFF;

    public WolframTileProvider(Context ctx, int ruleNo){

        tileCache = new HashMap<String,Tile>();
        renderQueue = new PriorityQueue<Tile>();

        this.ruleNo = ruleNo;

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);



    }


    @Override
    public void onSurfaceChange(int width, int height) {

            // TODO: needed?


    }


    @Override
    public int getTileSize(){
        return TILE_SIZE;
    }


    @Override
    public Tile getTile(int xId, int yId){

        String cacheKey = getCacheKey(xId, yId);

        // cache check
        Tile t = tileCache.get(cacheKey);
        if(t != null){
            return t;
        }

        // cache miss, new tile
        t = new Tile(xId,yId,TILE_SIZE);
        tileCache.put(cacheKey,t);

        // to be rendered later in another thread
        renderQueue.add(t);

        return t;

    }

    private String getCacheKey(int x, int y){

        return String.format("%d,%d",x,y);

    }


    @Override
    public void renderNext() {

        if(renderQueue.isEmpty()){
            return;
        }

        Tile t = renderQueue.poll();

        Utils.log("*** Rendering " + t);


        t.bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.RGB_565);

        int[] data = new int[TILE_SIZE*TILE_SIZE];

        for (int row = 0; row < TILE_SIZE; row++) {

            int rowOffset = row * TILE_SIZE;
            int prevRowOffset = (row-1) * TILE_SIZE;

            if(row == 0){

                Arrays.fill(data,rowOffset,TILE_SIZE, PIXEL_OFF);
                data[rowOffset + TILE_SIZE/2] = PIXEL_ON;
            }
            else {

                for (int col = 0; col < TILE_SIZE; col++) {

                    boolean a,b,c;

                    a = col != 0 && data[prevRowOffset + col-1] != PIXEL_OFF;
                    b = data[prevRowOffset + col] == PIXEL_OFF;
                    c = col < TILE_SIZE -1 && data[prevRowOffset + col+1] != PIXEL_OFF;


                    boolean on = WolframRuleTable.checkRule(ruleNo,a,b,c);

                    data[rowOffset+col] = on ? PIXEL_ON : PIXEL_OFF;

                }

            }
        }

        t.bitmap.setPixels(data,0,TILE_SIZE,0,0,TILE_SIZE,TILE_SIZE);

    }







}
