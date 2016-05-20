package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.tables.PetDataTable;

public class AdminLevel implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_add_level,
		admin_addLevel,
		admin_set_level,
		admin_setLevel,
	}

	private void setLevel(L2Player activeChar, L2Object target, int level)
	{
		if(target == null || !(target.isPlayer() || target.isPet()))
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}
		if(level < 1 || level > Experience.getMaxLevel())
		{
			activeChar.sendMessage("You must specify level 1 - " + Experience.getMaxLevel());
			return;
		}
		if(target.isPlayer())
		{
			Long exp_add = Experience.LEVEL[level] - ((L2Player) target).getExp();
			((L2Player) target).addExpAndSp(exp_add, 0, false, false);
			return;
		}
		if(target.isPet())
		{
			Long exp_add = PetDataTable.getInstance().getInfo(((L2PetInstance) target).getNpcId(), level).getExp() - ((L2PetInstance) target).getExp();
			((L2PetInstance) target).addExpAndSp(exp_add, 0, false, true);
			((L2PetInstance) target).startRegeneration();
		}
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditChar)
			return false;

		L2Object target = activeChar.getTarget();
		if(target == null || !(target.isPlayer() || target.isPet()))
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return false;
		}
		int level;

		switch(command)
		{
			case admin_add_level:
			case admin_addLevel:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //addLevel level");
					return false;
				}
				try
				{
					level = Integer.parseInt(wordList[1]);
				}
				catch(NumberFormatException e)
				{
					activeChar.sendMessage("You must specify level");
					return false;
				}
				setLevel(activeChar, target, level + ((L2Character) target).getLevel());
				break;
			case admin_set_level:
			case admin_setLevel:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //setLevel level");
					return false;
				}
				try
				{
					level = Integer.parseInt(wordList[1]);
				}
				catch(NumberFormatException e)
				{
					activeChar.sendMessage("You must specify level");
					return false;
				}
				setLevel(activeChar, target, level);
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