#!/bin/bash
# Set recording duration for both Docker and Android app

if [ -z "$1" ]; then
    echo "Usage: $0 <duration_in_minutes>"
    echo "Example: $0 5"
    exit 1
fi

DURATION=$1

echo "Setting recording duration to $DURATION minutes..."

# Update .env file
if [ -f .env ]; then
    sed -i "s/^RECORDING_DURATION_MINUTES=.*/RECORDING_DURATION_MINUTES=$DURATION/" .env
else
    echo "RECORDING_DURATION_MINUTES=$DURATION" > .env
fi

# Set duration in Android app via ADB
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_DURATION --ei duration_minutes $DURATION

# Restart sync container to apply new duration
docker compose restart sync

echo "✓ Recording duration set to $DURATION minutes"
echo "  - Android app updated"
echo "  - Sync interval updated to $((DURATION * 60)) seconds"
