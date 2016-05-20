package l2d.status.gshandlers;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

import l2d.Config;
import l2d.ext.scripts.Scripts;
import l2d.ext.scripts.Scripts.ScriptClassAndMethod;
import l2d.game.Shutdown;
import l2d.game.loginservercon.LSConnection;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.status.GameStatusThread;
import l2d.util.GsaTr;

public class HandlerWorld
{
	private static final Logger _log = Logger.getLogger(GameStatusThread.class.getName());

	public static void Whois(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: whois Player");
		else
		{
			L2Player player = L2World.getPlayer(argv[1]);
			if(player == null)
				_print.println("Unable to find Player: " + argv[1]);
			else
			{
				long adenaCWH = 0, adenaWH = player.getWarehouse().findItemId(57) == null ? 0 : player.getWarehouse().findItemId(57).getCount();
				String clanName = "-", allyName = "-", HWID;
				if(player.getClan() != null)
				{
					adenaCWH = player.getClan().getAdena() == null ? 0 : player.getClan().getAdena().getCount();
					clanName = player.getClan().getName();
					if(player.getAlliance() != null)
						allyName = player.getAlliance().getAllyName();
				}
				HWID = player.getNetConnection() == null ? "-" : player.getNetConnection().HWID;

				_print.println("Account / IP / HWID ........: " + player.getAccountName() + " / " + player.getIP() + " / " + HWID);
				_print.println("Level / EXP / SP ...........: " + player.getLevel() + " / " + player.getExp() + " / " + player.getSp());
				_print.println("Location ...................: " + player.getLoc());
				_print.println("Adena Inv / WH / CWH .......: " + player.getAdena() + " / " + adenaWH + " / " + adenaCWH);
				_print.println("Clan / Ally ................: " + clanName + " / " + allyName);
				// TODO расширить
			}
		}
	}

	public static void ListEnemy(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: enemy Player");
		else
		{
			L2Player player = L2World.getPlayer(argv[1]);
			if(player == null)
				_print.println("Unable to find Player: " + argv[1]);
			else
				for(L2NpcInstance enemy : player.getHateList().keySet())
					_print.println("--> " + enemy.getName() + " <--");
		}
	}

	public static void Reload(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: reload skills|npc|multisell|gmaccess|scripts|pktlogger");
		else if(argv[1].equalsIgnoreCase("skills"))
		{
			SkillTable.getInstance().reload();
			_print.println("Skills table reloaded...");
		}
		else if(argv[1].equalsIgnoreCase("npc"))
		{
			NpcTable.getInstance().reloadAllNpc();
			_print.println("Npc table reloaded...");
		}
		else if(argv[1].equalsIgnoreCase("multisell"))
		{
			L2Multisell.getInstance().reload();
			for(ScriptClassAndMethod handler : Scripts.onReloadMultiSell)
				Scripts.callScriptsNoOwner(handler.scriptClass, handler.method);
			_print.println("Multisell reloaded...");
		}
		else if(argv[1].equalsIgnoreCase("gmaccess"))
		{
			Config.loadGMAccess();
			for(L2Player player : L2World.getAllPlayers())
				if(!Config.EVERYBODY_HAS_ADMIN_RIGHTS)
					player.setPlayerAccess(Config.gmlist.get(player.getObjectId()));
				else
					player.setPlayerAccess(Config.gmlist.get(new Integer(0)));
			_print.println("GMAccess reloaded...");
		}
		else if(argv[1].equalsIgnoreCase("scripts"))
		{
			Scripts.getInstance().reload();
			_print.println("Scripts reloaded...");
		}
		else if(argv[1].equalsIgnoreCase("pktlogger"))
		{
			Config.reloadPacketLoggerConfig();
			_print.println("Packet-logger config reloaded...");
		}
		else
			_print.println("Unknown reload component: " + argv[1]);
	}

	public static void Shutdown(String fullCmd, String[] argv, PrintWriter _print, Socket _csocket)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: shutdown seconds|NOW [dumpSnapshot:true|false]");
		else if(argv[1].equalsIgnoreCase("NOW"))
		{
			_log.warning("Shutting down via TELNET by host: " + _csocket.getInetAddress().getHostAddress());
			_print.println("Shutting down...");
			System.exit(-1);
		}
		else
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(argv[1]);
				if(argv.length > 2)
					Config.DUMP_MEMORY_ON_SHUTDOWN = Boolean.parseBoolean(argv[2]);
			}
			catch(Exception e)
			{
				_print.println("USAGE: shutdown seconds|NOW [dumpSnapshot:true|false]");
				return;
			}
			Shutdown.getInstance().startTelnetShutdown(_csocket.getInetAddress().getHostAddress(), val, false);
			_print.println("Server will shutdown in " + val + " seconds!");
			_print.println("Type \"abort\" to abort shutdown!");
		}
	}

	public static void Restart(String fullCmd, String[] argv, PrintWriter _print, Socket _csocket)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: restart seconds [dumpSnapshot:true|false]");
		else
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(argv[1]);
				if(argv.length > 2)
					Config.DUMP_MEMORY_ON_SHUTDOWN = Boolean.parseBoolean(argv[2]);
			}
			catch(Exception e)
			{
				_print.println("USAGE: restart seconds [dumpSnapshot:true|false]");
				return;
			}
			Shutdown.getInstance().startTelnetShutdown(_csocket.getInetAddress().getHostAddress(), val, true);
			_print.println("Server will restart in " + val + " seconds!");
			_print.println("Type \"abort\" to abort restart!");
		}
	}

	public static void AbortShutdown(String fullCmd, String[] argv, PrintWriter _print, Socket _csocket)
	{
		Shutdown.getInstance().Telnetabort(_csocket.getInetAddress().getHostAddress());
		_print.println("OK! - Shutdown/Restart aborted.");
	}

	public static void StopLogin(String fullCmd, String[] argv, PrintWriter _print, Socket _csocket)
	{
		GsaTr.TrialOnline = 0;
		Config.MAX_PROTOCOL_REVISION = 1;
		Config.MIN_PROTOCOL_REVISION = 0;

		_print.println("Shutdown LSConnection...");
		_print.flush();
		LSConnection.getInstance().shutdown();

		_print.println("Kicking players...");
		_print.flush();
		for(L2Player player : L2World.getAllPlayers())
			player.logout(true, false, false);

		_print.println("Forcing gc...");
		_print.flush();
		new Thread(new Runnable(){
			@Override
			public void run()
			{
				for(int i = 0; i < 10; i++)
				{
					System.gc();
					try
					{
						Thread.sleep(1000);
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}