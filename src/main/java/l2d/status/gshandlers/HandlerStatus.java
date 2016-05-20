package l2d.status.gshandlers;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import l2d.Config;
import l2d.game.GameTimeController;
import l2d.game.Shutdown;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.tables.GmListTable;
import l2d.util.GsaTr;
import l2d.util.Util;

public class HandlerStatus
{
	public static void Version(String fullCmd, String[] argv, PrintWriter _print)
	{
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
		_print.println("L2Dream server : " + Config.SERVER_VERSION + " builded " + Config.SERVER_BUILD_DATE);
		_print.println("JVM .............: " + rt.getVmVendor() + " " + rt.getVmName() + " " + rt.getVmVersion());
		_print.println("OS ..............: " + os.getName() + " " + os.getVersion() + " " + os.getArch() + ", " + os.getAvailableProcessors() + " CPUs, Load Average: " + os.getSystemLoadAverage());
	}

	public static void Config(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: config parameter[=value]");
		else
			_print.println(Config.HandleConfig(null, Util.joinStrings(" ", argv, 1)));
	}

	public static void GmList(String fullCmd, String[] argv, PrintWriter _print)
	{
		int gmsCount = 0;
		String gmList = "";

		for(L2Player player : GmListTable.getAllGMs())
		{
			gmList = gmList + ", " + player.getName();
			gmsCount++;
		}
		_print.print("There are currently " + gmsCount + " GM(s) online");
		_print.println(gmsCount > 0 ? ": " + gmList : "...");
	}

	public static void Database(String fullCmd, String[] argv, PrintWriter _print)
	{
		_print.println("Database Usage Status: ");
		_print.println("+... Players operation: ");
		_print.println("-->  Update characters: " + L2World.getUpdatePlayerBase());
		_print.println("+..... Items operation: ");
		_print.println("-->      Insert: " + L2World.getInsertItemCount());
		_print.println("-->      Delete: " + L2World.getDeleteItemCount());
		_print.println("-->      Update: " + L2World.getUpdateItemCount());
		_print.println("--> Lazy Update: " + L2World.getLazyUpdateItem());
		_print.println("+... Lazy items update: " + Config.LAZY_ITEM_UPDATE);
		_print.println("+... Released ObjectId: " + IdFactory.getInstance().getReleasedCount());
	}

	public static void Status(String fullCmd, String[] argv, PrintWriter _print)
	{
		int playerCount = L2World.getAllPlayersCount();
		int objectCount = L2World.getAllObjectsCount();
		int itemCount = 0;
		int itemVoidCount = 0;
		int monsterCount = 0;
		int minionCount = 0;
		int npcCount = 0;
		int guardCount = 0;
		int charCount = 0;
		int doorCount = 0;
		int summonCount = 0;
		int AICount = 0;
		int extendedAICount = 0;
		int summonAICount = 0;
		int activeAICount = 0;

		for(L2Object obj : L2World.getAllObjects().values())
			if(obj.isCharacter())
			{
				charCount++;
				if(obj.isNpc())
				{
					npcCount++;
					if(obj.isMonster())
					{
						monsterCount++;
						minionCount += ((L2MonsterInstance) obj).getTotalSpawnedMinionsInstances();
					}
				}
				else if(obj.isSummon() || obj.isPet())
					summonCount++;
				else if(obj instanceof L2DoorInstance)
					doorCount++;
				if(obj.hasAI())
				{
					AICount++;
					if(obj.getAI().isActive())
						activeAICount++;
					if(obj.isNpc())
						extendedAICount++;
					else if(obj.isSummon() || obj.isPet())
						summonAICount++;
				}
			}
			else if(obj instanceof L2ItemInstance)
				if(((L2ItemInstance) obj).getLocation() == L2ItemInstance.ItemLocation.VOID)
					itemVoidCount++;
				else
					itemCount++;
		_print.println("Server Status: ");
		_print.println(" +.............. Players: " + playerCount + "/" + GsaTr.TrialOnline);
		_print.println(" +.............. Summons: " + summonCount);
		_print.println(" +............. Monsters: " + monsterCount);
		_print.println(" +.............. Minions: " + minionCount);
		_print.println(" +........ Castle Guards: " + guardCount);
		_print.println(" +................ Doors: " + doorCount);
		_print.println(" +................. Npcs: " + npcCount);
		_print.println(" +........... Characters: " + charCount);
		_print.println(" +.............. Objects: " + objectCount);
		_print.println(" +............... All AI: " + AICount);
		_print.println(" +...... Active AI Count: " + activeAICount);
		_print.println(" +.......... Extended AI: " + extendedAICount);
		_print.println(" +............ Summon AI: " + summonAICount);
		_print.println(" +......... Ground Items: " + itemVoidCount);
		_print.println(" +.......... Owned Items: " + itemCount);
		_print.println(" +................... GM: " + GmListTable.getAllGMs().size());
		_print.println(" + Game Time / Real Time: " + GameTime() + " / " + getCurrentTime());
		_print.println(" +.. Start Time / Uptime: " + getStartTime() + " / " + getUptime());
		_print.println(" +.. Shutdown_sec / mode: " + Shutdown.getInstance().getSeconds() + " / " + Shutdown.getInstance().getMode());
		_print.println(" +....... Active Regions: " + L2World.getActiveRegions().size());
		_print.println(" +.............. Threads: " + Thread.activeCount());
		_print.println(" +............. RAM Used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576);
	}

	public static String GameTime()
	{
		int t = GameTimeController.getInstance().getGameTime();
		int h = t / 60;
		int m = t % 60;
		SimpleDateFormat format = new SimpleDateFormat("H:mm");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, m);
		return format.format(cal.getTime());
	}

	public static String getUptime()
	{
		return Util.formatTime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
	}

	public static String getStartTime()
	{
		return new Date(ManagementFactory.getRuntimeMXBean().getStartTime()).toString();
	}

	public static String getCurrentTime()
	{
		return new Date().toString();
	}
}