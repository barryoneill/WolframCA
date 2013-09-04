/*
 *    WolframCA - an android application to view 1-dimensional cellular automata (CA)
 *    Copyright 2013 Barry O'Neill (http://barryoneill.net/)
 *
 *    Licensed under Apache 2.0 with limited permission from, and no affiliation with Steven
 *    Wolfram, LLC. See the LICENSE file in the root of this project for the full license terms.
 */
package net.nologin.meep.ca.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import net.nologin.meep.ca.model.WolframTileProvider;
import net.nologin.meep.ca.WolframUtils;
import net.nologin.meep.tbv.TiledBitmapView;

/**
 * This is a subclass of {@link TiledBitmapView} with some modifications to simplify use for wolfram tiles.
 * Most of the actual work for the view is done by the {@link WolframTileProvider} - This subclass exists
 * mainly to provide some setup routines, and to do standard onSaveInstanceState/onRestoreInstanceState work
 * for the rule & zoomlevel vars.
 *
 * @see WolframTileProvider
 * @see TiledBitmapView
 */
public class WolframCAView extends TiledBitmapView {

    // state save/restore keys
    private static final String STATEKEY_SUPERCLASS = "net.nologin.meep.ca.view.superclass";
    private static final String STATEKEY_RULENO = "net.nologin.meep.ca.view.ruleno";
    private static final String STATEKEY_PXPERCELL = "net.nologin.meep.ca.view.pxpercell";

    public WolframCAView(Context context, AttributeSet attrs) {

        super(context, attrs);

        // register our custom provider that does most of the work
        registerProvider(new WolframTileProvider(getContext()));

    }

    /**
     * Tell the registered {@link WolframTileProvider} to generate tiles for the specified rule
     * @param newRule The {@link net.nologin.meep.ca.model.WolframRuleTable rule number}
     */
    public void setupForRule(int newRule){

        WolframTileProvider tp = getProvider();
        Log.i(WolframUtils.LOG_TAG,"Changing rule to " + newRule);
        tp.setRule(newRule);

        moveToOriginTile(true);
    }

    /**
     * Tell the registered {@link WolframTileProvider} to generate tiles with the provided px-per-cell value
     * @param newZoomLevel The number of pixels wide each CA cell should be
     */
    public void setupForZoom(int newZoomLevel){

        WolframTileProvider tp = getProvider();
        Log.i(WolframUtils.LOG_TAG,"Changing zoom to level " + newZoomLevel);
        tp.setPixelsPerCell(newZoomLevel);

        moveToOriginTile(true);
    }

    /**
     * @return The current {@link net.nologin.meep.ca.model.WolframRuleTable rule} value
     */
    public int getCurrentRule(){
        return getProvider().getRule();
    }

    /**
     * @return The current size in pixels of each CA cell
     */
    public int getCurrentPxPerCell(){
        return getProvider().getPixelsPerCell();
    }

    /**
     * @return The currently registered {@link WolframTileProvider}
     */
    public WolframTileProvider getProvider(){
        WolframTileProvider tp = (WolframTileProvider)super.getProvider();
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
