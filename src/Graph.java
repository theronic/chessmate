
package Chess;

import java.awt.*;
import java.util.Vector;

/**
 * The Graph class is used to draw a graphical representation of the AI's subjective
 * scoring method.  It provides a small guide of which way the game is going.
 * The class is an extension of a panel and has its own paint function, whereby it draws
 * the actual graph.  It is included by the main class in the tabbed panel to the right
 * of the Chessmate window.  Since we expect only one such class to exist, most data is static.
 */

class Graph extends Panel
{
/**
 * A vector to store the previous scores, as determined by the alpha-beta search.
 */
	public static Vector data = new Vector();

/**
 * Draws the actual graph.
 */

 	static Point prev = new Point();
 	static Point cur = new Point();

 	static int xSpacing = 10;
 	static int tempSize = 0;

	public void paint( Graphics g )
	{
		g.setColor( Color.lightGray );

		g.setColor( Color.blue );

		g.drawLine( 0, getHeight() / 2, getWidth(), getHeight() / 2 );

		tempSize = data.size();
		if ( tempSize == 0 )
			return;

		prev.x = 0;
		prev.y = getHeight() / 2;

		int nDraw = getWidth() / 2 / xSpacing;
		float fv = 0;

		for ( int i = tempSize; i != 0 ; --i )
		{
			Float f = (Float) data.get( tempSize - i );
			fv = f.floatValue() * 2;
			if ( fv > 160 )
				fv = 160.0f;
			else
			if ( fv < -160 )
				fv = -160.0f;
			cur.x = getWidth()/2 - (int)( xSpacing * i );
			cur.y = (int)(getHeight() / 2 + fv);

			if ( f.floatValue() > 0 )
				g.setColor( Color.black );
			else
				g.setColor( Color.white );

			g.drawLine( prev.x, prev.y, cur.x, cur.y );
			g.fillOval( cur.x - 2, cur.y - 2, 5, 5 );

			prev.x = cur.x;
			prev.y = cur.y;
		}

	}
}