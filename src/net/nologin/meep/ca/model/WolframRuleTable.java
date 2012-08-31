package net.nologin.meep.ca.model;

public class WolframRuleTable {

    private static final boolean[][] ruleTable = new boolean[256][];

    private WolframRuleTable() {}

    public static boolean checkRule(int ruleNo, boolean a, boolean b, boolean c){

        if(ruleNo < 0 || ruleNo > 255){
            throw new IllegalArgumentException("Rule must be between 0 and 255");
        }

        // if the rule to lookup hasn't been inited yet, do so now
        if(ruleTable[ruleNo] == null){
            ruleTable[ruleNo] = new boolean[8];

            // ruleTable to binary lookup array, eg '50' = 00110010 {false,false,true,true,false,false,true,false}
            for(int i=0;i<8;i++){
                ruleTable[ruleNo][i] = (ruleNo & (1L << i)) != 0;
            }
        }

        int lookupIdx = a ? 4 : 0;
        lookupIdx += b ? 2 : 0;
        lookupIdx += c ? 1 : 0;

        return ruleTable[ruleNo][lookupIdx];

    }

}
