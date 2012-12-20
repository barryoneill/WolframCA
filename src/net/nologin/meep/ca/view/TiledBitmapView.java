package net.nologin.meep.ca.view;

import android.graphics.*;
import android.view.*;
import android.content.Context;
import android.util.AttributeSet;
import net.nologin.meep.ca.model.Tile;
import net.nologin.meep.ca.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static net.nologin.meep.ca.util.Utils.log;

public abstract class TiledBitmapView extends SurfaceView implements SurfaceHolder.Callback {

    GestureDetector gestureDetector;
    ScaleGestureDetector scaleDetector;

    Paint paint_bg;
    Paint paint_msgText;
    Paint paint_errText;
    Paint paint_gridLine;
    Paint paint_debugBG;

    TileGenerationThread tgThread;

    ScreenState state;

    TileProvider tileProvider;

    public TiledBitmapView(Context context, AttributeSet attrs) {

        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        state = new ScreenState();
        tgThread = new TileGenerationThread(holder, this);

        // background paint
        paint_bg = new Paint();
        paint_bg.setColor(Color.DKGRAY); // LTGRAY
        paint_bg.setStyle(Paint.Style.FILL);

        // background status text paint (needed?)
        paint_msgText = new Paint();
        paint_msgText.setColor(Color.WHITE);
        paint_msgText.setTextSize(20);
        paint_msgText.setAntiAlias(true);
        paint_msgText.setTextAlign(Paint.Align.CENTER);

        paint_errText = new Paint(paint_msgText);
        paint_errText.setColor(Color.RED);

        // background paint
        paint_debugBG = new Paint();
        paint_debugBG.setColor(Color.BLACK);
        paint_debugBG.setStyle(Paint.Style.FILL);
        paint_debugBG.setAlpha(140);

        // grid line
        paint_gridLine = new Paint();
        paint_gridLine.setColor(Color.LTGRAY); // DKGRAY
        paint_gridLine.setStyle(Paint.Style.STROKE);
        paint_gridLine.setStrokeWidth(1);

        gestureDetector = new GestureDetector(new GestureListener());
        scaleDetector = new ScaleGestureDetector(context,new ScaleListener());


    }

    protected void setTileProvider(TileProvider tileProvider){
        this.tileProvider = tileProvider;
    }

    protected TileProvider getTileProvider(){
        return this.tileProvider;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        state.width = width;
        state.height = height;

        resetCanvasOffset();
    }

    protected void resetCanvasOffset(){

        if(tileProvider == null){
            return;
        }

        int SCROLL_BUFFER = 1;

        // we need enough to cover the whole width (hence the ceil), + 1 for scroll buffer
        state.numVisibleTiles_w = (int)Math.ceil(state.width / (float)tileProvider.getTileSize()) + SCROLL_BUFFER;
        state.numVisibleTiles_h = (int)Math.ceil(state.height / (float)tileProvider.getTileSize()) + SCROLL_BUFFER;


        // in the case of an odd number of horizontal tiles, we need an offset to move the origin to the middle
        state.canvasOffsetX = state.numVisibleTiles_w % 2 != 0 ? -tileProvider.getTileSize() : 0;
        state.canvasOffsetY = 0;

        state.updateVisibleTileRange();
    }



    private List<List<Tile>> getVisibleTileGrid(Rect tileIdRange) {

        List<List<Tile>> result = new ArrayList<List<Tile>>();

        for(int y = tileIdRange.top; y <= tileIdRange.bottom; y++){

            List<Tile> curRow = new ArrayList<Tile>();
            for(int x = tileIdRange.left; x <= tileIdRange.right; x++){
                curRow.add(tileProvider.getTile(x,y));
            }
            result.add(curRow);
        }

        return result;
    }


    class TileGenerationThread extends Thread {

        private final SurfaceHolder holder;
        private TiledBitmapView view;
        private boolean running = false;



        public TileGenerationThread(SurfaceHolder holder, TiledBitmapView view) {
            this.holder = holder;
            this.view = view;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }



        @Override
        public void run() {

            Canvas c;
            while (running) {

                c = null;

                try {
                    c = holder.lockCanvas(null);
                    if(c == null || tileProvider == null){
                        continue; // is this right?
                    }
                    synchronized (holder) {

                        view.doDraw(c);

                        tileProvider.generateNextTile();

                        // TODO: revisit! tileProvider.flushCache(calculateVisibleTileRange(state.canvasOffsetX, state.canvasOffsetY));

                    }
                    Thread.sleep(5); // so we can interact in a reasonable time

                }
                catch (InterruptedException e) {
                    // nop
                }
                finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent bitmap
                    if (c != null) {
                        holder.unlockCanvasAndPost(c);
                    }
                }
            }

        }

    }


    public void doDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.save();

        // the offsets may change during doDraw; Use a copy, otherwise there'll be flickering/tearing of tiles!
        int moffX = state.canvasOffsetX;
        int moffY = state.canvasOffsetY;

        // draw BG
        canvas.drawRect(new Rect(0, 0, state.width, state.height), paint_bg);

        if (tileProvider != null) {

            List<List<Tile>> visibleGrid = getVisibleTileGrid(state.visibleTileIdRange);

            int size = tileProvider.getTileSize();

            int y = moffY % size;

            for(List<Tile> tileRow : visibleGrid){

                int x = moffX % size - size; // start with one left

                for(Tile t : tileRow){

                    Bitmap bmp = t.getBitmap();
                    if (bmp != null) {

                        //bitmap.setPixels(t.bitmap, 0, tileSize, xOff, yOff, tileSize, tileSize);
                        canvas.drawBitmap(bmp, x ,y ,null);

                        // TODO: remove or make debug dependent
                        canvas.drawRect(t.getRect(x,y), paint_gridLine);

                    } else {

                        canvas.drawRect(t.getRect(x,y), paint_gridLine);

                        String fmt1 = "Tile(%d,%d)";
                        String msg1 = String.format(fmt1, t.xId, t.yId);
                        canvas.drawText(msg1, x + (size/2), y + (size/2), paint_msgText);

                    }

                    x += size; // move right one tile width
                }

                y += size; // move down one tile width
            }

            drawDebugBox(canvas);
        }

        canvas.restore();

    }

    private void drawDebugBox(Canvas canvas) {

        // draw a bunch of debug stuff
        String fmt1 = "%dx%d, s=%1.3f";
        String fmt2 = "offset x=%d y=%d";
        String fmt3 = "tiles %s";
        String msgResAndScale = String.format(fmt1, state.width, state.height,state.scaleFactor);
        String msgOffset = String.format(fmt2,state.canvasOffsetX,state.canvasOffsetY);
        String msgVisibleIds = String.format(fmt3, state.visibleTileIdRange);
        String msgProvider = tileProvider.toString();
        String msgMemory = Utils.getMemStatus();
        Paint paintMem = Utils.isHeapAlmostFull() ? paint_errText : paint_msgText;

        float boxWidth = 330, boxHeight = 145;

        float debug_x = state.width - boxWidth;
        float debug_y = state.height - boxHeight;

        canvas.drawRect(debug_x, debug_y, state.width, state.height, paint_debugBG);

        canvas.drawText(msgResAndScale, debug_x + boxWidth / 2, debug_y + 30, paint_msgText);
        canvas.drawText(msgOffset, debug_x + boxWidth / 2, debug_y + 55, paint_msgText);
        canvas.drawText(msgVisibleIds, debug_x + boxWidth / 2, debug_y + 80, paint_msgText);
        canvas.drawText(msgProvider, debug_x + boxWidth / 2, debug_y + 105, paint_msgText);
        canvas.drawText(msgMemory, debug_x + boxWidth / 2, debug_y + 130, paintMem);


    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!tgThread.isAlive()) {
            tgThread = new TileGenerationThread(holder, this);
            tgThread.setRunning(true);
            tgThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Based on android example 'LunarLander' app
        // we have to tell tgThread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        tgThread.setRunning(false);
        while (retry) {
            try {
                tgThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // loop until we've
            }
        }
    }


    @Override // register GD
    public boolean onTouchEvent(MotionEvent me) {

        invalidate();

        gestureDetector.onTouchEvent(me);
        scaleDetector.onTouchEvent(me);

        return true;
    }


    class ScreenState {

        int width, height;

        int numVisibleTiles_w, numVisibleTiles_h;

        float scaleFactor = 0.5f;

        int canvasOffsetX = 0, canvasOffsetY = 0;

        Rect visibleTileIdRange;

        public void updateVisibleTileRange(){

            // put the tiles either side of the axis
            int left = - (state.numVisibleTiles_w - (state.numVisibleTiles_w /2)); // int rounding puts possible larger on right
            // then apply offset
            left -= canvasOffsetX/tileProvider.getTileSize();

            // all tiles on one side of y axis
            int top = 0 - canvasOffsetY/tileProvider.getTileSize();

            int bottom = top + state.numVisibleTiles_h;
            int right = left + state.numVisibleTiles_w;

            state.visibleTileIdRange = new Rect(left,top,right,bottom);

        }


    }

    // http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            state.scaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            state.scaleFactor = Math.max(0.1f, Math.min(state.scaleFactor, 5.0f));

            log("Scale factor now " + state.scaleFactor + " - " + tgThread.running);


            return true;
        }
    }


    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onShowPress(MotionEvent motionEvent) {

            log("show press");

        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {

            //log("scroll x=" + distanceX + ", y=" + distanceY);
            state.canvasOffsetX -= (int)distanceX;

            int newOffY = state.canvasOffsetY - (int)distanceY;
            state.canvasOffsetY = newOffY > tileProvider.getTileIndexBounds().top ? tileProvider.getTileIndexBounds().top : newOffY;

            state.updateVisibleTileRange();

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

            log("long press");

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {

            log("fling");

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            log("double tap");

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            log("single tap");

            return false;
        }

    }

    public interface TileProvider {

        public int getTileSize();

        public Tile getTile(int x, int y);

        public void generateNextTile();

        public Rect getTileIndexBounds();

        // public List<List<Tile>> getTilesForCurrent(Rect tileIdRange);
        public void setTileViewPort(Rect tileIdRange);

        // public void flushCache(Rect tileIdRange);

    }
}
