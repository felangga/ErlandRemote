package com.lampu.lib;

import android.content.Context;
import android.content.SharedPreferences;

import com.lampu.MainActivity;

public class Storage {
    public static void saveMemory(Context ctx, String memory) {
        SharedPreferences preference = ctx.getSharedPreferences("123dfwf", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();
        editor.putString("memory", memory);
        editor.apply();
    }

    public static String getMemory(Context ctx) {

        SharedPreferences mSettings = ctx.getSharedPreferences("123dfwf", Context.MODE_PRIVATE);
        return mSettings.getString("memory", null);

    }
}
