package net.nologin.meep.ca.model;

import android.graphics.Color;
import net.nologin.meep.ca.view.TiledBitmapView;

import java.util.*;

public class WolframTileProvider implements TiledBitmapView.TileProvider {

    public static final int TILE_SIZE = 256;

    List<Tile> tiles;
    int staleCnt = 0;
    int ruleNo = 0;

    @Deprecated
    int[] initVal;

    public WolframTileProvider(int ruleNo){
        tiles = new ArrayList<Tile>();
        this.ruleNo = ruleNo;
        initVal = new int[256];
        Arrays.fill(initVal,Color.BLACK);
        initVal[initVal.length/2] = Color.GREEN;
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

        t.bmpData = new int[TILE_SIZE * TILE_SIZE];

        for (int row = 0; row < TILE_SIZE; row++) {
            int rowOffset = row * TILE_SIZE;

            // boolean[] prev = GRID[time-1];
            // boolean[] next = GRID[time];

            for (int col = 0; col < TILE_SIZE; col++) {

                boolean a,b,c;
                if(row == 0){

                    a = col != 0 && initVal[col-1] != Color.BLACK;
                    b = initVal[col] == Color.BLACK;
                    c = col < initVal.length -1 && initVal[col+1] != Color.BLACK;
                }
                else{

                    a = col != 0 && initVal[col-1] != Color.BLACK;
                    b = initVal[col] == Color.BLACK;
                    c = col < initVal.length -1 && initVal[col+1] != Color.BLACK;
                }

//                boolean a = col != 0 &&
//
//                next[i] = checkRule(i!=0 && prev[i-1],
//                        prev[i],
//                        i<prev.length-1 && prev[i+1]);

                //boolean set = initVal[col];

                BitSet bs = new BitSet();



                t.bmpData[rowOffset + col] = true ? Color.GREEN : Color.BLACK;

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
