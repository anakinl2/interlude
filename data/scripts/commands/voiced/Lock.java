package commands.voiced;

import l2d.Config;
import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.loginservercon.LSConnection;
import l2d.game.loginservercon.gspackets.LockAccountIP;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Files;

public class Lock extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private String[] _commandList = new String[] { "lock" };

	private static String defaultPage = "data/scripts/commands/voiced/lock.html";

	public void onLoad()
	{
		if(Config.SERVICES_LOCK_ACCOUNT_IP)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	private void showDefaultPage(L2Player activeChar)
	{
		String html = Files.read(defaultPage, activeChar);
		html = html.replaceFirst("%IP%", activeChar.getIP());
		show(html, activeChar);
	}

	public static void lock_on()
	{
		L2Player activeChar = (L2Player) self;
		LSConnection.getInstance().sendPacket(new LockAccountIP(activeChar.getAccountName(), activeChar.getIP()));
		activeChar.sendMessage("Account locked.");
	}

	public static void lock_off()
	{
		L2Player activeChar = (L2Player) self;
		LSConnection.getInstance().sendPacket(new LockAccountIP(activeChar.getAccountName(), "*"));
		activeChar.sendMessage("Account unlocked.");
	}

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(command.equals("lock") && (target == null || target.equals("")))
		{
			showDefaultPage(activeChar);
			return true;
		}

		if(target.equalsIgnoreCase("on"))
		{
			LSConnection.getInstance().sendPacket(new LockAccountIP(activeChar.getAccountName(), activeChar.getIP()));
			activeChar.sendMessage("Account locked.");
			return true;
		}

		if(target.equalsIgnoreCase("off"))
		{
			LSConnection.getInstance().sendPacket(new LockAccountIP(activeChar.getAccountName(), "*"));
			activeChar.sendMessage("Account unlocked.");
			return true;
		}

		showDefaultPage(activeChar);
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}