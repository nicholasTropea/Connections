# Connections

Connections is a client-server multiplayer word puzzle game implemented in Java.
Players try to identify thematic groups of words in a shared global round.

## Requirements
1. Java 21
2. Maven 3.9+

## Build
```bash
mvn clean compile
```

## Configuration
Runtime parameters are loaded automatically from resource files.

1. Server config: [src/main/resources/config/server.properties](src/main/resources/config/server.properties)
2. Client config: [src/main/resources/config/client.properties](src/main/resources/config/client.properties)

Server properties:
1. `server.tcp.port`
2. `server.round.duration.ms`
3. `server.session.autosave.seconds`

Client properties:
1. `client.server.host`
2. `client.server.port`
3. `client.udp.listen.port` (`0` means ephemeral port)

## Run
Start server:
```bash
mvn -Pserver exec:java@run-server
```

Start client:
```bash
mvn -Pclient exec:java@run-client
```

Alternative scripts:
1. [startServer.sh](startServer.sh)
2. [startClient.sh](startClient.sh)

## Main Features
1. Persistent TCP request/response communication.
2. One global active game round with automatic timed rotation.
3. Per-player game state tracking with login rejoin support.
4. Spec-compliant proposal rules and scoring.
5. UDP asynchronous round-ended notifications.
6. Persistent player data and persistent session/game snapshots.

## Request Operations
Client CLI supports these operations:
1. Register
2. Login
3. Logout
4. Request game information
5. Request game statistics
6. Request leaderboard
7. Request player statistics
8. Submit proposal
9. Update credentials

## Persistence Files
1. Player repository: [src/main/resources/data/players.json](src/main/resources/data/players.json)
2. Games catalog: [src/main/resources/data/games.json](src/main/resources/data/games.json)
3. Session/game snapshots: [src/main/resources/data/gameStates.json](src/main/resources/data/gameStates.json)

## Testing
Run tests:
```bash
mvn test
```

Manual validation checklist is available in
[TESTING.md](TESTING.md).

## Notes
1. Current build targets Java 21 in [pom.xml](pom.xml).
2. The project includes command-line clients only.