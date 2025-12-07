#!/bin/bash

# Test script for simple chat functionality

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=== Chat Functionality Test ==="
echo ""

# Build classpath
echo "Building classpath..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q > /dev/null 2>&1

if [ ! -f cp.txt ]; then
    echo -e "${RED}Failed to build classpath${NC}"
    exit 1
fi

# Start the server in background
echo "Starting server..."
java -cp "target/classes:$(cat cp.txt)" uno_server.ServerLauncher > server_test.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 2

# Check if server is running
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo -e "${RED}Server failed to start${NC}"
    cat server_test.log
    exit 1
fi

echo -e "${GREEN}Server started with PID $SERVER_PID${NC}"

# Function to send and log chat messages
send_chat() {
    local player_name=$1
    local message=$2
    echo "CHAT_MESSAGE|$player_name|$message"
}

# Test sending chat messages via netcat
echo ""
echo "Test 1: Sending chat message from Player1..."
{
    sleep 1
    # First create a room
    echo '{"messageId":1,"version":"V1","method":"CREATE_ROOM","payload":{"roomName":"TestRoom","password":null,"maxPlayers":4,"allowStack":false},"timestamp":1234567890}'
    sleep 1
    # Send a chat message
    send_chat "Player1" "Hello from Player1!"
    sleep 1
} | nc localhost 9090 > client1.log 2>&1 &
CLIENT1_PID=$!

sleep 3

echo ""
echo "Test 2: Sending chat message from Player2..."
{
    sleep 1
    # Join the room
    echo '{"messageId":1,"version":"V1","method":"JOIN_ROOM","payload":{"roomId":1,"password":null},"timestamp":1234567890}'
    sleep 1
    # Send a chat message
    send_chat "Player2" "Hi from Player2!"
    sleep 1
} | nc localhost 9090 > client2.log 2>&1 &
CLIENT2_PID=$!

# Wait for clients to finish
wait $CLIENT1_PID
wait $CLIENT2_PID

echo ""
echo "=== Server Log ==="
cat server_test.log | grep -i chat || echo "No chat messages in server log"

echo ""
echo "=== Client 1 Log ==="
cat client1.log | grep "CHAT_MESSAGE" || echo "No chat messages received by Client 1"

echo ""
echo "=== Client 2 Log ==="
cat client2.log | grep "CHAT_MESSAGE" || echo "No chat messages received by Client 2"

# Check if CHAT_MESSAGE appears in logs
if grep -q "CHAT_MESSAGE" server_test.log; then
    echo -e "\n${GREEN}✓ Chat messages detected in server log${NC}"
else
    echo -e "\n${RED}✗ No chat messages in server log${NC}"
fi

# Cleanup
echo ""
echo "Cleaning up..."
kill $SERVER_PID 2>/dev/null
rm -f cp.txt server_test.log client1.log client2.log

echo -e "${GREEN}Test completed!${NC}"
