# Copilot Instructions for Connections Game

## Project Overview
**Connections** is a multiplayer word-puzzle game server/client application built with Java 8 and Maven. Players attempt to group words by thematic categories.

## Architecture

### Client-Server Model
- **Server** ([ServerMain.java](../../src/main/java/com/nicholasTropea/game/server/ServerMain.java)): Runs on port 5555, uses `NetworkManager` to accept multiple client connections
- **Client** ([ClientMain.java](../../src/main/java/com/nicholasTropea/game/client/ClientMain.java)): Connects to server, sends requests, receives responses
- **Threading**: `NetworkManager` spawns in separate thread with `ExecutorService` thread pool. `ClientHandler` processes each client connection independently.

### Network Communication
- **Protocol**: JSON-based over TCP sockets
- **Serialization**: Gson library (v2.11.0)
- **Request/Response Pattern**: All network messages inherit from request/response base classes with specific operation types
  - Example: `LoginRequest` → `LoginResponse`, `SubmitProposalRequest` → `SubmitProposalResponse`
- **Request Types Enum**: [Request.java](../../src/main/java/com/nicholasTropea/game/net/Request.java) defines all operation codes (REGISTER=1, LOGIN=2, LOGOUT=3, etc.)

### Game Data
- **Immutable data structure**: [Game.java](../../src/main/java/com/nicholasTropea/game/model/Game.java) contains nested `Group` class
- **Data source**: [games.json](../../src/main/resources/data/games.json) (~37k lines) preloads all game puzzles with themes and word groups
- **Data integrity**: Lists are copied defensively with `List.copyOf()` to prevent external mutation

## Critical Patterns & Conventions

### 1. Request/Response Classes
Every network operation requires a matching pair:
- Both extend base classes from `com.nicholasTropea.game.net` package
- Use `@SerializedName("fieldName")` to map Java fields to JSON keys
- Include Javadoc explaining JSON structure and possible errors
- Example pattern from `LoginRequest`:
  ```java
  @SerializedName("username")
  private final String username;
  ```

### 2. Language & Documentation
- **All Javadoc and comments are in English** (non-negotiable convention)
- Use proper Javadoc tags: `@param`, `@return`, `@throws`, `@author`, `@see`
- Example: "Logout request", "Manages communication"

### 3. Operation Field
- Every request must specify `operation` field in constructor calling `super("operationName")`
- Example: `public LogoutRequest() { super("logout"); }`

### 4. Code Quality Issues (Watch For)
- Lines must not exceed **90 columns**
- Use **2 empty lines** in between classes and/or methods
- All classes and methods must have a **JavaDoc comment**
- Use PascalCase for classes and camelCase for other things 
- Classes often have **unused imports** (`Objects`, `SerializedName` where not used)—clean these up
- Avoid hardcoding in factory methods; pass parameters

## Build & Development

### Maven Commands
```bash
mvn clean compile           # Build project
mvn exec:java -Pserver      # Run server on port 5555
mvn exec:java -Pclient      # Run client (connects to localhost:5555)
mvn test                    # Run JUnit 5 tests
```

### Project Structure
```
src/main/java/com/nicholasTropea/game/
├── client/        (ClientMain.java)
├── server/        (ServerMain.java, NetworkManager.java, ClientHandler.java)
├── model/         (Game.java, Player.java, GameState models)
└── net/           (All Request/Response classes, RequestTypes enum)
src/main/resources/data/
└── games.json     (Pre-loaded game puzzles)
```

### Dependencies
- **Gson 2.11.0**: JSON serialization
- **JUnit 5.10.3**: Testing
- **Java 8**: Target/source compilation

## Implementation Guidelines

### When Adding New Network Operations
1. Create `NewOperationRequest` extending `Request` with operation name
2. Create matching `NewOperationResponse` class
3. Add enum value to `RequestTypes` with increment code
4. Update `ClientHandler` to parse new operation type
5. **Always write Javadoc with expected JSON structure and errors**

### When Modifying Game Logic
- Game state lives in `PlayerGameState` (per-player state) and `Game` (puzzle data)
- Never mutate lists returned from `Game.getGroups()` or `Group.getWords()`—they're defensive copies
- Leaderboard logic uses `LeaderboardRecord` model with player stats aggregation

### When Working with JSON
- Use `@SerializedName` annotation for all fields that differ from Java camelCase
- Keep field names in JSON lowercase (`"psw"`, `"gameId"`, `"timeLeft"`)
- Response classes may include null fields; document which are conditional on `success` boolean

## Common Pitfalls
- Server port is hardcoded as `5555`—don't assume configurable without updates to `ServerMain`
- `ClientMain` currently only handles logout responses—expand switch/handler for other response types
- JSON deserialization in `ClientHandler` hardcodes `LogoutRequest`—needs dynamic request-type routing
