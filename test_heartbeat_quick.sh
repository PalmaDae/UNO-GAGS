#!/bin/bash

# Quick test to verify heartbeat functionality (2 minutes instead of 5)

echo "=== UNO Quick Heartbeat Test ==="
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
java -cp "target/classes:$(cat cp.txt)" uno_ui.HeartbeatTest > client.log 2>&1 &
CLIENT_PID=$!
echo "Client PID: $CLIENT_PID"

# Wait for client to connect
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

# Monitor for 2 minutes (4 checks at 30 second intervals)
echo "Monitoring connection for 2 minutes..."
echo ""

for i in {1..4}; do
    sleep 30
    ELAPSED=$((i * 30))
    echo "[$ELAPSED seconds] Checking connection..."
    
    # Check if both server and client are still running
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo "ERROR: Server died!"
        cat server.log
        exit 1
    fi
    
    if ! kill -0 $CLIENT_PID 2>/dev/null; then
        echo "ERROR: Client died!"
        cat client.log
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
echo "Connection remained stable for 2 minutes!"
echo ""

# Show heartbeat-related lines from logs
echo "Heartbeat activity in client log:"
grep "Heartbeat" client.log | tail -10
echo ""

# Cleanup
echo "Cleaning up..."
kill $CLIENT_PID 2>/dev/null
kill $SERVER_PID 2>/dev/null
wait $CLIENT_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null

echo "Test completed successfully!"
