package net.nologin.meep.ca.view;

import android.graphics.*;
import android.view.SurfaceHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import net.nologin.meep.ca.model.Tile;
import net.nologin.meep.ca.model.WolframTileProvider;

import java.util.Iterator;


public class TiledBitmapView extends SurfaceView implements SurfaceHolder.Callback {


    Paint paint_bg;
    Paint paint_msgText;
    Paint paint_gridLine;

    TileGenerationThread tgThread;
    int width;
    int height;

    TileProvider tileProvider;

    public TiledBitmapView(Context context, AttributeSet attrs) {

        super(context, attrs);

        SurfaceHolder holder = getHolder();

        holder.addCallback(this);
        tgThread = new TileGenerationThread(holder, this);

        // TODO: register tileProvider
        tileProvider = new WolframTileProvider(context,90);

        // background paint
        paint_bg = new Paint();
        paint_bg.setColor(Color.LTGRAY);
        paint_bg.setStyle(Paint.Style.FILL);

        // background status text paint (needed?)
        paint_msgText = new Paint();
        paint_msgText.setColor(Color.WHITE);
        paint_msgText.setTextSize(30);
        paint_msgText.setAntiAlias(true);
        paint_msgText.setTextAlign(Paint.Align.CENTER);

        // grid line
        paint_gridLine = new Paint();
        paint_gridLine.setColor(Color.DKGRAY);
        paint_gridLine.setStyle(Paint.Style.STROKE);
        paint_gridLine.setStrokeWidth(1);

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
                    synchronized (holder) {

                        if(tileProvider.hasStaleTiles()){
                            // render another tile
                            tileProvider.updateNextStale();
                        }
                        else{
                            // nothing to do, end thread
                            running = false;

                        }

                        view.onDraw(c);


                    }
                    Thread.sleep(1); // so we can interact in a reasonable time
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


    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.save();

        // draw BG
        canvas.drawRect(new Rect(0, 0, width, height), paint_bg);

        if (tileProvider != null) {

            int tileSize = tileProvider.getTileSize();
            Bitmap bmp = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.RGB_565);

            Iterator<Tile> tilesIter = tileProvider.getActiveTilesIter();
            while (tilesIter.hasNext()) {

                Tile t = tilesIter.next();

                if (t.state != null) {
                    bmp.setPixels(t.state, 0, tileSize, 0, 0, tileSize, tileSize);
                    canvas.drawBitmap(bmp, t.x, t.y, null);
                } else {
                    canvas.drawRect(t.rect, paint_gridLine);
                }

            }


        }

        String msg = width + "x" + height;
        canvas.drawText(msg, width / 2, height / 2, paint_msgText);

        canvas.restore();

    }


    @Override
    public void invalidate() {

        super.invalidate();

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        this.width = width;
        this.height = height;

        if (tileProvider != null) {
            tileProvider.onSurfaceChange(width, height);
        }

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


    public interface TileProvider {

        public void onSurfaceChange(int newWidthPx, int newHeightPx);

        public int getTileSize();

        public Iterator<Tile> getActiveTilesIter();

        public boolean hasStaleTiles();

        public void updateNextStale();

    }
}
