/*
 *    WolframCA - an android application to view 1-dimensional cellular automata (CA)
 *    Copyright 2013 Barry O'Neill (http://barryoneill.net/)
 *
 *    Licensed under Apache 2.0 with limited permission from, and no affiliation with Steven
 *    Wolfram, LLC. See the LICENSE file in the root of this project for the full license terms.
 */
package net.nologin.meep.ca.model;

import net.nologin.meep.tbv.Tile;

/**
 * We use a sublcass of {@link Tile} because we need to store some of the calculated rule data in
 * each tile as well as the bitmap data that {@link Tile} stores.
 * <br/><br/>
 * The {@link WolframTileProvider} will clear the bitmap data from tiles that are scrolled off-screen.
 * Should we need that bitmap data again, the provider can use the saved state that's still in each
 * tile to quickly regenerate that tile's contents, instead of regenerating _all_ dependent tiles again.
 *
 * @see WolframTileProvider
 */
public class WolframTile extends Tile {

    // the state that we'll keep even when the provider wipes the bitmap content
    boolean[] lastCellRow = null;

    public WolframTile(int xId, int yId) {
        super(xId, yId);
    }

}
