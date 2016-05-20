package l2d.game.model.instances;

import java.util.StringTokenizer;

import javolution.util.FastList;
import l2d.game.TradeController;
import l2d.game.ai.CtrlIntention;
import l2d.game.cache.Msg;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.instancemanager.CastleManorManager.SeedProduction;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.serverpackets.BuyList;
import l2d.game.serverpackets.BuyListSeed;
import l2d.game.serverpackets.ExShowCropInfo;
import l2d.game.serverpackets.ExShowManorDefaultInfo;
import l2d.game.serverpackets.ExShowProcureCropDetail;
import l2d.game.serverpackets.ExShowSeedInfo;
import l2d.game.serverpackets.ExShowSellCropList;
import l2d.game.serverpackets.MyTargetSelected;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.ValidateLocation;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2NpcTemplate;

public class L2ManorManagerInstance extends L2MerchantInstance
{
	public L2ManorManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2Player player)
	{
		if(this != player.getTarget())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			if(!isInRange(player, INTERACTION_DISTANCE))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				player.sendActionFailed();
			}
			else
			{
				if(CastleManorManager.getInstance().isDisabled())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/npcdefault.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				else if(!player.isGM() // Player is not GM
						&& getCastle() != null && getCastle().getId() > 0 // Verification of castle
						&& player.getClan() != null // Player have clan
						&& getCastle().getOwnerId() == player.getClanId() // Player's clan owning the castle
						&& player.isClanLeader() // Player is clan leader of clan (then he is the lord)
				)
					showMessageWindow(player, "manager-lord.htm");
				else
					showMessageWindow(player, "manager.htm");
				player.sendActionFailed();
			}
		}
	}

	@Override
	protected void showBuyWindow(L2Player player, int val)
	{
		double taxRate = 0;
		player.tempInvetoryDisable();
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null)
		{
			list.getItems().get(0).setCount(1);
			BuyList bl = new BuyList(list, player, taxRate);
			player.sendPacket(bl);
		}

		player.sendActionFailed();
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc().getDistance(player) > 300)
			return;

		if(command.startsWith("manor_menu_select"))
		{ // input string format:
			// manor_menu_select?ask=X&state=Y&time=X
			if(CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendActionFailed();
				player.sendPacket(Msg.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				return;
			}

			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			int state = Integer.parseInt(st.nextToken().split("=")[1]);
			int time = Integer.parseInt(st.nextToken().split("=")[1]);

			int castleId;
			if(state == -1) // info for current manor
				castleId = getCastle().getId();
			else
				// info for requested manor
				castleId = state;

			switch(ask)
			{ // Main action
				case 1: // Seed purchase
					if(castleId != getCastle().getId())
						player.sendPacket(new SystemMessage(SystemMessage._HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR));
					else
					{
						L2TradeList tradeList = new L2TradeList(0);
						FastList<SeedProduction> seeds = getCastle().getSeedProduction(CastleManorManager.PERIOD_CURRENT);

						for(SeedProduction s : seeds)
						{
							L2ItemInstance item = ItemTable.getInstance().createDummyItem(s.getId());
							item.setPriceToSell(s.getPrice());
							item.setCount(s.getCanProduce());
							if(item.getIntegerLimitedCount() > 0 && item.getPriceToSell() > 0)
								tradeList.addItem(item);
						}

						BuyListSeed bl = new BuyListSeed(tradeList, castleId, player.getAdena());
						player.sendPacket(bl);
					}
					break;
				case 2: // Crop sales
					player.sendPacket(new ExShowSellCropList(player, castleId, getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT)));
					break;
				case 3: // Current seeds (Manor info)
					if(time == 1 && !CastleManager.getInstance().getCastleByIndex(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					else
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleByIndex(castleId).getSeedProduction(time)));
					break;
				case 4: // Current crops (Manor info)
					if(time == 1 && !CastleManager.getInstance().getCastleByIndex(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowCropInfo(castleId, null));
					else
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleByIndex(castleId).getCropProcure(time)));
					break;
				case 5: // Basic info (Manor info)
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				case 6: // Buy harvester
					showBuyWindow(player, Integer.parseInt("3" + getNpcId()));
					break;
				case 9: // Edit sales (Crop sales)
					player.sendPacket(new ExShowProcureCropDetail(state));
					break;
			}
		}
		else if(command.startsWith("help"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // discard first
			String filename = "manor_client_help00" + st.nextToken() + ".htm";
			showMessageWindow(player, filename);
		}
		else
			super.onBypassFeedback(player, command);
	}

	public String getHtmlPath()
	{
		return "data/html/manormanager/";
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/manormanager/manager.htm"; // Used only in parent method
		// to return from "Territory status"
		// to initial screen.
	}

	private void showMessageWindow(L2Player player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile(getHtmlPath() + filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
