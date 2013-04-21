
package Chess;

import java.awt.*;
import java.util.*;
import java.io.*;

public class Chess
{
/**
 * A circular reference to the parent Main class.
 */
	public static Main main;

/**
 * Used during debug compilations.  Most print-outs have been removed, however,
 * to increase performance.
 */
	public static final boolean DEBUG = false;

    final static public boolean WHITE = true;
    final static public boolean BLACK = false;

	public static boolean HUMAN		= WHITE;
	public static boolean PROGRAM	= BLACK;

	public boolean bWhoseTurn = WHITE;

	public boolean bIterativeDeepening = true;

/**
 * An integer that stores the number of nodes traversed.
 */
	public static int nodeCount = 0; // number of many alpha-beta nodes being searched

/**
 * An integer that holds the maximum recursive depth.  This is controlled by the
 * difficulty slider in the Main class.
 * @see difficultySlider
 */
	public static int maxDepth = 5; // The Default AI Difficulty level

/**
 * True while the AICaller thread is working.  If it is falsed, the thread will stop ASAP.
 */
	public static boolean bThinking = false;

/**
 * A stack to hold a history of board positions used for takebacks.
 */
	public static Stack boardHistory = new Stack();

/**
 * A hard-coded array bearing the traditional chess board orientation.
 */
	private final static int [] initialBoard = {
		4, 2, 3, 6, 5, 3, 2, 4,	7, 7,   // white pieces
		1, 1, 1, 1, 1, 1, 1, 1, 7, 7,   // white pawns
		0, 0, 0, 0, 0, 0, 0, 0, 7, 7,   // 8 blank squares, 2 off board
		0, 0, 0, 0, 0, 0, 0, 0, 7, 7,   // 8 blank squares, 2 off board
		0, 0, 0, 0, 0, 0, 0, 0, 7, 7,   // 8 blank squares, 2 off board
		0, 0, 0, 0, 0, 0, 0, 0, 7, 7,   // 8 blank squares, 2 off board
		-1,-1,-1,-1,-1,-1,-1,-1,7, 7,  // black pawns
		-4,-2,-3,-6,-5,-3,-2,-4,7, 7  // black pieces
	};

/**
 * Piece indexes used to index the piece movement table when generating moves.
 * @see pieceMovementTable
 */
	private final static int [] index = {
		0, 12, 15, 10, 1, 6, 6
	};

/**
 * The current state of the board, used for drawing by Main Class
 */
	public static ChessPosition pos = new ChessPosition();
	public static ChessPosition workPos = new ChessPosition();
/**
 * The all-important movement table for generating possible moves for a given piece.
 * This is indexed via index.
 * @see index
 */
	private final static int [] pieceMovementTable = {
			0, -1, 1, 10, -10, 0, // Rook
			-1, 1, 10, -10, -9, -11, 9, 11, 0, // Queen / Bishop / King
			8, -8, 12, -12, 19, -19, 21, -21, 0, // Knight
			10, 20, 0
	};

/**
 * Arbitrary piece values.  Note how the king is indispensable - important for checks.
 */
	private final static float [] value = {
			0.0f, 1.0f, 3.0f, 3.2f, 5.0f, 9.0f, 300.0f
	};

/**
 * A temporary list of all possible moves for a given position.
 */
	private static ChessMove [] possibleCaptures = new ChessMove[256];
	private static ChessMove [] possibleMoveList = new ChessMove[256];
/**
 * A temporary list of possible moves for a given piece.  Contains a maximum of 32 moves.
 */
	private static int [] piece_moves = new int[32];

/**
 * Test for a stalemate.
 */
	public boolean drawnPosition( ChessPosition p )
	{
		return false;
	}
/**
 * Tests whether a side has won the game, i.e. check for checkmates.
 */
	public boolean wonPosition( ChessPosition p, boolean player )
	{
		return false;
	}
/**
 * Called by the alphaBetaHelper function to decide whether to cut-off at a certain depth.
 * This function has been deprecated and is now in-lined with the alphaBetaHelper function for speed.
 */
	/*public boolean reachedMaxDepth( ChessPosition p, int depth )
	{
		if ( depth <= maxDepth && bThinking )
			return false;
		return true;
	}*/

/**
 * A function used by the main class to check if a player's move is legal.
 * Returns true if it is legal, or false if it is not.  Not used by AI.
 */

	boolean isValidMove( ChessPosition p, ChessMove move )
	{
		if ( bThinking )
			return false;

		boolean player = (p.board[move.from] < 0) ? BLACK : WHITE;

		boolean bOK = false;

		// is it en-passant?
		if ( p.enPassantSquare > 0 &&
			 p.board[ move.from ] == (player?ChessPosition.PAWN:-ChessPosition.PAWN) )
		{
			int offset = move.to - move.from;
			if ( offset < 0 )
				offset = -offset;
			if ( offset == 11 || offset == 9 )
			{
				if ( p.board[ move.from - 1 ] == (player?-ChessPosition.PAWN:ChessPosition.PAWN) ||
					 p.board[ move.from + 1 ] == (player?-ChessPosition.PAWN:ChessPosition.PAWN) )
				{
					bOK = true;
				}
			}
		}
	/*		if ( p.board[ move.to
			 p.board[ move.to ] == 0 &&
			 p.board[ )
		{
			if ( p.board[ move.to ] == 0 &&
			if ( p.board[ move.from ] == player ? PAWN : -PAWN )
			{
						// En-passant for black
						if ( piece < 0 && (b[move_offset-1] == 1) && move_offset >= 30 && move_offset < 40 )
						{
							piece_moves[count++] = move_offset;
						} else
						// En-passant for white
						if ( move_offset >= 40 && move_offset < 48 ) // fifth row
						{
						}
			}*/

		int nMoves = calcPieceMoves( p, move.from );

		for ( int i = 0; i < nMoves && !bOK ; i++ )
		{
			if ( move.to == piece_moves[i] )
			{
				bOK = true;
			}
		}
		if ( !bOK )
			return false;

		ChessPosition np = new ChessPosition( p );
		np.makeMove( move );

		calcPossibleMoves(np,!player);

		if ( player ? np.bWhiteChecked : np.bBlackChecked )
		{
			return false;
		}
		return true;
	}

/**
 * Initiates the alphaBetaHelper function and handles iterative deepening.
 */
	protected ChessMove alphaBeta(int depth, ChessPosition p, boolean player)
	{
		nodeCount	= 0;
		reachedDepth= 0;

		// Find out if anyone is currently in check (this is kinda skipped by 0-level a-b)
		//calcPossibleMoves( p, player );

		if ( maxDepth > 3 && bIterativeDeepening )
		{
			int prevDepth = maxDepth;
			maxDepth = 3;

			float beta = 100000.0f;

			for ( int md = 3; md < prevDepth+1; md++ )
			{
				maxDepth = md;

				beta = alphaBetaHelper(depth, p, player, 100000.0f, -beta);
				if ( !bThinking )
					break;
			}
//			int nodeCount
			maxDepth = prevDepth;
			bestMoveEval = beta;
		} else
		{
			bestMoveEval = alphaBetaHelper(depth, p, player, 100000.0f, -100000.0f);
		}

		return bestMove;
	}

/**
 * An integer to display the maximum depth reached while searching for a move.
 */
	public static int reachedDepth = 0;

	public static ChessMove bestMove = new ChessMove(); // Used to reference the ACTUAL move

	static int localMaxima = 0;

/**
 * This function is the core of Chessmate.  It does all the node searching by
 * recursively calling itself and storing the best move.
 */
 	public static ChessMove localBestMove = null;

	public static boolean bCheck = false;

	public static ChessMove [] principalVariation = new ChessMove[16]; // there will under no circumstances be more than 16 moves in there :)


	protected float alphaBetaHelper(int depth, ChessPosition p, boolean player, float alpha, float beta)
	{
 		/**
		  * This used to decrease speed by a magnitude of 5,
		  * but we're optimizing it by only searching for checks if we're already in check,
		  * thus finding only mates.  Checks will register as 'king takes' and will count as such.
		  */

		int num = 0;

		/**
		 * First, check whether the other player is in check due to previous move
		 */
		if ( player ? p.bBlackChecked : p.bWhiteChecked )
		{
			num = calcPossibleMoves(p, player);

			/**
			 * If the player making the move is still checked - he is effectively mated,
			 * which must be avoided at all costs.  This is worse if it was his turn.
			 * If he is not checked any more, that is OK.  If
			 */

			if ( player ? p.bBlackChecked : p.bWhiteChecked )// && !(player ? p.bWhiteChecked : p.bBlackChecked) )
			{
				/**
				 * This forces the side to move out of check, despite seeing a riposting check(mate).
				 */
				if ( depth == 2 )
				{
					return (maxDepth-depth+localMaxima+1) * 2000;
				}
				//System.out.println("Detected checkmate.");
				return (maxDepth-depth+localMaxima+1) * 1000; // +1 so it's never 0;
			}// else
			//	return (maxDepth-depth+localMaxima+1) * -2000; // +1 so it's never 0;
		}

		if ( depth > reachedDepth )
			reachedDepth = depth;

		// Have we reached our maximum depths?
		if ( ( depth - localMaxima >= maxDepth ) || !bThinking ) // Test if we have reached our maximum depth and cut-off here
		{
			return positionEvaluation( p, player);
		}

		if ( num == 0 ) // we're hoping this isn't a stalemate :)
			num = calcPossibleMoves(p, player);

		float value = 0;

		ChessMove [] chessMove = new ChessMove[num];// = new ChessMove[num];

		int nMoves = num - nPossibleCaptures;
		int i;

		for ( i = 0; i < nPossibleCaptures; i++)
		{
			chessMove[ i ] = new ChessMove( possibleCaptures[i] );
		}

		for ( i = 0; i < nMoves ; i++ )
		{
			chessMove[nPossibleCaptures + i] = new ChessMove( possibleMoveList[i] );
		}

		int prevToVal = 0; // the previous value of the to block
		int prevFromVal = 0;

	    boolean bWhiteKingMoved;// = false;
	    boolean bBlackKingMoved;// = false;

		for ( i = 0; i < num; i++ ) // Iterate through moves and recursively call self
		{
			// If we are taking a piece, make sure to search ahead so that it's not bogus!
			if ( p.board[chessMove[i].to] != 0 || p.board[chessMove[i].from] == ( player ? ChessPosition.PAWN : -ChessPosition.PAWN ) )
			{
				localMaxima = 2;
			}

			/**
			 * Make the move
			 */

			bWhiteKingMoved = p.bWhiteKingMoved;
			bBlackKingMoved = p.bBlackKingMoved;

			prevFromVal = p.board[chessMove[i].from];
			prevToVal = p.board[chessMove[i].to];

			p.makeMove ( chessMove[i] );

			++nodeCount; // A diagnostic, counting the positions we have searched.

			value = -alphaBetaHelper(depth + 1, p, !player, -beta, -alpha);

			/**
			 * Unmake the move
			 */
			p.board[ chessMove[i].from ] = prevFromVal;
			p.board[ chessMove[i].to ] = prevToVal;
			p.bWhiteKingMoved = bWhiteKingMoved;
			p.bBlackKingMoved = bBlackKingMoved;

			if ( localMaxima != 0 )
				localMaxima = 0;

			if ( value >= alpha )
				return alpha;

			if ( value > beta )
			{
				beta = value;
				principalVariation[depth].from = chessMove[i].from;
				principalVariation[depth].to = chessMove[i].to;

				if ( depth == 0 )
					bestMove = chessMove[i];
			}

		}

		return beta;
	}

/*
	protected Vector alphaBetaHelper(int depth, ChessPosition p, boolean player, float alpha, float beta)
	{

		float value;
		if ( reachedMaxDepth( p, depth - localMaxima) ) // Test if we have reached our maximum depth and cut-off here
		{
			Vector v = new Vector(2);

			v.addElement(new Float(positionEvaluation(p, player)));
			v.addElement(null);

			return v;
		}

		if ( depth > reachedDepth )
			reachedDepth = depth;

		++nodeCount; // A counter for the amount of positions we have searched.

		Vector best = new Vector(); // Contains a list of the best moves so far

		int num = calcPossibleMoves(p, player);

		if ( num == 0 )
		{
			System.out.println("Stalemate");
		}

		ChessMove [] chessMove = new ChessMove[num];
		for ( int nMoves = 0; nMoves < num; nMoves++)
		{
			chessMove[nMoves] = new ChessMove( possibleMoveList[nMoves] );
		}

		ChessPosition [] chessPos = new ChessPosition[num];

	//	int localMaxima;

		for ( int i = 0; i < num; i++ ) // Iterate through moves and recursively call self
		{
			chessPos[i] = new ChessPosition( p );

			// If we are taking a piece, make sure to search ahead so that it's not bogus!
			if ( chessMove[i].to != 0 )
				localMaxima = 1;

			chessPos[i].makeMove ( chessMove[i] );

			Vector v2 = alphaBetaHelper(depth + 1, chessPos[i], !player, -beta, -alpha);

			if ( localMaxima != 0 )
				localMaxima = 0;

			value = -((Float)v2.elementAt(0)).floatValue();

			if ( value > beta )
			{
				beta = value;
				best = new Vector();
				best.addElement(chessMove[i]);

				Enumeration enum = v2.elements();

				enum.nextElement(); // skip first value

				while ( enum.hasMoreElements() )
				{
					Object o = enum.nextElement();
					if ( o != null )
						best.addElement(o);
				}
			}

			// Alpha-beta Cut-off point, decreases search time by not expanding "bad" moves
			if (beta >= alpha)
				break;

		}

		Vector v3 = new Vector();

		v3.addElement( new Float(beta) );

		Enumeration enum = best.elements();

		while ( enum.hasMoreElements() )
		{
			v3.addElement( enum.nextElement() );
		}

		return v3;
	}
*/
/**
 * A temporary varialbe used to display the score in the info panel and to draw
 * the score graph.
 */
	public static float bestMoveEval; // used for display purposes

/**
 * Invokes AI Move Computation.  Calculates the best move and makes it.
 */
	public ChessPosition playGame(ChessPosition startingPosition, boolean bAsPlayer )
	{
		// Let's clear the principal variation
		for ( int i = 0; i < 16; i++ )
		{
			principalVariation[i].from = 0;
			principalVariation[i].to = 0;
		}

		workPos = new ChessPosition( startingPosition );

		bestMove = null;

		// First, build a vector of "good" possible positions
		// we're using iterative deepening to first a get a feel for a good move
		bestMove = alphaBeta(0, workPos, bAsPlayer);

		if ( !bThinking )
			return startingPosition;

		if ( bestMove == null )
		{
			main.alert("It's a draw", "Stalemate.");
		} else
		{
			main.playerMoved( Chess.PROGRAM, bestMove );
			startingPosition.makeMove(bestMove);
		}

		while (true) // a dummy to avoid silly returns
		{
		// Do some tests about the current position
		if ( wonPosition( startingPosition, PROGRAM ) )
		{
			System.out.println("Program won");
			break;
		}
		if ( wonPosition( startingPosition, HUMAN ) )
		{
			System.out.println("Human won");
			break;
		}
		if (drawnPosition(startingPosition))
		{
			System.out.println("Drawn game");
			break;
		}
			break;
		}

		return startingPosition;

	}

/**
 * Some variables to hold check information
 */
 int sideChecked = 0; // if 0, no side, if 1, white, if -1, black

/**
 * A grand part of Chessmate.  This function analyses a given position and assigns
 * a floating-point value to it, guessing as to the chances of a win arising from that position for a given side.
 * It takes into account material values (the pieces on the board) and calls a special setControlData
 * function to determine board control.  This control value is adjusted and added to the evaluation value,
 * before it is returned to the caller, alphaBetaHelper.
 */
	public float positionEvaluation(ChessPosition p, boolean player)
	{
		ChessPosition pos = p;
		int [] b = pos.board;
		float ret = 0.0f;
		int pieceType = 0;
		float val = 0;
		int i;

		float control = 0;

		// adjust for material:
		for ( int y = 0; y < 8; y++ )
		{
			for ( int x = 0; x < 8; x++ )
			{
				i = y * 10 + x;
				if ( b[i] == 0 )
					continue;

				// Calculate Positional Advantage

				control += humanControl[i];
				control -= computerControl[i];

				if (b[i] < 0)
				{
					if (humanControl[i] > computerControl[i])
					{
						control += value[-b[i]];
					}
				} else
				{
					if (humanControl[i] < computerControl[i])
					{
						control -= value[b[i]];
					}
				}

				// Calculate Material Advantage

				pieceType = b[i];
				if ( pieceType < 0 )
					pieceType = -pieceType;

				val = value[ pieceType ];
				val *= 5; // quadruple material value
				if ( b[i] < 0 )
					val = -val;


				ret += val;
			}
		}

		// Count center squares extra:
		control += humanControl[33] - computerControl[33];
		control += humanControl[34] - computerControl[34];
		control += humanControl[43] - computerControl[43];
		control += humanControl[44] - computerControl[44];

		control *= 0.333;///= 30;//20;
		ret += control;

		// adjust if computer side to move:
		if ( !player )
			ret = -ret;

		return ret;
	}

/**
 * Generates all possible moves for a given piece (square_index) on a given position (pos),
 * storing the moves in piece_moves and returning the number of available moves.
 * This function is extensively called indirectly by the alphaBetaHelper function.
 */
	private int calcPieceMoves(ChessPosition pos, int square_index)
	{
		int [] b = pos.board;
		int piece = b[square_index];
		int piece_type = piece;
		if ( piece_type < 0 )
			piece_type = -piece_type;
		int count = 0, side_index, move_offset, temp, next_square;
		int piece_index = index[piece_type];
		int move_index = pieceMovementTable[piece_index];
		int target = 0;

		if ( piece < 0 )
			side_index = -1;
		else
			side_index = 1;

		int [] control = ( piece < 0 ) ? computerControl : humanControl;

		switch ( piece_type )
		{
		case 0:
		case 7:
			break;

		case ChessPosition.PAWN:
			{
				// Check if pawn can take left
				move_offset = square_index + (side_index * 10) + 1;
				if ( move_offset >= 0 && move_offset < 80 )
				{
					target = b[move_offset];

					if ((piece > 0 && target < 0 && target != 7 ) ||
						(piece < 0 && target > 0 && target != 7 ))
					{
						piece_moves[count++] = move_offset;
						control[move_offset] += 12;

						/**
						 * Is the pawn checking the player?
						 */
						if ( target == pos.KING )
						 	pos.bWhiteChecked = true;
						else
						if ( target == -pos.KING )
							pos.bBlackChecked = true;
					}
				}

				// Can pawn take to the right?
				move_offset = square_index + (side_index * 10) - 1;
				if ( move_offset >= 0 && move_offset < 80 )
				{
					target = b[move_offset];

					if ((piece > 0 && target < 0 && target != 7 ) ||
						(piece < 0 && target > 0 && target != 7 ))
					{
						piece_moves[count++] = move_offset;
						control[move_offset] += 12;

						/**
						 * Is the pawn checking the player?
						 */
						if ( target == pos.KING )
						 	pos.bWhiteChecked = true;
						else
						if ( target == -pos.KING )
							pos.bBlackChecked = true;
					}
				}

				// check for initial pawn move of 2 squares forward:
				move_offset = square_index + (side_index * 20);

				if (move_offset >= 0 && move_offset < 80 )
				{
					temp = (piece > 0) ? 1 : 6;

					if ( b[move_offset] == 0 &&
						 (square_index / 10) == temp &&
						 ((piece < 0 && b[square_index - 10]==0) ||
						 (piece > 0 && b[square_index + 10]==0)))
					{
						piece_moves[count++] = move_offset;
					}
				}

				// try to move forward 1 square:
				move_offset = square_index + side_index * 10;

				if ( move_offset >= 0 && move_offset < 80 )
					if (b[move_offset] == 0)
						piece_moves[count++] = move_offset;

			}
			break;

		default:
			{
				move_index = piece;
				if (move_index < 0)
					move_index = -move_index;
				move_index = index[move_index];

				next_square = square_index + pieceMovementTable[move_index];

			outer:

				while (true)
				{
				inner:

					while (true)
					{
						if ( next_square >= 80	||
							 next_square < 0	||
							 b[next_square] == 7 ) // can't I just % 10 ?
							break inner;

						target = b[next_square];

						// check for piece on the same side:
						if ( (side_index < 0 && target < 0) ||
							 (side_index > 0 && target > 0) )
						 	break inner;

						control[next_square] += 1;

						piece_moves[count++] = next_square;

						/**
						 * Is the opponent in check?
						 */
						if ( target == pos.KING )
						 	pos.bWhiteChecked = true;
						else
						if ( target == -pos.KING )
							pos.bBlackChecked = true;

						if ( target != 0 )
						{
							//System.out.println(square_index + " can attack " + next_square + "(" + b[next_square] + ")");
							break inner;
						}

						// Kings and Knights don't re-iterate their moves because they can only take a single "step"
						if ( piece_type == ChessPosition.KNIGHT ||
					 		 piece_type == ChessPosition.KING )
					 		break inner;

						// Important to advance the move table to cover all directions
						next_square += pieceMovementTable[move_index];
					}

					++move_index;

					if ( pieceMovementTable[move_index] == 0 )
						break outer;

					next_square = square_index + pieceMovementTable[move_index];
				}
			}
		}
		return count;
	}

/**
 * When the user decides to take back a move (or two), this function handles restoring
 * the board to its previous state in time and refreshing the move list and graph.
 */
	void Takeback()
	{
		if ( !boardHistory.empty() )
		{
			pos = new ChessPosition( (ChessPosition)boardHistory.pop() );
		}
	}

/**
 * The following routine cycles through all the pieces on the board, calculating moves for each piece
 */

 	public static int nPossibleCaptures = 0;

	public int calcPossibleMoves(ChessPosition pos, boolean player)
	{
		// Clear our control data, so it can be cleanly manipulated by the calcPiece function
		Arrays.fill(humanControl,0);
		Arrays.fill(computerControl,0);

		int [] b = pos.board;

		pos.bWhiteChecked = false; // we will shortly determine this
		pos.bBlackChecked = false; // we will shortly determine this

		int count = 0;
		nPossibleCaptures = 0;
		int i = 0;

		for ( int y = 0; y < 8; y++ )
			for ( int x = 0; x < 8; x++ )
			{
				i = y * 10 + x;

				if ( b[i] == 0 )
					continue;

				int board_val = b[i];

				int num = calcPieceMoves(pos, i);
				if ( board_val < 0 && !player || board_val > 0 && player )
				{

					for (int j = 0; j < num; j++ )
					{
						// if it's a capture add it to the capture list, to be evaluated first.
						if ( b[ piece_moves[j] ] != 0 ||
							 b[i] == ( player ? ChessPosition.PAWN : -ChessPosition.PAWN ) ||
							 piece_moves[j] == 33 || piece_moves[j] == 34 ||
							 piece_moves[j] == 43 || piece_moves[j] == 44 )
						{
							possibleCaptures[nPossibleCaptures].from = i;
							possibleCaptures[nPossibleCaptures].to = piece_moves[j];
							++nPossibleCaptures;
						} else
					//	if ( b[ piece_moves[j] ] == 0 && (b[i] != PAWN && b[i] != -PAWN) )
						{
							possibleMoveList[count].from = i;
							possibleMoveList[count].to = piece_moves[j];
							++count;
						}
					}
				}

		}

		/**
		 * Now that we have generated all possible moves, sort the moves.
		 * We will put captures first.
		 */
		 //if ( count > 0 )
		 //	Arrays.sort( possibleMoveList, possibleMoveList[0] );


		return count + nPossibleCaptures;
	}

/**
 * NewGame() clears the board history and resets the playing area.
 */
	public void NewGame()
	{
		boardHistory.clear();
		// Reset the board to the traditional chess starting position
		pos = new ChessPosition();

		System.arraycopy( initialBoard, 0, pos.board, 0, 80 );

		workPos = new ChessPosition();
		System.arraycopy( initialBoard, 0, workPos.board, 0, 80 );
	}

/**
 * Calles NewGame() and initialises the temoporary piece move lists.
 */
	public Chess(Main main)
	{
		// Allocate temporary possibleMoveList for AI iteration
		for ( int i = 0; i < 256; i++)
			possibleMoveList[i] = new ChessMove();

		// Allocate temporary captureList for AI iteration - better Alpha-Beta move-sorting
		for ( int i = 0; i < 256; i++)
			possibleCaptures[i] = new ChessMove();

		// Allocate space for a principal variation list, existing mainly for visual display
		for ( int i = 0; i < 16; i++)
			principalVariation[i] = new ChessMove();

		this.main = main;
		NewGame();
	}

/**
 * Data to store board control for each player, used when evaluating board position
 * This data is static that can be re-used (assume single threading!)
 * Used by setControlData
*/
	static private int [] computerControl = new int[80];
/**
 * Data to store board control for each player, used when evaluating board position
 * This data is static that can be re-used (assume single threading!)
 * Used by setControlData
*/
	static private int [] humanControl	= new int[80];

/**
 * This function calculates positional advantage for control of the chess board.
 * It also eats up most of our CPU time.
 */
	private static void setControlData(ChessPosition pos)
	{
		for ( int i = 0; i < 80; i++ )
		{
			computerControl[i] = 0;
			humanControl[i]		= 0;
		}
		int [] b = pos.board;
		int [] control; // set to computerControl or humanControl, as appropriate

		for ( int square_index = 0; square_index < 80; square_index++ )
		{
			int piece = b[square_index];

			if ( piece == 7 || piece == 0 )
				continue;
			int piece_type = piece;

			if ( piece_type < 0 )
			{
				piece_type = -piece_type;
				control = computerControl;
			} else
			{
				control = humanControl;
			}
			int count = 0, side_index, move_offset, temp, next_square;
			int piece_index = index[piece_type];
			int move_index = pieceMovementTable[piece_index];
			if ( piece < 0 )
				side_index = -1;
			else		   side_index = 1;
			switch (piece_type) {
			case ChessPosition.PAWN:
				{
					// first check for possible pawn captures:
					for (int delta=-1; delta<= 1; delta += 2)
					{
						move_offset = square_index + side_index * 10 + delta;
						if ( move_offset > 0 && move_offset < 80 )
						{
							int target = b[move_offset];
							if ((target <= -1 && target != 7 && piece > 0) ||
								(target >= 1 && target != 7 && piece < 0)) {
								// kluge: count pawn control more:
								control[square_index + side_index * delta] += 12;
							}
						}
					}
				}
				// Note: no break here: we want pawns to use move table also:
			case ChessPosition.KNIGHT:
			case ChessPosition.BISHOP:
			case ChessPosition.ROOK:
			case ChessPosition.KING:
			case ChessPosition.QUEEN:
				{
					move_index = piece; if (move_index < 0) move_index = -move_index;
					move_index = index[move_index];

					next_square = square_index + pieceMovementTable[move_index];
				outer:
					while (true)
					{
					inner:
						while (true)
						{
							if ( next_square > 79 )
								break inner;
							if ( next_square < 0 )
								break inner;
							if (b[next_square] == 7)
								break inner;
							control[next_square] += 1;

							// the next statement should be augmented for x-ray analysis:
							if ( side_index < 0 && b[next_square] < 0 )
								break inner;
							if ( side_index > 0 && b[next_square] > 0 && b[next_square] != 7)
								break inner;

							// NOTE: prevents calculating guarding:
							//if (b[next_square] != 0) break inner;

							if ( piece_type == ChessPosition.PAWN &&
								(square_index / 10 == 3) )
								break inner;
							if ( piece_type == ChessPosition.KNIGHT )
								break inner;
							if ( piece_type == ChessPosition.KING )
								break inner;

							next_square += pieceMovementTable[move_index];
						}
						move_index += 1;
						if ( pieceMovementTable[move_index] == 0 )
							break outer;
						next_square = square_index + pieceMovementTable[move_index];
					}
				}
			}
		}
	}

/**
 * An index for determining the text values of pieces for position encoding/decoding.
 * @see encodePosition
 * @see decodePosition
 */
	final static char [] pieceChars = {
		'p','n','b','r','q','k'
	};

/**
 * Determines the appropriate text character for a given piece.  For example,
 * the value 6, returns a 'k' for king, or if -6, a 'K' for a black king.  This is
 * used when encoding/decoding positions for storing and retrieving from database.
 * @see encodePosition
 * @see decodePosition
 */
	public static char getPieceChar( int square_index )
	{
		if ( square_index == 0 )
			return '0';
		if ( square_index > 0 )
			return pieceChars[square_index-1];

		square_index = -square_index;

		return Character.toUpperCase(pieceChars[square_index-1]);
	}

/**
 * Takes a position stored in a string and converts it to a ChessPosition for retrieving
 * old games from database.
 */
	public static ChessPosition decodePosition( String s )
	{
		ChessPosition p = new ChessPosition();
		int index = 0;
		for ( int y = 0; y < 8; y++ )
		{
			for (int x = 0; x < 10; x++ )
			{
				index = y * 10 + x;
				if ( x > 7 )
					p.board[index] = 7;
				else
				{
					int str_index = y * 8 + x;

					char c = s.charAt( str_index );
					if ( c == '0' )
					{
						p.board[ index ] = 0;
						continue;
					}
					for ( int k = 0; k < 6; k++ )
						if ( Character.toLowerCase(c) == pieceChars[k] )
							p.board[ index ] = k+1;

					// Adjust for black pieces
					if ( Character.isUpperCase(c) )
						p.board[index] = -p.board[index];

				}
			}
		}

		return p;
	}

/**
 * Takes a ChessPosition class and encodes it as a string for easy storing in database.
 */
	public static String encodePosition(ChessPosition p)
	{
		String str = new String();
		for ( int y = 0; y < 8; y++ )
		{
			for (int x = 0; x < 8; x++ )
			{
				str += getPieceChar( p.board[ y*10 + x ] );
			}
		}
		return str;
	}
}