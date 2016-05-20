package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.tables.DoorTable;

public class AdminDoorControl implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_open,
		admin_close,
		admin_openall,
		admin_closeall
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().Door)
			return false;

		switch(command)
		{
			case admin_open:
				if(wordList.length > 1)
					DoorTable.getInstance().getDoor(Integer.parseInt(wordList[1])).openMe();
				else
				{
					L2Object target = activeChar.getTarget();
					if(target instanceof L2DoorInstance)
						((L2DoorInstance) target).openMe();
					else
						activeChar.sendPacket(Msg.INVALID_TARGET);
				}
				break;
			case admin_close:
				if(wordList.length > 1)
					DoorTable.getInstance().getDoor(Integer.parseInt(wordList[1])).closeMe();
				else
				{
					L2Object target = activeChar.getTarget();
					if(target instanceof L2DoorInstance)
						((L2DoorInstance) target).closeMe();
					else
						activeChar.sendPacket(Msg.INVALID_TARGET);
				}
				break;
			case admin_closeall:
				for(L2DoorInstance door : DoorTable.getInstance().getDoors())
					door.closeMe();
				break;
			case admin_openall:
				for(L2DoorInstance door : DoorTable.getInstance().getDoors())
					door.openMe();
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