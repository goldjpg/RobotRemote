package de.horn.robot.remote;

import android.content.SharedPreferences;

public class Settings {

    static String lastIp = "";
    static SharedPreferences myprefs;
    static float lastLat;
    static float lastLon;
    static float lastRobotLat;
    static float lastRobotLon;
    static float lastZoom;
    static float lastRobotLatOffset;
    static float lastRobotLonOffset;

    static void loadSettings(){
        lastIp = myprefs.getString("lastIp", "0.0.0.0");
        lastLat = myprefs.getFloat("lastLat", 0f);
        lastLon = myprefs.getFloat("lastLon", 0f);
        lastRobotLat = myprefs.getFloat("lastRobotLat", 0f);
        lastRobotLon = myprefs.getFloat("lastRobotLon", 0f);
        lastRobotLatOffset = myprefs.getFloat("lastRobotLatOffset", 0f);
        lastRobotLonOffset = myprefs.getFloat("lastRobotLonOffset", 0f);
        lastZoom = myprefs.getFloat("lastZoom", 0f);
    }

    static void saveSettings(){
        myprefs.edit().putString("lastIp", lastIp)
                .putFloat("lastLat", lastLat)
                .putFloat("lastLon", lastLon)
                .putFloat("lastRobotLat", lastRobotLat)
                .putFloat("lastRobotLon", lastRobotLon)
                .putFloat("lastRobotLatOffset", lastRobotLatOffset)
                .putFloat("lastRobotLonOffset", lastRobotLonOffset)
                .putFloat("lastZoom", lastZoom)
                .apply();
    }

}
