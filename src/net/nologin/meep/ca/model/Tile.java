package net.nologin.meep.ca.model;

import android.graphics.*;

import java.util.Random;

public class Tile {

    public boolean[][] state;
    public int x;
    public int y;
    public int size;
    public Rect rect;

    public Tile(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        rect = new Rect(x, y, x + size, y + size);
        state = null;
    }

    public String toString() {

        return "Tile[x=" + x + ",y=" + y
                + ", size=" + size + "]";

    }

}