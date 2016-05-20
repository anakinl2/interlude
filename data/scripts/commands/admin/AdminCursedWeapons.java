package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.model.CursedWeapon;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.tables.ItemTable;

public class AdminCursedWeapons implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_cw_info,
		admin_cw_remove,
		admin_cw_goto,
		admin_cw_reload,
		admin_cw_add
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().Menu)
			return false;

		CursedWeaponsManager cwm = CursedWeaponsManager.getInstance();

		CursedWeapon cw = null;
		switch(command)
		{
			case admin_cw_remove:
			case admin_cw_goto:
			case admin_cw_add:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("Вы не указали id");
					return false;
				}
				for(CursedWeapon cwp : CursedWeaponsManager.getInstance().getCursedWeapons())
					if(cwp.getName().toLowerCase().contains(wordList[1].toLowerCase()))
						cw = cwp;
				if(cw == null)
				{
					activeChar.sendMessage("Неизвестный id");
					return false;
				}
				break;
		}

		switch(command)
		{
			case admin_cw_info:
				activeChar.sendMessage("======= Cursed Weapons: =======");
				for(CursedWeapon c : cwm.getCursedWeapons())
				{
					activeChar.sendMessage("> " + c.getName() + " (" + c.getItemId() + ")");
					if(c.isActivated())
					{
						L2Player pl = c.getPlayer();
						activeChar.sendMessage("  Player holding: " + pl.getName());
						activeChar.sendMessage("  Player karma: " + c.getPlayerKarma());
						activeChar.sendMessage("  Time Remaining: " + c.getTimeLeft() / 60000 + " min.");
						activeChar.sendMessage("  Kills : " + c.getNbKills());
					}
					else if(c.isDropped())
					{
						activeChar.sendMessage("  Lying on the ground.");
						activeChar.sendMessage("  Time Remaining: " + c.getTimeLeft() / 60000 + " min.");
						activeChar.sendMessage("  Kills : " + c.getNbKills());
					}
					else
						activeChar.sendMessage("  Don't exist in the world.");
				}
				break;
			case admin_cw_reload:
				cwm.reload();
				activeChar.sendMessage("Cursed weapons reloaded.");
				break;
			case admin_cw_remove:
				if(cw == null)
					return false;
				CursedWeaponsManager.getInstance().endOfLife(cw);
				break;
			case admin_cw_goto:
				if(cw == null)
					return false;
				activeChar.teleToLocation(cw.getLoc());
				break;
			case admin_cw_add:
				if(cw == null)
					return false;
				if(cw.isActive())
					activeChar.sendMessage("This cursed weapon is already active.");
				else
				{
					L2Object target = activeChar.getTarget();
					if(target != null && target.isPlayer())
					{
						L2Player player = (L2Player) target;
						L2ItemInstance item = ItemTable.getInstance().createItem(cw.getItemId());
						cwm.activate(player, player.getInventory().addItem(item));
						cwm.showUsageTime(player, cw);
					}
				}
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