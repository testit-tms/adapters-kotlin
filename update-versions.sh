#!/bin/bash

# Get versions from versions.yml
VERSION=$(grep "version:" versions.yml | awk '{print $2}')
API_VERSION=$(grep "api:" versions.yml | awk '{print $2}')

# Update version and apiVersion in testit-kotlin-commons/build.gradle.kts
sed -i "s/version = \".*\"/version = \"$VERSION\"/" testit-kotlin-commons/build.gradle.kts
sed -i "s/val apiVersion = \".*\"/val apiVersion = \"$API_VERSION\"/" testit-kotlin-commons/build.gradle.kts

# Update version in testit-adapter-kotest/build.gradle.kts
sed -i "s/version = \".*\"/version = \"$VERSION\"/" testit-adapter-kotest/build.gradle.kts

echo "Commons and kotest version updated to $VERSION"
echo "Commons apiVersion updated to $API_VERSION"

echo "Building..."
./gradlew clean build --info
BUILD_RESULT=$?

if [ $BUILD_RESULT -eq 0 ]; then
    echo "Build successful, running tests..."
    ./gradlew test --info
    TEST_RESULT=$?
    
    if [ $TEST_RESULT -eq 0 ]; then
        echo "All tests passed successfully!"
    else
        echo "Tests failed!"
        exit 1
    fi
else
    echo "Build failed!"
    exit 1
fi 