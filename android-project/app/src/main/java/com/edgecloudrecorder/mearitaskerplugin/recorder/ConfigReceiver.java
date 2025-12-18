package com.edgecloudrecorder.mearitaskerplugin.recorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Broadcast receiver to change configuration via ADB
 * Usage:
 * adb shell am broadcast -a com.edgecloudrecorder.SET_DURATION --ei duration_minutes 15
 * adb shell am broadcast -a com.edgecloudrecorder.SET_QUALITY --es quality HD
 * adb shell am broadcast -a com.edgecloudrecorder.SET_CREDENTIALS --es username "email" --es password "pass" --es country "US"
 */
public class ConfigReceiver extends BroadcastReceiver {
    private static final String TAG = "ConfigReceiver";
    
    private static final String ACTION_SET_DURATION = "com.edgecloudrecorder.SET_DURATION";
    private static final String ACTION_SET_QUALITY = "com.edgecloudrecorder.SET_QUALITY";
    private static final String ACTION_SET_CREDENTIALS = "com.edgecloudrecorder.SET_CREDENTIALS";
    private static final String ACTION_SET_USERNAME = "com.edgecloudrecorder.SET_USERNAME";
    private static final String ACTION_SET_PASSWORD = "com.edgecloudrecorder.SET_PASSWORD";
    private static final String ACTION_SET_COUNTRY = "com.edgecloudrecorder.SET_COUNTRY";
    private static final String ACTION_SET_COUNTRY_CODE = "com.edgecloudrecorder.SET_COUNTRY_CODE";
    private static final String ACTION_GET_STATUS = "com.edgecloudrecorder.GET_STATUS";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        
        RecorderConfig config = new RecorderConfig(context);
        
        switch (action) {
            case ACTION_SET_DURATION:
                int minutes = intent.getIntExtra("duration_minutes", -1);
                if (minutes > 0) {
                    config.setDurationMinutes(minutes);
                    Log.i(TAG, "Duration set to " + minutes + " minutes");
                    showToast(context, "Video duration set to " + minutes + " minutes");
                }
                break;
                
            case ACTION_SET_QUALITY:
                String quality = intent.getStringExtra("quality");
                if (quality != null) {
                    config.setVideoQuality(quality);
                    Log.i(TAG, "Quality set to " + quality);
                    showToast(context, "Video quality set to " + quality);
                }
                break;
                
            case ACTION_SET_CREDENTIALS:
                String username = intent.getStringExtra("username");
                String password = intent.getStringExtra("password");
                String country = intent.getStringExtra("country");
                
                if (username != null) config.setUsername(username);
                if (password != null) config.setPassword(password);
                if (country != null) config.setCountryCode(country);
                
                Log.i(TAG, "Credentials updated");
                showToast(context, "Credentials updated");
                break;
                
            case ACTION_SET_USERNAME:
                String user = intent.getStringExtra("username");
                if (user != null) {
                    config.setUsername(user);
                    Log.i(TAG, "Username set to: " + user);
                }
                break;
                
            case ACTION_SET_PASSWORD:
                String pass = intent.getStringExtra("password");
                if (pass != null) {
                    config.setPassword(pass);
                    Log.i(TAG, "Password set");
                }
                break;
                
            case ACTION_SET_COUNTRY:
                String ctry = intent.getStringExtra("country");
                if (ctry != null) {
                    config.setCountry(ctry);
                    Log.i(TAG, "Country set to: " + ctry);
                }
                break;
                
            case ACTION_SET_COUNTRY_CODE:
                String code = intent.getStringExtra("code");
                if (code != null) {
                    config.setCountryCode(code);
                    Log.i(TAG, "Country code set to: " + code);
                }
                break;
                
            case ACTION_GET_STATUS:
                String status = String.format(
                    "Duration: %d min, Quality: %s, Username: %s, Has credentials: %b",
                    config.getDurationMinutes(),
                    config.getVideoQuality(),
                    config.getUsername(),
                    config.hasCredentials()
                );
                Log.i(TAG, "Status: " + status);
                showToast(context, status);
                break;
        }
    }
    
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
