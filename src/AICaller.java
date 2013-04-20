package Chess;



public class AICaller extends Thread
{
/**
 * A reference to the parent Chess class.
 * @see class Chess
 */
	Chess chess;

	boolean bStart = false;

	public void go()
	{
		bStart = true;
	}

	public void cancel()
	{
		bStart = false;
	}

	public void exit()
	{
		bRunning = false;
	}


/**
 * A boolean variable indicating whether the AICaller class is running.
 * This prevents the possibility of two threads simultaneously manipulating the
 * chess board.  This should never happen, but one can never be too safe.
 */
	private static boolean bRunning = false;

	public AICaller( Chess chess )
	{
		this.chess = chess;
		//Thread.currentThread().setPriority(MAX_PRIORITY);
	}

/**
 * This function is called when a player moves a piece and it is the AI's turn to
 * make a move.  It simply processes the search in a separate thread so the application is not
 * locked up.
 * The thread will exit if chess.bThinking is falsified.
 */
	public void run()
	{
		if ( bRunning )
			return;
		bRunning = true;

		while ( !chess.main.bQuit && bRunning )
		{
			if ( bStart )
			{
				bStart = false;

				chess.bThinking = true;

				chess.main.difficultySlider.setEnabled(false);
				chess.main.chk_IterativeDeep.setEnabled(false);
				chess.main.butt_SetupBoard.setEnabled(false);
				chess.main.menu_Game_SetPosition.setEnabled(false);

				ChessPosition n = chess.playGame( chess.pos, chess.PROGRAM );
				if ( chess.bThinking )
				{
					chess.bThinking = false; // consider removing the bRunning?
					chess.pos = n;
				}
				chess.main.difficultySlider.setEnabled(true);
				chess.main.chk_IterativeDeep.setEnabled(true);
				chess.main.butt_SetupBoard.setEnabled(true);
				chess.main.menu_Game_SetPosition.setEnabled(true);
			}

			try {
				Thread.currentThread().sleep( 50 );
			} catch ( InterruptedException ex )
			{
				System.out.println("AICaller Thread Sleep Error");
			}

		}

		bRunning = false;

	}
}