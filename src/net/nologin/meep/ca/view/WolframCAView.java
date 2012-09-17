package net.nologin.meep.ca.view;

import android.content.Context;
import android.util.AttributeSet;
import net.nologin.meep.ca.model.WolframTileProvider;
import net.nologin.meep.ca.util.Utils;

public class WolframCAView extends TiledBitmapView {

    private WolframTileProvider provider;

    public WolframCAView(Context context, AttributeSet attrs) {

        super(context, attrs);

    }

    public TileProvider getTileProvider(){

        if(provider == null){
            provider = new WolframTileProvider(getContext(),90);
        }
        return provider;

    }

    public void changeRule(int newRule){

        provider.changeRule(newRule);

        resetCanvasOffset();

    }

}
