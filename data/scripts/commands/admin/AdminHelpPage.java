package commands.admin;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.util.Files;

public class AdminHelpPage implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_showhtml
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().Menu)
			return false;

		switch(command)
		{
			case admin_showhtml:
				if(wordList.length != 2)
				{
					activeChar.sendMessage("Usage: //showhtml <file>");
					return false;
				}
				showHelpPage(activeChar, wordList[1]);
				break;
		}

		return true;
	}

	/**
	 * PUBLIC & STATIC so other classes from package can include it directly
	 */
	public static void showHelpPage(final L2Player targetChar, final String filename)
	{
		String _str;
		_str = targetChar.getVar("lang@") == "ru" ? "data/html-ru/admin/" : "data/html/admin/";
		final String content = Files.read(_str + filename, targetChar);

		if(content == null)
		{
			targetChar.sendMessage("Not found filename: " + filename);
			return;
		}

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		adminReply.setHtml(content);
		targetChar.sendPacket(adminReply);
	}

	@SuppressWarnings("unchecked")
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}