package de.horn.robot.remote;

import android.content.SharedPreferences;

public class Settings {

    static String lastIp = "";
    static SharedPreferences myprefs;

    static void loadSettings(){
        lastIp = myprefs.getString("lastIp", "0.0.0.0");
    }

    static void saveSettings(){
        myprefs.edit().putString("lastIp", lastIp).apply();
    }

}
