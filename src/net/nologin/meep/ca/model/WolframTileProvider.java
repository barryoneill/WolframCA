package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.Color;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.view.TiledBitmapView;

import java.util.*;

public class WolframTileProvider implements TiledBitmapView.TileProvider {

    public static final int TILE_SIZE = 256;

    private Context ctx;
    List<Tile> tiles;
    int staleCnt = 0;
    int ruleNo = 0;
    Random r = new Random();

    @Deprecated
    int[] initVal;

    private int PIXEL_ON, PIXEL_OFF;

    public WolframTileProvider(Context ctx, int ruleNo){
        tiles = new ArrayList<Tile>();
        this.ruleNo = ruleNo;

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);


        initVal = new int[256];
        Arrays.fill(initVal,PIXEL_OFF);
        initVal[initVal.length/2] = PIXEL_ON;
    }


    @Override
    public void onSurfaceChange(int width, int height) {

        int NUM_HORIZ = width / TILE_SIZE + 1;
        int NUM_VERT = height / TILE_SIZE + 1;

        for (int row = 0; row < NUM_HORIZ; row++) {
            for (int col = 0; col < NUM_VERT; col++) {
                tiles.add(new Tile(row * TILE_SIZE, col * TILE_SIZE, TILE_SIZE));
            }
        }

        staleCnt = tiles.size();
    }

    @Override
    public int getTileSize(){
        return TILE_SIZE;
    }



    @Override
    public boolean hasStaleTiles() {
        return staleCnt > 0;
    }

    @Override
    public void updateNextStale() {

        Tile t = tiles.get(staleCnt-1);

        //t.state = new boolean[TILE_SIZE][TILE_SIZE];

        t.state = new int[TILE_SIZE*TILE_SIZE];

        for (int row = 0; row < TILE_SIZE; row++) {
            int rowOffset = row * TILE_SIZE;

            for (int col = 0; col < TILE_SIZE; col++) {

//                boolean a,b,c;
//                if(row == 0){
//
//                    a = col != 0 && initVal[col-1] != Color.BLACK;
//                    b = initVal[col] == Color.BLACK;
//                    c = col < initVal.length -1 && initVal[col+1] != Color.BLACK;
//                }
//                else{
//
//                    a = col != 0 && initVal[col-1] != Color.BLACK;
//                    b = initVal[col] == Color.BLACK;
//                    c = col < initVal.length -1 && initVal[col+1] != Color.BLACK;
//                }

                //boolean set = initVal[col];



                t.state[rowOffset+col] = r.nextInt(10) == 5 ? PIXEL_ON : PIXEL_OFF;

            }

        }

        staleCnt--;
    }


    @Override
    public Iterator<Tile> getActiveTilesIter() {

        // TODO: revisit
        return tiles.iterator();

    }



}
