package com.edgecloudrecorder.mearitaskerplugin.recorder;

import com.meari.sdk.bean.CameraInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a single camera recording session
 */
public class CameraRecorder {
    private final CameraInfo cameraInfo;
    private final String cameraName;
    private final String basePath;
    private volatile boolean isRecording = false;
    private volatile boolean shouldStop = false;
    private String currentFilePath;
    private long recordingStartTime;
    
    public CameraRecorder(CameraInfo cameraInfo, String basePath) {
        this.cameraInfo = cameraInfo;
        this.cameraName = sanitizeCameraName(cameraInfo.getDeviceName());
        this.basePath = basePath + cameraName + "/";
    }
    
    public CameraInfo getCameraInfo() {
        return cameraInfo;
    }
    
    public String getCameraName() {
        return cameraName;
    }
    
    public String getCameraId() {
        return cameraInfo.getDeviceID();
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public void setRecording(boolean recording) {
        this.isRecording = recording;
    }
    
    public boolean shouldStop() {
        return shouldStop;
    }
    
    public void requestStop() {
        this.shouldStop = true;
    }
    
    public void resetStop() {
        this.shouldStop = false;
    }
    
    /**
     * Generate new video file path with timestamp
     * Format: /sdcard/recording/<cameraName>/<cameraName>_HH_mm_DD_MM_YYYY.mp4
     */
    public String generateNewFilePath() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH_mm_dd_MM_yyyy", Locale.US);
        String timestamp = sdf.format(new Date());
        currentFilePath = basePath + cameraName + "_" + timestamp + ".mp4";
        recordingStartTime = System.currentTimeMillis();
        return currentFilePath;
    }
    
    public String getCurrentFilePath() {
        return currentFilePath;
    }
    
    public long getRecordingDuration() {
        if (recordingStartTime == 0) return 0;
        return System.currentTimeMillis() - recordingStartTime;
    }
    
    public String getBasePath() {
        return basePath;
    }
    
    /**
     * Sanitize camera name for use in file paths
     */
    private String sanitizeCameraName(String name) {
        if (name == null || name.isEmpty()) {
            return "camera_" + cameraInfo.getDeviceID();
        }
        // Remove special characters and spaces
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
