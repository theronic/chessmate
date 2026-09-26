package Chess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free regression tests; run with ./build.sh test. */
public class ChessTest
{
    private static final Chess chess = new Chess(null);
    private static int assertions;
    private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    static void check(boolean condition, String message)
    {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    static int square(String name)
    {
        return (name.charAt(1) - '1') * 10 + ('h' - name.charAt(0));
    }

    static ChessPosition fen(String fen)
    {
        String[] parts = fen.split(" ");
        ChessPosition p = new ChessPosition();
        String[] ranks = parts[0].split("/");
        for (int y = 0; y < 8; y++) {
            int file = 0;
            for (char c : ranks[y].toCharArray()) {
                if (Character.isDigit(c)) file += c - '0';
                else {
                    int piece = "pnbrqk".indexOf(Character.toLowerCase(c)) + 1;
                    p.board[(7 - y) * 10 + 7 - file++] = Character.isUpperCase(c) ? piece : -piece;
                }
            }
        }
        p.whiteToMove = parts[1].equals("w");
        String rights = "KQkq";
        for (int i = 0; i < 4; i++) if (parts[2].indexOf(rights.charAt(i)) >= 0) p.castlingRights |= 1 << i;
        p.enPassantSquare = parts[3].equals("-") ? -1 : square(parts[3]);
        return p;
    }

    static ChessMove move(String uci)
    {
        return new ChessMove(square(uci.substring(0, 2)), square(uci.substring(2, 4)),
                uci.length() == 5 ? " pnbrqk".indexOf(uci.charAt(4)) : 0);
    }

    static Set<String> names(ChessPosition p)
    {
        Set<String> names = new HashSet<String>();
        for (ChessMove m : chess.legalMoves(p, p.whiteToMove)) names.add(m.toString());
        return names;
    }

    static void play(ChessPosition p, String uci)
    {
        ChessMove m = move(uci);
        check(chess.isValidMove(p, m), "Legal move: " + uci);
        p.makeMove(m);
    }

    static String state(ChessPosition p)
    {
        return Arrays.toString(p.board) + "/" + p.whiteToMove + "/" + p.castlingRights
                + "/" + p.enPassantSquare + "/" + p.bWhiteKingMoved + "/" + p.bBlackKingMoved
                + "/" + p.bWhiteChecked + "/" + p.bBlackChecked;
    }

    static long perft(ChessPosition p, int depth)
    {
        if (depth == 0) return 1;
        List<ChessMove> moves = chess.legalMoves(p, p.whiteToMove);
        if (depth == 1) return moves.size();
        long nodes = 0;
        for (ChessMove m : moves) {
            ChessPosition.Undo undo = p.make(m);
            nodes += perft(p, depth - 1);
            p.unmake(m, undo);
        }
        return nodes;
    }

    static void restores(ChessPosition p, int depth)
    {
        String before = state(p);
        List<ChessMove> legal = chess.legalMoves(p, p.whiteToMove);
        check(state(p).equals(before), "Move generation preserves all state");
        if (depth == 0) return;
        for (ChessMove m : legal) {
            ChessPosition.Undo undo = p.make(m);
            restores(p, depth - 1);
            p.unmake(m, undo);
            check(state(p).equals(before), "Undo restores " + m);
        }
    }

    static void moveCounts()
    {
        String[] positions = {
            START,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
        };
        long[][] expected = {
            {20, 400, 8902, 197281, 4865609},
            {48, 2039, 97862, 4085603},
            {14, 191, 2812, 43238, 674624},
            {6, 264, 9467, 422333},
            {44, 1486, 62379, 2103487},
            {46, 2079, 89890, 3894594}
        };
        for (int i = 0; i < positions.length; i++) {
            ChessPosition p = fen(positions[i]);
            restores(p, 2);
            int depth = "1".equals(System.getenv("CHESS_DEEP")) ? expected[i].length : (i == 0 || i == 2 ? 4 : 3);
            for (int d = 1; d <= depth; d++) {
                long actual = perft(p, d);
                check(actual == expected[i][d - 1], "Perft position " + (i + 1) + " depth " + d
                        + ": expected " + expected[i][d - 1] + ", got " + actual);
            }
            System.out.println("Perft position " + (i + 1) + " through depth " + depth + " passed");
        }
    }

    static void specialMoves()
    {
        ChessPosition p = fen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        check(names(p).containsAll(Arrays.asList("e1g1", "e1c1")), "Both white castles");
        play(p, "e1g1");
        check(p.board[square("f1")] == 4 && p.board[square("h1")] == 0, "White castling moves rook");
        check(p.castlingRights == 12, "Castling clears both white rights");
        play(p, "e8c8");
        check(p.board[square("d8")] == -4 && p.board[square("a8")] == 0, "Black castling moves rook");

        p = fen("r3kr2/8/8/8/8/8/8/R3K2R w KQq - 0 1");
        check(!names(p).contains("e1g1") && names(p).contains("e1c1"), "No castle through check");
        p = fen("4k3/8/8/8/8/8/6p1/4K2R w K - 0 1");
        check(!names(p).contains("e1g1"), "Pawn attacks empty transit square");
        p = fen("4k3/8/8/8/8/8/8/4K3 w KQ - 0 1");
        check(!names(p).contains("e1g1") && !names(p).contains("e1c1"), "Rook required");
        p = fen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        for (String uci : new String[] {"h1h2", "h8h7", "h2h1", "h7h8"}) play(p, uci);
        check(p.castlingRights == 10, "Returning rooks do not regain rights");
        play(p, "e1f1"); play(p, "e8f8"); play(p, "f1e1"); play(p, "f8e8");
        check(p.castlingRights == 0, "Returning kings do not regain rights");
        p = fen("4k3/8/8/8/8/8/6b1/R3K2R b KQ - 0 1");
        play(p, "g2h1");
        check(p.castlingRights == 2, "Captured rook loses its right");

        p = fen(START);
        for (String uci : new String[] {"e2e4", "a7a6", "e4e5", "d7d5"}) play(p, uci);
        check(p.enPassantSquare == square("d6"), "En passant stores passed square");
        check(state(p).equals(state(new ChessPosition(p))), "Copy preserves en passant");
        restores(p, 2);
        ChessPosition expired = new ChessPosition(p);
        play(expired, "g1f3"); play(expired, "a6a5");
        check(!names(expired).contains("e5d6"), "En passant expires on next move");
        play(p, "e5d6");
        check(p.board[square("d5")] == 0 && p.board[square("d6")] == 1, "White en passant removes pawn");
        p = fen("4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 1");
        restores(p, 2);
        play(p, "e4d3");
        check(p.board[square("d4")] == 0 && p.board[square("d3")] == -1, "Black en passant removes pawn");
        p = fen("8/8/8/KPp4r/8/8/8/7k w - c6 0 1");
        check(!names(p).contains("b5c6"), "En passant cannot expose king");
        p = fen("4k3/8/8/3pP3/4K3/8/8/8 w - d6 0 1");
        check(names(p).contains("e5d6"), "En passant can escape pawn check");
        p = fen("k7/2Q5/8/8/8/8/7p/5K1R b - - 0 1");
        check(chess.drawnPosition(p), "No phantom en passant off h2");
        p = fen("4k3/8/8/4P3/8/8/8/4K3 w - d6 0 1");
        check(!names(p).contains("e5d6"), "En passant requires enemy pawn");

        for (String position : new String[] {
                "8/P7/8/8/8/8/8/k6K w - - 0 1", "K6k/8/8/8/8/8/p7/8 b - - 0 1"}) {
            p = fen(position);
            String prefix = p.whiteToMove ? "a7a8" : "a2a1";
            for (char c : "qrbn".toCharArray()) check(names(p).contains(prefix + c), "Promotion " + c);
            restores(p, 2);
            int sign = p.whiteToMove ? 1 : -1;
            play(p, prefix + "n");
            check(p.board[square(prefix.substring(2))] == sign * 2, "Underpromotion applies");
        }
        p = fen(START);
        check(!chess.isValidMove(p, new ChessMove(-1, 0)), "Reject off-board source");
        check(!chess.isValidMove(p, new ChessMove(10, 80)), "Reject off-board target");
        check(!chess.isValidMove(p, move("e7e5")), "Reject wrong side");
        check(!chess.isValidMove(p, move("e2e4q")), "Reject promotion away from last rank");
    }

    static void evaluation()
    {
        ChessPosition p = fen("4k3/8/8/8/3P4/4N3/8/4K3 w - - 0 1");
        int[][] counts = Chess.controlData(p);
        check(counts[0][square("e5")] == 12 && counts[0][square("c5")] == 12, "Pawns control empty diagonals");
        check(counts[0][square("d5")] == 1, "Pawn does not control its forward square; knight does");
        p = fen("4k3/8/8/8/8/p7/R7/R3K3 w - - 0 1");
        counts = Chess.controlData(p);
        check(counts[0][square("a2")] == 1, "Rook counts defended friendly blocker");
        check(counts[0][square("a4")] == 0, "Rooks stop at enemy blocker");
        p = fen("4k3/8/8/8/8/8/7p/R3K3 b - - 0 1");
        counts = Chess.controlData(p);
        check(counts[1][square("g1")] == 12 && counts[1][square("h1")] == 0, "Pawn geometry at edge");
        float score = chess.positionEvaluation(p, true);
        chess.calcPossibleMoves(fen(START), false);
        chess.positionEvaluation(fen("7k/8/8/8/8/8/8/KQ6 w - - 0 1"), false);
        check(score == chess.positionEvaluation(p, true), "Evaluation independent of prior positions");
        check(score == -chess.positionEvaluation(p, false), "Score changes sign with perspective");
        ChessPosition mirror = new ChessPosition();
        for (int sq = 0; sq < 80; sq++) if (Chess.onBoard(sq))
            mirror.board[(7 - sq / 10) * 10 + sq % 10] = -p.board[sq];
        check(Math.abs(score + chess.positionEvaluation(mirror, true)) < 0.001, "Colour symmetry");
    }

    static ChessMove search(ChessPosition p, int depth, boolean iterative)
    {
        String before = state(p);
        Chess.maxDepth = depth;
        chess.bIterativeDeepening = iterative;
        Chess.bThinking = true;
        ChessMove found = chess.alphaBeta(0, p, p.whiteToMove);
        Chess.bThinking = false;
        check(before.equals(state(p)), "Search preserves position");
        ChessPosition line = new ChessPosition(p);
        for (ChessMove m : Chess.principalVariation) {
            check(chess.isValidMove(line, m), "Principal variation move is legal: " + m);
            line.makeMove(m);
        }
        return found;
    }

    static void searches() throws Exception
    {
        String mate = "7k/8/5K2/8/8/8/8/6Q1 w - - 0 1";
        for (int depth = 1; depth <= 5; depth++) {
            ChessPosition p = fen(mate);
            ChessMove found = search(p, depth, true);
            check(found != null, "Mate move found at depth " + depth);
            p.makeMove(found);
            check(chess.wonPosition(p, true), "Choose mate at depth " + depth + ", got " + found);
            check(Chess.bestMoveEval == Chess.MATE - 1, "Mate distance stable across depths");
        }
        ChessPosition p = fen("2r3k1/5ppp/8/8/8/8/3R1PPP/3R2K1 w - - 0 1");
        check(search(p, 3, true).toString().equals("d2d8"), "Find mate in two");
        check(Chess.bestMoveEval == Chess.MATE - 3, "Mate in two distance");
        p = fen("4k3/8/8/3q4/8/8/8/3QK3 w - - 0 1");
        check(search(p, 2, false).toString().equals("d1d5"), "Take hanging queen");
        p = fen("6k1/5ppp/8/8/8/8/q4PPP/3R2K1 w - - 0 1");
        ChessMove direct = search(p, 4, false);
        float directScore = Chess.bestMoveEval;
        check(search(p, 4, true).toString().equals(direct.toString())
                && Chess.bestMoveEval == directScore, "Iterative and direct search agree");
        p = fen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        check(search(p, 1, false) == null && Chess.bestMoveEval == -Chess.MATE, "Root checkmate clears old move");
        check(chess.wonPosition(p, true), "Win detection");
        p = fen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
        check(search(p, 1, false) == null && Chess.bestMoveEval == 0, "Root stalemate is zero");
        check(chess.drawnPosition(p), "Draw detection");
        // Terminal nodes must be recognized even exactly at the depth cutoff.
        java.lang.reflect.Method helper = Chess.class.getDeclaredMethod("search", ChessPosition.class,
                boolean.class, int.class, int.class, int.class, float.class, float.class);
        helper.setAccessible(true);
        Chess.bThinking = true;
        check((Float) helper.invoke(chess, p, false, 1, 1, 0, -100000f, 100000f) == 0,
                "Stalemate at horizon scores zero");
        ChessPosition mated = fen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        check((Float) helper.invoke(chess, mated, false, 1, 1, 0, -100000f, 100000f) == -Chess.MATE + 1,
                "Checkmate at horizon scores mate");
        Chess.bThinking = false;
        p = fen("4r1k1/8/8/8/8/8/5PPP/3QK3 w - - 0 1");
        ChessMove escape = search(p, 2, false);
        p.makeMove(escape);
        check(!Chess.kingAttacked(p.board, true), "Escape check instead of counter-mating");

        p = fen("6k1/5ppp/8/8/8/8/q4PPP/3R2K1 w - - 0 1");
        ChessMove completed = search(p, 3, false);
        float completedScore = Chess.bestMoveEval;
        String completedLine = Arrays.toString(Chess.principalVariation);
        Chess cancellable = new Chess(null) {
            @Override public float positionEvaluation(ChessPosition position, boolean player) {
                if (Chess.principalVariation.length > 0) Chess.bThinking = false;
                return super.positionEvaluation(position, player);
            }
        };
        Chess.maxDepth = 5;
        Chess.bThinking = true;
        String beforeStop = state(p);
        ChessMove stopped = cancellable.alphaBeta(0, p, p.whiteToMove);
        check(stopped.toString().equals(completed.toString()) && Chess.bestMoveEval == completedScore
                && Arrays.toString(Chess.principalVariation).equals(completedLine),
                "Cancellation keeps the completed iteration's move, score and line");
        check(beforeStop.equals(state(p)), "Partial iteration restores full position");

        // Interrupt a live search, then check state, fallback move and prompt return.
        final ChessPosition longPosition = fen(START);
        final String before = state(longPosition);
        Chess.maxDepth = 8;
        Chess.nodeCount = 0;
        Chess.bThinking = true;
        final Throwable[] failure = new Throwable[1];
        Thread worker = new Thread(() -> {
            try { chess.alphaBeta(0, longPosition, true); }
            catch (Throwable error) { failure[0] = error; }
        });
        worker.start();
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (Chess.nodeCount < 1000 && worker.isAlive() && System.nanoTime() < deadline) Thread.yield();
        Chess.bThinking = false;
        worker.join(2000);
        check(!worker.isAlive() && failure[0] == null, "Cancellation returns promptly");
        check(before.equals(state(longPosition)), "Cancellation restores state");
        check(Chess.bestMove != null && chess.isValidMove(longPosition, Chess.bestMove), "Cancellation has legal fallback");
    }

    static void history()
    {
        chess.NewGame();
        play(Chess.pos, "e2e4");
        ChessPosition saved = new ChessPosition(Chess.pos);
        Chess.boardHistory.push(saved);
        play(Chess.pos, "h7h5");
        chess.Takeback();
        check(state(Chess.pos).equals(state(saved)) && !chess.bWhoseTurn, "Takeback restores turn and special moves");
        ChessPosition decoded = Chess.decodePosition(Chess.encodePosition(saved));
        check(Arrays.equals(decoded.board, saved.board) && decoded.whiteToMove == saved.whiteToMove
                && decoded.castlingRights == saved.castlingRights
                && decoded.enPassantSquare == saved.enPassantSquare, "Saved position preserves rules state");
        decoded = Chess.decodePosition(Chess.encodePosition(saved).substring(0, 64));
        check(decoded.castlingRights == 0 && decoded.enPassantSquare == -1, "Legacy records do not invent rights");
        chess.NewGame();
        check(chess.bWhoseTurn && Chess.pos.whiteToMove && Chess.pos.castlingRights == 15
                && Chess.boardHistory.empty(), "New game resets turn, rights and history");
    }

    public static void main(String[] args) throws Exception
    {
        moveCounts();
        specialMoves();
        evaluation();
        searches();
        history();
        System.out.println("Passed " + assertions + " checks");
    }
}
