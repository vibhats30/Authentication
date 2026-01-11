#!/bin/bash

# Start Backend with Environment Variables
# This script loads environment variables from .env file and starts the Spring Boot backend

echo "Starting Authentication Module Backend..."

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    echo "Please create a .env file with required environment variables."
    exit 1
fi

# Switch to Java 21
if [ -f ./use-java-21.sh ]; then
    source ./use-java-21.sh
fi

# Load environment variables from .env file
set -a
source .env
set +a

echo "Environment variables loaded from .env"
echo "Starting backend on port 8080..."

# Start Spring Boot application
mvn spring-boot:run
