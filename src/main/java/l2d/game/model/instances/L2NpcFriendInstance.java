package l2d.game.model.instances;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import l2d.Config;
import l2d.ext.scripts.Events;
import l2d.game.TradeController;
import l2d.game.ai.CtrlIntention;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.model.Warehouse.WarehouseType;
import l2d.game.model.instances.L2ItemInstance.ItemClass;
import l2d.game.serverpackets.BuyList;
import l2d.game.serverpackets.MyTargetSelected;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.SellList;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.ValidateLocation;
import l2d.game.serverpackets.WareHouseDepositList;
import l2d.game.serverpackets.WareHouseWithdrawList;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2NpcTemplate;

public final class L2NpcFriendInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2NpcFriendInstance.class.getName());

	public L2NpcFriendInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	private long _lastSocialAction;

	/**
	 * this is called when a player interacts with this NPC
	 * 
	 * @param player
	 */
	@Override
	public void onAction(L2Player player)
	{
		if(this != player.getTarget())
		{
			if(Config.DEBUG)
				_log.fine("new target selected:" + getObjectId());
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			if(isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			player.sendPacket(new ValidateLocation(this));
			player.sendActionFailed();
			return;
		}

		player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

		if(Events.onAction(player, this))
			return;

		if(isAutoAttackable(player))
		{
			player.getAI().Attack(this, false);
			return;
		}

		if(!isInRange(player, INTERACTION_DISTANCE))
		{
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
			return;
		}

		if(player.getKarma() > 0 && !player.isGM())
		{
			player.sendActionFailed();
			return;
		}

		// С NPC нельзя разговаривать мертвым и сидя
		if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
			return;

		if(System.currentTimeMillis() - _lastSocialAction > 10000)
			broadcastPacket(new SocialAction(getObjectId(), 2));

		_lastSocialAction = System.currentTimeMillis();

		player.sendActionFailed();

		String filename = "";

		if(getNpcId() >= 31370 && getNpcId() <= 31376 && player.getVarka() > 0 || getNpcId() >= 31377 && getNpcId() < 31384 && player.getKetra() > 0)
		{
			filename = "data/html/npc_friend/" + getNpcId() + "-nofriend.htm";
			showChatWindow(player, filename);
			return;
		}

		switch(getNpcId())
		{
			case 31370:
			case 31371:
			case 31373:
			case 31377:
			case 31378:
			case 31380:
			case 31553:
			case 31554:
				filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31372:
				if(player.getKetra() > 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-bufflist.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31379:
				if(player.getVarka() > 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-bufflist.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31374:
				if(player.getKetra() > 1)
					filename = "data/html/npc_friend/" + getNpcId() + "-warehouse.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31381:
				if(player.getVarka() > 1)
					filename = "data/html/npc_friend/" + getNpcId() + "-warehouse.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31375:
				if(player.getKetra() == 3 || player.getKetra() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-special1.htm";
				else if(player.getKetra() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special2.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31382:
				if(player.getVarka() == 3 || player.getVarka() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-special1.htm";
				else if(player.getVarka() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special2.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31376:
				if(player.getKetra() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-normal.htm";
				else if(player.getKetra() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31383:
				if(player.getVarka() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-normal.htm";
				else if(player.getVarka() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31555:
				if(player.getRam() == 1)
					filename = "data/html/npc_friend/" + getNpcId() + "-special1.htm";
				else if(player.getRam() == 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-special2.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31556:
				if(player.getRam() == 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-bufflist.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
		}

		showChatWindow(player, filename);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("Buff"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			int item = 0;

			switch(getNpcId())
			{
				case 31372:
					item = 7186;
					break;
				case 31379:
					item = 7187;
					break;
				case 31556:
					item = 7251;
					break;
			}

			int skill = 0;
			int level = 0;
			int count = 0;

			switch(val)
			{
				case 1:
					skill = 4359;
					level = 2;
					count = 2;
					break;
				case 2:
					skill = 4360;
					level = 2;
					count = 2;
					break;
				case 3:
					skill = 4345;
					level = 3;
					count = 3;
					break;
				case 4:
					skill = 4355;
					level = 2;
					count = 3;
					break;
				case 5:
					skill = 4352;
					level = 1;
					count = 3;
					break;
				case 6:
					skill = 4354;
					level = 3;
					count = 3;
					break;
				case 7:
					skill = 4356;
					level = 1;
					count = 6;
					break;
				case 8:
					skill = 4357;
					level = 2;
					count = 6;
					break;
			}

			if(skill != 0 && player.getInventory().getItemByItemId(item) != null && item > 0 && player.getInventory().getItemByItemId(item).getIntegerLimitedCount() >= count)
			{
				if(player.getInventory().destroyItemByItemId(item, count, false) == null)
					_log.info("L2NpcFriendInstance[274]: Item not found!!!");
				player.doCast(SkillTable.getInstance().getInfo(skill, level), player, true);
			}
			else
				showChatWindow(player, "data/html/npc_friend/" + getNpcId() + "-havenotitems.htm");
		}
		else if(command.startsWith("Chat"))
		{
			int val = Integer.parseInt(command.substring(5));
			String fname = "";
			fname = "data/html/npc_friend/" + getNpcId() + "-" + val + ".htm";
			if(!fname.equals(""))
				showChatWindow(player, fname);
		}
		else if(command.startsWith("Buy"))
		{
			int val = Integer.parseInt(command.substring(4));
			showBuyWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Sell"))
			showSellWindow(player);
		else if(command.startsWith("WithdrawP"))
		{
			int val = Integer.parseInt(command.substring(10));
			if(val == 9)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/npc-friend/personal.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else
				showRetrieveWindow(player, val);
		}
		else if(command.equals("DepositP"))
			showDepositWindow(player);
		else
			super.onBypassFeedback(player, command);
	}

	private void showBuyWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseShop)
			return;

		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.fine("Showing buylist");
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player, 0));
		else
		{
			_log.warning("[L2NpcFriendInstance] possible client hacker: " + player.getName() + " attempting to buy from GM shop! < Ban him!");
			_log.warning("buylist id:" + val + " / list_npc = " + (list == null ? "nulllist" : list.getNpcId()) + " / npc = " + getNpcId());
		}
	}

	private void showSellWindow(L2Player player)
	{
		if(!player.getPlayerAccess().UseShop)
			return;

		if(Config.DEBUG)
			_log.fine("Showing selllist");
		SellList sl = new SellList(player);
		player.sendPacket(sl);
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

	private void showRetrieveWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		if(Config.DEBUG)
			_log.fine("Showing stored items");

		player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.PRIVATE, ItemClass.values()[val]));
	}

}
