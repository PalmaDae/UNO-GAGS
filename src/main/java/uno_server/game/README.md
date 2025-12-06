# UNO Game Engine

This package contains a complete UNO rules engine implementation for the UNO server project.

## Package Structure

### `uno_server.game`

The main game logic package containing:

- **`PlayerState`** - Manages individual player state including hand, UNO declaration, and scoring
- **`DeckBuilder`** - Creates and manages standard UNO decks with all 108 cards
- **`GameSession`** - Core game logic handling turn management, card play validation, and game rules
- **`GameSessionManager`** - Manages multiple game sessions and provides server interface
- **`UnoGameDemo`** - Demonstration and test class showing the game engine in action

## Features Implemented

### Core Game Mechanics
- ✅ Initial deal (7 cards per player)
- ✅ Turn management with clockwise/counterclockwise direction
- ✅ Card play validation (color/number/type matching)
- ✅ Wild card support with color selection
- ✅ Drawing cards when no valid play available

### Action Card Effects
- ✅ **Skip**: Next player loses their turn
- ✅ **Reverse**: Changes play direction (acts as skip in 2-player games)
- ✅ **Draw Two**: Next player draws 2 cards and loses turn
- ✅ **Wild**: Player chooses next color
- ✅ **Wild Draw Four**: Next player draws 4 cards, loses turn, player chooses color

### UNO Rules
- ✅ UNO declaration when player has 2 cards
- ✅ Penalty (2 cards) for forgetting to declare UNO
- ✅ Hand scoring when game ends
- ✅ Winner gets points equal to all opponents' card values

### Server Integration
- ✅ `GameSessionManager` provides clean API for server integration
- ✅ Methods: `startGame`, `playCard`, `drawCard`, `sayUno`, `getGameState`
- ✅ Thread-safe session management
- ✅ Proper error handling and validation

## Card Scoring

- **Number cards**: Face value (0-9 points)
- **Action cards** (Skip, Reverse, Draw Two): 20 points each
- **Wild cards** (Wild, Wild Draw Four): 50 points each

## Usage Example

```java
// Create game session manager
GameSessionManager manager = new GameSessionManager();

// Create players
List<PlayerState> players = Arrays.asList(
    new PlayerState(1L, "Alice"),
    new PlayerState(2L, "Bob")
);

// Start a new game
long roomId = 1001L;
GameSession session = manager.createSession(roomId, players);

// Get initial game state
GameState state = manager.getGameState(roomId);

// Player plays a card
manager.playCard(roomId, playerId, cardIndex, chosenColor);

// Player draws a card
manager.drawCard(roomId, playerId);

// Player declares UNO
manager.sayUno(roomId, playerId);

// Get final scores when game ends
Map<Long, Integer> scores = manager.calculateScores(roomId);
```

## Running the Demo

To see the game engine in action, run:

```bash
mvn compile exec:java
```

This will run the `UnoGameDemo` which demonstrates:
- Game initialization and card dealing
- Turn-based gameplay with validation
- Action card effects
- UNO declaration mechanics
- Invalid move rejection
- Score calculation

## Game Flow

1. **Game Setup**: 7 cards dealt to each player, one card placed on discard pile
2. **Player Turn**: Current player may play a valid card or draw
3. **Card Validation**: Card must match color, number, or type (or be wild)
4. **Action Effects**: Skip, Reverse, Draw cards applied immediately
5. **UNO Declaration**: Must declare when playing second-to-last card
6. **Win Condition**: First player to empty hand wins
7. **Scoring**: Winner receives points from all remaining cards

The implementation follows standard UNO rules and provides a solid foundation for the multiplayer server experience.