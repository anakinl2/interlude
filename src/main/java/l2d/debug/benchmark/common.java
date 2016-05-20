package l2d.debug.benchmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import l2d.Config;
import l2d.Server;

/**
 * L2Dream Java Benchmarks Suite
 */
public class common
{
	public static final Logger log = Logger.getLogger(Geodata.class.getName());
	public static byte[] dummy;

	public static void SetDummyUseMem(final int sz)
	{
		dummy = new byte[sz];
	}

	public static void GC()
	{
		System.gc();
		logMem();
	}

	public static void logMem()
	{
		final long maxMem = Runtime.getRuntime().maxMemory();
		final long freeMem = Runtime.getRuntime().freeMemory() + maxMem - Runtime.getRuntime().totalMemory();
		final long usedMem = maxMem - freeMem;
		log.info("Used memory " + (int) (usedMem / 1048576) + " Mb of " + (int) (maxMem / 1048576) + " Mb (" + (int) (freeMem / 1048576) + " Mb is free)");
	}

	public static void init() throws Exception
	{
		Server.SERVER_MODE = Server.MODE_GAMESERVER;

		final InputStream is = new FileInputStream(new File("config/log.properties"));
		LogManager.getLogManager().readConfiguration(is);
		is.close();

		Config.loadAllConfigs();
		GC();
	}

	public static boolean YesNoPrompt(final String prompt)
	{
		while(true)
		{
			System.out.print(prompt + " [Y/N]: ");
			final String s = System.console().readLine();
			if(s.equalsIgnoreCase("Y") || s.equalsIgnoreCase("Yes") || s.equalsIgnoreCase("True"))
				return true;
			if(s.equalsIgnoreCase("N") || s.equalsIgnoreCase("No") || s.equalsIgnoreCase("False"))
				return false;
		}
	}

	public static void PromptEnterToContinue()
	{
		System.out.print("Press Enter to continue...");
		System.console().readLine();
	}
}