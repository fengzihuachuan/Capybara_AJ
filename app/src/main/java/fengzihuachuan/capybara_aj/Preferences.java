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

    public static String get(String def) {
        return sharedPref.getString("lastName", def);
    }

    public static int get(int def) {
        return sharedPref.getInt("lastPos", def);
    }

    public static String get(String name, int defa, int defb) {
        return sharedPref.getString("INFO_"+name, "0/0");
    }

    public static void set(String baseName) {
        editor.putString("lastName", baseName);
        editor.apply();
    }

    public static void set(int pos) {
        editor.putInt("lastPos", pos);
        editor.apply();
    }

    public static void set(String name, int recSum, int sbtSum) {
        editor.putString("INFO_"+name, recSum+"/"+sbtSum);
        editor.apply();
    }
}
