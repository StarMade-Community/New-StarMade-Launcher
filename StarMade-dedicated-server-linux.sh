#!/bin/bash
cd "$(dirname "$0")"
if [ ! -d "./StarMade" ]; then
    echo "Please run the StarMade launcher at least once before starting a server."
    echo "The launcher will create the necessary folders and files for the server to run."
    exit 1
fi
java -jar ./lib/StarMade-Launcher.jar -server -port: 4242