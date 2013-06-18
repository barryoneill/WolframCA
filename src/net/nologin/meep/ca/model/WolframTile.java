/*
 *    WolframCA - an android application to view 1-dimensional cellular automata (CA)
 *    Copyright 2013 Barry O'Neill (http://meep.nologin.net/)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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
