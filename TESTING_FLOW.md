# Testing the Complete UI Flow

## Prerequisites
1. Server must be running on localhost:9090
2. JavaFX runtime properly configured (already done)

## Starting the Server

In one terminal:
```bash
cd /home/engine/project
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" uno_server.ServerLauncher
```

## Starting the UI Client

In another terminal:
```bash
cd /home/engine/project
mvn javafx:run
```

## Complete Flow Test

### 1. Login Screen
- **What you see**: Title "UNO Game", name input field, "Start Game" button
- **Action**: Enter a player name (e.g., "Player1")
- **Expected**: Connects to server and moves to Room Select Screen

### 2. Room Select Screen
- **What you see**: Welcome message, "Create New Room" and "Join Existing Room" buttons
- **Action Option A**: Click "Create New Room"
- **Action Option B**: Click "Join Existing Room"

### 3A. Create Room Screen (if you chose Create)
- **What you see**: Form with Room Name, Password (optional), Max Players
- **Action**: 
  - Enter room name (e.g., "Test Room")
  - Set max players (e.g., 4)
  - Click "Create"
- **Expected**: Room created, automatically moves to Lobby Screen

### 3B. Join Room Screen (if you chose Join)
- **What you see**: Room ID input field
- **Action**: 
  - Enter a valid room ID (get from another player or server logs)
  - Click "Join"
- **Expected**: Joins room, automatically moves to Lobby Screen

### 4. Lobby Screen
- **What you see**: 
  - Room title with ID
  - List of players with [READY] status
  - "Start Game" button (enabled when 2+ players)
  - "Leave Room" button
- **Action**: 
  - Wait for at least 2 players to join
  - Click "Start Game"
- **Expected**: Game starts, automatically moves to Game Screen

### 5. Game Screen
- **What you see**:
  - Status label showing current player and phase
  - Current card on table
  - Your hand (empty initially, will show cards when dealt)
  - Action buttons: Play Card, Draw Card, Say UNO!
  - Chat area at bottom
- **Action**: 
  - Play game normally
  - Use chat to communicate
  - Click buttons to interact
- **Expected**: Full game functionality

## Testing Multiple Players

1. Start server once
2. Run `mvn javafx:run` in multiple terminals
3. Each client will open a separate window
4. Create room in first client
5. Join with room ID in other clients
6. Start game when all ready

## Navigation Back

From any screen you can:
- **Create/Join/Lobby screens**: Use "Back" or "Leave Room" to return to Room Select
- **Room Select**: Will need to restart app to change player name (or add logout feature)

## Error Cases to Test

1. **Connection Failure**: Try starting UI without server running
   - Should show "Failed to connect to server" message
   
2. **Invalid Room ID**: Enter non-existent room ID
   - Should show error (if implemented)
   
3. **Empty Name**: Try to login with empty name
   - Should show "Please enter a name"

4. **Short Name**: Try name with 1 character
   - Should show "Name must be at least 2 characters"

## Screen Transitions Summary

```
LoginScreen (manual: click "Start Game")
    ↓
RoomSelectScreen (manual: click button)
    ↓
CreateRoomScreen/JoinRoomScreen (manual: click "Create"/"Join")
    ↓
LobbyScreen (auto: after room created/joined)
    ↓
GameScreen (auto: after game started)
```

## What Gets Tested

✅ Screen switching works correctly
✅ Data flows between screens
✅ GameController state changes trigger screen transitions
✅ UI updates based on server responses
✅ Complete user journey from login to game
✅ Navigation back works (Leave Room, Back buttons)
✅ Error handling for invalid inputs

## Known Limitations

- No "logout" button on Room Select (need to restart app)
- GameScreen doesn't show actual cards yet (placeholder)
- No visual feedback for network delays
- Chat only works in Game Screen
