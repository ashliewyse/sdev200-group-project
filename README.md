# SDEV 200 Group Project: Networked Tic-Tac-Toe

A Java host/server and client/player application for the Module 7 group project.

- Server contribution: Ashlie M Wyse. Source, tests, and run instructions are in [server/README.md](server/README.md).
- Client contribution: Nicholas Barry Garcia (AlexBG). The server follows the API examples Alex supplied on Discord. His client has not yet been added to this checkout or tested against the server.

To run on Windows with JDK 17 or newer:

```powershell
.\server\run.ps1
```

To run the server tests:

```powershell
.\server\test.ps1
```

Default server URL: `http://127.0.0.1:8080`. Run two clients against the same server: X creates a game, shares its returned game ID, and O joins that game. The server README describes the five endpoints, position numbering, response formats, and sample requests.

Final group handoff still requires testing Alex's actual client, combining both contributions, and recording the gameplay video. A server test run alone is not the final group submission.
