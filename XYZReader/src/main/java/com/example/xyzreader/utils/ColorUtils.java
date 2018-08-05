package com.example.xyzreader.utils;

import android.os.Build;
import android.view.Window;

public class ColorUtils {

    public static void setStatusBarColor(Window window, int statusBarColor){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(statusBarColor);
        }
    }
}
