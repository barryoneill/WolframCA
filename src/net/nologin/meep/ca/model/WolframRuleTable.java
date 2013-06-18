package net.nologin.meep.ca.model;

import android.util.Log;
import net.nologin.meep.ca.WolframUtils;

/**
 * This class contains a lookup cache for the 256 elementary 1-dimensional cellular automata (CA), as described in:
 * <br/><br/>
 * <a href="http://mathworld.wolfram.com/ElementaryCellularAutomaton.html">http://mathworld.wolfram.com/ElementaryCellularAutomaton.html</a>
 * <br/><br/>
 * In short, the CA is a row of cells with boolean values (on or off) representing a 'generation' in the CA.
 * For the next generation/row, the value of every cell changes, depending on its current state, and that of its left
 * and right neighbour.  This means that to calculate the next state of a cell, there are three boolean inputs,
 * meaning 8 possible permutations:
 * <pre>
 *  numbers 7 to 0 as binary:
 *          111 | 110 | 101 | 100 | 011 | 010 | 001 | 000
 * </pre>
 *
 * Each entry above is a possible set of cell states (e.g. 111 means all cells are on, 101 means that the left and right
 * cells are on, but the middle one was off, and so on)
 * <br/><br/>
 * Next, we need to define what the next state of a cell will be - this is defined by a <b>rule</b> number.  Since
 * there are 8 possible inputs, the CA uses an 8-bit number to define the rule to use. Each bit of the rule number
 * is assigned a position to the table.  So, for <i>Rule 30</i>:
 *
 * <pre>
 *    input permutations: 111 | 110 | 101 | 100 | 011 | 010 | 001 | 000
 *    ------------------|-----|-----|-----|-----|-----|-----|-----|-----
 *             Rule  30 |  0  |  0  |  0  |  1  |  1  |  1  |  1  |  0
 * </pre>
 *
 * For rule 30, if all cells are on (111), we can see the next state is off (0).  Or, if the current cell is on, but the
 * left and right are off (010), then the next state is on (1).
 *
 */
public class WolframRuleTable {

    /* Rule lookup array.  Main index is the rule number, subarrays are that rule number converted to binary and
     * stored as an 8 element boolean array.  Eg
     * [
     *  ..
     *  [50]-->[false,false,true,true,false,false,true,false]  // (rule '50' = 00110010 in binary)
     *  ..
     * ]
     */
    private static final boolean[][] ruleTable = new boolean[256][];

    private WolframRuleTable() {
    }

    /**
     * Calculate the value of a cell in the next generation based on the state of the relevant cells in the current
     * generation.
     *
     * @param rule  The rule number (0-255 inclusive)
     * @param curStateLeft The current state of the cell's left neighbour
     * @param curState The current state of the cell in question
     * @param curStateRight The current state of the cell's right neighbour
     * @return The cell's state in the next generation
     * @throws IllegalArgumentException If the rule is not a value from 0 to 255 (inclusive).
     */
    public static boolean getNextState(int rule, boolean curStateLeft, boolean curState, boolean curStateRight) {

        if (rule < 0 || rule > 255) {
            throw new IllegalArgumentException("Rule must be between 0 and 255");
        }

        // ruleTable entries are only generated as needed
        if (ruleTable[rule] == null) {

            ruleTable[rule] = new boolean[8];

            // store the rule number's binary value as an 8-bit boolean array
            for (int i = 0; i < 8; i++) {
                ruleTable[rule][i] = (rule & (1L << (7-i))) != 0;
            }

            Log.e(WolframUtils.LOG_TAG,String.format("Rule %d setup: %s", rule, joinArr(ruleTable[rule])));
        }

        /* 8 possible states can be represented using a 3-bit number.  Start with 0, then add 4, 2 or 1 to
         * set the left, center or right bit respectively.
         * e.g.
         * 101 = 4 + 1
         * 010 = 2
         * 111 = 4 + 2 + 1
         * and so on.
         *
         * We can then use this value (0-7) to index the ruleTable subarray for this rule.
         */

        int lookupIdx = curStateLeft ? 4 : 0;
        lookupIdx += curState ? 2 : 0;
        lookupIdx += curStateRight ? 1 : 0;

        /* For some reason the bit-pattern to rule binary matching (as described in the URL in the class javadoc)
         * is in reverse order (eg, 111(7),110(6),101(5) etc).  Not a huge deal, but we have to reverse the
         * subarray lookup (7-idx): */
        return ruleTable[rule][7 - lookupIdx];

    }

    // convert a bool array to descriptive string, "F,F,T,T.." for debugging messages
    private static String joinArr(boolean[] array){

        if(array == null || array.length < 1){
            return "";
        }

        StringBuilder buf = new StringBuilder(array.length * 16);
        for(int i = 0; i< array.length; i++) {
            if(i > 0){
                buf.append(",");
            }
            buf.append(array[i] ? "T" : "F");
        }

        return buf.toString();
    }



}
