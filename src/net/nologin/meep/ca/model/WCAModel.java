package net.nologin.meep.ca.model;

import android.graphics.*;
import android.util.Log;

import java.util.HashMap;

public class WCAModel {

    private boolean[][] GRID;
    private boolean[] ruleLookup;
    private int rule;
    private int numSteps;

    static HashMap<Integer, WCAModel> cache = new HashMap<Integer,WCAModel>();


    public WCAModel(int rule, int numSteps){

        WCAModel c = cache.get(rule);

        if(c == null) {
            long t = System.currentTimeMillis();
            GRID = new boolean[numSteps][numSteps*2+1];
            GRID[0][GRID[0].length/2] = true;
            buildRuleLookup(rule);
            Log.d("WolframCA", "Time to build rule " + rule + " = " + (System.currentTimeMillis()-t) + "ms");
        }
        else {
            Log.d("WolframCA","Cache hit on rule " + rule);
            GRID = c.GRID;
            ruleLookup = c.ruleLookup;
        }

        this.rule = rule;
        this.numSteps = numSteps;

        cache.put(rule,this);

    }

    private void buildRuleLookup(int rule){

        if(rule < 0 || rule > 255){
            throw new IllegalArgumentException("Rule must be between 0 and 255");
        }

        // ruleLookup to binary lookup array, eg '50' = 00110010 {false,false,true,true,false,false,true,false}
        this.ruleLookup = new boolean[8];
        for(int i=0;i<8;i++){
            this.ruleLookup[i] = (rule & (1L << i)) != 0;
        }

    }

    private boolean checkRule(boolean a, boolean b, boolean c){

        int lookupIdx = a ? 4 : 0;
        lookupIdx += b ? 2 : 0;
        lookupIdx += c ? 1 : 0;
        return ruleLookup[lookupIdx];

    }

    public void start(){

        for(int time=1; time < GRID.length; time++){
            boolean[] prev = GRID[time-1];
            boolean[] next = GRID[time];

            for(int i=0;i<next.length;i++){

                next[i] = checkRule(i!=0 && prev[i-1],
                        prev[i],
                        i<prev.length-1 && prev[i+1]);


            }

        }

    }



    public void renderCA(Canvas c, Paint p, float scaleFactor){

        int BOX_SIZE = 10;
        BOX_SIZE = (int)Math.floor(BOX_SIZE*scaleFactor);

        int y = 0;

        for (boolean[] step : GRID) {
            int x = 0;

            for (boolean state : step) {

                if(state){
                    c.drawRect(new Rect(x,y,x+BOX_SIZE,y+BOX_SIZE),p);
                }

                x += BOX_SIZE;
            }
            y += BOX_SIZE;
        }

    }

}
