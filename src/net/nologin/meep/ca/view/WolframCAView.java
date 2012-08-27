package net.nologin.meep.ca.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import net.nologin.meep.ca.model.WCAModel;

public class WolframCAView extends SurfaceView {

    GestureDetector gestureDetector;
    ScaleGestureDetector scaleDetector;
    Paint paint = new Paint();

    private int mNumSteps = 20;
    private int mRuleNo = 90;

    private float mScaleFactor = 0.5f;


    public WolframCAView(Context ctx) {
        super(ctx);
        setup(ctx);
    }

    public WolframCAView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setup(ctx);
    }

    public WolframCAView(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        setup(ctx);
    }

    private void setup(Context ctx) {
        paint.setColor(Color.BLACK);

        gestureDetector = new GestureDetector(new GestureListener());
        scaleDetector = new ScaleGestureDetector(ctx,new ScaleListener());
    }


    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.save();

        int w = getWidth();
        int h = getHeight();

        // mNumSteps = h - 100;

        long t = System.currentTimeMillis();
        renderCA(canvas);
        Log.d("WolframCA","CA Rendered in " + (System.currentTimeMillis() - t) + "ms");
        renderTitle(canvas);

        canvas.restore();

    }

    private void renderCA(Canvas canvas){

        paint.setColor(Color.BLACK);

        WCAModel model = new WCAModel(mRuleNo, mNumSteps);
        model.start();
        model.renderCA(canvas, paint, mScaleFactor);

        paint.reset();

    }

    private void renderTitle(Canvas c){


        paint.setTextSize(20);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setAntiAlias(true);


        String msg = "Wolfram CA Rule " + mRuleNo + ", " + mNumSteps + " steps (Scale factor " + mScaleFactor + ")";
        int msgWidth = (int) paint.measureText(msg) + 20;

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(new Rect(0, 0, msgWidth, 50), paint);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        c.drawRect(new Rect(0, 0, msgWidth, 50), paint);
        c.drawText(msg, 10, 30, paint);


        paint.reset();

    }



    public void changeRuleNo(int newRuleNo) {

        mRuleNo = newRuleNo;
        invalidate();

    }


    @Override // register GD
    public boolean onTouchEvent(MotionEvent me) {

        invalidate();

        gestureDetector.onTouchEvent(me);
        scaleDetector.onTouchEvent(me);

        return true;
    }


    // http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            invalidate();
            return true;
        }
    }


    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            return false;
        }

    }

}