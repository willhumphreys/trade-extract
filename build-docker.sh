#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    source .env
else
    echo "Error: .env file not found"
    exit 1
fi

# Check if required variables are set
if [ -z "$REPSY_USERNAME" ] || [ -z "$REPSY_PASSWORD" ]; then
    echo "Error: REPSY_USERNAME and REPSY_PASSWORD must be set in .env file"
    exit 1
fi

# Build the Docker image
docker build \
  --build-arg REPSY_USERNAME=$REPSY_USERNAME \
  --build-arg REPSY_PASSWORD=$REPSY_PASSWORD \
  -t trade-extract:local .

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Docker build successful! Image tagged as trade-extract:local"
else
    echo "Docker build failed."
fi