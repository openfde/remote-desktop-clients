#!/bin/bash

VERSION_NAME=""

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -versionname) VERSION_NAME="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

if [ -z "$VERSION_NAME" ]; then
    echo "Please provide a version name using like \"-versionname v6.0.1\""
    exit 1
fi

echo "Starting Gradle build...........................$VERSION_NAME"

./gradlew :bVNC-app:clean
./gradlew :bVNC-app:assembleRelease  -PVersionName="$VERSION_NAME"

if [ $? -eq 0 ]; then
    echo "Gradle build completed successfully."
else
    echo "Error: Gradle build failed."
    exit 1
fi


COMMIT_HASH=$(git rev-parse HEAD)

UPLOAD_TIME=$(date +"%Y-%m-%d %T")
UPLOAD_URL="YOUR_UPLOAD_URL"

# upload
#curl -F "file=@$APK_FILE" -F "version=$VERSION_NAME" -F "commit=$COMMIT_HASH" -F "upload_time=$UPLOAD_TIME" $UPLOAD_URL

if [ $? -eq 0 ]; then
    echo "APK upload completed successfully."
else
    echo "Error: Failed to upload APK."
    exit 1
fi


INFO_FILE="build_info.txt"
echo "Version: $VERSION_NAME" > $INFO_FILE
echo "Commit: $COMMIT_HASH" >> $INFO_FILE
echo "Upload Time: $UPLOAD_TIME" >> $INFO_FILE

echo "Version: $VERSION_NAME"
echo "Commit: $COMMIT_HASH"
echo "Upload Time: $UPLOAD_TIME"

echo "All tasks completed successfully."

