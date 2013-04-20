
package Chess;

import java.awt.*;
import java.sql.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

class Database
{
    static String[] ConnectOptionNames = { "Connect", "Cancel" };
    static String   ConnectTitle = "Connection Information";

	Component parent = null;

    JPanel      connectionPanel;

    JLabel      userNameLabel;
    JTextField  userNameField;
    JLabel      passwordLabel;
    JTextField  passwordField;

    JLabel      serverLabel;
    JTextField  serverField;
    JLabel      driverLabel;
    JTextField  driverField;

    JPanel      mainPanel;

    public JDBCAdapter dataBase = null;


    /**
     * Brigs up a JDialog using JOptionPane containing the connectionPanel.
     * If the user clicks on the 'Connect' button the connection is reset.
     */
    void activateConnectionDialog()
	{
		if( JOptionPane.showOptionDialog( parent, connectionPanel, ConnectTitle,
			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
	        null, ConnectOptionNames, ConnectOptionNames[0]) == 0)
		{
		    connect();

	    } else
	    {
	    	dataBase = null;
	    }
    }

/**
 * Creates the connectionPanel, which will contain all the fields for
 * the connection information.
 */
    public void createConnectionDialog()
	{
 	// Create the labels and text fields.
	userNameLabel = new JLabel("User name: ", JLabel.RIGHT);
 	userNameField = new JTextField("guest");

	passwordLabel = new JLabel("Password: ", JLabel.RIGHT);
	passwordField = new JTextField("");

    serverLabel = new JLabel("Database URL: ", JLabel.RIGHT);
	serverField = new JTextField("jdbc:odbc:Chessmate");

	driverLabel = new JLabel("Driver: ", JLabel.RIGHT);
	driverField = new JTextField("sun.jdbc.odbc.JdbcOdbcDriver");


	connectionPanel = new JPanel(false);
	connectionPanel.setLayout(new BoxLayout(connectionPanel,
						BoxLayout.X_AXIS));

	JPanel namePanel = new JPanel(false);
	namePanel.setLayout(new GridLayout(0, 1));
	namePanel.add(userNameLabel);
	namePanel.add(passwordLabel);
	namePanel.add(serverLabel);
	namePanel.add(driverLabel);

	JPanel fieldPanel = new JPanel(false);
	fieldPanel.setLayout(new GridLayout(0, 1));
	fieldPanel.add(userNameField);
	fieldPanel.add(passwordField);
	fieldPanel.add(serverField);
    fieldPanel.add(driverField);

	connectionPanel.add(namePanel);
	connectionPanel.add(fieldPanel);
    }

    private static Main main;

    public Database(Component parent)
	{
		mainPanel = new JPanel();
		this.parent = parent;
		createConnectionDialog();
		//activateConnectionDialog(); // disabled this so people are not pestered by silly Access database
	}


    public void connect()
	{
		dataBase = new JDBCAdapter(
			serverField.getText(),
			driverField.getText(),
			userNameField.getText(),
			passwordField.getText()
		 );
		if ( !dataBase.isConnected() )
			dataBase = null;
   }

}