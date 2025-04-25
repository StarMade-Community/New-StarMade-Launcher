/**
 * StarMade Launcher - Native Component
 *
 * This is a unified C launcher for StarMade that works on Windows, MacOS, and Linux.
 * It detects the current platform, finds the appropriate Java runtime, and launches
 * the StarMade Launcher JAR.
 *
 * Compilation:
 *
 * Windows (MinGW):
 *   gcc -o StarMade-Launcher.exe starmade_launcher.c -mwindows -lshlwapi
 *
 * macOS:
 *   gcc -o StarMade-Launcher starmade_launcher.c -framework CoreFoundation
 *
 * Linux:
 *   gcc -o StarMade-Launcher starmade_launcher.c
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Platform-specific includes
#ifdef _WIN32
    #include <windows.h>
    #include <shlwapi.h>
    #pragma comment(lib, "shlwapi.lib")
    #define PATH_SEPARATOR '\\'
    #define PATH_MAX 260
#elif defined(__APPLE__)
    #include <CoreFoundation/CoreFoundation.h>
    #include <unistd.h>
    #include <sys/stat.h>
    #include <libgen.h>
    #define PATH_SEPARATOR '/'
#else
    #include <unistd.h>
    #include <sys/stat.h>
    #include <libgen.h>
    #include <limits.h>  // Added for PATH_MAX on Linux
    #define PATH_SEPARATOR '/'
    // Define PATH_MAX if it's not defined (some Linux systems)
    #ifndef PATH_MAX
        #define PATH_MAX 4096
    #endif
#endif

// Define constants
#define MAX_CMD_LENGTH 4096
#define MAX_PATH_LENGTH 1024
#define LAUNCHER_JAR "StarMade-Launcher.jar"
#define JAVA8_DIR "jre8"
#define JAVA23_DIR "jre23"

// Java executable paths for each platform
#ifdef _WIN32
    #define JAVA8_RELATIVE_PATH "\\jre8\\bin\\java.exe"
    #define JAVA23_RELATIVE_PATH "\\jre23\\bin\\java.exe"
#elif defined(__APPLE__)
    #define JAVA8_RELATIVE_PATH "/jre8/Contents/Home/bin/java"
    #define JAVA23_RELATIVE_PATH "/jre23/Contents/Home/bin/java"
#else
    #define JAVA8_RELATIVE_PATH "/jre8/bin/java"
    #define JAVA23_RELATIVE_PATH "/jre23/bin/java"
#endif

// Function declarations
int file_exists(const char *path);
void get_executable_path(char *buffer, size_t buffer_size);
int build_command_line(char *cmd_line, size_t cmd_line_size, const char *java_path,
                      const char *jar_path, int argc, char *argv[]);
int launch_process(const char *cmd_line, const char *working_dir);
void show_error_message(const char *message);

int main(int argc, char *argv[]) {
    char exe_path[MAX_PATH_LENGTH] = {0};
    char launcher_jar[MAX_PATH_LENGTH] = {0};
    char java8_path[MAX_PATH_LENGTH] = {0};
    char java23_path[MAX_PATH_LENGTH] = {0};
    char cmd_line[MAX_CMD_LENGTH] = {0};
    char *java_to_use = NULL;

    // Get the executable directory
    get_executable_path(exe_path, sizeof(exe_path));

    // Resolve paths
    #ifdef _WIN32
        snprintf(launcher_jar, sizeof(launcher_jar), "%s\\%s", exe_path, LAUNCHER_JAR);
        snprintf(java8_path, sizeof(java8_path), "%s%s", exe_path, JAVA8_RELATIVE_PATH);
        snprintf(java23_path, sizeof(java23_path), "%s%s", exe_path, JAVA23_RELATIVE_PATH);
    #else
        snprintf(launcher_jar, sizeof(launcher_jar), "%s/%s", exe_path, LAUNCHER_JAR);
        snprintf(java8_path, sizeof(java8_path), "%s%s", exe_path, JAVA8_RELATIVE_PATH);
        snprintf(java23_path, sizeof(java23_path), "%s%s", exe_path, JAVA23_RELATIVE_PATH);
    #endif

    // Check if JAR exists
    if (!file_exists(launcher_jar)) {
        char error_msg[MAX_PATH_LENGTH * 2];
        snprintf(error_msg, sizeof(error_msg),
                "Could not find the StarMade Launcher JAR file at:\n%s\n"
                "Please make sure you have extracted all files correctly.",
                launcher_jar);
        show_error_message(error_msg);
        return 1;
    }

    // Check Java runtimes - prefer Java 23
    if (file_exists(java23_path)) {
        java_to_use = java23_path;
    } else if (file_exists(java8_path)) {
        java_to_use = java8_path;
    } else {
        show_error_message(
            "Could not find Java runtime.\n"
            "Please make sure both Java 8 and Java 23 runtimes are installed in the correct location."
        );
        return 1;
    }

    // Make Java executable on Unix-like systems
    #ifndef _WIN32
        if (chmod(java_to_use, 0755) != 0) {
            printf("Warning: Failed to set Java executable permissions\n");
        }
    #endif

    // Build command line
    if (!build_command_line(cmd_line, sizeof(cmd_line), java_to_use, launcher_jar, argc, argv)) {
        show_error_message("Failed to build command line");
        return 1;
    }

    // Launch process
    if (!launch_process(cmd_line, exe_path)) {
        char error_msg[MAX_CMD_LENGTH + 100];
        snprintf(error_msg, sizeof(error_msg),
                "Failed to start StarMade Launcher.\nCommand line: %s", cmd_line);
        show_error_message(error_msg);
        return 1;
    }

    return 0;
}

// Function to check if a file exists
int file_exists(const char *path) {
    #ifdef _WIN32
        return GetFileAttributesA(path) != INVALID_FILE_ATTRIBUTES;
    #else
        struct stat buffer;
        return (stat(path, &buffer) == 0);
    #endif
}

// Function to get the executable path
void get_executable_path(char *buffer, size_t buffer_size) {
    #ifdef _WIN32
        GetModuleFileNameA(NULL, buffer, (DWORD)buffer_size);
        PathRemoveFileSpecA(buffer);
    #elif defined(__APPLE__)
        CFBundleRef mainBundle = CFBundleGetMainBundle();
        if (mainBundle) {
            CFURLRef bundleURL = CFBundleCopyBundleURL(mainBundle);
            if (bundleURL) {
                CFURLRef resourcesURL = CFBundleCopyResourcesDirectoryURL(mainBundle);
                if (resourcesURL) {
                    if (CFURLGetFileSystemRepresentation(resourcesURL, true, (UInt8 *)buffer, buffer_size)) {
                        // Path now points to Resources/ directory
                    } else {
                        // Fallback to executable path
                        char temp[PATH_MAX];
                        uint32_t size = sizeof(temp);
                        _NSGetExecutablePath(temp, &size);
                        char *dir = dirname(temp);
                        strncpy(buffer, dir, buffer_size);
                    }
                    CFRelease(resourcesURL);
                }
                CFRelease(bundleURL);
            }
        } else {
            // Not running from a bundle, use executable path
            char temp[PATH_MAX];
            uint32_t size = sizeof(temp);
            _NSGetExecutablePath(temp, &size);
            char *dir = dirname(temp);
            strncpy(buffer, dir, buffer_size);
        }
    #else
        // Linux implementation
        char temp[PATH_MAX];
        ssize_t count = readlink("/proc/self/exe", temp, PATH_MAX);
        if (count != -1) {
            temp[count] = '\0';
            char *dir = dirname(temp);
            strncpy(buffer, dir, buffer_size);
        } else {
            // Fallback
            getcwd(buffer, buffer_size);
        }
    #endif
}

// Function to build the command line
int build_command_line(char *cmd_line, size_t cmd_line_size, const char *java_path,
                      const char *jar_path, int argc, char *argv[]) {
    size_t offset = 0;

    #ifdef _WIN32
        // On Windows, we need to quote paths with spaces
        offset += snprintf(cmd_line + offset, cmd_line_size - offset, "\"%s\" ", java_path);
    #else
        offset += snprintf(cmd_line + offset, cmd_line_size - offset, "%s ", java_path);
    #endif

    // Add platform-specific Java args
    #ifdef __APPLE__
        offset += snprintf(cmd_line + offset, cmd_line_size - offset, "-XstartOnFirstThread ");
    #endif

    // Check if it's Java 23 (simple check for "23" in the path)
    if (strstr(java_path, "23") != NULL) {
        offset += snprintf(cmd_line + offset, cmd_line_size - offset,
                         "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED ");
    }

    // Add JAR argument
    #ifdef _WIN32
        offset += snprintf(cmd_line + offset, cmd_line_size - offset, "-jar \"%s\" ", jar_path);
    #else
        offset += snprintf(cmd_line + offset, cmd_line_size - offset, "-jar %s ", jar_path);
    #endif

    // Add any additional arguments passed to the launcher
    for (int i = 1; i < argc; i++) {
        offset += snprintf(cmd_line + offset, cmd_line_size - offset, "%s ", argv[i]);
    }

    return (offset > 0 && offset < cmd_line_size);
}

// Function to launch the process
int launch_process(const char *cmd_line, const char *working_dir) {
    #ifdef _WIN32
        STARTUPINFOA si;
        PROCESS_INFORMATION pi;

        ZeroMemory(&si, sizeof(si));
        si.cb = sizeof(si);
        ZeroMemory(&pi, sizeof(pi));

        // Create process
        if (!CreateProcessA(
                NULL,               // No module name (use command line)
                (LPSTR)cmd_line,    // Command line
                NULL,               // Process handle not inheritable
                NULL,               // Thread handle not inheritable
                FALSE,              // Set handle inheritance to FALSE
                0,                  // No creation flags
                NULL,               // Use parent's environment block
                working_dir,        // Working directory
                &si,                // Pointer to STARTUPINFO structure
                &pi)                // Pointer to PROCESS_INFORMATION structure
        ) {
            return 0;
        }

        // Close process and thread handles
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);

        return 1;
    #else
        // Unix implementation - using system()
        // Not ideal but simpler than implementing fork/exec
        int result = system(cmd_line);
        return (result != -1);
    #endif
}

// Function to show an error message
void show_error_message(const char *message) {
    #ifdef _WIN32
        MessageBoxA(NULL, message, "StarMade Launcher Error", MB_ICONERROR | MB_OK);
    #elif defined(__APPLE__)
        // Create an AppleScript command to display a dialog
        char cmd[MAX_CMD_LENGTH + 100];
        snprintf(cmd, sizeof(cmd),
                "osascript -e 'display dialog \"%s\" buttons {\"OK\"} "
                "default button \"OK\" with icon stop with title \"StarMade Launcher Error\"'",
                message);
        system(cmd);
    #else
        // Try graphical tools first, fallback to console
        char cmd[MAX_CMD_LENGTH + 100];

        // Try zenity first
        snprintf(cmd, sizeof(cmd), "zenity --error --title=\"StarMade Launcher Error\" --text=\"%s\" 2>/dev/null", message);
        if (system(cmd) != 0) {
            // Try xmessage
            snprintf(cmd, sizeof(cmd), "xmessage -center \"ERROR: %s\" 2>/dev/null", message);
            if (system(cmd) != 0) {
                // Fallback to console
                fprintf(stderr, "ERROR: %s\n", message);
            }
        }
    #endif
}