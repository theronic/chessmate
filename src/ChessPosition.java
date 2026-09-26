package Chess;

/** The original 8 by 10 board, including the state needed for special moves. */
public class ChessPosition
{
    public static final int BLANK = 0, PAWN = 1, KNIGHT = 2, BISHOP = 3,
                            ROOK = 4, QUEEN = 5, KING = 6;
    public static final int WHITE_KINGSIDE = 1, WHITE_QUEENSIDE = 2,
                            BLACK_KINGSIDE = 4, BLACK_QUEENSIDE = 8;

    public int[] board = new int[80];
    public boolean whiteToMove = true;
    public int castlingRights = 0;
    public int enPassantSquare = -1;
    boolean bWhiteKingMoved, bBlackKingMoved;
    boolean bWhiteChecked, bBlackChecked;

    public ChessPosition()
    {
        for (int y = 0; y < 8; y++) {
            board[y * 10 + 8] = 7;
            board[y * 10 + 9] = 7;
        }
    }

    public ChessPosition(ChessPosition p)
    {
        System.arraycopy(p.board, 0, board, 0, 80);
        whiteToMove = p.whiteToMove;
        castlingRights = p.castlingRights;
        enPassantSquare = p.enPassantSquare;
        bWhiteKingMoved = p.bWhiteKingMoved;
        bBlackKingMoved = p.bBlackKingMoved;
        bWhiteChecked = p.bWhiteChecked;
        bBlackChecked = p.bBlackChecked;
    }

    /** Callers validate moves before applying them. An omitted promotion means queen. */
    public void makeMove(ChessMove move)
    {
        make(move);
    }

    /** Save all changed squares and state, so probes and search can undo exactly. */
    Undo make(ChessMove move)
    {
        Undo undo = new Undo(this, move);
        int piece = board[move.from];
        int side = piece > 0 ? 1 : -1;
        int kind = Math.abs(piece);

        if (kind == PAWN && move.to == enPassantSquare && board[move.to] == 0
                && move.from % 10 != move.to % 10) {
            undo.captureSquare = move.to - side * 10;
            undo.captured = board[undo.captureSquare];
            board[undo.captureSquare] = 0;
        }

        // A rook's right is lost when it moves or is captured, never regained.
        castlingRights &= ~(rookRight(move.from) | rookRight(move.to));
        if (kind == KING) {
            if (side > 0) {
                bWhiteKingMoved = true;
                castlingRights &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
            } else {
                bBlackKingMoved = true;
                castlingRights &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
            }
            if (Math.abs(move.to - move.from) == 2) {
                int rank = move.from / 10 * 10;
                undo.rookFrom = rank + (move.to < move.from ? 0 : 7);
                undo.rookTo = (move.from + move.to) / 2;
                undo.rook = board[undo.rookFrom];
                undo.rookTarget = board[undo.rookTo];
                board[undo.rookTo] = board[undo.rookFrom];
                board[undo.rookFrom] = 0;
            }
        }

        board[move.from] = 0;
        board[move.to] = piece;
        if (kind == PAWN && (move.to / 10 == 0 || move.to / 10 == 7))
            board[move.to] = side * (move.promotion == 0 ? QUEEN : move.promotion);

        enPassantSquare = kind == PAWN && Math.abs(move.to - move.from) == 20
                ? (move.from + move.to) / 2 : -1;
        whiteToMove = side < 0;
        bWhiteChecked = bBlackChecked = false;
        return undo;
    }

    void unmake(ChessMove move, Undo undo)
    {
        board[move.from] = undo.piece;
        board[move.to] = undo.target;
        board[undo.captureSquare] = undo.captured;
        if (undo.rookFrom >= 0) {
            board[undo.rookFrom] = undo.rook;
            board[undo.rookTo] = undo.rookTarget;
        }
        castlingRights = undo.rights;
        enPassantSquare = undo.enPassant;
        whiteToMove = undo.turn;
        bWhiteKingMoved = undo.whiteMoved;
        bBlackKingMoved = undo.blackMoved;
        bWhiteChecked = undo.whiteChecked;
        bBlackChecked = undo.blackChecked;
    }

    private static int rookRight(int square)
    {
        switch (square) {
            case 0: return WHITE_KINGSIDE;
            case 7: return WHITE_QUEENSIDE;
            case 70: return BLACK_KINGSIDE;
            case 77: return BLACK_QUEENSIDE;
            default: return 0;
        }
    }

    static class Undo
    {
        final int piece, target, rights, enPassant;
        final boolean turn, whiteMoved, blackMoved, whiteChecked, blackChecked;
        int captureSquare, captured, rookFrom = -1, rookTo, rook, rookTarget;

        Undo(ChessPosition p, ChessMove move) {
            piece = p.board[move.from];
            target = p.board[move.to];
            captureSquare = move.to;
            captured = target;
            rights = p.castlingRights;
            enPassant = p.enPassantSquare;
            turn = p.whiteToMove;
            whiteMoved = p.bWhiteKingMoved;
            blackMoved = p.bBlackKingMoved;
            whiteChecked = p.bWhiteChecked;
            blackChecked = p.bBlackChecked;
        }
    }
}
