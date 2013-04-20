# Chessmate Chess AI
Matric Computer Class Project in Java

## How to run
Run `src/build.bat` to compile and run. You will need the Java SDK.

I wrote this chess engine in Java for my grade 12 (matric) project. I was 17 at the time. Is still works! It won a regional prize or something, but the cash must be lost in the mail.

## How does it work?
Chessmate uses an iterative deepening minimax search algorithm with alpha-beta pruning and simple horizon detection during exchanges.

That's a fancy way of saying it builds a tree of moves, rates each move with a number (using a heuristic function), then keeps exploring the leaf branches that seem promising. Minimax assumes its opponent will make the best sequence of moves it can think of and will then counter with a move that minimises your opportunities while maximising its own.

## Performance 
Chessmate has a terrible early game due the lack of an opening book, an average medium game and a pretty strong end game. It runs pretty fast for a Java application. I remember it searching through 500k moves/second on my shitty PC in 2005.

## Database?
The Access database is just a gimmick that tracks a history of moves, because the project had a database requirement.

## Limitations
Chessmate cannot castle or take en passant. You could modify the Board class and heuristic function relatively easily to add castling, if you have sado-masochistic tendencies.

## Heuristic Function
The original heuristic function is probably the most interesting part of the project. I will write something about it when I have read it for the first time after 8 years.
