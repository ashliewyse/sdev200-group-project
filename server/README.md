# Java tic-tac-toe server

Server contribution by Ashlie M Wyse for SDEV 200. This server owns the board and rules. It implements the five HTTP endpoints supplied by Nicholas Barry Garcia (AlexBG) on Discord on October 7, 2026.

## Requirements and run commands

Use a JDK version 17 or newer. Both `java` and `javac` must be on PATH, or set JAVA_HOME to the JDK installation directory on Windows. Gson 2.14.0 is included in `lib/` with its upstream Apache 2.0 license and published SHA-256 checksum. No Maven or dependency download is required to run this package.

Windows PowerShell, from the repository folder:

```powershell
.\server\build.ps1
.\server\test.ps1
.\server\run.ps1
```

If Windows blocks the local scripts, the individual-command alternative is:

```powershell
javac --release 17 -cp server/lib/gson-2.14.0.jar -d server/build server/src/TicTacToeGame.java server/src/GameRegistry.java server/src/GameServer.java
java -cp 'server/build;server/lib/gson-2.14.0.jar' GameServer
```

Create `server/build` first if using the individual compilation command. Do not change the machine's script policy to run this project.

Linux/macOS shell commands (included; Windows is the verified environment):

```sh
sh server/build.sh
sh server/test.sh
sh server/run.sh
```

The default URL is `http://127.0.0.1:8080`. Stop the server with Ctrl+C. To choose another port:

```powershell
.\server\run.ps1 -Port 8081
```

For a classroom LAN test, choose the computer's LAN address with `-BindAddress`, and configure the client to use that address and port. The server has no authentication or TLS and is intended for local/classroom demonstrations. Restarting it discards all games.

## Endpoints

| Method/path | JSON input | Success response |
| --- | --- | --- |
| POST /game/create | `{}` | `gameId`, `player`, `board`, `status` |
| POST /game/join | `{"gameId":"returned-id"}` | `gameId`, `player`, `board`, `status` |
| GET /game/state?gameId=returned-id | None | `gameId`, `board`, `currentPlayer`, `status` |
| POST /game/move | `{"gameId":"returned-id","player":"X","position":4}` | `valid`, `board`, `status` |
| GET /game/result?gameId=returned-id | None | `status`; also `winner` for WIN |

POST requests must use `Content-Type: application/json`. The server generates the game ID; do not hard-code Alex's sample `abc123`.

Create assigns X and returns:

```json
{"gameId":"returned-id","player":"X","board":"---------","status":"WAITING_FOR_PLAYER"}
```

Join assigns O once and starts X's turn. Both clients poll state while waiting. A state response during play looks like:

```json
{"gameId":"returned-id","board":"X--O-----","currentPlayer":"X","status":"IN_PROGRESS"}
```

The board is nine row-major characters. A dash is an empty cell. Position numbering is:

```text
0 | 1 | 2
3 | 4 | 5
6 | 7 | 8
```

X and O alternate. The server rejects occupied cells, the wrong player's turn, moves while waiting for O, and moves after completion. A rejected move leaves all state unchanged and returns, for example:

```json
{"valid":false,"reason":"Position already taken"}
```

A valid center move in Alex's example returns:

```json
{"valid":true,"board":"X--OX----","status":"IN_PROGRESS"}
```

State always includes `currentPlayer`; it is null while waiting and after a win or draw. Result responses are `{"status":"IN_PROGRESS"}`, `{"status":"WAITING_FOR_PLAYER"}`, `{"status":"WIN","winner":"X"}` (or O), and `{"status":"DRAW"}`. To replay, create a new game and have O join its new ID. Separate game IDs keep separate boards.

## Quick PowerShell demonstration

Start the server in one terminal. In another terminal:

```powershell
$game = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/game/create' -ContentType 'application/json' -Body '{}'
$gameId = $game.gameId
$joinBody = @{ gameId = $gameId } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/game/join' -ContentType 'application/json' -Body $joinBody
$moveBody = @{ gameId = $gameId; player = 'X'; position = 4 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/game/move' -ContentType 'application/json' -Body $moveBody
Invoke-RestMethod -Uri "http://127.0.0.1:8080/game/state?gameId=$gameId"
Invoke-RestMethod -Uri "http://127.0.0.1:8080/game/result?gameId=$gameId"
```

This exercises the server directly; it does not replace the required two-client gameplay demonstration.

## Error handling and details to check with Alex's client

Alex supplied payloads but did not specify HTTP status codes or waiting/terminal `currentPlayer` values. The implementation choices are:

- HTTP 200 for successful requests and rule-based move rejections. Clients should read the `valid` field for moves.
- 400 for malformed input, missing fields, wrong JSON types, non-integer/out-of-range positions, or missing/duplicate query game IDs.
- 404 for unknown routes or game IDs; 405 for a wrong HTTP method (with Allow); 409 for a second join; 413 for bodies exceeding 1024 bytes; 415 for non-JSON POST media types.
- All move errors use `valid:false` and `reason`. Other errors use `error`. Unexpected internal errors use 500 and a generic message.
- Strict JSON objects only. Position is a JSON integer from 0 to 8, not a string, decimal, or exponent. Player is exactly X or O. Extra fields are ignored.

The console client is in `../src/Client/` and connects to `http://localhost:8080` with positions 0–8. It compiles with JDK 17. On October 9, 2026, an integration check launched two actual console client processes: the creator remained running while waiting, the second client joined, both alternated moves, and both displayed the final X win. The JSP client also completed a win and a nine-move draw through two independent browser sessions. See the [repository README](../README.md) for setup and gameplay instructions.

## Source and test layout

- `src/TicTacToeGame.java`: synchronized game rules and immutable snapshots.
- `src/GameRegistry.java`: concurrent game lookup and unique creation.
- `src/GameServer.java`: bounded JSON input, routing, responses, and server lifecycle.
- `tests/GameRulesTest.java`: independent win/draw/validation/concurrency fixtures.
- `tests/HttpApiTest.java`: real HTTP requests for endpoint formats, full games, malformed input, limits, and concurrent requests.

Tests start a loopback server on an automatically selected port and close it when finished. Build scripts enable source lint checks; optional annotation metadata warnings from the Gson jar are excluded with `-Xlint:all,-classfile`.

Dependency source: [Google Gson](https://github.com/google/gson), [Gson 2.14.0 release](https://github.com/google/gson/releases/tag/gson-parent-2.14.0), and [Maven Central artifact](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.14.0/).

Gson jar SHA-256: `2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f`.
