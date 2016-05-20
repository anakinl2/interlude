package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;

@SuppressWarnings("unused")
public class AdminTarget implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_target
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().CanViewChar)
			return false;

		try
		{
			final String targetName = wordList[1];
			final L2Object obj = L2World.getPlayer(targetName);
			if(obj != null && obj.isPlayer())
				obj.onAction(activeChar);
			else
				activeChar.sendMessage("Player " + targetName + " not found");
		}
		catch(final IndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Please specify correct name.");
		}

		return true;
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