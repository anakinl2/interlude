package l2d.status.gshandlers;

import java.io.PrintWriter;

import l2d.Config;
import l2d.game.ThreadPoolManager;

public class HandlerPerfomance
{
	public static void LazyItems(String fullCmd, String[] argv, PrintWriter _print)
	{
		Config.LAZY_ITEM_UPDATE = !Config.LAZY_ITEM_UPDATE;
		_print.println("Lazy items update set to: " + Config.LAZY_ITEM_UPDATE);
	}

	public static void ThreadPool(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty())
			for(String line : ThreadPoolManager.getInstance().getStats())
				_print.println(line);
		else if(argv[1].equalsIgnoreCase("packets"))
			_print.println(ThreadPoolManager.getInstance().getGPacketStats());
		else if(argv[1].equalsIgnoreCase("iopackets"))
			_print.println(ThreadPoolManager.getInstance().getIOPacketStats());
		else if(argv[1].equalsIgnoreCase("general"))
			_print.println(ThreadPoolManager.getInstance().getGeneralPoolStats());
		else if(argv[1].equalsIgnoreCase("move"))
			_print.println(ThreadPoolManager.getInstance().getMovePoolStats());
		else if(argv[1].equalsIgnoreCase("npcAi"))
			_print.println(ThreadPoolManager.getInstance().getNpcAIPoolStats());
		else if(argv[1].equalsIgnoreCase("playerAi"))
			_print.println(ThreadPoolManager.getInstance().getPlayerAIPoolStats());
		else if(argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: performance [packets|iopackets|general|move|npcAi|playerAi]");
		else
			_print.println("Unknown ThreadPool: " + argv[1]);
	}

	public static void GC(String fullCmd, String[] argv, PrintWriter _print)
	{
		long freeMemBefore = Runtime.getRuntime().freeMemory() + Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
		System.gc();
		long maxMem = Runtime.getRuntime().maxMemory();
		long freeMem = Runtime.getRuntime().freeMemory() + maxMem - Runtime.getRuntime().totalMemory();
		long usedMem = maxMem - freeMem;
		_print.println("Collected: " + (int) ((freeMem - freeMemBefore) / 1048576) + " Mb / Now used memory " + (int) (usedMem / 1048576) + " Mb of " + (int) (maxMem / 1048576) + " Mb (" + (int) (freeMem / 1048576) + " Mb is free)");
	}
}