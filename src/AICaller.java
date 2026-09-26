package Chess;

import javax.swing.SwingUtilities;

/** One worker searches private board copies; only the event thread commits a move. */
public class AICaller extends Thread
{
    private final Chess chess;
    private boolean running = true;
    private ChessPosition pending;
    private boolean pendingPlayer;
    private long generation;

    public AICaller(Chess chess)
    {
        this.chess = chess;
        setDaemon(true);
    }

    public synchronized void go()
    {
        cancel();
        if (!Chess.main.bPlaying || chess.bWhoseTurn != Chess.PROGRAM) return;
        pending = Chess.pos;
        pendingPlayer = Chess.PROGRAM;
        setControls(false);
        notifyAll();
    }

    public synchronized void cancel()
    {
        ++generation;
        pending = null;
        Chess.bThinking = false;
        setControls(true);
    }

    public synchronized void exit()
    {
        cancel();
        running = false;
        notifyAll();
    }

    private void setControls(boolean enabled)
    {
        Chess.main.difficultySlider.setEnabled(enabled);
        Chess.main.chk_IterativeDeep.setEnabled(enabled);
        Chess.main.butt_SetupBoard.setEnabled(enabled);
        Chess.main.menu_Game_SetPosition.setEnabled(enabled);
    }

    public void run()
    {
        while (true) {
            final ChessPosition starting;
            final boolean player;
            final long request;
            synchronized (this) {
                while (running && pending == null) {
                    try {
                        wait();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (!running) return;
                starting = pending;
                player = pendingPlayer;
                request = generation;
                pending = null;
                Chess.bThinking = true;
            }

            ChessMove found = null;
            RuntimeException failure = null;
            try {
                chess.playGame(starting, player);
                if (Chess.bestMove != null) found = new ChessMove(Chess.bestMove);
            } catch (RuntimeException problem) {
                failure = problem;
            }
            final ChessMove move = found;
            final RuntimeException error = failure;
            SwingUtilities.invokeLater(() -> {
                synchronized (AICaller.this) {
                    if (request != generation) return;
                    Chess.bThinking = false;
                    setControls(true);
                    if (error != null) {
                        Chess.main.alert("Search stopped", error.toString());
                    } else if (Chess.pos == starting && chess.bWhoseTurn == player && Chess.main.bPlaying) {
                        if (move == null) Chess.main.checkGameOver();
                        else Chess.main.playerMoved(player, move);
                    }
                }
            });
        }
    }
}
