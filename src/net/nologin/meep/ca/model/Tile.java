package net.nologin.meep.ca.model;

import android.graphics.*;


public class Tile implements Comparable<Tile> {

    public int renderOrder;

    public int size;
    public int xId;
    public int yId;
    public Bitmap bitmap;

    public Tile(int xId, int yId, int size) {

        this.xId = xId;
        this.yId = yId;
        this.size = size;
        bitmap = null;
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

    public boolean renderFinished(){
        return bitmap != null;
    }

    public String toString() {

        return String.format("Tile[(%d,%d),%d*%d]",xId,yId,size,size);

    }

    @Override
    public int compareTo(Tile tile) {
        // TODO: investigate render queue ordering - this just does 'as added'
       return 0;
    }

}