package Chess;

import java.util.Vector;
import java.util.Enumeration;

public class ChessPosition
{
    final static public int BLANK = 0;
    final static public int PAWN = 1;
    final static public int KNIGHT = 2;
    final static public int BISHOP = 3;
    final static public int ROOK = 4;
    final static public int QUEEN = 5;
    final static public int KING = 6;

/**
 * An array of board squares.
 */
    public int [] board = new int[80];
/**
 * Stores the index of pieces on the board.  Exists for faster move generation.
 */
 //   public Vector pieces = new Vector();

    boolean bWhiteKingMoved = false;
    boolean bBlackKingMoved = false;

    boolean bWhiteChecked = false;
    boolean bBlackChecked = false;

    int enPassantSquare = 0;

/**
 * Applies the given move parameter to the board position saved in this instance of the class.
 */
    public void makeMove(ChessMove move)
    {
   /* 	pieces.remove(move.from);
    	if ( !pieces.contains( new Integer(move.to) ) )
    		pieces.add( new Integer(move.to) );

		pieces.set( pieces.indexOf( new Integer(move.from)) , new Integer(move.to) );
*/

    	board[ move.to ] = board[ move.from ];
		board[ move.from ] = 0;

		if ( move.to >= 70  )
		{
			if ( board[move.to] == PAWN )
				board[move.to] = QUEEN;
		} else
		if ( move.to < 8 )
		{
			if ( board[move.to] == -PAWN )
				board[move.to] = -QUEEN;
		} else
		if ( board[ move.to ] == KING && !bWhiteKingMoved )
		{
			bWhiteKingMoved = true;
		} else
		if ( board[ move.to ] == -KING && !bBlackKingMoved )
		{
			bBlackKingMoved = true;
		}// else
/*		if ( enPassantSquare > 0 )
		{
			if ( board[ move.to ] == PAWN && move.to-10 == enPassantSquare )
			{
				board[move.to-10] = 0;
				enPassantSquare = 0;
			} else
			if ( board[ move.to ] == -PAWN && move.to+10 == enPassantSquare )
			{
				board[move.to+10] = 0;
				enPassantSquare = 0;
			}
		}*/
	}

/**
 * FindPieces() scans the board array and saves the pieces to the pieces vector, after clearing it.
 */
 /*	public Vector FindPieces()
 	{
 		pieces.clear();
 		for ( int y = 0; y < 8; y++ )
 			for ( int x = 0; x < 8; x++ )
 			{
 				int i = y*10+x;
 				if ( board[i] != 0 )
 					pieces.add( new Integer(i) );
 			}
 		return pieces;
 	}*/

/**
 * Instantiates the board position by mirroring another board position.
 * Used extensively during alpha-beta search.
 */
 	public ChessPosition( ChessPosition p )
	{
		System.arraycopy( p.board, 0, board, 0, 80 );
		//eval = p.eval;
		bWhiteKingMoved = p.bWhiteKingMoved;
		bBlackKingMoved = p.bBlackKingMoved;

		bWhiteChecked = p.bWhiteChecked;
		bBlackChecked = p.bBlackChecked;

/*		pieces = new Vector();

		Enumeration e = p.pieces.elements();

		while ( e.hasMoreElements() )
		{
			pieces.add( e.nextElement() );
		}*/
	}

/**
 * Constructs an empty chess board.
 */
    public ChessPosition()
    {
    }
}