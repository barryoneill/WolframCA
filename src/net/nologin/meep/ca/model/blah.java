package net.nologin.meep.ca.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class blah {

    public static void main(String args[]) {

        Set<Integer> test = new HashSet<Integer>();

        for(int i=0;i<1000;i++){
            for(int j=0;j<10000;j++){

                int val = i << 16 ^ j;
                System.out.println("Uh oh @ i=" + i + " and j=" + j + " (val=" + val + ")");
                if(test.contains(val)){
                    System.out.println("Uh oh @ i=" + i + " and j=" + j + " (val=" + val + ")");
                }
                else{
                    test.add(val);
                }
            }
        }

        System.out.println("Got " + test.size() + " elems");

    }

}
