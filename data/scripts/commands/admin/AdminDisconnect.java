package commands.admin;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.SystemMessage;

public class AdminDisconnect implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_disconnect,
		admin_kick
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanKick)
			return false;

		switch(command)
		{
			case admin_disconnect:
			case admin_kick:
				final L2Player player;
				if(wordList.length == 1)
				{
					// Обработка по таргету
					L2Object target = activeChar.getTarget();
					if(target == null)
					{
						activeChar.sendMessage("Select character or specify player name.");
						break;
					}
					if(!target.isPlayer())
					{
						activeChar.sendPacket(Msg.INVALID_TARGET);
						break;
					}
					player = (L2Player) target;
				}
				else
				{
					// Обработка по нику
					player = L2World.getPlayer(wordList[1]);
					if(player == null)
					{
						activeChar.sendMessage("Character " + wordList[1] + " not found in game.");
						break;
					}
				}

				if(player.getObjectId() == activeChar.getObjectId())
				{
					activeChar.sendMessage("You can't logout your character.");
					break;
				}

				activeChar.sendMessage("Character " + player.getName() + " disconnected from server.");

				if(player.isInOfflineMode())
				{
					player.setOfflineMode(false);
					player.logout(false, false, true);
					if(player.getNetConnection() != null)
						player.getNetConnection().disconnectOffline();
					return true;
				}

				player.sendMessage(new CustomMessage("scripts.commands.admin.AdminDisconnect.YoureKickedByGM", player));
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_DISCONNECTED_FROM_THE_SERVER_PLEASE_LOGIN_AGAIN));
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
					public void run()
					{
						player.logout(false, false, true);
					}
				}, 500);
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