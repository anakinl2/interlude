package commands.admin;

import l2d.Config;
import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.entity.olympiad.Olympiad;
import l2d.game.model.entity.olympiad.OlympiadDatabase;
import l2d.util.Log;

@SuppressWarnings("unused")
public class AdminOlympiad implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_oly_start,
		admin_oly_save,
		admin_manualhero,
		admin_add_oly_points
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(fullString.startsWith("admin_oly_start"))
		{
			if(!Olympiad.manualStartOlympiad())
				activeChar.sendMessage("Olympiad already started.");
		}
		else if(fullString.startsWith("admin_oly_save"))
		{
			if(!Config.ENABLE_OLYMPIAD)
				return false;

			try
			{
				OlympiadDatabase.save();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			activeChar.sendMessage("olympaid data saved.");
		}
		else if(fullString.startsWith("admin_manualhero"))
		{
			if(!Config.ENABLE_OLYMPIAD)
				return false;

			try
			{
				Olympiad.manualSelectHeroes();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			activeChar.sendMessage("Heroes formed.");
		}
		else if(fullString.startsWith("admin_add_oly_points"))
		{
			if(wordList.length < 3)
			{
				activeChar.sendMessage("Command syntax: //add_oly_points <char_name> <point_to_add>");
				activeChar.sendMessage("This command can be applied only for online players.");
				return false;
			}

			L2Player player = L2World.getPlayer(wordList[1]);
			if(player == null)
			{
				activeChar.sendMessage("Character " + wordList[1] + " not found in game.");
				return false;
			}

			int pointToAdd;

			try
			{
				pointToAdd = Integer.parseInt(wordList[2]);
			}
			catch(NumberFormatException e)
			{
				activeChar.sendMessage("Please specify integer value for olympiad points.");
				return false;
			}

			int curPoints = Olympiad.getNoblePoints(player.getObjectId());
			Olympiad.manualSetNoblePoints(player.getObjectId(), curPoints + pointToAdd);
			int newPoints = Olympiad.getNoblePoints(player.getObjectId());

			activeChar.sendMessage("Added " + pointToAdd + " points to character " + player.getName());
			activeChar.sendMessage("Old points: " + curPoints + ", new points: " + newPoints);

			Log.add("add olympiad points to player " + player.getName(), "gm_ext_actions", activeChar);
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