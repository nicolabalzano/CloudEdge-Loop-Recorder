package com.edgecloudrecorder.mearitaskerplugin.recorder;

import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logger for recording errors and events
 * Logs to logcat only (file logging disabled due to Android 11+ scoped storage)
 */
public class RecorderLogger {
    private static final String TAG = "RecorderLogger";
    private static final boolean ENABLE_FILE_LOGGING = false; // Disabled for compatibility
    private static final String LOG_FILE = "/sdcard/recording/recorder.log";
    private static RecorderLogger instance;
    
    private RecorderLogger() {
        ensureLogDirectory();
    }
    
    public static synchronized RecorderLogger getInstance() {
        if (instance == null) {
            instance = new RecorderLogger();
        }
        return instance;
    }
    
    private void ensureLogDirectory() {
        File logDir = new File("/sdcard/recording/");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }
    
    public void log(String tag, String message) {
        log(tag, message, null);
    }
    
    public void log(String tag, String message, Throwable throwable) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, tag, message);
        
        // Log to Android logcat
        if (throwable != null) {
            Log.e(tag, message, throwable);
            logMessage += "\n" + Log.getStackTraceString(throwable);
        } else {
            Log.i(tag, message);
        }
        
        // Log to file (disabled by default)
        if (ENABLE_FILE_LOGGING) {
            writeToFile(logMessage);
        }
    }
    
    public void error(String tag, String message) {
        log(tag, "ERROR: " + message);
    }
    
    public void error(String tag, String message, Throwable throwable) {
        log(tag, "ERROR: " + message, throwable);
    }
    
    public void info(String tag, String message) {
        log(tag, "INFO: " + message);
    }
    
    public void warning(String tag, String message) {
        log(tag, "WARNING: " + message);
    }
    
    private synchronized void writeToFile(String message) {
        try {
            File logFile = new File(LOG_FILE);
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(message + "\n");
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    public void clearLog() {
        try {
            File logFile = new File(LOG_FILE);
            if (logFile.exists()) {
                logFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear log file", e);
        }
    }
}
