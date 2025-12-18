#!/bin/bash

echo "=== CloudEdge Recorder - Logs ==="
echo "Press Ctrl+C to stop"
echo ""

adb -s localhost:5555 logcat -s \
  VideoRecorderService:V \
  AutoLoginManager:V \
  CameraRecorder:V \
  RecorderLogger:V \
  RecorderConfig:V \
  ConfigReceiver:V \
  RecorderMainActivity:V \
  AndroidRuntime:E \
  System.err:E
