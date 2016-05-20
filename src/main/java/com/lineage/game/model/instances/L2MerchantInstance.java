package com.lineage.game.model.instances;

import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.TradeController;
import com.lineage.game.communitybbs.Manager.TopBBSManager;
import com.lineage.game.model.L2Multisell;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.serverpackets.BuyList;
import com.lineage.game.serverpackets.SellList;
import com.lineage.game.serverpackets.ShopPreviewList;
import com.lineage.game.templates.L2NpcTemplate;

public class L2MerchantInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2MerchantInstance.class.getName());

	public L2MerchantInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		String temp = "data/html/merchant/" + pom + ".htm";
		File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/teleporter/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/petmanager/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/default/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		return "data/html/teleporter/" + pom + ".htm";
	}

	private void showWearWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseShop)
			return;

		if(!Config.WEAR_ENABLED)
		{
			player.sendActionFailed();
			return;
		}

		player.tempInvetoryDisable();

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if(list != null)
			player.sendPacket(new ShopPreviewList(list, player.getAdena(), player.expertiseIndex));
		else
			player.sendActionFailed();
	}

	protected void showBuyWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseShop)
			return;

		double taxRate = 0;

		if(getCastle() != null)
			taxRate = getCastle().getTaxRate();

		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.fine("Showing buylist");
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player, taxRate));
		else
		{
			_log.warning("[L2MerchantInstance] possible client hacker: " + player.getName() + " attempting to buy from GM shop! < Ban him!");
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

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("Buy"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("topclass"))
		{
			if(st.countTokens() < 1)
				return;
			TopBBSManager.getInstance().showTopbyClass(player, Integer.parseInt(st.nextToken()));
		}
		else if(actualCommand.equalsIgnoreCase("Sell"))
			showSellWindow(player);
		else if(actualCommand.equalsIgnoreCase("Wear"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Multisell"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().SeparateAndSend(val, player, getCastle() != null ? getCastle().getTaxRate() : 0);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
