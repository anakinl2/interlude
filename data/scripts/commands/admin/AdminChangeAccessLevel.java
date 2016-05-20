package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;

/**
 * This class handles following admin commands: - invul = turns invulnerability
 * on/off
 */
public class AdminChangeAccessLevel implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_changelvl
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanGmEdit)
			return false;

		switch(command)
		{
			case admin_changelvl:
				if(wordList.length == 2)
				{
					int lvl = Integer.parseInt(wordList[1]);
					if(activeChar.getTarget().isPlayer())
						((L2Player) activeChar.getTarget()).setAccessLevel(lvl);
				}
				else if(wordList.length == 3)
				{
					int lvl = Integer.parseInt(wordList[2]);
					L2Player player = L2World.getPlayer(wordList[1]);
					if(player != null)
						player.setAccessLevel(lvl);
				}
				break;
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