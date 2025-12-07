#!/bin/bash

# Script to test the chat functionality

echo "=== Chat Functionality Test ==="
echo ""

# Build classpath
echo "Building classpath..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

if [ ! -f cp.txt ]; then
    echo "Failed to build classpath"
    exit 1
fi

# Compile
echo "Compiling..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "Compilation failed"
    exit 1
fi

# Start server in background
echo "Starting server..."
java -cp "target/classes:$(cat cp.txt)" uno_server.ServerLauncher > server_chat_test.log 2>&1 &
SERVER_PID=$!

# Give server time to start
sleep 2

# Check if server is running
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "Server failed to start!"
    cat server_chat_test.log
    exit 1
fi

echo "Server started (PID: $SERVER_PID)"
echo ""

# Run the chat test
echo "Running chat test..."
echo ""
java -cp "target/classes:$(cat cp.txt)" uno_ui.ChatTestKt

TEST_RESULT=$?

# Wait a bit to see final messages
sleep 1

# Show server log
echo ""
echo "=== Server Log (Chat Messages Only) ==="
grep -i "chat\|CHAT_MESSAGE" server_chat_test.log || echo "(No chat messages in server log)"

# Kill server
echo ""
echo "Stopping server..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null

# Cleanup
rm -f cp.txt

echo ""
if [ $TEST_RESULT -eq 0 ]; then
    echo "✓ Test completed successfully!"
else
    echo "✗ Test failed!"
fi

exit $TEST_RESULT
