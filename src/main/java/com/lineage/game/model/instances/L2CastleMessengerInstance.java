package com.lineage.game.model.instances;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.templates.L2NpcTemplate;

public class L2CastleMessengerInstance extends L2NpcInstance
{
	public L2CastleMessengerInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	/**
	 * If siege is in progress shows the Busy HTML<BR>
	 * else Shows the SiegeInfo window
	 * @param player
	 */
	@Override
	public void showChatWindow(L2Player player, int val)
	{
		if(!getCastle().getSiege().isInProgress())
			getCastle().getSiege().listRegisterClan(player);
		else
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setHtml("<html><body><font color=\"LEVEL\">Oh! Our castle is being attacked and I can't do anything for you right now.</font></body></html>");
			player.sendPacket(html);
		}
	}
}