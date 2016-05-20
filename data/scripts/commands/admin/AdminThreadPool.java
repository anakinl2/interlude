package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Player;

public class AdminThreadPool implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_setcore,
		admin_setpool
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().Menu)
			return false;

		if(wordList.length != 3)
		{
			activeChar.sendMessage("Incorrect arguments count");
			return false;
		}

		try
		{
			switch(command)
			{
				case admin_setcore:
					if(wordList[1].equals("e"))
					{
						ThreadPoolManager.getInstance().getEffectsScheduledThreadPool().setCorePoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New Effects theadpool core size: " + Integer.parseInt(wordList[2]));
					}
					else if(wordList[1].equals("g"))
					{
						ThreadPoolManager.getInstance().getGeneralScheduledThreadPool().setCorePoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New General theadpool core size: " + Integer.parseInt(wordList[2]));
					}
					else if(wordList[1].equals("n"))
					{
						ThreadPoolManager.getInstance().getNpcAiScheduledThreadPool().setCorePoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New NpcAI theadpool core size: " + Integer.parseInt(wordList[2]));
					}
					else if(wordList[1].equals("p"))
					{
						ThreadPoolManager.getInstance().getPlayerAiScheduledThreadPool().setCorePoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New PlayerAI theadpool core size: " + Integer.parseInt(wordList[2]));
					}
					break;
				case admin_setpool:
					if(wordList[1].equals("e"))
					{
						ThreadPoolManager.getInstance().getEffectsScheduledThreadPool().setMaximumPoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New Effects theadpool query size: " + Integer.parseInt(wordList[2]));
					}
					else if(wordList[1].equals("g"))
					{
						ThreadPoolManager.getInstance().getGeneralScheduledThreadPool().setMaximumPoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New General theadpool query size: " + Integer.parseInt(wordList[2]));
					}
					else if(wordList[1].equals("n"))
					{
						ThreadPoolManager.getInstance().getNpcAiScheduledThreadPool().setMaximumPoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New NpcAI theadpool query size: " + Integer.parseInt(wordList[2]));
					}
					else if(wordList[1].equals("p"))
					{
						ThreadPoolManager.getInstance().getPlayerAiScheduledThreadPool().setMaximumPoolSize(Integer.parseInt(wordList[2]));
						activeChar.sendMessage("New PlayerAI theadpool query size: " + Integer.parseInt(wordList[2]));
					}
					break;
			}
		}
		catch(Exception e)
		{
			activeChar.sendMessage("Error!");
			e.printStackTrace();
			return false;
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