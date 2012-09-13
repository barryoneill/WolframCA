package net.nologin.meep.ca.model;

import android.graphics.*;

import java.util.Random;

public class Tile {

    public Bitmap state;
    public int size;

    public Tile(int size) {

        this.size = size;
        state = null;
    }

    public Rect getRect(int atX, int atY){
        return new Rect(atX, atY, atX + size, atY + size);
    }

    public String toString() {

        return "Tile[size=" + size + "]";

    }

}