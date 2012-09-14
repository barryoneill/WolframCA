package net.nologin.meep.ca.view;

import android.content.Context;
import android.util.AttributeSet;
import net.nologin.meep.ca.model.WolframTileProvider;

public class WolframCAView extends TiledBitmapView {

    public WolframCAView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public TileProvider getTileProvider(){

        return new WolframTileProvider(getContext(),90);

    }

}
