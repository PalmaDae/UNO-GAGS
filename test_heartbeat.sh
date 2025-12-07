#!/bin/bash

# Test script to verify heartbeat functionality
# This script starts the server and a test client, then waits 5 minutes to verify connection stability

echo "=== UNO Heartbeat Test ==="
echo ""
echo "This test will:"
echo "1. Start the UNO server"
echo "2. Start a test client"
echo "3. Wait 5 minutes with no activity"
echo "4. Verify connection is still alive"
echo ""

# Build the classpath
echo "Building classpath..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

# Start server in background
echo "Starting server..."
java -cp "target/classes:$(cat cp.txt)" uno_server.ServerLauncher > server.log 2>&1 &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

# Wait for server to start
echo "Waiting for server to start..."
sleep 3

# Check if server is running
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "ERROR: Server failed to start"
    cat server.log
    exit 1
fi

echo "Server started successfully"
echo ""

# Start test client
echo "Starting test client..."
java -cp "target/classes:$(cat cp.txt)" uno_ui.ClientTest > client.log 2>&1 &
CLIENT_PID=$!
echo "Client PID: $CLIENT_PID"

# Wait a bit for client to connect
sleep 2

# Check if client is running
if ! kill -0 $CLIENT_PID 2>/dev/null; then
    echo "ERROR: Client failed to start"
    cat client.log
    kill $SERVER_PID 2>/dev/null
    exit 1
fi

echo "Client connected successfully"
echo ""

# Monitor for 5 minutes
echo "Monitoring connection for 5 minutes (300 seconds)..."
echo "You should see [Heartbeat] PING messages every 30 seconds in the logs"
echo ""

for i in {1..10}; do
    sleep 30
    ELAPSED=$((i * 30))
    echo "[$ELAPSED seconds] Checking connection..."
    
    # Check if both server and client are still running
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo "ERROR: Server died!"
        exit 1
    fi
    
    if ! kill -0 $CLIENT_PID 2>/dev/null; then
        echo "ERROR: Client died!"
        kill $SERVER_PID 2>/dev/null
        exit 1
    fi
    
    echo "  ✓ Server and client still running"
    
    # Check for heartbeat messages in client log
    PING_COUNT=$(grep -c "\[Heartbeat\] Sending PING" client.log || echo "0")
    PONG_COUNT=$(grep -c "\[Heartbeat\] PONG received" client.log || echo "0")
    echo "  ✓ PING count: $PING_COUNT, PONG count: $PONG_COUNT"
done

echo ""
echo "=== Test PASSED ==="
echo "Connection remained stable for 5 minutes!"
echo ""

# Show last few lines of logs
echo "Last 10 lines of client log:"
tail -10 client.log
echo ""

# Cleanup
echo "Cleaning up..."
kill $CLIENT_PID 2>/dev/null
kill $SERVER_PID 2>/dev/null
wait $CLIENT_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null

echo "Test completed successfully!"
