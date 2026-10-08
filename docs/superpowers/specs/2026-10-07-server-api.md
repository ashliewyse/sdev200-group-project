# Tic-tac-toe server design

Ashlie M Wyse — SDEV 200, Group1 2 — October 7, 2026.

Ashlie approved matching the server to AlexBG's Discord API examples and proceeding with the server. Nicholas Barry Garcia (AlexBG) builds the client; Ashlie builds the server. The final group submission requires both sides on GitHub, a README, and a gameplay video by October 12 at 11:59 p.m.

## Architecture

Java 17, built-in HttpServer, Gson 2.14.0 for strict JSON parsing. A small registry holds separate in-memory games by generated gameId. Each game synchronizes joins, moves, and snapshots. Default address is http://127.0.0.1:8080; host and port can be specified. The classroom server has no player authentication, database, or persistence.

Components: TicTacToeGame owns rules and state; GameRegistry creates and looks up games; GameServer handles HTTP and JSON. PowerShell and shell scripts build/run/test without Maven. A pinned Gson jar is supplied with its upstream license for offline use.

## Alex's API

| Method and path | Request | Response |
| --- | --- | --- |
| POST /game/create | {} | gameId, player X, board ---------, status WAITING_FOR_PLAYER |
| POST /game/join | {"gameId":"abc123"} | gameId, player O, board ---------, status IN_PROGRESS |
| GET /game/state?gameId=abc123 | No body | gameId, board, currentPlayer, status |
| POST /game/move | {"gameId":"abc123","player":"X","position":4} | valid true, board, status; or valid false and reason |
| GET /game/result?gameId=abc123 | No body | status; winner X/O only when status is WIN |

Create generates a fresh identifier; abc123 is an example. Joining admits O once and starts X's turn. Creating a new game does not reset another game's board. Play another round by creating and joining a new ID.

The board is nine characters in row order, with - for empty. Positions are 0–8, with 4 at the center, consistent with Alex's example. Statuses are WAITING_FOR_PLAYER, IN_PROGRESS, WIN, and DRAW. X moves first; a win immediately ends play; a full board without a winner is a draw.

Accepted move example: {"valid":true,"board":"X--OX----","status":"IN_PROGRESS"}. Occupied-cell rejection: {"valid":false,"reason":"Position already taken"}. Rejections leave all game state unchanged. Result examples: {"status":"WIN","winner":"X"}, {"status":"DRAW"}, and {"status":"IN_PROGRESS"}.

## Assumptions beyond Alex's examples

- State includes currentPlayer null while waiting and after completion. Waiting result is {"status":"WAITING_FOR_PLAYER"}.
- HTTP 200 for successful operations and rule-based move rejection, allowing a console client to read valid/reason normally. Invalid input is 400; unknown routes/games 404; wrong method 405 with Allow; duplicate join 409; bodies over 1 KiB 413; non-JSON POST content 415.
- Move errors always use valid false and reason, including transport/input errors. Other errors use an error field. Internal failures use 500 without stack traces.
- JSON must be a single strict object. Position must be a JSON integer in 0–8; player exactly X or O; gameId a nonempty string. Unknown fields are ignored. Query gameId must occur exactly once.
- These details must be checked against Alex's actual code when pushed. His source was not in the shared repository when checked for this revision; main still contained only README.md.

## Validation and handoff

Test both players' wins, all eight winning lines, draws, rejection without mutation, waiting/joining, duplicate joins, independent games, and simultaneous requests. Test all five endpoints through real HTTP, exact response shapes, malformed JSON, wrong types, bounds, methods, unknown IDs, content type, and request size. Verify build/run instructions on the installed Java 17 runtime.

Keep server changes on a separate branch. Connect two actual clients and play a full game before calling integration complete or recording the video. A console client is acceptable in the instructor's project overview; JSP is optional follow-up work.

Sources: AlexBG's Discord messages October 7, 10:21–10:23 p.m.; [shared repository](https://github.com/ashliewyse/sdev200-group-project); [project overview](https://ivylearn.ivytech.edu/courses/1422263/pages/group-project-overview); [final assignment](https://ivylearn.ivytech.edu/courses/1422263/assignments/24564717); [Gson upstream](https://github.com/google/gson).
