package com.edgecloudrecorder.mearitaskerplugin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.edgecloudrecorder.R;
import com.edgecloudrecorder.mearitaskerplugin.recorder.RecorderConfig;
import com.edgecloudrecorder.mearitaskerplugin.recorder.RecorderLogger;
import com.edgecloudrecorder.mearitaskerplugin.recorder.VideoRecorderService;

import java.io.File;

/**
 * Simplified MainActivity for headless video recording
 * Automatically starts the recording service on launch
 */
public class RecorderMainActivity extends AppCompatActivity {
    private static final String TAG = "RecorderMainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private RecorderLogger logger;
    private RecorderConfig config;
    private TextView statusText;
    private Button startButton;
    private Button stopButton;
    private boolean isServiceRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder_main);
        
        logger = RecorderLogger.getInstance();
        config = new RecorderConfig(this);
        
        setupUI();
        checkPermissions();
    }
    
    private void setupUI() {
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        
        startButton.setOnClickListener(v -> startRecordingService());
        stopButton.setOnClickListener(v -> stopRecordingService());
        
        updateStatus();
    }
    
    private void updateStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Video Recorder Status\n\n");
        status.append("Duration: ").append(config.getDurationMinutes()).append(" minutes\n");
        status.append("Quality: ").append(config.getVideoQuality()).append("\n");
        status.append("Recording path: ").append(config.getRecordingBasePath()).append("\n\n");
        
        if (config.hasCredentials()) {
            status.append("Username: ").append(config.getUsername()).append("\n");
            status.append("Country: ").append(config.getCountryCode()).append("\n");
            status.append("✓ Credentials configured\n\n");
        } else {
            status.append("⚠ No credentials configured!\n");
            status.append("Set via ADB:\n");
            status.append("adb shell am broadcast -a com.edgecloudrecorder.SET_CREDENTIALS ");
            status.append("--es username \"email\" --es password \"pass\" --es country \"US\"\n\n");
        }
        
        status.append("Commands:\n");
        status.append("• Change duration:\n  adb shell am broadcast -a com.edgecloudrecorder.SET_DURATION --ei duration_minutes 15\n\n");
        status.append("• Change quality:\n  adb shell am broadcast -a com.edgecloudrecorder.SET_QUALITY --es quality HD\n\n");
        status.append("• Get status:\n  adb shell am broadcast -a com.edgecloudrecorder.GET_STATUS\n");
        
        statusText.setText(status.toString());
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        };
        
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            checkStoragePermission();
        }
    }
    
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Please grant storage permission", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            } else {
                createRecordingDirectory();
                // Auto-start service if credentials are configured
                if (config.hasCredentials()) {
                    startRecordingService();
                }
            }
        } else {
            createRecordingDirectory();
            if (config.hasCredentials()) {
                startRecordingService();
            }
        }
    }
    
    private void createRecordingDirectory() {
        File recordingDir = new File(config.getRecordingBasePath());
        if (!recordingDir.exists()) {
            if (recordingDir.mkdirs()) {
                logger.info(TAG, "Recording directory created: " + config.getRecordingBasePath());
            } else {
                logger.error(TAG, "Failed to create recording directory");
            }
        }
    }
    
    private void startRecordingService() {
        if (!config.hasCredentials()) {
            Toast.makeText(this, "Please configure credentials first!", Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent serviceIntent = new Intent(this, VideoRecorderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        isServiceRunning = true;
        Toast.makeText(this, "Recording service started", Toast.LENGTH_SHORT).show();
        logger.info(TAG, "Recording service started by user");
    }
    
    private void stopRecordingService() {
        Intent serviceIntent = new Intent(this, VideoRecorderService.class);
        stopService(serviceIntent);
        
        isServiceRunning = false;
        Toast.makeText(this, "Recording service stopped", Toast.LENGTH_SHORT).show();
        logger.info(TAG, "Recording service stopped by user");
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                checkStoragePermission();
            } else {
                Toast.makeText(this, "Permissions required for recording!", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
