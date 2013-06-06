package net.nologin.meep.ca.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import net.nologin.meep.ca.model.WolframTileProvider;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.tbv.TiledBitmapView;

public class WolframCAView extends TiledBitmapView {

    private static final String STATEKEY_SUPERCLASS = "net.nologin.meep.ca.view.superclass";
    private static final String STATEKEY_RULENO = "net.nologin.meep.ca.view.ruleno";
    private static final String STATEKEY_PXPERCELL = "net.nologin.meep.ca.view.pxpercell";

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

    public int getCurrentPxPerCell(){
        return getProvider().getPixelsPerCell();
    }

    private WolframTileProvider getProvider(){
        WolframTileProvider tp = (WolframTileProvider)getTileProvider();
        if(tp == null){
            throw new IllegalStateException("No provider initialised, perhaps setupForRule hasn't been called yet");
        }
        return tp;
    }

    @Override
    public Parcelable onSaveInstanceState() {

        Bundle bundle = new Bundle();
        bundle.putParcelable(STATEKEY_SUPERCLASS, super.onSaveInstanceState());
        bundle.putInt(STATEKEY_RULENO, getCurrentRule());
        bundle.putInt(STATEKEY_PXPERCELL, getCurrentPxPerCell());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            int ruleNo = bundle.getInt(STATEKEY_RULENO, -1);
            if(ruleNo > 0){
                setupForRule(ruleNo);
            }

            int pxPerCell = bundle.getInt(STATEKEY_PXPERCELL, -1);
            if(pxPerCell > 0){
                setupForZoom(pxPerCell);
            }

            super.onRestoreInstanceState(bundle.getParcelable(STATEKEY_SUPERCLASS));
            return;
        }

        super.onRestoreInstanceState(state);
    }


}
