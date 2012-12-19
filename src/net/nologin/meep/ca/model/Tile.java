package net.nologin.meep.ca.model;

import android.graphics.*;


public abstract class Tile implements Comparable<Tile> {

    //public int renderOrder;

    public int size;
    public int xId;
    public int yId;


    public Tile(int xId, int yId, int size) {

        this.xId = xId;
        this.yId = yId;
        this.size = size;
    }

    public boolean isOrigin(){
        return xId == 0 && yId ==0;
    }

    public boolean hasId(int xId, int yId){
        return this.xId == xId && this.yId == yId;
    }

    public Rect getRect(int canvasX, int canvasY){
        return new Rect(canvasX, canvasY, canvasX + size, canvasY + size);
    }

    public String toString() {

        return String.format("Tile[(%d,%d),%d*%d]",xId,yId,size,size);

    }

    public abstract Bitmap getBitmap();

    @Override
    public int compareTo(Tile tile) {
        // TODO: investigate render queue ordering - this just does 'as added'
       return 0;
    }

}