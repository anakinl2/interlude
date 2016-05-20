package events.autoannouncer;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.Announcements;
import l2d.game.instancemanager.ServerVariables;
import l2d.game.model.L2Player;
import l2d.util.Files;

public class Announcer extends Functions implements ScriptFile
{
	private static boolean _active = false;
	private static String[][] text = { { "Test announce 1", "120000" }, { "Test anounce 2", "60000" } };

	private static boolean isActive()
	{
		return ServerVariables.getString("event_Announcer", "off").equalsIgnoreCase("on");
	}

	public void startEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(!isActive())
		{
			ServerVariables.set("event_Announcer", "on");
			announceRun();
			System.out.println("Event: AutoAnnouncer started.");
		}
		else
			player.sendMessage("Event 'AutoAnnouncer' already started.");

		_active = true;
		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public void stopEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;
		if(isActive())
		{
			ServerVariables.unset("event_Announcer");
			System.out.println("Event: AutoAnnouncer stopped.");
		}
		else
			player.sendMessage("Event 'AutoAnnouncer' not started.");

		_active = false;
		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public static void announceRun()
	{
		if(_active)
			for(String[] element : text)
				executeTask("events.autoannouncer.Announcer", "announce", new Object[] { element[0], Integer.valueOf(element[1]) }, Integer.valueOf(element[1]));
	}

	public static void announce(String text, Integer inter)
	{
		if(_active)
		{
			Announcements.getInstance().announceToAll(text);
			executeTask("events.autoannouncer.Announcer", "announce", new Object[] { text, inter }, inter);
		}
	}

	public void onLoad()
	{
		if(isActive())
		{
			_active = true;
			announceRun();
			System.out.println("Loaded Event: AutoAnnouncer [state: activated]");
		}
		else
			System.out.println("Loaded Event: AutoAnnouncer [state: deactivated]");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}