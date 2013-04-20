package Chess;

import java.util.*;

public abstract class Search
{
	// Search Methods

	/* Used to test if a draw (stalemate) has occured. */
    public abstract boolean drawnPosition(Position p);

    /* Tests if a player has a winning position (checkmate). */
    public abstract boolean wonPosition(Position p, boolean player);

    /* Returns a float indicating the arbitrary score given to a certain position. */
    public abstract float positionEvaluation(Position p, boolean player);

    /* Updates the chess position displayed in main program. */
    public abstract void printPosition(Position p);

    /* Returns an array of possible moves for a given player (either HUMAN/COMPUTER). */
    public abstract Position [] possibleMoves(Position p, boolean player);

    /* Alters the array that keeps track of where pieces are located according to move. */
    public abstract Position makeMove(Position p, boolean player, Move move);

    /* Alpha-Beta search does not extend its search beyond a certain recursion depth.*/
    public abstract boolean reachedMaxDepth(Position p, int depth);

    /* Who knows WHAT this does! */
    public abstract Move getMove();
}