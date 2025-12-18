# CloudEdge4Tasker - EdgeCloudRecorder

Automatic video recording system from Meari camera in a containerized environment.

## Project Structure

```
.
├── android-project/       # Android project with EdgeCloudRecorder app
│   ├── app/              # Application source code
│   ├── gradle/           # Gradle wrapper files
│   ├── build.gradle      # Build configuration
│   └── gradlew           # Gradle wrapper script
├── recordings/           # Recorded videos (synced from container)
├── docker-compose.yml    # Docker configuration for Redroid
├── .env                  # Centralized configuration
├── setup_redroid.sh      # Initial container and app setup
├── watch_logs.sh         # Monitor recorder logs
└── set_duration.sh       # Change recording duration
```

## Configuration

Create the `.env` file with Meari credentials:

```bash
MEARI_USERNAME=your_email@example.com
MEARI_PASSWORD=your_password
MEARI_COUNTRY=IT
MEARI_CODE=39
RECORDING_DURATION_MINUTES=1
VIDEO_QUALITY=HD
```

## Initial Setup

1. **Prepare binder modules** (required for Redroid):
   ```bash
   ./setup_binder.sh
   ```

2. **Start the system**:
   ```bash
   ./setup_redroid.sh
   ```

   This script:
   - Starts the Redroid container (Android 11)
   - Compiles and installs the EdgeCloudRecorder app
   - Configures Meari credentials
   - Starts the recording service

## Usage

### Monitoring
```bash
./watch_logs.sh
```

### Change recording duration
```bash
./set_duration.sh 15  # 15 minutes per file
```

### Video Synchronization
Videos are automatically saved to `./recordings/` from the container.

### Manual Commands

**Configuration**:
```bash
# Change quality (HD/SD/LOW)
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SET_QUALITY -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver --es quality HD

# Show current configuration
adb -s localhost:5555 shell am broadcast -a com.edgecloudrecorder.SHOW_CONFIG -n com.edgecloudrecorder/.mearitaskerplugin.recorder.ConfigReceiver
```

**Service Control**:
```bash
# Stop
adb -s localhost:5555 shell am stopservice com.edgecloudrecorder/com.edgecloudrecorder.mearitaskerplugin.recorder.VideoRecorderService

# Start
adb -s localhost:5555 shell am start-foreground-service com.edgecloudrecorder/com.edgecloudrecorder.mearitaskerplugin.recorder.VideoRecorderService
```

**Container**:
```bash
# Stop container
docker compose down

# Restart container
docker compose up -d

# Shell into container
adb -s localhost:5555 shell
```

## Requirements

- Docker and Docker Compose
- ADB (Android Debug Bridge)
- Linux with kernel supporting binder
- Binder modules loaded (see `setup_binder.sh`)

## Architecture

The system uses:
- **Redroid**: Containerized Android 11
- **Meari SDK**: IP camera communication
- **VideoRecorderService**: Android foreground service for continuous recording
- **File Rotation**: Creates new files every N minutes without streaming interruption

## License

See LICENSE file.

