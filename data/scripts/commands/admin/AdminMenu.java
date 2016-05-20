package commands.admin;

import java.util.StringTokenizer;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import com.lineage.util.Location;

@SuppressWarnings("unused")
public class AdminMenu implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_char_manage, //
		admin_teleport_character_to_menu, //
		admin_recall_char_menu, //
		admin_goto_char_menu, //
		admin_kick_menu, //
		admin_kill_menu, //
		admin_ban_menu, //
		admin_unban_menu
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().Menu)
			return false;

		if(fullString.equals("admin_char_manage"))
			AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
		else if(fullString.startsWith("admin_teleport_character_to_menu"))
		{
			final String[] data = fullString.split(" ");
			if(data.length == 5)
			{
				final String playerName = data[1];
				final L2Player player = L2World.getPlayer(playerName);
				if(player != null)
					teleportCharacter(player, new Location(Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4])), activeChar);
			}
			AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
		}
		else if(fullString.startsWith("admin_recall_char_menu"))
			try
			{
				final String targetName = fullString.substring(23);
				final L2Player player = L2World.getPlayer(targetName);
				teleportCharacter(player, activeChar.getLoc(), activeChar);
			}
			catch(final StringIndexOutOfBoundsException e)
			{}
		else if(fullString.startsWith("admin_goto_char_menu"))
			try
			{
				final String targetName = fullString.substring(21);
				final L2Player player = L2World.getPlayer(targetName);
				teleportToCharacter(activeChar, player);
			}
			catch(final StringIndexOutOfBoundsException e)
			{}
		else if(fullString.equals("admin_kill_menu"))
		{
			L2Object obj = activeChar.getTarget();
			final StringTokenizer st = new StringTokenizer(fullString);
			if(st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final L2Player plyr = L2World.getPlayer(player);
				if(plyr != null)
					activeChar.sendMessage("You kicked " + plyr.getName() + " from the game.");
				else
					activeChar.sendMessage("Player " + player + " not found in game.");
				obj = plyr;
			}
			if(obj != null && obj.isCharacter())
			{
				final L2Character target = (L2Character) obj;
				target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null, true, true, true, false);
			}
			else
				activeChar.sendPacket(Msg.INVALID_TARGET);
			AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
		}
		else if(fullString.startsWith("admin_kick_menu"))
		{
			final StringTokenizer st = new StringTokenizer(fullString);
			if(st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final L2Player plyr = L2World.getPlayer(player);
				if(plyr != null)
					plyr.logout(false, false, true);
				if(plyr != null)
					activeChar.sendMessage("You kicked " + plyr.getName() + " from the game.");
				else
					activeChar.sendMessage("Player " + player + " not found in game.");
			}
			AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
		}
		else if(fullString.startsWith("admin_ban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(fullString);
			if(st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final L2Player plyr = L2World.getPlayer(player);
				if(plyr != null)
				{
					plyr.setAccountAccesslevel( -100, "admin_ban_menu", -1);
					plyr.logout(false, false, true);
				}
			}
			AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
		}
		else if(fullString.startsWith("admin_unban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(fullString);
			if(st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final L2Player plyr = L2World.getPlayer(player);
				if(plyr != null)
					plyr.setAccountAccesslevel(0, "admin_unban_menu", 0);
			}
			AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void teleportCharacter(final L2Player player, final Location loc, final L2Player activeChar)
	{
		if(player != null)
		{
			player.sendMessage("Admin is teleporting you.");
			player.teleToLocation(loc);
		}
		AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
	}

	private void teleportToCharacter(final L2Player activeChar, final L2Object target)
	{
		L2Player player;
		if(target != null && target.isPlayer())
			player = (L2Player) target;
		else
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		if(player.getObjectId() == activeChar.getObjectId())
			activeChar.sendMessage("You cannot self teleport.");
		else
		{
			activeChar.teleToLocation(player.getLoc());
			activeChar.sendMessage("You have teleported to character " + player.getName() + ".");
		}
		AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
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