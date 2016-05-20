package l2d.game.model.instances;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.TradeController;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.model.entity.SevenSigns;
import l2d.game.serverpackets.BuyList;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.templates.L2NpcTemplate;

public final class L2MercManagerInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2MercManagerInstance.class.getName());

	private static int COND_ALL_FALSE = 0;
	private static int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static int COND_OWNER = 2;

	public L2MercManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();

		if(!isInRange(player, INTERACTION_DISTANCE))
			return;

		int condition = validateCondition(player);
		if(condition <= COND_ALL_FALSE || condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;

		if(condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command

			String val = "";
			if(st.countTokens() >= 1)
				val = st.nextToken();

			if(actualCommand.equalsIgnoreCase("hire"))
			{
				if(val.equals(""))
					return;

				showBuyWindow(player, Integer.parseInt(val));
			}
			else
				super.onBypassFeedback(player, command);
		}
	}

	private void showBuyWindow(L2Player player, int val)
	{
		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.fine("Showing buylist");
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player, 0));
		else
		{
			_log.warning("[L2MercManagerInstance] possible client hacker: " + player.getName() + " attempting to buy from GM shop! < Ban him!");
			_log.warning("buylist id:" + val + " / list_npc = " + (list == null ? "nulllist" : list.getNpcId()) + " / npc = " + getNpcId());
		}
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = "data/html/castle/mercmanager/mercmanager-no.htm";
		int condition = validateCondition(player);
		if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/castle/mercmanager/mercmanager-busy.htm"; // Busy because of siege
		else if(condition == COND_OWNER)
			if(SevenSigns.getInstance().getCurrentPeriod() == SevenSigns.PERIOD_SEAL_VALIDATION)
			{
				if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
					filename = "data/html/castle/mercmanager/mercmanager_dawn.htm";
				else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
					filename = "data/html/castle/mercmanager/mercmanager_dusk.htm";
				else
					filename = "data/html/castle/mercmanager/mercmanager.htm";
			}
			else
				filename = "data/html/castle/mercmanager/mercmanager_nohire.htm";
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private int validateCondition(L2Player player)
	{
		if(player.isGM())
			return COND_OWNER;
		if(getCastle() != null && getCastle().getId() > 0)
			if(player.getClan() != null)
				if(getCastle().getSiege().isInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if(getCastle().getOwnerId() == player.getClanId() // Clan owns castle
						&& (player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES) // has merc rights
					return COND_OWNER; // Owner

		return COND_ALL_FALSE;
	}
}