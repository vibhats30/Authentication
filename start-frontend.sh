#!/bin/bash

# Start Frontend Development Server
# This script starts the React + Vite frontend

echo "Starting Authentication Module Frontend..."

cd frontend

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "node_modules not found. Installing dependencies..."
    npm install
fi

echo "Starting frontend on port 3000..."
npm run dev
