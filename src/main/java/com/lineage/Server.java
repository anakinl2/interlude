package com.lineage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import com.lineage.auth.L2LoginServer;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.game.GameServer;
import com.lineage.status.Status;

/**
 * This class ...
 *
 * @version $Revision: 1.5.4.9 $ $Date: 2005/03/29 23:15:33 $
 */
/***********************************************
 * This class appears to be the starter class. The class the calls upon the
 * login and game server classes. It's purpose is to initiate the log file
 * and start the servers
 * *********************************************
 */
public class Server
{
	public static final int MODE_GAMESERVER = 1;
	public static final int MODE_LOGINSERVER = 2;
	public static final int MODE_COMBOSERVER = 3;
	public static int SERVER_MODE = 0;
	// Global class instantiations. Used to actually ready the classes
	public static GameServer gameServer;
	public static L2LoginServer loginServer;
	public static Status statusServer;
	public static String L2DreamHomeDir = System.getProperty("user.dir") + "/";

	// Main Method
	public static void main(String[] args) throws Exception
	{
		String L2DreamHome = System.getProperty("l2d.home");
		if(L2DreamHome != null)
		{
			File home = new File(L2DreamHome);
			if(!home.isAbsolute())
				try
				{
					L2DreamHome = home.getCanonicalPath();
				}
				catch(IOException e)
				{
					L2DreamHome = home.getAbsolutePath();
				}
			L2DreamHomeDir = L2DreamHome + "/";
			System.setProperty("user.dir", L2DreamHome);
		}

		// Local Constants
		final String LOG_FOLDER = "log"; // Name of folder for log file
		final String LOG_NAME = "/config/log.properties"; // Name of log file
		final String CONF_LOC = "com.lineage.Config"; // Location of configuration data

		/*** Main ***/
		// Initialize config
		Class.forName(CONF_LOC);

		// Create log folder
		File logFolder = new File("./", LOG_FOLDER);
		logFolder.mkdir();

		// Create input stream for log file -- or store file data into memory
		InputStream is = new FileInputStream(L2DreamHomeDir + LOG_NAME);
		LogManager.getLogManager().readConfiguration(is);
		is.close();

		L2DatabaseFactory.getInstance();

		// Run server threads
		gameServer = new GameServer();
		loginServer = L2LoginServer.getInstance();
		//loginServer = new LoginServer();
		//loginServer.start();

		// run telnet server if enabled

		if(Config.MAT_BANCHAT)
			System.out.println("MAT AutoBANChat filter enable.");
		else
			System.out.println("MAT AutoBANChat filter disable.");
	}
}
