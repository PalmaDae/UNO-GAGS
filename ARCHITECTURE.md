# UNO Game Architecture (After Refactoring)

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         UNO Game System                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐              ┌──────────────┐                │
│  │   Client 1   │              │   Client 2   │                │
│  │  (uno_ui)    │              │  (uno_ui)    │                │
│  └──────┬───────┘              └──────┬───────┘                │
│         │         TCP Socket          │                         │
│         └─────────────┬───────────────┘                         │
│                       │                                          │
│                       ▼                                          │
│              ┌────────────────┐                                 │
│              │  Server        │                                 │
│              │  (uno_server)  │                                 │
│              └────────────────┘                                 │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Server Architecture (uno_server.protocol)

### Message Flow

```
Connection (Socket)
      │
      ▼
┌─────────────────┐
│ MessageRouter   │ ◄─── Entry point for all messages
└────────┬────────┘
         │
         ├─────► ConnectionManager ─────► User ID mapping
         │
         ├─────► RoomManager ────────────► Room operations
         │              │
         │              └──► MessageSender ─────► Broadcast
         │
         ├─────► GameHandler ────────────► Game actions
         │              │
         │              └──► GameSessionManager
         │
         ├─────► ChatHandler ────────────► Chat messages
         │
         └─────► PingHandler ─────────────► Health checks
```

### Class Relationships

```
MessageRouter
├── uses → MessageParser (JSON serialization)
├── uses → ConnectionManager
├── uses → RoomManager
│   ├── uses → ConnectionManager
│   └── uses → MessageSender
├── uses → GameHandler
│   ├── uses → ConnectionManager
│   ├── uses → RoomManager
│   ├── uses → MessageSender
│   └── uses → GameSessionManager
├── uses → ChatHandler
│   ├── uses → ConnectionManager
│   ├── uses → RoomManager
│   └── uses → MessageSender
└── uses → PingHandler
    └── uses → MessageSender
```

## Client Architecture (uno_ui)

### UI Flow

```
MainApp (JavaFX UI)
      │
      ▼
┌──────────────────┐
│ GameController   │ ◄─── Main coordinator
└────────┬─────────┘
         │
         ├─────► NetworkClient ──────────► TCP communication
         │              │
         │              ├──► MessageSerializer (JSON)
         │              └──► ReconnectManager (retry logic)
         │
         ├─────► PlayerModel ────────────► Player state
         │
         ├─────► RoomModel ──────────────► Room/lobby state
         │
         ├─────► GameStateModel ─────────► Game state
         │
         └─────► ChatModel ──────────────► Chat messages
```

### Data Flow

```
Server Message
      │
      ▼
NetworkClient (receive)
      │
      ▼
MessageSerializer (deserialize)
      │
      ▼
GameController.handleMessage()
      │
      ├──► ROOM_CREATED ───────► RoomModel.joinRoom()
      │
      ├──► LOBBY_UPDATE ───────► RoomModel.updateLobby()
      │
      ├──► GAME_STATE ─────────► GameStateModel.updateState()
      │
      ├──► GAME_CHAT ──────────► ChatModel.addMessage()
      │
      └──► (any event) ────────► notifyStateChanged() → UI Update
```

## Key Design Patterns

### 1. **Single Responsibility Principle**
Each class has one clear purpose:
- `RoomManager` - only room operations
- `GameHandler` - only game actions
- `PlayerModel` - only player state

### 2. **Dependency Injection**
Components receive dependencies through constructor:
```kotlin
class GameHandler(
    private val connectionManager: ConnectionManager,
    private val roomManager: RoomManager,
    private val messageSender: MessageSender,
    private val gameManager: GameSessionManager
)
```

### 3. **Observer Pattern**
UI observes controller for changes:
```kotlin
controller.setOnStateChanged { updateUI() }
controller.setOnChatMessage { updateChat() }
```

### 4. **Strategy Pattern**
Different handlers for different message types:
```kotlin
when (message.method) {
    Method.CREATE_ROOM -> roomManager.handle(...)
    Method.PLAY_CARD -> gameHandler.handle(...)
    Method.PING -> pingHandler.handle(...)
}
```

## Threading Model

### Server
```
Main Thread
├── Server.handle() - accepts connections
│
└── Per-Client Threads
    └── MessageRouter.routeMessage()
        └── Handler methods (synchronized via ConcurrentHashMap)
```

### Client
```
JavaFX Application Thread (UI)
├── GameController (runs on FX thread)
│
└── NetworkClient Threads
    ├── Sender Thread (sends messages)
    └── Receiver Thread (receives messages)
        └── Platform.runLater { notifyUI() }
```

## Message Protocol

### Request/Response Flow

```
Client                          Server
  │                               │
  │──── CREATE_ROOM ────────────► │
  │                               ├─ RoomManager.handleCreateRoom()
  │                               ├─ Create room
  │ ◄──── ROOM_CREATED_SUCCESS ──┤
  │                               │
  │──── START_GAME ──────────────► │
  │                               ├─ GameHandler.handleStartGame()
  │                               ├─ Create GameSession
  │ ◄──── GAME_START ────────────┤
  │                               │
  │──── PLAY_CARD ───────────────► │
  │                               ├─ GameHandler.handlePlayCard()
  │                               ├─ Validate & apply
  │ ◄──── GAME_STATE ────────────┤ (broadcast to all)
  │                               │
```

## State Management

### Server State
```
ConnectionManager
├── Map<Connection, UserId>
└── Map<UserId, PlayerConnection>

RoomManager
└── Map<RoomId, Room>
    └── List<PlayerConnection>

GameSessionManager
└── Map<RoomId, GameSession>
    └── List<PlayerState>
```

### Client State
```
PlayerModel
├── playerId: Long?
├── username: String
├── hand: List<Card>
└── selectedCardIndex: Int

RoomModel
├── currentRoomId: Long?
└── lobbyState: LobbyUpdate?

GameStateModel
└── gameState: GameState?

ChatModel
└── messages: List<ChatMessage>
```

## Error Handling

### Server
```
MessageRouter
  └── try/catch
      ├── Parse error → ERROR message
      ├── Handler error → ERROR message
      └── Log error
```

### Client
```
GameController
  └── handleError(ErrorPayload)
      └── Display to user

NetworkClient
  └── ReconnectManager
      ├── Exponential backoff
      └── Max retry limit
```

## Testing Strategy

### Unit Tests (recommended)
```
ConnectionManager
├── Test user creation
├── Test user removal
└── Test connection mapping

RoomManager
├── Test room creation
├── Test join validation
├── Test password check
└── Test broadcast

GameHandler
├── Test turn validation
├── Test card play rules
└── Test UNO declaration
```

### Integration Tests (recommended)
```
Protocol Flow
├── Create room → Join → Start → Play
├── Multiple players in same room
└── Disconnect handling
```

## Performance Considerations

1. **ConcurrentHashMap** for thread-safe state
2. **Connection pooling** via CopyOnWriteArrayList
3. **Lazy initialization** of game sessions
4. **Message batching** possible with BlockingQueue
5. **JSON parsing** cached via Gson

## Future Extensions

### Easy to add:
1. New message types → Add handler class
2. New game modes → Extend GameSession
3. New client features → Add model class
4. Authentication → Add AuthHandler
5. Database persistence → Add DAO layer

### Structure supports:
- Multiple game rooms simultaneously
- Different game variants
- Custom card decks
- Tournament modes
- Spectator mode
- Replay functionality
