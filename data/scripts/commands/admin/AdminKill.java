package commands.admin;

import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.util.Log;

public class AdminKill implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_kill
	}

	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().CanEditNPC)
			return false;

		switch(command)
		{
			case admin_kill:
				if(wordList.length == 1)
					handleKill(activeChar);
				else
					handleKill(activeChar, wordList[1]);
				break;
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void handleKill(final L2Player activeChar)
	{
		handleKill(activeChar, null);
	}

	private void handleKill(final L2Player activeChar, final String player)
	{
		L2Object obj = activeChar.getTarget();
		if(player != null)
		{
			final L2Player plyr = L2World.getPlayer(player);
			if(plyr != null)
				obj = plyr;
			else
			{
				final int radius = Math.max(Integer.parseInt(player), 100);
				for(final L2Character character : activeChar.getAroundCharacters(radius, 200))
					character.reduceCurrentHp(character.getMaxHp() + character.getMaxCp() + 1, character, null, true, true, false, false);
				activeChar.sendMessage("Killed within " + radius + " unit radius.");
				return;
			}
		}

		if(obj != null && obj.isCharacter())
		{
			final L2Character target = (L2Character) obj;
			target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null, true, true, false, true);
			Log.add("kill character " + target.getObjectId() + " " + target.getName(), "gm_ext_actions", activeChar);
		}
		else
			activeChar.sendPacket(Msg.INVALID_TARGET);
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