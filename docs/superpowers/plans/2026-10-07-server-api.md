# Tic-tac-toe Server Implementation Plan

> For agentic workers: use superpowers:executing-plans to implement task by task in this session. Steps track verification before completion.

Goal: Build Ashlie's host/server contribution compatible with Alex's five HTTP endpoints.

Architecture: Synchronized game objects in a concurrent registry. A Java HttpServer adapter validates requests and produces Alex's JSON response formats.

Tech stack: Java 17, jdk.httpserver, Gson 2.14.0, PowerShell and POSIX shell scripts.

Spec: [Server design](../specs/2026-10-07-server-api.md).

## Global constraints

- Match Alex's five endpoint names and flat board format exactly.
- Game IDs distinguish in-memory games; X creates and O joins.
- Run without Maven. Default binding 127.0.0.1:8080.
- Keep the implementation on server/alex-api in the dedicated local project checkout.
- Test actual game rules and actual HTTP responses; do not claim real-client integration until Alex's client is tested.

## Review focus

- Concurrent duplicate joins and same-turn moves must admit only one request.
- JSON strings, decimals, nulls, arrays, and trailing data must not be accepted as valid moves.
- Query parameters must be decoded safely and reject duplicate/absent game IDs.
- Terminal states must stop moves and expose the correct WIN/DRAW result.
- The packaged scripts must work with directory names containing spaces and report compiler failures.

## Task 1: Game rules and registry

Files: server/src/TicTacToeGame.java, server/src/GameRegistry.java, server/tests/GameRulesTest.java.

Interfaces: TicTacToeGame(String gameId), snapshot(), join(), move(String player, int position); immutable Snapshot and MoveResult records. GameRegistry.create() returns a new TicTacToeGame; find(String gameId) returns a game or null.

- [ ] Write independent game fixtures covering create/join, both players' wins, eight winning lines, draw, rejection without mutation, independent games, and concurrency.
- [ ] Compile before implementation; expect missing TicTacToeGame/GameRegistry.
- [ ] Implement game and registry with synchronized game operations and unique IDs.
- [ ] Compile/run GameRulesTest; expect all tests to pass without compiler warnings.
- [ ] Commit the tested game layer.

## Task 2: HTTP API

Files: server/src/GameServer.java, server/tests/HttpApiTest.java, server/lib/gson-2.14.0.jar and upstream license.

Interfaces: GameServer(String host, int port), start(), port(), close(); main(String[] args) starts the console server. Uses Task 1 interfaces. JSON output fields and status codes follow the spec.

- [ ] Write real Java HttpClient checks for all five endpoints, a complete win/draw, exact response shapes, strict JSON types, errors/methods/body limit, and concurrent requests.
- [ ] Compile before the adapter exists; expect missing GameServer.
- [ ] Implement exact route handling, strict bounded JSON, status codes, and resource lifecycle.
- [ ] Run the entire Java test suite; expect all tests to pass without warnings or leftover listeners.
- [ ] Commit the tested adapter and dependency.

## Task 3: Run instructions and package

Files: server/build.ps1, server/run.ps1, server/test.ps1; equivalent .sh scripts; server/README.md; repository README.md; .gitignore.

Interfaces: build creates server/build; run starts GameServer; test runs both test programs and reports failure through its exit code.

- [ ] Write scripts and document create/join/play and Alex's exact contract, plus the assumptions needing client validation.
- [ ] Execute the PowerShell scripts from the project and a copied package path containing spaces. Verify a real launch/create/join/move/result flow.
- [ ] Review the complete branch with a fresh reviewer and resolve material findings with failing regression tests before fixes.
- [ ] Package source, tests, scripts, dependency/license, and evidence into outputs/Module_7_Group_Project_Server.zip.
- [ ] Report local verification and the remaining client/video/group-submission work accurately.
