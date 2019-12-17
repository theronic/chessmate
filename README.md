# Chessmate: Chess AI in Java

**Update:** Woohoo, my 10+ year-old Java Chess AI has made it into [a Minecraft mod called MineChess](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1288528-minechess)!

![Chessmate in the Minecraft mod, MineChess](http://s3-eu-west-1.amazonaws.com/petrus-blog/minechess-screenshot.png)

## How to run

Run `src/build.bat` to compile and run. You will need the Java SDK. You can beat her easily on a depth of 4, but if you give it ~20-30s at depth setting of 5, she plays a decent game.

I wrote this Java chess engine eight years ago in 2005 for my grade 12 high school project. I was 17 at the time, so I thought the code would be really bad, but it still works and beats me most of the time, bearing in mind that I'm not a very good chess player. It won a regional prize or something (cash must have gotten lost in the mail). I'm pretty proud of it :).

![Chessmate Screenshot](/chessmate-screenshot.png "Chessmate Playing")

## Limitations
Chessmate has no opening book and cannot castle, take en passant or promote pawns. If you are feeling masochistic, you can add castling by modifying the `Board` class and heuristic function, `positionEvaluation`.

## How does it work?
Chessmate uses an iterative deepening minimax search algorithm with alpha-beta pruning and simple horizon detection during exchanges with an original heuristic function.

That's a fancy way of saying:

 1. Chessmate builds a tree of all possible moves to some depth,
 2. rates each position with a heuristic function
 3. choose the move that minimises your advantage while maximising its own advantage,
 4. and looks beyond the horizon during exchanges.

## Performance 
Chessmate has a naive early game due the lack of an opening book, an average mid-game and a pretty strong end-game. It runs pretty fast for a Java application. I remember it searching through 500k moves/second on my slow computer in 2005. I now get about 1.2M nodes/second on my 2012 Macbook Air running on a single thread.

## Database?
The project had a database requirement, but I ripped out the Access database prompt. It was mostly a gimmick for the project requirements that tracks a history of moves.

## Design of Heuristic Function

The heuristic function is quite simple, but performs well given sufficient search depth (5 seems to be the sweet spot). The `positionEvaluation` method returns a floating-point value that is positive or negative based on whether it's winning or losing, respectively. It also uses some of the board control data set in the `setControlData` method.

The following factors are weighted to evaluate each board position:
 - Material gain (sum of the value of your pieces minus the opponent's)
 - Attacking the opponent's pieces
 - Defending it's own pieces
 - Controlling the board, with extra weight for controlling the four squares in the centre of the board.

The structure of these evaluation functions are terrible and really hard to unit test. Having read through the evaluation code, I am almost certain that the board control data is fundamentally broken. Oh well.
