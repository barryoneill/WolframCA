package net.nologin.meep.ca.model;

import android.content.Context;
import android.graphics.Bitmap;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.view.TiledBitmapView;

import java.util.*;

public class WolframTileProvider implements TiledBitmapView.TileProvider {

    public static final int TILE_SIZE = 256;

    private Context ctx;
    List<Tile> tiles;
    int staleCnt = 0;
    int ruleNo = 0;

    private int PIXEL_ON, PIXEL_OFF;

    public WolframTileProvider(Context ctx, int ruleNo){
        tiles = new ArrayList<Tile>();
        this.ruleNo = ruleNo;

        PIXEL_ON = ctx.getResources().getColor(R.color.CAView_PixelOn);
        PIXEL_OFF = ctx.getResources().getColor(R.color.CAView_PixelOff);



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

        t.state = Bitmap.createBitmap(TILE_SIZE,TILE_SIZE, Bitmap.Config.RGB_565);
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

        t.state.setPixels(data,0,TILE_SIZE,0,0,TILE_SIZE,TILE_SIZE);

        staleCnt--;
    }


    @Override
    public Iterator<Tile> getActiveTilesIter() {

        // TODO: revisit
        return tiles.iterator();

    }



}
