package com.lineage.game.model.instances;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.siege.clanhall.ClanHallSiege;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.SiegeInfo;
import com.lineage.game.templates.L2NpcTemplate;

public class L2ClanHallMessengerInstance extends L2NpcInstance
{
	public L2ClanHallMessengerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		ClanHall clanhall = getClanHall();
		if(clanhall == null)
			return;
		ClanHallSiege siege = clanhall.getSiege();
		if(siege == null)
			return;
		if(siege.isInProgress() || siege.isRegistrationOver())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setHtml("<html><body><font color=\"LEVEL\">I can't do anything for you right now.</font></body></html>");
			player.sendPacket(html);
		}
		else
			player.sendPacket(new SiegeInfo(getClanHall()));
	}
}
