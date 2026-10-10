<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>API Tic Tac Toe</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<div class="page">
    <h1>API Tic Tac Toe</h1>

    <div class="card">
        <h2>Create a New Game</h2>
        <form action="game.jsp" method="post">
            <input type="hidden" name="action" value="create">
            <button type="submit">Create Game</button>
        </form>
    </div>

    <div class="card">
        <h2>Join an Existing Game</h2>
        <form action="game.jsp" method="post">
            <input type="hidden" name="action" value="join">

            <label for="gameId">Game ID</label>
            <input id="gameId" name="gameId" type="text" required>

            <button type="submit">Join Game</button>
        </form>
    </div>
</div>
</body>
</html>