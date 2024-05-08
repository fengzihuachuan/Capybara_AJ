package fengzihuachuan.capybara_aj;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    private static SharedPreferences sharedPref = null;
    private static SharedPreferences.Editor editor = null;

    public static void init(MainActivity main) {
        sharedPref = main.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
    }

    public static String getLastVideo() {
        return sharedPref.getString("lastVideo", null);
    }

    public static void setLastVideo(String videoName) {
        editor.putString("lastVideo", videoName);
        editor.apply();
    }

    public static int getLastPos() {
        return sharedPref.getInt("lastPos", 0);
    }

    public static void setLastPos(int pos) {
        editor.putInt("lastPos", pos);
        editor.apply();
    }
}
