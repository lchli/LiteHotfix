package com.lch.menote;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by lichenghang on 2018/7/7.
 */

@Modified
public class NewP3 {

    private String s;

    public static void run(Context context){
        Toast.makeText(context,"p 333",1).show();
    }

    private void haha(){
        s="haha";
    }

    private void xxx(){
        haha();
    }
}
