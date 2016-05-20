package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.util.Log;

/**
 * This class handles following admin commands: - gm = turns gm mode on/off
 */
public class AdminGm implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_gm
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		// TODO зачем отключено?
		if(true)
			return false;

		if(!activeChar.getPlayerAccess().CanEditChar)
			return false;

		switch(command)
		{
			case admin_gm:
				handleGm(activeChar);
				break;
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void handleGm(L2Player activeChar)
	{
		if(activeChar.getPlayerAccess().IsGM)
		{
			activeChar.getPlayerAccess().IsGM = false;
			activeChar.sendMessage("You no longer have GM status.");
			Log.add("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") turned his GM status off", "gm_ext_actions", activeChar);
		}
		else
		{
			activeChar.getPlayerAccess().IsGM = true;
			activeChar.sendMessage("You have GM status now.");
			Log.add("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") turned his GM status on", "gm_ext_actions", activeChar);
		}
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