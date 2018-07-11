package com.lch.menote;

import android.util.Log;

@Modified
public class NewPExt extends NewP {

    @Override
    protected int rInt() {
        int i= super.rInt();

        Log.e("i",i+"");
        return 2;
    }
}
