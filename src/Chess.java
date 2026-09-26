package Chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Chess
{
    public static Main main;
    public static final boolean DEBUG = false;
    public static final boolean WHITE = true, BLACK = false;
    public static boolean HUMAN = WHITE, PROGRAM = BLACK;
    public boolean bWhoseTurn = WHITE;
    public boolean bIterativeDeepening = true;
    public static volatile boolean bThinking = false;
    public static volatile int nodeCount, reachedDepth;
    public static int maxDepth = 5;
    public static final int MATE = 600000;
    private static final int INFINITY = 2000000;
    private static final int MAX_PLY = 64;
    private static final int QUIET_DEPTH = 8;

    public static Stack<ChessPosition> boardHistory = new Stack<ChessPosition>();
    public static ChessPosition pos = new ChessPosition();
    public static volatile ChessPosition workPos = new ChessPosition();
    public static ChessMove bestMove;
    public static int bestMoveEval;
    public static volatile ChessMove[] principalVariation = new ChessMove[0];
    private final ChessMove[][] lines = new ChessMove[MAX_PLY + 1][];
    private ChessMove iterationMove;

    private static final int[] initialBoard = {
         4, 2, 3, 6, 5, 3, 2, 4, 7, 7,
         1, 1, 1, 1, 1, 1, 1, 1, 7, 7,
         0, 0, 0, 0, 0, 0, 0, 0, 7, 7,
         0, 0, 0, 0, 0, 0, 0, 0, 7, 7,
         0, 0, 0, 0, 0, 0, 0, 0, 7, 7,
         0, 0, 0, 0, 0, 0, 0, 0, 7, 7,
        -1,-1,-1,-1,-1,-1,-1,-1, 7, 7,
        -4,-2,-3,-6,-5,-3,-2,-4, 7, 7
    };

    // Keep the 2005 movement order and piece values.
    private static final int[] index = {0, 12, 15, 10, 1, 6, 6};
    private static final int[] pieceMovementTable = {
        0, -1, 1, 10, -10, 0,
        -1, 1, 10, -10, -9, -11, 9, 11, 0,
        8, -8, 12, -12, 19, -19, 21, -21, 0,
        10, 20, 0
    };
    private static final int[] value = {0, 100, 300, 320, 500, 900, 30000};
    private static final int[] knightSteps = {8, -8, 12, -12, 19, -19, 21, -21};
    private static final int[] kingSteps = {-1, 1, 10, -10, -9, -11, 9, 11};
    private static final int[] promotions = {
        ChessPosition.QUEEN, ChessPosition.ROOK, ChessPosition.BISHOP, ChessPosition.KNIGHT
    };

    static boolean onBoard(int square)
    {
        return square >= 0 && square < 80 && square % 10 < 8;
    }

    /** Attack geometry includes defended pieces and pinned attackers. */
    static boolean attacked(int[] board, int square, boolean byWhite)
    {
        int side = byWhite ? 1 : -1;
        for (int delta : new int[] {-1, 1}) {
            int from = square - side * 10 + delta;
            if (onBoard(from) && board[from] == side * ChessPosition.PAWN) return true;
        }
        for (int step : knightSteps) {
            int from = square + step;
            if (onBoard(from) && board[from] == side * ChessPosition.KNIGHT) return true;
        }
        for (int i = 0; i < kingSteps.length; i++) {
            int step = kingSteps[i];
            int from = square + step;
            if (onBoard(from) && board[from] == side * ChessPosition.KING) return true;
            while (onBoard(from) && board[from] == 0) from += step;
            if (onBoard(from) && (board[from] == side * ChessPosition.QUEEN
                    || board[from] == side * (i < 4 ? ChessPosition.ROOK : ChessPosition.BISHOP)))
                return true;
        }
        return false;
    }

    static boolean kingAttacked(int[] board, boolean white)
    {
        int king = white ? ChessPosition.KING : -ChessPosition.KING;
        for (int sq = 0; sq < 80; sq++) {
            if (onBoard(sq) && board[sq] == king) return attacked(board, sq, !white);
        }
        return true;
    }

    /** Compute each side's control from this board, independently of move generation. */
    static int[][] controlData(ChessPosition p)
    {
        int[][] control = new int[2][80];
        for (int from = 0; from < 80; from++) {
            if (!onBoard(from) || p.board[from] == 0) continue;
            int piece = p.board[from], kind = Math.abs(piece);
            int side = piece > 0 ? 1 : -1;
            int[] counts = control[piece > 0 ? 0 : 1];
            if (kind == ChessPosition.PAWN) {
                for (int delta : new int[] {-1, 1}) {
                    int to = from + side * 10 + delta;
                    if (onBoard(to)) counts[to] += 12;
                }
            } else {
                for (int i = index[kind]; pieceMovementTable[i] != 0; i++) {
                    int step = pieceMovementTable[i];
                    for (int to = from + step; onBoard(to); to += step) {
                        counts[to]++;
                        if (p.board[to] != 0 || kind == ChessPosition.KNIGHT
                                || kind == ChessPosition.KING) break;
                    }
                }
            }
        }
        return control;
    }

    /** The original heuristic in centipawns: a pawn contributes 100 in material. */
    public int positionEvaluation(ChessPosition p, boolean player)
    {
        int[][] counts = controlData(p);
        int[] white = counts[0], black = counts[1];
        int material = 0, control = 0;
        for (int sq = 0; sq < 80; sq++) {
            if (!onBoard(sq) || p.board[sq] == 0) continue;
            int piece = p.board[sq];
            // Keep hundredths of the old control units until the final weighting.
            control += 100 * (white[sq] - black[sq]);
            if (piece < 0 && white[sq] > black[sq]) control += value[-piece];
            if (piece > 0 && white[sq] < black[sq]) control -= value[piece];
            material += piece > 0 ? value[piece] : -value[-piece];
        }
        control += 100 * (white[33] - black[33]);
        control += 100 * (white[34] - black[34]);
        control += 100 * (white[43] - black[43]);
        control += 100 * (white[44] - black[44]);
        // Old material was multiplied by 5, so one old score unit is 20 cp.
        // control / 100 * 0.333 * 20 = control * 333 / 5000.
        // Round once, with halves away from zero, so colour symmetry is exact.
        int weightedControl = control * 333;
        int score = material + (weightedControl + (weightedControl >= 0 ? 2500 : -2500)) / 5000;
        return player ? score : -score;
    }

    private void addMove(List<ChessMove> moves, ChessPosition p, int from, int to)
    {
        if (Math.abs(p.board[from]) == ChessPosition.PAWN && (to / 10 == 0 || to / 10 == 7)) {
            for (int promotion : promotions) moves.add(new ChessMove(from, to, promotion));
        } else {
            moves.add(new ChessMove(from, to));
        }
    }

    private List<ChessMove> pseudoMoves(ChessPosition p, boolean player)
    {
        List<ChessMove> moves = new ArrayList<ChessMove>();
        int side = player ? 1 : -1;
        for (int from = 0; from < 80; from++) {
            if (!onBoard(from) || p.board[from] * side <= 0) continue;
            int kind = Math.abs(p.board[from]);
            if (kind == ChessPosition.PAWN) {
                int ahead = from + side * 10;
                for (int delta : new int[] {1, -1}) {
                    int to = ahead + delta;
                    if (!onBoard(to)) continue;
                    boolean capture = p.board[to] * side < 0
                            && Math.abs(p.board[to]) != ChessPosition.KING;
                    boolean enPassant = player == p.whiteToMove && to == p.enPassantSquare
                            && from / 10 == (player ? 4 : 3) && p.board[to] == 0
                            && p.board[to - side * 10] == -side * ChessPosition.PAWN;
                    if (capture || enPassant) addMove(moves, p, from, to);
                }
                if (onBoard(ahead) && p.board[ahead] == 0) {
                    int twice = ahead + side * 10;
                    if (from / 10 == (player ? 1 : 6) && p.board[twice] == 0)
                        addMove(moves, p, from, twice);
                    addMove(moves, p, from, ahead);
                }
            } else {
                for (int i = index[kind]; pieceMovementTable[i] != 0; i++) {
                    int step = pieceMovementTable[i];
                    for (int to = from + step; onBoard(to); to += step) {
                        int target = p.board[to];
                        if (target * side > 0 || Math.abs(target) == ChessPosition.KING) break;
                        addMove(moves, p, from, to);
                        if (target != 0 || kind == ChessPosition.KING || kind == ChessPosition.KNIGHT) break;
                    }
                }
                if (kind == ChessPosition.KING) addCastles(moves, p, from, player);
            }
        }
        // Preserve the original capture, pawn and centre ordering.
        List<ChessMove> ordered = new ArrayList<ChessMove>(moves.size());
        for (ChessMove m : moves) if (priority(p, m)) ordered.add(m);
        for (ChessMove m : moves) if (!priority(p, m)) ordered.add(m);
        return ordered;
    }

    private static boolean priority(ChessPosition p, ChessMove m)
    {
        return p.board[m.to] != 0 || Math.abs(p.board[m.from]) == ChessPosition.PAWN
                || m.to == 33 || m.to == 34 || m.to == 43 || m.to == 44;
    }

    private void addCastles(List<ChessMove> moves, ChessPosition p, int from, boolean white)
    {
        int rank = white ? 0 : 70;
        int side = white ? 1 : -1;
        if (from != rank + 3 || (white ? p.bWhiteKingMoved : p.bBlackKingMoved)
                || attacked(p.board, from, !white)) return;
        int kingside = white ? ChessPosition.WHITE_KINGSIDE : ChessPosition.BLACK_KINGSIDE;
        int queenside = white ? ChessPosition.WHITE_QUEENSIDE : ChessPosition.BLACK_QUEENSIDE;
        if ((p.castlingRights & kingside) != 0 && p.board[rank] == side * ChessPosition.ROOK
                && p.board[rank + 1] == 0 && p.board[rank + 2] == 0
                && !attacked(p.board, rank + 2, !white) && !attacked(p.board, rank + 1, !white))
            moves.add(new ChessMove(from, rank + 1));
        if ((p.castlingRights & queenside) != 0 && p.board[rank + 7] == side * ChessPosition.ROOK
                && p.board[rank + 4] == 0 && p.board[rank + 5] == 0 && p.board[rank + 6] == 0
                && !attacked(p.board, rank + 4, !white) && !attacked(p.board, rank + 5, !white))
            moves.add(new ChessMove(from, rank + 5));
    }

    public List<ChessMove> legalMoves(ChessPosition p, boolean player)
    {
        List<ChessMove> legal = new ArrayList<ChessMove>();
        for (ChessMove m : pseudoMoves(p, player)) {
            ChessPosition.Undo undo = p.make(m);
            boolean safe = !kingAttacked(p.board, player);
            p.unmake(m, undo);
            if (safe) legal.add(m);
        }
        return legal;
    }

    public int calcPossibleMoves(ChessPosition p, boolean player)
    {
        p.bWhiteChecked = kingAttacked(p.board, WHITE);
        p.bBlackChecked = kingAttacked(p.board, BLACK);
        return legalMoves(p, player).size();
    }

    boolean isValidMove(ChessPosition p, ChessMove move)
    {
        if (bThinking || move == null || !onBoard(move.from) || !onBoard(move.to)
                || p.board[move.from] == 0 || (p.board[move.from] > 0) != p.whiteToMove) return false;
        for (ChessMove legal : legalMoves(p, p.whiteToMove)) {
            if (move.from == legal.from && move.to == legal.to
                    && (move.promotion == legal.promotion
                        || move.promotion == 0 && legal.promotion == ChessPosition.QUEEN)) return true;
        }
        return false;
    }

    public boolean drawnPosition(ChessPosition p)
    {
        return !kingAttacked(p.board, p.whiteToMove) && legalMoves(p, p.whiteToMove).isEmpty();
    }

    public boolean wonPosition(ChessPosition p, boolean player)
    {
        return p.whiteToMove != player && kingAttacked(p.board, !player)
                && legalMoves(p, !player).isEmpty();
    }

    private static class SearchStopped extends RuntimeException {
        @Override public synchronized Throwable fillInStackTrace() { return this; }
    }

    /** Full-window iterative deepening; publish only completed iterations. */
    protected ChessMove alphaBeta(int depth, ChessPosition p, boolean player)
    {
        if (maxDepth < 1 || maxDepth > MAX_PLY - 12)
            throw new IllegalArgumentException("Search depth must be between 1 and " + (MAX_PLY - 12));
        nodeCount = reachedDepth = 0;
        bestMove = null;
        bestMoveEval = 0;
        principalVariation = new ChessMove[0];
        List<ChessMove> legal = legalMoves(p, player);
        if (legal.isEmpty()) {
            bestMoveEval = kingAttacked(p.board, player) ? -MATE : 0;
            return null;
        }
        bestMove = new ChessMove(legal.get(0));
        bestMoveEval = positionEvaluation(p, player);
        int start = bIterativeDeepening && maxDepth > 3 ? 3 : maxDepth;
        for (int limit = start; limit <= maxDepth; limit++) {
            iterationMove = null;
            try {
                int score = search(p, player, depth, limit, 0, -INFINITY, INFINITY);
                if (!bThinking) break;
                bestMove = iterationMove == null ? bestMove : new ChessMove(iterationMove);
                bestMoveEval = score;
                principalVariation = lines[depth].clone();
            } catch (SearchStopped stopped) {
                break;
            }
        }
        return bestMove;
    }

    private void visit(int ply)
    {
        if (!bThinking) throw new SearchStopped();
        ++nodeCount;
        reachedDepth = Math.max(reachedDepth, ply);
        lines[ply] = new ChessMove[0];
    }

    private void record(int ply, ChessMove move)
    {
        ChessMove[] child = lines[ply + 1];
        ChessMove[] line = new ChessMove[child.length + 1];
        line[0] = new ChessMove(move);
        System.arraycopy(child, 0, line, 1, child.length);
        lines[ply] = line;
        if (ply == 0) iterationMove = move;
    }

    private int search(ChessPosition p, boolean player, int ply, int limit,
                       int extension, int alpha, int beta)
    {
        visit(ply);
        List<ChessMove> moves = legalMoves(p, player);
        if (moves.isEmpty()) return kingAttacked(p.board, player) ? -MATE + ply : 0;
        if (ply - extension >= limit)
            return quiescence(p, player, ply, 0, alpha, beta, moves);
        for (ChessMove move : moves) {
            // The original two-ply extension belongs to this branch only.
            int childExtension = p.board[move.to] != 0
                    || Math.abs(p.board[move.from]) == ChessPosition.PAWN ? 2 : 0;
            ChessPosition.Undo undo = p.make(move);
            int score;
            try {
                score = -search(p, !player, ply + 1, limit, childExtension, -beta, -alpha);
            } finally {
                p.unmake(move, undo);
            }
            if (score > alpha) {
                alpha = score;
                record(ply, move);
            }
            if (alpha >= beta) break;
        }
        return alpha;
    }

    /** Finish exchanges at the horizon; a checked side must try its legal evasions. */
    private int quiescence(ChessPosition p, boolean player, int ply, int quietPly,
                           int alpha, int beta, List<ChessMove> moves)
    {
        if (moves.isEmpty()) return kingAttacked(p.board, player) ? -MATE + ply : 0;
        boolean checked = kingAttacked(p.board, player);
        int score = positionEvaluation(p, player);
        // Bound long checking sequences as well as capture sequences.
        if (ply == MAX_PLY || (!checked && quietPly >= QUIET_DEPTH)) return score;
        if (!checked) {
            if (score >= beta) return score;
            alpha = Math.max(alpha, score);
        }
        for (ChessMove move : moves) {
            boolean capture = p.board[move.to] != 0
                    || Math.abs(p.board[move.from]) == ChessPosition.PAWN
                       && move.to == p.enPassantSquare;
            if (!checked && !capture && move.promotion == 0) continue;
            ChessPosition.Undo undo = p.make(move);
            try {
                visit(ply + 1);
                score = -quiescence(p, !player, ply + 1, quietPly + 1, -beta, -alpha,
                        legalMoves(p, !player));
            } finally {
                p.unmake(move, undo);
            }
            if (score > alpha) {
                alpha = score;
                record(ply, move);
            }
            if (alpha >= beta) break;
        }
        return alpha;
    }

    /** Search only. The UI applies the result on its event thread. */
    public ChessPosition playGame(ChessPosition startingPosition, boolean player)
    {
        ChessPosition searchPosition = new ChessPosition(startingPosition);
        workPos = searchPosition;
        alphaBeta(0, searchPosition, player);
        ChessPosition result = new ChessPosition(startingPosition);
        if (bThinking && bestMove != null) result.makeMove(bestMove);
        return result;
    }

    void Takeback()
    {
        if (!boardHistory.empty()) {
            pos = new ChessPosition(boardHistory.pop());
            bWhoseTurn = pos.whiteToMove;
        }
    }

    public void NewGame()
    {
        boardHistory.clear();
        pos = new ChessPosition();
        System.arraycopy(initialBoard, 0, pos.board, 0, 80);
        pos.castlingRights = 15;
        bWhoseTurn = WHITE;
        workPos = new ChessPosition(pos);
        bestMove = null;
        bestMoveEval = 0;
        principalVariation = new ChessMove[0];
    }

    public Chess(Main parent)
    {
        main = parent;
        NewGame();
    }

    static final char[] pieceChars = {'p', 'n', 'b', 'r', 'q', 'k'};

    public static char getPieceChar(int piece)
    {
        if (piece == 0) return '0';
        return piece > 0 ? pieceChars[piece - 1] : Character.toUpperCase(pieceChars[-piece - 1]);
    }

    /** Read legacy board-only records too; they cannot establish special-move rights. */
    public static ChessPosition decodePosition(String text)
    {
        String[] fields = text.split(" ");
        if (fields[0].length() != 64) throw new IllegalArgumentException("Expected 64 board squares");
        ChessPosition p = new ChessPosition();
        for (int i = 0; i < 64; i++) {
            char c = fields[0].charAt(i);
            if (c == '0') continue;
            int piece = "pnbrqk".indexOf(Character.toLowerCase(c)) + 1;
            if (piece == 0) throw new IllegalArgumentException("Invalid piece: " + c);
            p.board[i / 8 * 10 + i % 8] = Character.isUpperCase(c) ? -piece : piece;
        }
        if (fields.length == 4) {
            p.whiteToMove = fields[1].equals("w");
            p.castlingRights = Integer.parseInt(fields[2]);
            p.enPassantSquare = Integer.parseInt(fields[3]);
        }
        return p;
    }

    public static String encodePosition(ChessPosition p)
    {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) text.append(getPieceChar(p.board[y * 10 + x]));
        return text + (p.whiteToMove ? " w " : " b ") + p.castlingRights + " " + p.enPassantSquare;
    }
}
