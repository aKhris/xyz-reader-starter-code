package com.akhris.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    static {
        URL url = null;
        try {
            url = new URL("https://go.udacity.com/xyz-reader-json" );
        } catch (MalformedURLException ignored) {
            //instead of throwing an error the snackbar in ArticleListFragment is used
            // telling there is no connection, would you like to retry?
            Log.e(TAG, "Please check your internet connection.");
        }

        BASE_URL = url;
    }
}
