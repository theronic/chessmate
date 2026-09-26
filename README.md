# Chessmate: Chess AI in Java

**Update:** Woohoo, my 10+ year-old Java Chess AI has made it into [a Minecraft mod called MineChess](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1288528-minechess)!

![Chessmate in the Minecraft mod, MineChess](http://s3-eu-west-1.amazonaws.com/petrus-blog/minechess-screenshot.png)

## How to run

Install a Java SDK (Java 8 or later).

- macOS/Linux: `./build.sh run`
- Windows: `src\build.bat`
- Tests: `./build.sh test` or `src\build.bat test`
- Deeper move-count tests: `CHESS_DEEP=1 ./build.sh test`

The scripts compile the `Chess` package into `build/` and run with the correct image directory.

I wrote this Java chess engine eight years ago in 2005 for my grade 12 high school project. I was 17 at the time, so I thought the code would be really bad, but it still works and beats me most of the time, bearing in mind that I'm not a very good chess player. It won a regional prize or something (cash must have gotten lost in the mail). I'm pretty proud of it :).

![Chessmate Screenshot](/chessmate-screenshot.png "Chessmate Playing")

## Limitations
Chessmate has no opening book. It detects checkmate and stalemate, but does not adjudicate repetition, move-count draws or insufficient material. Board setup clears castling and en passant rights.

## Fixes

- Added castling, en passant and all four pawn promotion choices.
- Reject moves that leave the king in check; distinguish checkmate from stalemate.
- Recompute board control for each evaluation, including defended pieces and pawn attacks on empty squares.
- Keep search extensions local to each branch, use full search windows, and finish exchanges and check evasions at the horizon.
- Preserve special-move state on takeback and save; discard cancelled computer moves after a new game, takeback or side change.
- Restore the Windows build script and remove unused applet imports.

## How does it work?
Chessmate uses iterative deepening minimax search with alpha-beta pruning, two-ply extensions after captures and pawn moves, and a bounded capture search at the horizon. It retains the 2005 board representation, piece values, move ordering and material/control heuristic.

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

The `positionEvaluation` method returns a floating-point score for the requested side: positive for an advantage, negative for a disadvantage. It calculates fresh attack and defence counts with `controlData`.

The following factors are weighted to evaluate each board position:
 - Material gain (sum of the value of your pieces minus the opponent's)
 - Attacking the opponent's pieces
 - Defending its own pieces
 - Controlling the board, with extra weight for controlling the four squares in the centre of the board.
