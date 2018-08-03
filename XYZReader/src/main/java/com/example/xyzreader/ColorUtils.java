package com.example.xyzreader;

import android.graphics.Color;
import android.os.Build;
import android.view.Window;

public class ColorUtils {

    public static void setStatusBarColor(Window window, int statusBarColor){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(statusBarColor);
        }
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
