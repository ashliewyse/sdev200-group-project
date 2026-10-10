# SDEV 200 Group Project: Networked Tic-Tac-Toe

A Java host/server and client/player application for the SDEV 200 group project.

This project includes a Java HTTP API server, the original console-based Java client, and a JSP web-based client.

## Requirements

- Java JDK 17 or newer
- Apache Tomcat 9 or newer for the JSP client
- IntelliJ IDEA with Tomcat support, or a standalone Tomcat installation
- Windows PowerShell for the included `.ps1` scripts

## Project Structure

The main project layout is:

```text
sdev200-group-project/
├── server/
│   ├── src/
│   ├── tests/
│   ├── run.ps1
│   ├── test.ps1
│   └── README.md
├── src/
│   └── Client/
│       ├── ApiClient.java
│       ├── BoardPrinter.java
│       ├── GameClient.java
│       ├── Main.java
│       ├── index.jsp
│       ├── game.jsp
│       └── style.css
└── README.md
```

## Running the Project

The project has two parts that must run at the same time: the API server and the JSP web client.

The API server runs the game logic and should run on:

```text
http://localhost:8080
```

The JSP client runs through Apache Tomcat and should run on:

```text
http://localhost:8081/sdev_200_group_project/index.jsp
```

Recommended setup:

```text
API server: http://localhost:8080
JSP client: http://localhost:8081/sdev_200_group_project/index.jsp
```

First, start the API server from the project root:

```powershell
.\server\run.ps1
```

Leave this terminal open while playing the game.

The server should be available at:

```text
http://localhost:8080
```

Next, start the JSP client using Apache Tomcat.

In IntelliJ IDEA:

1. Open `Run > Edit Configurations...`
2. Add or select a `Tomcat Server > Local` configuration.
3. Set the Tomcat HTTP port to `8081`.
4. Open the `Deployment` tab.
5. Add the exploded web artifact for the project.
6. Set the application context to `/sdev_200_group_project`.

The artifact output layout should place the JSP client files directly under `<output root>`:

```text
<output root>
├── index.jsp
├── game.jsp
└── style.css
```

These files are located in the project at:

```text
src/Client/index.jsp
src/Client/game.jsp
src/Client/style.css
```

After the artifact is deployed, start the Tomcat configuration from IntelliJ and open:

```text
http://localhost:8081/sdev_200_group_project/index.jsp
```

If Tomcat shows a 404 error, check that Tomcat is running on port `8081`, the artifact is listed in the Tomcat `Deployment` tab, the application context is `/sdev_200_group_project`, and `index.jsp`, `game.jsp`, and `style.css` are directly under `<output root>` in the artifact layout.

## Playing a Game

Use two separate browser sessions so each player has a separate session.

Do not use two tabs in the same browser session because they may share the same player session.

A good setup is:

- Chrome for one player and Edge for the other player
- Or one normal browser window and one private/incognito window

Player X should open:

```text
http://localhost:8081/sdev_200_group_project/index.jsp
```

Then Player X clicks `Create Game as X` or `Create Game`.

The game page will display a game ID. Copy that game ID and give it to Player O.

Player O should open a separate browser session and go to:

```text
http://localhost:8081/sdev_200_group_project/index.jsp
```

Player O enters the game ID created by Player X and clicks `Join Game as O` or `Join Game`.

After Player O joins, the game begins.

X moves first. Only the player whose turn it is can click an open square. The page refreshes automatically while the game is active. The game ends with either a win or a draw.

Game statuses include:

```text
WAITING_FOR_PLAYER
IN_PROGRESS
WIN
DRAW
```

To play another round, click `Leave Game`, return to the start page, create a new game, and join with the new game ID.

## Running Server Tests

From the project root, run:

```powershell
.\server\test.ps1
```

## Console Client

The original console client is still available and has not been replaced.

The console client files are located in:

```text
src/Client/
```

The console client entry point is:

```text
src/Client/Main.java
```

The JSP client was added separately, so the console client can still be used or tested independently.

## API Endpoints

The clients communicate with the server using these endpoints:

| Method | Endpoint                  | Description                                        |
|--------|---------------------------|----------------------------------------------------|
| `POST` | `/game/create`            | Creates a new game and assigns the creator as X    |
| `POST` | `/game/join`              | Joins an existing game and assigns the joiner as O |
| `GET`  | `/game/state?gameId=...`  | Gets the current game board and status             |
| `POST` | `/game/move`              | Sends a move for the current player                |
| `GET`  | `/game/result?gameId=...` | Gets the final game result                         |

X is the player who creates the game, and O is the player who joins the game. X always moves first.

To choose a side in the current version:

- Choose X by creating a game.
- Choose O by joining an existing game.

A true custom side-selection feature would require changing the server API.

## Troubleshooting

If Tomcat shows `HTTP Status 404`, make sure the browser URL includes the application context:

```text
http://localhost:8081/sdev_200_group_project/index.jsp
```

The shorter URL below will only work if the application context is set to `/`:

```text
http://localhost:8081/index.jsp
```

If there is a port conflict on `8080`, make sure Tomcat is using `8081` and the API server is using `8080`.

Recommended setup:

```text
API server: http://localhost:8080
JSP client: http://localhost:8081/sdev_200_group_project/index.jsp
```

If the second player is not joining correctly, use a separate browser session for Player O. Two tabs in the same browser may share the same session and act like the same player.

If the JSP client cannot connect to the server, make sure the API server is running first:

```powershell
.\server\run.ps1
```

The JSP client expects the API server at:

```text
http://localhost:8080
```