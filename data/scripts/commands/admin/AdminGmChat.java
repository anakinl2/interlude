package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.Say2;
import com.lineage.game.tables.GmListTable;

public class AdminGmChat implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_gmchat, admin_snoop
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().CanAnnounce)
			return false;

		switch(command)
		{
			case admin_gmchat:
				try
				{
					final String text = fullString.replaceFirst(Commands.admin_gmchat.name(), "");
					final Say2 cs = new Say2(0, 9, activeChar.getName(), text);
					GmListTable.broadcastToGMs(cs);
				}
				catch(final StringIndexOutOfBoundsException e)
				{}
				AdminHelpPage.showHelpPage(activeChar, "admin.htm");
				break;
			case admin_snoop:
			{
				final L2Object target = activeChar.getTarget();
				if(target == null)
				{
					activeChar.sendMessage("You must select a target.");
					return false;
				}
				if( !target.isPlayer())
				{
					activeChar.sendMessage("Target must be a player.");
					return false;
				}
				final L2Player player = (L2Player) target;
				player.addSnooper(activeChar);
				activeChar.addSnooped(player);
				AdminHelpPage.showHelpPage(activeChar, "admin.htm");
			}
		}
		return true;
	}

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