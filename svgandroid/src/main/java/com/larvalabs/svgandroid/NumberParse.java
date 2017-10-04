package com.larvalabs.svgandroid;

import java.util.ArrayList;

/**
 * Created by zhengxianlzx on 17-10-5.
 */
public class NumberParse {
    private ArrayList<Float> numbers;
    private int nextCmd;

    public NumberParse(ArrayList<Float> numbers, int nextCmd) {
        this.numbers = numbers;
        this.nextCmd = nextCmd;
    }

    @SuppressWarnings("unused")
    public int getNextCmd() {
        return nextCmd;
    }

    @SuppressWarnings("unused")
    public float getNumber(int index) {
        return numbers.get(index);
    }

    public int size(){
        return numbers == null ? 0 :numbers.size();
    }
}
