# Final Relation (Concise)

## 1. Design Choices
1. Single centralized server runtime with dependency injection to handlers.
2. One global round coordinator controlling active game and timing.
3. Thread-safe session manager storing per-user, per-game states.
4. TCP request/response for synchronous operations.
5. UDP notifications for asynchronous round-ended events.
6. JSON persistence with atomic write strategy for consistency.

## 2. Thread Model
1. Server process:
   - main thread: bootstraps runtime and network manager.
   - network acceptor thread: accepts TCP clients.
   - worker thread pool: one runnable handler per connected client.
   - round coordinator scheduler thread: checks round expiration and rotates.
   - session persistence scheduler thread: periodic autosave.
2. Client process:
   - main thread: CLI input and TCP request/response loop.
   - UDP listener daemon thread: receives async notifications.

## 3. Data Structures
1. Player repository:
   - `Map<Integer, Player>` for primary ID index.
   - `Map<String, Integer>` for nickname-to-id lookup.
2. Session manager:
   - `Map<Integer, PlayerGameState>` active sessions.
   - `Map<Integer, Map<Integer, PlayerGameState>>` all user game states.
3. Game repository:
   - `Map<Integer, Game>` indexed games.
   - sorted game ID list for deterministic rotation.
4. Notification service:
   - `Map<Integer, UdpEndpoint>` active UDP subscribers.

## 4. Synchronization Strategy
1. Session and player repositories expose synchronized public methods.
2. Concurrent maps are used where lock-free access is beneficial.
3. Round coordinator guards rotation-critical fields via internal lock.
4. Listener notifications are isolated from coordinator lock sensitivity.
5. Persistence operations are atomic using temp-file move.

## 5. Build and Run Instructions
1. Compile and test:
```bash
mvn clean test
```
2. Start server:
```bash
mvn -Pserver exec:java@run-server
```
3. Start client:
```bash
mvn -Pclient exec:java@run-client
```

## 6. Configuration Files
1. Server config: [src/main/resources/config/server.properties](src/main/resources/config/server.properties)
2. Client config: [src/main/resources/config/client.properties](src/main/resources/config/client.properties)

## 7. Persisted Files
1. Players: [src/main/resources/data/players.json](src/main/resources/data/players.json)
2. Games: [src/main/resources/data/games.json](src/main/resources/data/games.json)
3. Session snapshots: [src/main/resources/data/gameStates.json](src/main/resources/data/gameStates.json)
