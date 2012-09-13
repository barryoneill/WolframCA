package net.nologin.meep.ca.view;

import android.graphics.*;
import android.view.*;
import android.content.Context;
import android.util.AttributeSet;
import net.nologin.meep.ca.model.Tile;
import net.nologin.meep.ca.model.WolframTileProvider;
import static net.nologin.meep.ca.util.Utils.log;

import java.util.Iterator;

public class TiledBitmapView extends SurfaceView implements SurfaceHolder.Callback {

    GestureDetector gestureDetector;
    ScaleGestureDetector scaleDetector;

    Paint paint_bg;
    Paint paint_msgText;
    Paint paint_gridLine;

    TileGenerationThread tgThread;

    ScreenState state;

    TileProvider tileProvider;

    //Bitmap bitmap;

    private float mScaleFactor = 0.5f;
    private int mOffsetX = 0, mOffsetY = 0;

    public TiledBitmapView(Context context, AttributeSet attrs) {

        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        state = new ScreenState();
        tgThread = new TileGenerationThread(holder, this);

        // TODO: register tileProvider
        tileProvider = new WolframTileProvider(context,90);

        // background paint
        paint_bg = new Paint();
        paint_bg.setColor(Color.DKGRAY); // LTGRAY
        paint_bg.setStyle(Paint.Style.FILL);

        // background status text paint (needed?)
        paint_msgText = new Paint();
        paint_msgText.setColor(Color.WHITE);
        paint_msgText.setTextSize(30);
        paint_msgText.setAntiAlias(true);
        paint_msgText.setTextAlign(Paint.Align.CENTER);

        // grid line
        paint_gridLine = new Paint();
        paint_gridLine.setColor(Color.LTGRAY); // DKGRAY
        paint_gridLine.setStyle(Paint.Style.STROKE);
        paint_gridLine.setStrokeWidth(1);

        gestureDetector = new GestureDetector(new GestureListener());
        scaleDetector = new ScaleGestureDetector(context,new ScaleListener());


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
                    if(c == null){
                        continue; // is this right?
                    }
                    synchronized (holder) {



                        // TODO:
//                        if(tileProvider.hasStaleTiles()){
//                            // render another tile
//                            tileProvider.updateNextStale();
//                        }

                        view.doDraw(c);


                    }
                    Thread.sleep(5); // so we can interact in a reasonable time
                    if(2>3){
                        throw new InterruptedException("bark bark");
                    }


                } catch (InterruptedException e) {
                    // nop
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
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

        // draw BG
        canvas.drawRect(new Rect(0, 0, state.width, state.height), paint_bg);

        if (tileProvider != null) {

            for(int tilePosX=state.minX;tilePosX<state.maxX;tilePosX++){

                for(int tilePosY=state.minY;tilePosY<state.maxY;tilePosY++){

                    int size = tileProvider.getTileSize();
                    int x = tilePosX * size + mOffsetX;
                    int y = tilePosY * size + mOffsetY;


                    Tile t = tileProvider.getTile(tilePosX,tilePosY);
                    if(t == null){
                        continue; // TODO: log?
                    }

                    if (t.state != null) {

                        //bitmap.setPixels(t.state, 0, tileSize, xOff, yOff, tileSize, tileSize);
                        canvas.drawBitmap(t.state,x ,y ,null);

                    } else {
                        canvas.drawRect(t.getRect(x,y), paint_gridLine);

                        String fmt1 = "Tile(%d,%d)";
                        String msg1 = String.format(fmt1, tilePosX, tilePosY);
                        canvas.drawText(msg1, x + (size/2), y + (size/2), paint_msgText);


                    }


                }

            }






            //canvas.drawBitmap(bitmap, mOffsetX, mOffsetY, null);


        }

        String fmt1 = "%dx%d, s=%1.3f";
        String fmt2 = "offset %d,%d";
        String msg1 = String.format(fmt1, state.width, state.height,mScaleFactor);
        String msg2 = String.format(fmt2,mOffsetX, mOffsetY);
        canvas.drawText(msg1, state.width / 2, state.height / 2, paint_msgText);
        canvas.drawText(msg2, state.width / 2, state.height / 2 + 35, paint_msgText);

        canvas.restore();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        state.width = width;
        state.height = height;

        int NUM_HORIZ = width / tileProvider.getTileSize() + 1;
        int NUM_VERT = height / tileProvider.getTileSize() + 1;


        state.maxX = NUM_HORIZ;
        state.maxY = NUM_VERT;


        if (tileProvider != null) {
            tileProvider.onSurfaceChange(width, height);
        }

        // bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

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

        int height;
        int width;

        int minX = 0, maxX = 0, minY = 0, maxY;

    }

    // http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            log("Scale factor now " + mScaleFactor + " - " + tgThread.running);


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

            log("scroll x=" + distanceX + ", y=" + distanceY);
            mOffsetX -= (int)distanceX;
            mOffsetY -= (int)distanceY;

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

        public void onSurfaceChange(int newWidthPx, int newHeightPx);

        public int getTileSize();

        public Tile getTile(int x, int y);



        /*
        public Iterator<Tile> getActiveTilesIter();

        public boolean hasStaleTiles();

        public void updateNextStale();
        */

    }
}
