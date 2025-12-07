# UNO UI Screens Structure

## Overview
The UI has been split into multiple screens for better organization and maintainability.

## Screen Flow
```
LoginScreen → RoomSelectScreen → CreateRoomScreen / JoinRoomScreen → LobbyScreen → GameScreen
                    ↑                         ↓                             ↓
                    └─────────────────────────┴─────────────────────────────┘
```

## Screens

### 1. MainApp (99 lines)
- **Purpose**: Entry point and screen manager
- **Responsibilities**: 
  - Initialize all screens
  - Switch between screens via `scene.root`
  - Handle state changes from GameController
  - Manage screen transitions based on game state

### 2. LoginScreen (85 lines)
- **Purpose**: Player name input
- **UI Elements**:
  - TextField for player name
  - Login button
  - Status label for errors
- **Flow**: After successful connection → RoomSelectScreen

### 3. RoomSelectScreen (57 lines)
- **Purpose**: Choose to create or join a room
- **UI Elements**:
  - Welcome message with player name
  - "Create New Room" button
  - "Join Existing Room" button
- **Flow**: 
  - Create → CreateRoomScreen
  - Join → JoinRoomScreen

### 4. CreateRoomScreen (95 lines)
- **Purpose**: Configure new room settings
- **UI Elements**:
  - Room name TextField
  - Password PasswordField (optional)
  - Max players Spinner (2-10)
  - Create/Back buttons
- **Flow**: After creation → LobbyScreen (automatic via GameController)

### 5. JoinRoomScreen (86 lines)
- **Purpose**: Join existing room by ID
- **UI Elements**:
  - Room ID TextField
  - Join/Back buttons
  - Status label for errors
- **Flow**: After joining → LobbyScreen (automatic via GameController)

### 6. LobbyScreen (98 lines)
- **Purpose**: Wait for players and start game
- **UI Elements**:
  - Room info label
  - Players ListView with ready status
  - Start Game button
  - Leave Room button
- **Flow**: When game starts → GameScreen (automatic via GameController)

### 7. GameScreen (104 lines)
- **Purpose**: Main game interface
- **UI Elements**:
  - Status label (current player, phase)
  - Current card display
  - Player hand (cards pane)
  - Action buttons (Play Card, Draw Card, Say UNO!)
  - Chat area with input
- **Flow**: Game continues until finished

## Technical Details

### Screen Base Class
All screens extend `BorderPane` or `Pane` and implement:
- `init()` - Initialize UI components
- `show()` - Called when screen becomes visible
- `hide()` - Called when screen is hidden (optional cleanup)

### Screen Switching
```kotlin
private fun switchScreen(newScreen: Pane) {
    currentScreen = newScreen
    scene.root = newScreen
}
```

### State Management
- GameController handles all networking and game state
- MainApp listens to state changes and switches screens automatically
- Screens are created once during initialization
- RoomSelectScreen is recreated each time to update player name

### Size Constraints
- All screens designed for 900x700 window
- Each screen is approximately 100 lines or less
- Minimal code, maximum clarity

## Running the Application

```bash
# Compile
mvn clean compile

# Run UI
mvn javafx:run

# Or with exec plugin
mvn exec:java -Dexec.mainClass="uno_ui.MainApp"
```

## Code Conventions
- Kotlin for all screens
- Minimal comments (self-documenting code)
- Lambda callbacks for navigation
- Platform.runLater for UI updates from controller
- Concise, idiomatic Kotlin
