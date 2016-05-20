package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;

/**
 * Created by IntelliJ IDEA.
 * User: Lelouch
 * Date: 19.09.2009
 * Time: 11:45:41
 * To change this template use File | Settings | File Templates.
 */
public class AdminClass implements IAdminCommandHandler, ScriptFile
{

	private static enum Commands
	{
		admin_setclass,
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditChar)
			return false;

		switch(command)
		{
			case admin_setclass:
				if(wordList.length == 3) ;
				setlevel(activeChar, Integer.parseInt(wordList[1]));
				break;
		}

		return false;
	}

	private void setlevel(L2Player activeChar, int ClassId)
	{
		L2Object target = activeChar.getTarget();
		if(target == null || !target.isPlayer())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_TARGET));
			return;
		}

		activeChar.setClassId(ClassId, true);
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
	{
	}

	public void onShutdown()
	{
	}
}
