# Heartbeat Implementation - Connection Timeout Fix

## Overview
This document describes the implementation of a heartbeat mechanism to fix frequent disconnections due to connection timeouts.

## Problem
Connection was being lost, especially during waiting periods, due to the 30-second socket timeout.

## Solution

### 1. Increased Socket Timeout (Connection.kt)
- **Previous timeout**: 30 seconds (30000ms)
- **New timeout**: 120 seconds (120000ms)
- **Implementation**: Made timeout configurable via constructor parameter with default value
- **Java compatibility**: Added `@JvmOverloads` annotation for Java interoperability

**File**: `src/main/java/uno_server/common/Connection.kt`

```kotlin
class Connection @JvmOverloads constructor(
    private val socket: Socket?,
    private val socketTimeoutMs: Int = 120000 // 120 seconds default
) : Closeable
```

### 2. Heartbeat Mechanism (NetworkClient.kt)
Added automatic PING/PONG heartbeat to keep connection alive:

**File**: `src/main/java/uno_ui/NetworkClient.kt`

#### Key Features:
- **PING interval**: 30 seconds
- **PONG timeout**: 10 seconds
- **Automatic reconnection**: If no PONG received within 10 seconds after PING

#### Implementation Details:

**New fields:**
```kotlin
private var heartbeatThread: Thread? = null
private var lastPongTime = AtomicLong(System.currentTimeMillis())
private var lastPingSentTime = AtomicLong(0L)

companion object {
    private const val HEARTBEAT_INTERVAL_MS = 30000L // 30 seconds
    private const val PONG_TIMEOUT_MS = 10000L // 10 seconds
}
```

**Heartbeat thread:**
- Sends PING every 30 seconds
- Waits 10 seconds for PONG response
- If no PONG received, triggers automatic reconnection
- Runs as daemon thread alongside sender and receiver threads

**PONG handling:**
- Receiver thread automatically detects PONG messages
- Updates `lastPongTime` timestamp
- Still forwards PONG to message listener for app-level handling if needed

**Reconnection logic:**
```kotlin
private fun reconnect() {
    println("[NetworkClient] Attempting to reconnect...")
    disconnect()
    Thread.sleep(1000) // Wait a bit before reconnecting
    connect()
}
```

### 3. Protocol Support
The PING/PONG protocol was already implemented on the server side:
- **File**: `src/main/java/uno_server/protocol/PingHandler.kt`
- **Method enum**: `Method.PING` and `Method.PONG` in `uno_proto.common.Method`
- **Empty payload**: Uses `MessageParser.EmptyPayload` for both PING and PONG

## Testing

### Test Client
A dedicated test client was created to verify heartbeat functionality:
- **File**: `src/main/java/uno_ui/HeartbeatTest.kt`
- **Purpose**: Connects to server and waits, allowing heartbeat to maintain connection

### Test Scripts
Two test scripts are provided:

1. **test_heartbeat_quick.sh** - 2-minute test
2. **test_heartbeat.sh** - 5-minute test (full verification)

### Running Tests

```bash
# Build the project
mvn compile

# Run quick test (2 minutes)
./test_heartbeat_quick.sh

# Run full test (5 minutes)
./test_heartbeat.sh

# Manual test
# Terminal 1: Start server
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" uno_server.ServerLauncher

# Terminal 2: Start test client
java -cp "target/classes:$(cat cp.txt)" uno_ui.HeartbeatTest
```

### Expected Behavior
When running the test client, you should see:
```
[Heartbeat] Thread started
[Heartbeat] Sending PING...
[Sender] Sending: {...PING...}
[Receiver] Received: {...PONG...}
[Heartbeat] PONG received
```

This pattern repeats every 30 seconds, keeping the connection alive indefinitely.

## Verification
✅ Connection timeout increased from 30s to 120s
✅ Heartbeat sends PING every 30 seconds
✅ Server responds with PONG without additional logic
✅ Client tracks PONG responses and reconnects if timeout occurs
✅ Connection remains stable for extended periods (tested up to 5 minutes)

## Backwards Compatibility
- Existing code using `Connection(socket)` continues to work (uses new 120s default)
- Can explicitly specify timeout: `Connection(socket, 60000)` for 60s timeout
- Heartbeat is automatic and transparent to application code
- PONG messages are handled internally but still forwarded to application listeners

## Thread Safety
- Uses `AtomicLong` for timestamp tracking
- Heartbeat thread is daemon thread (won't prevent JVM shutdown)
- Proper cleanup in `disconnect()` method joins all threads
- `@Volatile` flag for `running` state ensures thread visibility

## Performance Impact
- Minimal: One PING message every 30 seconds
- PONG responses are immediate (no additional server logic)
- Heartbeat thread sleeps between checks (no busy waiting)
- No impact on normal message flow

## Future Improvements
Possible enhancements:
- Make heartbeat interval configurable via constructor
- Add callbacks for connection state changes (connected, disconnected, reconnecting)
- Exponential backoff for reconnection attempts
- Maximum reconnection attempt limit
- Heartbeat statistics (latency, missed PONGs, etc.)
