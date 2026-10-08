# SDEV 200 Group Project: Networked Tic-Tac-Toe

A Java host/server and client/player application for the Module 7 group project.

- Server contribution: Ashlie M Wyse. Source, tests, and run instructions are in [server/README.md](server/README.md).
- Client contribution: Nicholas Barry Garcia (AlexBG). His console client is in `src/Client/`. The server follows the API examples he supplied on Discord.

To run on Windows with JDK 17 or newer:

```powershell
.\server\run.ps1
```

To run the server tests:

```powershell
.\server\test.ps1
```

Default server URL: `http://127.0.0.1:8080`. Run two clients against the same server: X creates a game, shares its returned game ID, and O joins that game. The server README describes the five endpoints, position numbering, response formats, and sample requests.

## Current integration checks

The combined checkout passes all 56 server checks, and all four original client source files compile with JDK 17. Against the real server, the original O client successfully joined a game, played two moves, and displayed an X win; the test drove X's moves through HTTP requests.

The create-game client currently exits with `Game finished: WAITING_FOR_PLAYER` before O joins. Its game loop treats every status other than `IN_PROGRESS` as finished. It needs to keep polling during `WAITING_FOR_PLAYER` before a full game with two console clients can be demonstrated. Alex's original client source is retained for him to review and update.

The client currently connects to `http://localhost:8080`. Run the server on its default port for local integration checks.

Final group handoff still requires a full game with two console clients, final run/play instructions, and the gameplay video. A server test run alone is not the final group submission.
