#!/bin/bash
set -e

echo "=== CloudEdge Recorder - Redroid Setup ==="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Load configuration from .env file
if [ -f .env ]; then
    echo -e "${GREEN}Loading configuration from .env file...${NC}"
    export $(cat .env | grep -v '^#' | xargs)
else
    echo -e "${RED}Error: .env file not found${NC}"
    exit 1
fi

echo -e "${GREEN}Configuration loaded:${NC}"
echo "  Username: $MEARI_USERNAME"
echo "  Country: $MEARI_COUNTRY ($MEARI_CODE)"
echo "  Duration: $RECORDING_DURATION_MINUTES minutes"
echo "  Quality: $VIDEO_QUALITY"
echo ""

# Check if binder modules are loaded
if ! lsmod | grep -q binder_linux; then
    echo -e "${YELLOW}Binder modules not loaded. Running setup...${NC}"
    ./setup_binder.sh
fi

echo -e "${YELLOW}Step 1: Starting Redroid container...${NC}"
docker compose up -d

echo -e "${YELLOW}Step 2: Waiting for Android to boot (10s)...${NC}"
sleep 10

echo -e "${YELLOW}Step 3: Connecting to ADB...${NC}"
adb connect localhost:5555
sleep 2

echo -e "${YELLOW}Step 4: Building and installing APK...${NC}"
cd android-project
./gradlew assembleDebug
cd ..
adb -s localhost:5555 install -r android-project/app/build/outputs/apk/debug/app-debug.apk

echo -e "${YELLOW}Step 5: Granting permissions...${NC}"
adb -s localhost:5555 shell pm grant com.edgecloudrecorder android.permission.WRITE_EXTERNAL_STORAGE
adb -s localhost:5555 shell pm grant com.edgecloudrecorder android.permission.READ_EXTERNAL_STORAGE
adb -s localhost:5555 shell pm grant com.edgecloudrecorder android.permission.CAMERA
adb -s localhost:5555 shell pm grant com.edgecloudrecorder android.permission.RECORD_AUDIO
adb -s localhost:5555 shell appops set com.edgecloudrecorder SYSTEM_ALERT_WINDOW allow

echo -e "${YELLOW}Step 6: Configuring credentials...${NC}"
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_USERNAME -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --es username "$MEARI_USERNAME"
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_PASSWORD -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --es password "$MEARI_PASSWORD"
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_COUNTRY -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --es country "$MEARI_COUNTRY"
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_COUNTRY_CODE -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --es code "$MEARI_CODE"

echo -e "${YELLOW}Step 7: Configuring recording settings...${NC}"
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_DURATION -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --ei duration_minutes $RECORDING_DURATION_MINUTES
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_QUALITY -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --es quality "$VIDEO_QUALITY"

echo -e "${YELLOW}Step 8: Creating recording directory...${NC}"
adb -s localhost:5555 shell mkdir -p /sdcard/recording

echo -e "${YELLOW}Step 9: Starting VideoRecorderService...${NC}"
adb -s localhost:5555 shell am start-foreground-service com.edgecloudrecorder/com.edgecloudrecorder.mearitaskerplugin.recorder.VideoRecorderService

echo -e "${GREEN}=== Setup Complete ===${NC}"
echo ""
echo "Commands to monitor:"
echo "  Watch logs:  ./watch_logs.sh"
echo "  Stop service: adb -s localhost:5555 shell am stopservice com.edgecloudrecorder/com.edgecloudrecorder.mearitaskerplugin.recorder.VideoRecorderService"
echo "  Shell:       adb -s localhost:5555 shell"
echo "  Stop Redroid: docker compose down"
echo ""
echo "Recordings will be saved in: ./recordings/"
