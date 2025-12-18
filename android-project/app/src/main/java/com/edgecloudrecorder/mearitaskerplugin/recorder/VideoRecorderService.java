package com.edgecloudrecorder.mearitaskerplugin.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

import com.meari.sdk.MeariDeviceController;
import com.meari.sdk.MeariUser;
import com.meari.sdk.bean.CameraInfo;
import com.meari.sdk.bean.MeariDevice;
import com.meari.sdk.callback.IDevListCallback;
import com.meari.sdk.listener.MeariDeviceListener;
import com.meari.sdk.listener.MeariDeviceRecordMp4Listener;
import com.meari.sdk.listener.MeariDeviceVideoStopListener;
import com.ppstrong.ppsplayer.PPSGLSurfaceView;

import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;

import com.edgecloudrecorder.mearitaskerplugin.CommonUtils;
import com.edgecloudrecorder.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service for continuous video recording from all cameras
 */
public class VideoRecorderService extends Service {
    private static final String TAG = "VideoRecorderService";
    private static final String CHANNEL_ID = "video_recorder_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private RecorderLogger logger;
    private RecorderConfig config;
    private AutoLoginManager loginManager;
    
    private Map<String, CameraRecorder> cameraRecorders = new HashMap<>();
    private Map<String, MeariDeviceController> deviceControllers = new HashMap<>();
    private Map<String, PPSGLSurfaceView> surfaceViews = new HashMap<>();
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler rotationHandler;
    private WindowManager windowManager;
    
    private volatile boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        logger = RecorderLogger.getInstance();
        config = new RecorderConfig(this);
        loginManager = new AutoLoginManager(this);
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        rotationHandler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        logger.info(TAG, "VideoRecorderService created");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Initializing..."));
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.info(TAG, "VideoRecorderService started");
        
        if (!isRunning) {
            isRunning = true;
            startRecordingProcess();
        }
        
        return START_STICKY;
    }
    
    private void startRecordingProcess() {
        logger.info(TAG, "Starting recording process...");
        
        if (loginManager.isLoggedIn()) {
            logger.info(TAG, "Already logged in, loading cameras...");
            loadCamerasAndStartRecording();
        } else {
            logger.info(TAG, "Not logged in, attempting login...");
            loginManager.login(new AutoLoginManager.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    logger.info(TAG, "Login successful, loading cameras...");
                    loadCamerasAndStartRecording();
                }
                
                @Override
                public void onLoginFailed(String error) {
                    logger.error(TAG, "Login failed: " + error);
                    updateNotification("Login failed: " + error);
                    // Retry login after 30 seconds
                    mainHandler.postDelayed(() -> {
                        logger.info(TAG, "Retrying login...");
                        startRecordingProcess();
                    }, 30000);
                }
            });
        }
    }
    
    private void loadCamerasAndStartRecording() {
        MeariUser.getInstance().getDeviceList(new IDevListCallback() {
            @Override
            public void onSuccess(MeariDevice meariDevice) {
                // Merge all camera types
                ArrayList<CameraInfo> cameraList = new ArrayList<>();
                cameraList.addAll(meariDevice.getFourthGenerations());
                cameraList.addAll(meariDevice.getBatteryCameras());
                
                logger.info(TAG, "Loaded " + cameraList.size() + " cameras");
                updateNotification("Found " + cameraList.size() + " cameras");
                
                for (CameraInfo camera : cameraList) {
                    String cameraId = camera.getDeviceID();
                    CameraRecorder recorder = new CameraRecorder(camera, config.getRecordingBasePath());
                    cameraRecorders.put(cameraId, recorder);
                    
                    // Create directory for this camera
                    File cameraDir = new File(recorder.getBasePath());
                    if (!cameraDir.exists()) {
                        cameraDir.mkdirs();
                    }
                    
                    // Start recording for this camera
                    startCameraRecording(recorder);
                }
                
                updateNotification("Recording " + cameraList.size() + " cameras");
            }
            
            @Override
            public void onError(int code, String error) {
                logger.error(TAG, "Failed to load cameras: [" + code + "] " + error);
                updateNotification("Failed to load cameras");
                // Retry after 30 seconds
                mainHandler.postDelayed(() -> loadCamerasAndStartRecording(), 30000);
            }
        });
    }
    
    private void startCameraRecording(final CameraRecorder recorder) {
        executorService.execute(() -> {
            logger.info(TAG, "Starting recording for camera: " + recorder.getCameraName());
            connectAndRecord(recorder);
        });
    }
    
    private void connectAndRecord(final CameraRecorder recorder) {
        final CameraInfo cameraInfo = recorder.getCameraInfo();
        final String cameraId = recorder.getCameraId();
        
        // Create controller if not exists
        MeariDeviceController controller = deviceControllers.get(cameraId);
        if (controller == null) {
            controller = new MeariDeviceController();
            controller.setCameraInfo(cameraInfo);
            deviceControllers.put(cameraId, controller);
        }
        
        final MeariDeviceController finalController = controller;
        
        // Connect to camera
        finalController.startConnect(new MeariDeviceListener() {
            @Override
            public void onSuccess(String successMsg) {
                logger.info(TAG, "Camera connected: " + recorder.getCameraName());
                startStreamRecording(recorder, finalController);
            }
            
            @Override
            public void onFailed(String errorMsg) {
                logger.error(TAG, "Camera connection failed: " + recorder.getCameraName() + " - " + errorMsg);
                
                // Release the failed controller to avoid reusing corrupted state
                try {
                    finalController.release();
                } catch (Exception e) {
                    logger.error(TAG, "Error releasing controller: " + e.getMessage());
                }
                deviceControllers.remove(cameraId);
                
                // Retry connection after 10 seconds with a fresh controller
                mainHandler.postDelayed(() -> {
                    logger.info(TAG, "Retrying connection for: " + recorder.getCameraName());
                    connectAndRecord(recorder);
                }, 10000);
            }
        });
    }
    
    private void startStreamRecording(final CameraRecorder recorder, final MeariDeviceController controller) {
        if (!controller.isConnected()) {
            logger.warning(TAG, "Controller not connected for " + recorder.getCameraName());
            return;
        }
        
        // Create invisible SurfaceView for this camera (required by SDK)
        PPSGLSurfaceView surfaceView = surfaceViews.get(recorder.getCameraId());
        if (surfaceView == null) {
            surfaceView = createInvisibleSurfaceView(recorder.getCameraId());
            surfaceViews.put(recorder.getCameraId(), surfaceView);
        }
        
        final PPSGLSurfaceView finalSurfaceView = surfaceView;
        final String filePath = recorder.generateNewFilePath();
        
        // Create directory if not exists
        File fileDir = new File(filePath).getParentFile();
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        
        logger.info(TAG, "Starting preview for " + recorder.getCameraName());
        
        // STEP 1: Get the correct stream ID for the desired quality
        int quality = getQualityPreference(config.getVideoQuality());
        CameraInfo cameraInfo = recorder.getCameraInfo();
        String streamIdStr = CommonUtils.getDefaultStreamId(cameraInfo, quality);
        int streamId = Integer.parseInt(streamIdStr);
        logger.info(TAG, "Using stream ID: " + streamId + " for quality: " + config.getVideoQuality());
        
        // STEP 2: Start preview with the correct stream ID (REQUIRED before recording)
        controller.startPreview(finalSurfaceView, streamId, new MeariDeviceListener() {
            @Override
            public void onSuccess(String msg) {
                logger.info(TAG, "Preview started for " + recorder.getCameraName() + ", now starting recording");
                
                // STEP 2: Now that preview is active, start recording
                controller.startRecordMP4(filePath, new MeariDeviceListener() {
                    @Override
                    public void onSuccess(String successMsg) {
                        recorder.setRecording(true);
                        logger.info(TAG, "Recording started successfully for " + recorder.getCameraName() + " to " + filePath);
                        
                        // Schedule file rotation based on configured duration
                        scheduleFileRotation(recorder, controller);
                    }
                    
                    @Override
                    public void onFailed(String errorMsg) {
                        logger.error(TAG, "Failed to start recording for " + recorder.getCameraName() + ": " + errorMsg);
                        recorder.setRecording(false);
                        
                        // Retry after 10 seconds
                        mainHandler.postDelayed(() -> {
                            logger.info(TAG, "Retrying recording for " + recorder.getCameraName());
                            connectAndRecord(recorder);
                        }, 10000);
                    }
                }, new MeariDeviceRecordMp4Listener() {
                    @Override
                    public void RecordMp4Interrupt(int code) {
                        logger.warning(TAG, "Recording interrupted for " + recorder.getCameraName() + ", code: " + code);
                        
                        if (code > 0) {
                            logger.info(TAG, "Recording completed successfully, file saved: " + filePath);
                        } else {
                            logger.error(TAG, "Recording failed with code: " + code);
                        }
                        
                        recorder.setRecording(false);
                        
                        // Start new recording automatically
                        mainHandler.postDelayed(() -> {
                            if (!recorder.shouldStop()) {
                                logger.info(TAG, "Auto-restarting recording for " + recorder.getCameraName());
                                startStreamRecording(recorder, controller);
                            }
                        }, 2000);
                    }
                });
            }
            
            @Override
            public void onFailed(String errorMsg) {
                logger.error(TAG, "Preview failed for " + recorder.getCameraName() + ": " + errorMsg);
                // Retry connection
                mainHandler.postDelayed(() -> {
                    logger.info(TAG, "Retrying connection for " + recorder.getCameraName());
                    connectAndRecord(recorder);
                }, 10000);
            }
        }, new MeariDeviceVideoStopListener() {
            @Override
            public void onVideoClosed(int code) {
                logger.warning(TAG, "Video stream closed for " + recorder.getCameraName() + ", code: " + code);
            }
        });
    }
    
    private void scheduleFileRotation(final CameraRecorder recorder, final MeariDeviceController controller) {
        int durationMinutes = config.getDurationMinutes();
        long durationMs = durationMinutes * 60 * 1000;
        
        logger.info(TAG, "Scheduled file rotation for " + recorder.getCameraName() + " in " + durationMinutes + " minutes");
        
        rotationHandler.postDelayed(() -> {
            if (recorder.isRecording() && !recorder.shouldStop()) {
                logger.info(TAG, "Rotating file for " + recorder.getCameraName());
                
                // Stop current recording using SDK method
                controller.stopRecordMP4(new MeariDeviceListener() {
                    @Override
                    public void onSuccess(String s) {
                        logger.info(TAG, "Recording stopped successfully for rotation: " + recorder.getCameraName());
                        recorder.setRecording(false);
                        
                        // Wait 3 seconds before starting new recording
                        // (SDK requires minimum 3 seconds between recordings)
                        mainHandler.postDelayed(() -> {
                            if (!recorder.shouldStop()) {
                                // DON'T restart preview - just continue recording with new file
                                continueRecording(recorder, controller);
                            }
                        }, 3000);
                    }
                    
                    @Override
                    public void onFailed(String s) {
                        logger.error(TAG, "Failed to stop recording for rotation: " + s);
                        recorder.setRecording(false);
                        
                        // Try to start new recording anyway
                        mainHandler.postDelayed(() -> {
                            if (!recorder.shouldStop()) {
                                continueRecording(recorder, controller);
                            }
                        }, 3000);
                    }
                });
            }
        }, durationMs);
    }
    
    /**
     * Continue recording with a new file without restarting preview
     * (preview is already active, just start new recording)
     */
    private void continueRecording(final CameraRecorder recorder, final MeariDeviceController controller) {
        if (!controller.isConnected()) {
            logger.warning(TAG, "Controller not connected for " + recorder.getCameraName());
            // If disconnected, need to reconnect fully
            connectAndRecord(recorder);
            return;
        }
        
        final String filePath = recorder.generateNewFilePath();
        
        // Create directory if not exists
        File fileDir = new File(filePath).getParentFile();
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        
        logger.info(TAG, "Continuing recording for " + recorder.getCameraName() + " to new file");
        
        // Start recording with new file (preview is already running)
        controller.startRecordMP4(filePath, new MeariDeviceListener() {
            @Override
            public void onSuccess(String successMsg) {
                recorder.setRecording(true);
                logger.info(TAG, "Recording continued successfully for " + recorder.getCameraName() + " to " + filePath);
                
                // Schedule next rotation
                scheduleFileRotation(recorder, controller);
            }
            
            @Override
            public void onFailed(String errorMsg) {
                logger.error(TAG, "Failed to continue recording for " + recorder.getCameraName() + ": " + errorMsg);
                recorder.setRecording(false);
                
                // If recording fails, try to restart everything
                mainHandler.postDelayed(() -> {
                    logger.info(TAG, "Reconnecting and restarting for " + recorder.getCameraName());
                    
                    // Stop preview and reconnect
                    controller.stopPreview(new MeariDeviceListener() {
                        @Override
                        public void onSuccess(String s) {
                            connectAndRecord(recorder);
                        }
                        
                        @Override
                        public void onFailed(String s) {
                            connectAndRecord(recorder);
                        }
                    });
                }, 10000);
            }
        }, new MeariDeviceRecordMp4Listener() {
            @Override
            public void RecordMp4Interrupt(int code) {
                logger.warning(TAG, "Recording interrupted for " + recorder.getCameraName() + ", code: " + code);
                recorder.setRecording(false);
            }
        });
    }
    
    private int getQualityPreference(String quality) {
        switch (quality.toUpperCase()) {
            case "HD":
                return CommonUtils.QUALITY_HD;
            case "SD":
                return CommonUtils.QUALITY_SD;
            case "LOW":
                return CommonUtils.QUALITY_LOW;
            default:
                return CommonUtils.QUALITY_HD;
        }
    }
    
    private PPSGLSurfaceView createInvisibleSurfaceView(String cameraId) {
        logger.info(TAG, "Creating invisible surface view for camera: " + cameraId);
        
        PPSGLSurfaceView surfaceView = new PPSGLSurfaceView(this, 1, 1);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );
        params.x = 0;
        params.y = 0;
        
        windowManager.addView(surfaceView, params);
        
        logger.info(TAG, "Surface view created for camera: " + cameraId);
        return surfaceView;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Video Recorder Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Continuous video recording from cameras");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Video Recorder")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text));
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.info(TAG, "VideoRecorderService destroyed");
        isRunning = false;
        
        // Stop all recordings
        for (Map.Entry<String, CameraRecorder> entry : cameraRecorders.entrySet()) {
            entry.getValue().requestStop();
        }
        
        // Remove all surface views
        for (Map.Entry<String, PPSGLSurfaceView> entry : surfaceViews.entrySet()) {
            try {
                windowManager.removeView(entry.getValue());
                logger.info(TAG, "Removed surface view for camera: " + entry.getKey());
            } catch (Exception e) {
                logger.error(TAG, "Error removing surface view", e);
            }
        }
        surfaceViews.clear();
        
        for (Map.Entry<String, MeariDeviceController> entry : deviceControllers.entrySet()) {
            try {
                final String cameraId = entry.getKey();
                final MeariDeviceController controller = entry.getValue();
                
                // Stop recording if active
                controller.stopRecordMP4(new MeariDeviceListener() {
                    @Override
                    public void onSuccess(String s) {
                        logger.info(TAG, "Stopped recording for camera: " + cameraId);
                    }
                    
                    @Override
                    public void onFailed(String s) {
                        logger.warning(TAG, "Failed to stop recording for camera: " + cameraId);
                    }
                });
                
                // Disconnect controller - just stop, no explicit disconnect needed
                // controller.disconnect() doesn't exist in this SDK version
            } catch (Exception e) {
                logger.error(TAG, "Error stopping controller", e);
            }
        }
        
        executorService.shutdown();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
