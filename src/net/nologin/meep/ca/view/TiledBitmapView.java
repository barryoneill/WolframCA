package net.nologin.meep.ca.view;

import android.graphics.*;
import android.view.SurfaceHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import net.nologin.meep.ca.R;
import net.nologin.meep.ca.model.Tile;

import java.util.ArrayList;
import java.util.List;


public class TiledBitmapView extends SurfaceView
        implements SurfaceHolder.Callback {

    public static final int TILE_SIZE = 256;

    Paint paint_bg;
    Paint paint_msgText;
    Paint paint_gridLine;

    int pixelOnColor;
    int pixelOffColor;

    TileGenerationThread tgThread;
    int width;
    int height;


    List<Tile> activeTiles;

    public TiledBitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }


    private void initView() {

        SurfaceHolder holder = getHolder();

        holder.addCallback(this);
        tgThread = new TileGenerationThread(holder, this);

        activeTiles = new ArrayList<Tile>();

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

        pixelOnColor = getResources().getColor(R.color.CAView_PixelOn);
        pixelOffColor = getResources().getColor(R.color.CAView_PixelOff);


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

            int loopCnt = 0;

            Canvas c;
            while (running) {
                c = null;
                try {
                    c = holder.lockCanvas(null);
                    synchronized (holder) {

                        view.onDraw(c);

                        for (Tile t : activeTiles) {
                            if (!t.fresh) {
                                t.populateRandom(pixelOnColor, pixelOffColor);
                                break;
                            }
                        }

                        if (loopCnt++ == activeTiles.size()) {
                            running = false;
                        }


                    }
                    Thread.sleep(16); // so we can interact in a reasonable time


                }
                catch(InterruptedException e){
                    // nop
                }
                finally {
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

        Bitmap bmp = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.RGB_565);

        if (activeTiles != null) {

            for (Tile t : activeTiles) {

                if (t.bmpData != null) {
                    bmp.setPixels(t.bmpData, 0, TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE);
                    canvas.drawBitmap(bmp, t.x, t.y, null);
                } else {

                    canvas.drawRect(t.rect, paint_gridLine);
                }



            }

        }

        int numTiles = activeTiles == null ? 0 : activeTiles.size();
        String msg = width  + "x" + height + ", " + numTiles + " tiles";
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

        int NUM_HORIZ = width / TILE_SIZE + 1;
        int NUM_VERT = height / TILE_SIZE + 1;

        for (int row = 0; row < NUM_HORIZ; row++) {
            for (int col = 0; col < NUM_VERT; col++) {
                activeTiles.add(new Tile(row * TILE_SIZE, col * TILE_SIZE, TILE_SIZE));
            }
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

}
