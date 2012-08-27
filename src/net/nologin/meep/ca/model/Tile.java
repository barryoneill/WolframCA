package net.nologin.meep.ca.model;

import android.graphics.*;
import net.nologin.meep.ca.R;

import java.util.Random;

public class Tile {

    public Bitmap bitmap;
    public int x;
    public int y;
    public int size;
    public Rect rect;
    public boolean fresh;

    Random r = new Random();

    public Tile(int x, int y, int length) {
        this.x = x;
        this.y = y;
        this.size = length;
        rect = new Rect(x,y,x+size,y+size);
    }

    public void populateRandom(int colorOnPixel, int colorOffPixel) {

        int NUM_ROWS = size;
        int NUM_COLS = size;

        bitmap = Bitmap.createBitmap(NUM_ROWS, NUM_COLS, Bitmap.Config.RGB_565);

        int[] bmpData = new int[NUM_ROWS * NUM_COLS];

        for (int row = 0; row < NUM_ROWS; row++) {
            int rowOffset = row * NUM_ROWS;

            for (int col = 0; col < NUM_COLS; col++) {

                boolean b = r.nextInt(10) == 0;

                bmpData[rowOffset + col] = b ? colorOnPixel : colorOffPixel;

            }

        }

        bitmap.setPixels(bmpData, 0, NUM_ROWS, 0, 0, NUM_ROWS, NUM_COLS);
        fresh = true;

    }



    public String toString(){

        return "Tile[x=" + x + ",y=" + y
                + ", size=" + size + "]";

    }

}