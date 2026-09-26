package Chess;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Run after ./build.sh test: java -Xmx512m -cp build Chess.ChessBenchmark eval|search.
 * The optional score multiplier lets the same harness measure the old float engine.
 */
public class ChessBenchmark
{
    private static volatile double result;

    static ChessPosition[] positions(Chess chess)
    {
        ChessPosition[] positions = new ChessPosition[128];
        ChessPosition p = new ChessPosition(Chess.pos);
        Random random = new Random(2005);
        for (int i = 0; i < positions.length; i++) {
            for (int ply = 0; ply < 4; ply++) {
                List<ChessMove> moves = chess.legalMoves(p, p.whiteToMove);
                if (moves.isEmpty() || i % 32 == 0 && ply == 0) {
                    chess.NewGame();
                    p = new ChessPosition(Chess.pos);
                    moves = chess.legalMoves(p, p.whiteToMove);
                }
                p.makeMove(moves.get(random.nextInt(moves.size())));
            }
            positions[i] = new ChessPosition(p);
        }
        return positions;
    }

    private static double evaluate(Chess chess, ChessPosition[] positions, int count)
    {
        double sum = 0;
        for (int i = 0; i < count; i++)
            sum += chess.positionEvaluation(positions[i % positions.length], true);
        result = sum;
        return sum;
    }

    public static void main(String[] args)
    {
        Chess chess = new Chess(null);
        double scale = args.length > 1 ? Double.parseDouble(args[1]) : 1;
        if (args.length == 0 || args[0].equals("eval")) {
            ChessPosition[] positions = positions(chess);
            for (int i = 0; i < 3; i++) evaluate(chess, positions, 250000);
            long[] times = new long[5];
            for (int i = 0; i < times.length; i++) {
                long start = System.nanoTime();
                double sum = evaluate(chess, positions, 1000000);
                times[i] = System.nanoTime() - start;
                System.out.printf(Locale.ROOT, "eval run %d: %.1f ns/position, checksum %.2f cp%n",
                        i + 1, times[i] / 1000000.0, sum * scale);
            }
            Arrays.sort(times);
            System.out.printf(Locale.ROOT, "eval median: %.1f ns/position%n", times[2] / 1000000.0);
            return;
        }
        String[] names = {"Initial", "Kiwipete", "Rook ending", "Middlegame"};
        String[] fens = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
        };
        Chess.maxDepth = 3;
        chess.bIterativeDeepening = false;
        for (int round = 0; round < 4; round++) {
            long totalTime = 0, totalNodes = 0;
            for (int i = 0; i < fens.length; i++) {
                ChessPosition p = ChessTest.fen(fens[i]);
                Chess.bThinking = true;
                long start = System.nanoTime();
                ChessMove move = chess.alphaBeta(0, p, p.whiteToMove);
                long elapsed = System.nanoTime() - start;
                Chess.bThinking = false;
                totalTime += elapsed;
                totalNodes += Chess.nodeCount;
                if (round > 0) System.out.printf(Locale.ROOT,
                        "search run %d %s: %s, %.2f cp, %d nodes, %.3f s%n",
                        round, names[i], move, Chess.bestMoveEval * scale, Chess.nodeCount, elapsed / 1e9);
            }
            if (round > 0) System.out.printf(Locale.ROOT,
                    "search total %d: %d nodes, %.3f s, %.0f nodes/s%n",
                    round, totalNodes, totalTime / 1e9, totalNodes * 1e9 / totalTime);
        }
    }
}
