package com.lineage.game.model.instances;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.Warehouse;
import com.lineage.game.model.Warehouse.WarehouseType;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.PackageToList;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.WareHouseDepositList;
import com.lineage.game.serverpackets.WareHouseWithdrawList;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Log;

public final class L2WarehouseInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2WarehouseInstance.class.getName());

	/**
	 * @param template
	 */
	public L2WarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		return "data/html/warehouse/" + pom + ".htm";
	}

	private void showRetrieveWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		if(Config.DEBUG)
			_log.fine("Showing stored items");
		player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.PRIVATE, L2ItemInstance.ItemClass.values()[val]));
	}

	private void showDepositWindow(L2Player player)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.fine("Showing items to deposit");

		player.sendPacket(new WareHouseDepositList(player, WarehouseType.PRIVATE));
		player.sendActionFailed();
	}

	private void showDepositWindowClan(L2Player player)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		if(player.getClan() == null)
		{
			player.sendActionFailed();
			return;
		}

		if(player.getClan().getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		player.setUsingWarehouseType(WarehouseType.CLAN);
		player.tempInvetoryDisable();

		if(Config.DEBUG)
			_log.fine("Showing items to deposit - clan");

		if(!(player.isClanLeader() || player.getVarB("canWhWithdraw") && (player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
			player.sendPacket(Msg.ITEMS_LEFT_AT_THE_CLAN_HALL_WAREHOUSE_CAN_ONLY_BE_RETRIEVED_BY_THE_CLAN_LEADER_DO_YOU_WANT_TO_CONTINUE);

		player.sendPacket(new WareHouseDepositList(player, WarehouseType.CLAN));
	}

	private void showWithdrawWindowClan(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		if(player.getClan() == null)
		{
			player.sendActionFailed();
			return;
		}

		L2Clan _clan = player.getClan();

		if(_clan.getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		if(/*Config.ALT_ALLOW_OTHERS_WITHDRAW_FROM_CLAN_WAREHOUSE&&*/(player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.setUsingWarehouseType(WarehouseType.CLAN);
			player.tempInvetoryDisable();
			player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.CLAN, L2ItemInstance.ItemClass.values()[val]));
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE));
			player.sendActionFailed();
		}
	}

	private void showWithdrawWindowFreight(L2Player player)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		player.setUsingWarehouseType(WarehouseType.FREIGHT);
		if(Config.DEBUG)
			_log.fine("Showing freightened items");

		Warehouse list = player.getFreight();

		if(list != null)
			player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.FREIGHT, L2ItemInstance.ItemClass.ALL));
		else if(Config.DEBUG)
			_log.fine("no items freightened");

		player.sendActionFailed();
	}

	private void showDepositWindowFreight(L2Player player)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		player.setUsingWarehouseType(WarehouseType.FREIGHT);

		if(Config.DEBUG)
			_log.fine("Showing destination chars to freight - char src: " + player.getName());

		player.sendPacket(new PackageToList());
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(player.getEnchantScroll() != null)
		{
			Log.add("Player " + player.getName() + " trying to use enchant exploit[Warehouse], ban this player!", "illegal-actions");
			player.setEnchantScroll(null);
			return;
		}

		if(command.startsWith("WithdrawP"))
		{
			int val = Integer.parseInt(command.substring(10));
			if(val == 9)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/warehouse/personal.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else
				showRetrieveWindow(player, val);
		}
		else if(command.equals("DepositP"))
			showDepositWindow(player);
		else if(command.startsWith("WithdrawC"))
		{
			int val = Integer.parseInt(command.substring(10));
			if(val == 9)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/warehouse/clan.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else
				showWithdrawWindowClan(player, val);
		}
		else if(command.equals("DepositC"))
			showDepositWindowClan(player);
		else if(command.startsWith("WithdrawF"))
		{
			if(Config.ALLOW_FREIGHT)
				showWithdrawWindowFreight(player);
		}
		else if(command.startsWith("DepositF"))
		{
			if(Config.ALLOW_FREIGHT)
				showDepositWindowFreight(player);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
