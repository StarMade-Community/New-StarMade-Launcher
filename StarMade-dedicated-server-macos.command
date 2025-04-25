#!/bin/bash
# StarMade Dedicated Server Launch Script for macOS
# This script launches the StarMade Launcher JAR in server mode

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Create log file if it doesn't exist
LOG_FILE="$SCRIPT_DIR/server_log.txt"
touch "$LOG_FILE"

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Check if StarMade directory exists
if [ ! -d "StarMade" ]; then
    log "StarMade directory not found."
    log "Please run the StarMade Launcher first to download the game."
    echo "Press enter to exit..."
    read
    exit 1
fi

# Check if launcher JAR exists
if [ ! -f "StarMade-Launcher.jar" ]; then
    log "StarMade-Launcher.jar not found."
    log "Please ensure the launcher is properly installed."
    echo "Press enter to exit..."
    read
    exit 1
fi

# Check if Java 23 exists
JAVA_PATH="$SCRIPT_DIR/jre23/Contents/Home/bin/java"
if [ ! -f "$JAVA_PATH" ]; then
    log "Java 23 runtime not found at $JAVA_PATH"
    log "Please run the StarMade Launcher first to install the required Java runtime."
    echo "Press enter to exit..."
    read
    exit 1
fi

# Ensure Java is executable
chmod +x "$JAVA_PATH"

# Determine port (default 4242)
PORT=4242
if [ -n "$1" ]; then
    PORT="$1"
fi

log "Starting StarMade Server on port $PORT..."

# Launch the launcher JAR in server mode
"$JAVA_PATH" -XstartOnFirstThread -jar StarMade-Launcher.jar -server -port: $PORT

log "Server has stopped."
echo "Press enter to exit..."
read
exit 0