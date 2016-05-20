package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.Revive;
import com.lineage.game.serverpackets.SocialAction;
import com.lineage.util.Log;

public class AdminCancel implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_cancel
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditChar)
			return false;

		switch(command)
		{
			case admin_cancel:
				handleCancel(activeChar, wordList.length > 1 ? wordList[1] : null);
				break;
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void handleCancel(L2Player activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();
		if(player != null)
		{
			L2Player plyr = L2World.getPlayer(player);
			if(plyr != null)
				obj = plyr;
			else
				try
				{
					int radius = Math.max(Integer.parseInt(player), 100);
					for(L2Character character : activeChar.getAroundCharacters(radius, 200))
					{
						character.getEffectList().stopAllEffects();
						character.broadcastPacket(new SocialAction(character.getObjectId(), 15));
						character.broadcastPacket(new Revive(character));
					}
					activeChar.sendMessage("Apply Cancel within " + radius + " unit radius.");
					return;
				}
				catch(NumberFormatException e)
				{
					activeChar.sendMessage("Enter valid player name or radius");
					return;
				}
		}

		if(obj == null)
			obj = activeChar;
		if(obj.isCharacter())
		{
			L2Character target = (L2Character) obj;

			for(L2Effect e : target.getEffectList().getAllEffects())
				e.exit();

			target.broadcastPacket(new SocialAction(target.getObjectId(), 15));
			target.broadcastPacket(new Revive(target));

			Log.LogCommand(activeChar, Log.Adm_DelSkill, "admin_cancel", 1);
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