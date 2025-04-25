#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <direct.h>

#define MAX_PATH_LENGTH 1024
#define JAVA_RELATIVE_PATH_8 "\\jre8\\bin\\java.exe"
#define JAVA_RELATIVE_PATH_23 "\\jre23\\bin\\java.exe"
#define JAR_RELATIVE_PATH "\\StarMade-Launcher.jar"

// Function to check if a file exists
int fileExists(const char* path) {
    DWORD attributes = GetFileAttributesA(path);
    return (attributes != INVALID_FILE_ATTRIBUTES &&
            !(attributes & FILE_ATTRIBUTE_DIRECTORY));
}

// Function to check if a directory exists
int dirExists(const char* path) {
    DWORD attributes = GetFileAttributesA(path);
    return (attributes != INVALID_FILE_ATTRIBUTES &&
            (attributes & FILE_ATTRIBUTE_DIRECTORY));
}

// Function to get the executable directory
void getExecutableDir(char* buffer, int bufferSize) {
    GetModuleFileNameA(NULL, buffer, bufferSize);

    // Remove the executable name to get the directory
    char* lastSlash = strrchr(buffer, '\\');
    if (lastSlash != NULL) {
        *lastSlash = '\0';
    }
}

// Function to create the command line for Java
void createCommandLine(char* cmdLine, int bufferSize, const char* javaPath, const char* jarPath, int argc, char* argv[]) {
    // Start with the Java path in quotes (in case it contains spaces)
    snprintf(cmdLine, bufferSize, "\"%s\" -jar \"%s\"", javaPath, jarPath);

    // Add any additional arguments passed to the launcher
    size_t currentLength = strlen(cmdLine);

    for (int i = 1; i < argc; i++) {
        // Check if we have space to add this argument
        if (currentLength + strlen(argv[i]) + 3 < bufferSize) { // +3 for space and quotes
            strcat(cmdLine, " ");
            strcat(cmdLine, argv[i]);
            currentLength = strlen(cmdLine);
        }
    }
}

int main(int argc, char* argv[]) {
    char exeDir[MAX_PATH_LENGTH] = {0};
    char workingDir[MAX_PATH_LENGTH] = {0};
    char javaPath8[MAX_PATH_LENGTH] = {0};
    char javaPath23[MAX_PATH_LENGTH] = {0};
    char jarPath[MAX_PATH_LENGTH] = {0};
    char commandLine[MAX_PATH_LENGTH * 2] = {0};

    // Get the directory where the executable is located
    getExecutableDir(exeDir, MAX_PATH_LENGTH);

    // Set up paths
    strcpy(workingDir, exeDir);

    // Construct Java paths
    strcpy(javaPath8, exeDir);
    strcat(javaPath8, JAVA_RELATIVE_PATH_8);

    strcpy(javaPath23, exeDir);
    strcat(javaPath23, JAVA_RELATIVE_PATH_23);

    // Construct JAR path
    strcpy(jarPath, exeDir);
    strcat(jarPath, JAR_RELATIVE_PATH);

    // Check if JAR exists
    if (!fileExists(jarPath)) {
        MessageBoxA(NULL,
                   "Could not find the StarMade Launcher JAR file.\n"
                   "Please make sure you have extracted all files correctly.",
                   "StarMade Launcher Error",
                   MB_ICONERROR | MB_OK);
        return 1;
    }

    // Check if either Java runtime exists
    char* javaToUse = NULL;
    if (fileExists(javaPath23)) {
        javaToUse = javaPath23;
    } else if (fileExists(javaPath8)) {
        javaToUse = javaPath8;
    } else {
        MessageBoxA(NULL,
                   "Could not find Java runtime.\n"
                   "Please make sure both Java 8 and Java 23 runtimes are installed in the correct location.",
                   "StarMade Launcher Error",
                   MB_ICONERROR | MB_OK);
        return 1;
    }

    // Create command line
    createCommandLine(commandLine, MAX_PATH_LENGTH * 2, javaToUse, jarPath, argc, argv);

    // Start the launcher
    STARTUPINFOA si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    // Create process
    if (!CreateProcessA(
            NULL,               // No module name (use command line)
            commandLine,        // Command line
            NULL,               // Process handle not inheritable
            NULL,               // Thread handle not inheritable
            FALSE,              // Set handle inheritance to FALSE
            0,                  // No creation flags
            NULL,               // Use parent's environment block
            workingDir,         // Use parent's starting directory
            &si,                // Pointer to STARTUPINFO structure
            &pi)                // Pointer to PROCESS_INFORMATION structure
    ) {
        char errorMsg[MAX_PATH_LENGTH + 100];
        snprintf(errorMsg, sizeof(errorMsg),
                "Failed to start StarMade Launcher.\nCommand line: %s\nError code: %lu",
                commandLine, GetLastError());

        MessageBoxA(NULL, errorMsg, "StarMade Launcher Error", MB_ICONERROR | MB_OK);
        return 1;
    }

    // Wait until child process exits
    WaitForSingleObject(pi.hProcess, INFINITE);

    // Close process and thread handles
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return 0;
}