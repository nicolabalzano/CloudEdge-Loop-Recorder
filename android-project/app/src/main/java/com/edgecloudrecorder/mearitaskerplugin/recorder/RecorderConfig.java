package com.edgecloudrecorder.mearitaskerplugin.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Configuration manager for video recorder
 * Settings can be changed via ADB:
 * adb shell am broadcast -a com.edgecloudrecorder.SET_DURATION --ei duration_minutes 15
 * adb shell am broadcast -a com.edgecloudrecorder.SET_QUALITY --es quality HD|SD|LOW
 */
public class RecorderConfig {
    private static final String TAG = "RecorderConfig";
    private static final String PREFS_NAME = "recorder_config";
    
    // Keys for SharedPreferences
    private static final String KEY_DURATION_MINUTES = "duration_minutes";
    private static final String KEY_VIDEO_QUALITY = "video_quality";
    private static final String KEY_USERNAME = "meari_username";
    private static final String KEY_PASSWORD = "meari_password";
    private static final String KEY_COUNTRY = "meari_country";
    private static final String KEY_COUNTRY_CODE = "meari_country_code";
    
    // Default values
    private static final int DEFAULT_DURATION_MINUTES = 1;
    private static final String DEFAULT_QUALITY = "HD";
    
    private final SharedPreferences prefs;
    private final Context context;
    
    public RecorderConfig(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadCredentialsFromEnv();
    }
    
    /**
     * Load credentials from environment variables (set via adb)
     * adb shell setprop debug.meari.username "your_email"
     * adb shell setprop debug.meari.password "your_password"
     * adb shell setprop debug.meari.country "US"
     */
    private void loadCredentialsFromEnv() {
        try {
            String username = System.getProperty("debug.meari.username");
            String password = System.getProperty("debug.meari.password");
            String countryCode = System.getProperty("debug.meari.country");
            
            if (username != null && !username.isEmpty()) {
                prefs.edit().putString(KEY_USERNAME, username).apply();
                Log.i(TAG, "Username loaded from env");
            }
            if (password != null && !password.isEmpty()) {
                prefs.edit().putString(KEY_PASSWORD, password).apply();
                Log.i(TAG, "Password loaded from env");
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                prefs.edit().putString(KEY_COUNTRY_CODE, countryCode).apply();
                Log.i(TAG, "Country code loaded from env");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading credentials from env", e);
        }
    }
    
    // Video duration in minutes
    public int getDurationMinutes() {
        return prefs.getInt(KEY_DURATION_MINUTES, DEFAULT_DURATION_MINUTES);
    }
    
    public void setDurationMinutes(int minutes) {
        prefs.edit().putInt(KEY_DURATION_MINUTES, minutes).apply();
        Log.i(TAG, "Duration set to " + minutes + " minutes");
    }
    
    // Video quality (HD, SD, LOW)
    public String getVideoQuality() {
        return prefs.getString(KEY_VIDEO_QUALITY, DEFAULT_QUALITY);
    }
    
    public void setVideoQuality(String quality) {
        prefs.edit().putString(KEY_VIDEO_QUALITY, quality.toUpperCase()).apply();
        Log.i(TAG, "Quality set to " + quality);
    }
    
    // Credentials - with hardcoded fallback for testing
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "your_email@example.com");
    }
    
    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }
    
    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "your_password");
    }
    
    public void setPassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }
    
    public String getCountryCode() {
        return prefs.getString(KEY_COUNTRY_CODE, "your_country_code");
    }
    
    public void setCountryCode(String countryCode) {
        prefs.edit().putString(KEY_COUNTRY_CODE, countryCode).apply();
    }
    
    public String getCountry() {
        return prefs.getString(KEY_COUNTRY, "your_country");
    }
    
    public void setCountry(String country) {
        prefs.edit().putString(KEY_COUNTRY, country).apply();
    }
    
    public boolean hasCredentials() {
        return getUsername() != null && getPassword() != null;
    }
    
    /**
     * Get absolute recording path on Android device
     * @return /sdcard/Download/recording/
     */
    public String getRecordingBasePath() {
        return "/sdcard/Download/recording/";
    }
}
