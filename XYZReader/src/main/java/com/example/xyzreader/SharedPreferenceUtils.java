package com.example.xyzreader;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtils {
    private static final String PREFS_NAME = "xyz_prefs";

    private static final String KEY_VIEW_PAGER_POSITION = "view_pager_position";

    public static void saveCurrentPosition(Context context, int position){
        getPrefs(context)
                .edit()
                .putInt(KEY_VIEW_PAGER_POSITION, position)
                .apply();
    }

    public static SharedPreferences getPrefs(Context context){
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


}
