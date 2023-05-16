package com.slimiot.hrv.Helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;
    //static int PRIVATE_MODE = 0;
    static String SharedPrefFileName = "MyPref";

    // constructor
    public PreferenceManager() {

    }

    public static void saveSharedPref(Context context, String key, String value) {
        /* save data to SharedPreferences */
        sharedPreferences = context.getSharedPreferences(SharedPrefFileName, Activity.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
        //editor.commit();
    }

    public static String getSharedPref(Context context, String key) {
        /* fetch data from SharedPreferences */
        sharedPreferences = context.getSharedPreferences(SharedPrefFileName, Activity.MODE_PRIVATE);
        //editor = sharedPreferences.edit();
        return sharedPreferences.getString(key, "");
    }

}

