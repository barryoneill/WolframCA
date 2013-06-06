package net.nologin.meep.ca.view;

import android.content.Context;
import android.util.AttributeSet;
import net.nologin.meep.ca.model.WolframTileProvider;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.tbv.TiledBitmapView;

public class WolframCAView extends TiledBitmapView {

    public WolframCAView(Context context, AttributeSet attrs) {

        super(context, attrs);

    }

    public void setupForRule(int newRule){

        WolframTileProvider tp = (WolframTileProvider)getTileProvider();
        if(tp == null){
            Utils.log("Init new provider, rule=" + newRule + ", default zoom");
            setTileProvider(new WolframTileProvider(getContext(),newRule, -1));
        }
        else {
            Utils.log("Changing rule to " + newRule);
            tp.setRule(newRule);
        }

        jumpToOriginTile(true);
    }

    public void setupForZoom(int newZoomLevel){

        WolframTileProvider tp = (WolframTileProvider)getTileProvider();
        if(tp == null){
            Utils.log("Init new provider, default rule, pxPerCell=" + newZoomLevel);
            setTileProvider(new WolframTileProvider(getContext(),-1, newZoomLevel));
        }
        else {
            Utils.log("Changing zoom to level " + newZoomLevel);
            tp.setPixelsPerCell(newZoomLevel);
        }

        jumpToOriginTile(true);
    }



    public int getCurrentRule(){
        return getProvider().getRule();
    }

    public int getCurrentZoom(){
        return getProvider().getPixelsPerCell();
    }

    private WolframTileProvider getProvider(){
        WolframTileProvider tp = (WolframTileProvider)getTileProvider();
        if(tp == null){
            throw new IllegalStateException("No provider initialised, perhaps setupForRule hasn't been called yet");
        }
        return tp;
    }

}
