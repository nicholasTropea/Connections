# Testing and Validation Matrix

This file describes the validation procedure for the implemented system.

## Automated Validation
1. Compile and run tests:
```bash
mvn clean test
```
2. Expected outcome: exit code `0` and no compilation errors.

## Manual End-to-End Validation
1. Start server:
```bash
mvn -Pserver exec:java@run-server
```
2. Start one or more clients:
```bash
mvn -Pclient exec:java@run-client
```

## Validation Matrix
1. Authentication and session lifecycle:
  Expected: register/login/logout/updateCredentials responses are coherent.
2. Persistent TCP communication:
  Expected: same client socket handles multiple sequential requests.
3. Global round lifecycle:
  Expected: one active game at a time, automatic rotation on timeout.
4. Proposal rules:
  Expected: malformed proposals return errors without state mutation.
5. Scoring semantics:
  Expected: bonus/penalty model and 3-correct win, 4-errors loss.
6. requestGameInfo:
  Expected: current and specific game paths both work.
7. requestGameStats:
  Expected: active and historical game aggregates are correct.
8. requestLeaderboard:
  Expected: all/top-k/single-player rank based on computed totals.
9. requestPlayerStats:
  Expected: solved/failed/unfinished, streaks, rates, histogram.
10. UDP async notifications:
  Expected: client receives `roundEnded` notification on round rotation.
11. Persistence and recovery:
  Expected: restart restores persisted user/game states.
12. Config-driven startup:
  Expected: changing properties files alters runtime behavior accordingly.

## Concurrency Scenarios
1. Multi-client simultaneous login and proposal submission.
2. One client logout/login during same round with state restoration.
3. Multiple clients online during round rotation and UDP notifications.

## Regression Checklist
1. No runtime exceptions in normal command flow.
2. No stale session after client disconnect.
3. Leaderboard/player stats remain consistent after restart.

## Residual Risks
1. No dedicated integration test suite yet for multi-process scenarios.
2. JAR packaging workflow is not yet explicitly documented with exact artifacts.
