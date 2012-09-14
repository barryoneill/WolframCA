package net.nologin.meep.ca.model;

import android.graphics.*;

public class Tile implements Comparable<Tile> {

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

    public Rect getRect(int atX, int atY){
        return new Rect(atX, atY, atX + size, atY + size);
    }

    public String toString() {

        return String.format("Tile[(%d,%d),%d*%d]",xId,yId,size,size);

    }

//        @Override
//        public boolean equals(Object o) {
//
//            if (this == o){
//                return true;
//            }
//
//            if (!(o instanceof TileID)){
//                return false;
//            }
//
//            TileID b = (TileID)o;
//            return x == b.x && y == b.y;
//        }
//
//        public int hashCode() {
//
//            int hc = 17;
//            hc = 31 * hc + x;
//            hc = 31 * hc + y;
//            return hc;
//        }


    @Override
    public int compareTo(Tile tile) {
       return 0; // TODO: investigate render queue ordering - this just does 'as added'
    }
}