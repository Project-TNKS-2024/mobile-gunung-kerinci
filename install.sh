#!/bin/bash

# install.sh - Build, install, and run Gunung Kerinci app
# Detects USB first, falls back to WiFi debugging, then deploys the latest code.

set -e

APP_PACKAGE="com.dicoding.gunungkerinci"
MAIN_ACTIVITY="${APP_PACKAGE}.SplashScreen.SplashScreeanActivity"
BUILD_VARIANT="debug"
WIFI_PORT=5555

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${CYAN}[*]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# --- Step 1: Detect device connection ---
detect_device() {
    print_step "Detecting connected devices..."

    # Check if adb is available
    if ! command -v adb &> /dev/null; then
        print_error "adb not found. Please install Android SDK platform-tools."
        exit 1
    fi

    # Start adb server if not running
    adb start-server &> /dev/null

    # Check for USB-connected devices first
    USB_DEVICES=$(adb devices | grep -w "device" | grep -v "emulator" | head -n 1)

    if [ -n "$USB_DEVICES" ]; then
        DEVICE_SERIAL=$(echo "$USB_DEVICES" | awk '{print $1}')
        print_success "USB device detected: $DEVICE_SERIAL"
        ADB_TARGET="-s $DEVICE_SERIAL"
        return 0
    fi

    print_warning "No USB device found. Attempting WiFi debugging..."
    connect_wifi
}

# --- Step 2: WiFi debugging fallback ---
connect_wifi() {
    # Check if there's already a WiFi-connected device
    WIFI_DEVICES=$(adb devices | grep -w "device" | grep ":" | head -n 1)

    if [ -n "$WIFI_DEVICES" ]; then
        DEVICE_SERIAL=$(echo "$WIFI_DEVICES" | awk '{print $1}')
        print_success "WiFi device already connected: $DEVICE_SERIAL"
        ADB_TARGET="-s $DEVICE_SERIAL"
        return 0
    fi

    # Try to get device IP from a temporarily connected USB device
    print_step "Looking for device IP address..."

    # Check if device was recently connected via USB to set up WiFi
    TEMP_USB=$(adb devices | grep -w "device" | head -n 1)
    if [ -n "$TEMP_USB" ]; then
        TEMP_SERIAL=$(echo "$TEMP_USB" | awk '{print $1}')
        DEVICE_IP=$(adb -s "$TEMP_SERIAL" shell ip route | grep "wlan0" | awk '{print $9}' | head -n 1)

        if [ -n "$DEVICE_IP" ]; then
            print_step "Setting device to TCP/IP mode on port $WIFI_PORT..."
            adb -s "$TEMP_SERIAL" tcpip $WIFI_PORT
            sleep 2

            print_step "Connecting to $DEVICE_IP:$WIFI_PORT..."
            adb connect "$DEVICE_IP:$WIFI_PORT"
            sleep 1

            # Verify connection
            WIFI_CHECK=$(adb devices | grep "$DEVICE_IP:$WIFI_PORT" | grep -w "device")
            if [ -n "$WIFI_CHECK" ]; then
                DEVICE_SERIAL="$DEVICE_IP:$WIFI_PORT"
                print_success "WiFi debugging connected: $DEVICE_SERIAL"
                ADB_TARGET="-s $DEVICE_SERIAL"
                return 0
            fi
        fi
    fi

    # Last resort: ask user for IP
    print_warning "Could not auto-detect device IP."
    echo -n "Enter device IP address (or press Enter to cancel): "
    read -r USER_IP

    if [ -z "$USER_IP" ]; then
        print_error "No device available. Please connect a device via USB or WiFi."
        exit 1
    fi

    print_step "Connecting to $USER_IP:$WIFI_PORT..."
    adb connect "$USER_IP:$WIFI_PORT"
    sleep 1

    WIFI_CHECK=$(adb devices | grep "$USER_IP:$WIFI_PORT" | grep -w "device")
    if [ -n "$WIFI_CHECK" ]; then
        DEVICE_SERIAL="$USER_IP:$WIFI_PORT"
        print_success "WiFi debugging connected: $DEVICE_SERIAL"
        ADB_TARGET="-s $DEVICE_SERIAL"
        return 0
    else
        print_error "Failed to connect to $USER_IP:$WIFI_PORT"
        exit 1
    fi
}

# --- Step 3: Build the project ---
build_project() {
    print_step "Building project (assembleDebug)..."
    ./gradlew assembleDebug --quiet
    if [ $? -eq 0 ]; then
        print_success "Build successful."
    else
        print_error "Build failed."
        exit 1
    fi
}

# --- Step 4: Stop existing app ---
stop_app() {
    print_step "Stopping existing app ($APP_PACKAGE)..."
    adb $ADB_TARGET shell am force-stop "$APP_PACKAGE" 2>/dev/null || true
    print_success "App stopped."
}

# --- Step 5: Install APK ---
install_apk() {
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

    if [ ! -f "$APK_PATH" ]; then
        print_error "APK not found at $APK_PATH"
        exit 1
    fi

    print_step "Installing APK..."
    adb $ADB_TARGET install -r "$APK_PATH"
    if [ $? -eq 0 ]; then
        print_success "APK installed successfully."
    else
        print_error "APK installation failed."
        exit 1
    fi
}

# --- Step 6: Launch the app ---
launch_app() {
    print_step "Launching app..."
    adb $ADB_TARGET shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY"
    if [ $? -eq 0 ]; then
        print_success "App launched successfully!"
    else
        print_warning "Could not launch activity. Trying with LAUNCHER intent..."
        adb $ADB_TARGET shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1
    fi
}

# --- Main execution ---
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  Gunung Kerinci - Install & Run${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

detect_device
echo ""
build_project
echo ""
stop_app
install_apk
echo ""
launch_app

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Done! App is running with latest code.${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
