package com.lineage.game.model.instances;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.residence.Residence;
import com.lineage.game.serverpackets.AgitDecoInfo;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.L2NpcTemplate;

public class L2ClanHallManagerInstance extends L2ResidenceManager
{
	protected static int Cond_All_False = 0;
	protected static int Cond_Busy_Because_Of_Siege = 1;
	protected static int Cond_Owner = 2;

	public L2ClanHallManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2Player player)
	{
		super.onAction(player);
		int condition = validateCondition(player);
		if(condition != Cond_Owner)
			return;
		ClanHall ch = getClanHall();
		if(ch.getOwner() == null)
			return;
		L2ItemInstance adena = ch.getOwner().getAdena();
		if(adena != null && adena.getCount() >= ch.getLease())
			return;
		if(ch.getPaidUntil() <= System.currentTimeMillis() + 24 * 60 * 60 * 1000 && ch.getPaidUntil() >= System.currentTimeMillis() + 12 * 60 * 60 * 1000)
			player.sendPacket(new SystemMessage(SystemMessage.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW).addNumber(ch.getLease()));
		else if(ch.isInDebt())
			player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = "data/html/residence/chamberlain-no.htm";
		int condition = validateCondition(player);
		if(condition > Cond_All_False)
			if(condition == Cond_Busy_Because_Of_Siege)
				filename = "data/html/residence/chamberlain-busy.htm"; // Busy because of siege
			else if(condition == Cond_Owner) // Clan owns Residence
				filename = "data/html/residence/chamberlain.htm"; // Owner message window
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	protected int validateCondition(L2Player player)
	{
		if(player.isGM())
			return Cond_Owner;
		if(player.getClan() != null)
			if(getResidence().getSiege() != null && getResidence().getSiege().isInProgress())
				return Cond_Busy_Because_Of_Siege;
			else if(getResidence().getOwnerId() == player.getClanId())
				return Cond_Owner;
		return Cond_All_False;
	}

	@Override
	protected Residence getResidence()
	{
		return getClanHall();
	}

	public void sendDecoInfo(L2Player player)
	{
		ClanHall clanHall = getClanHall();
		if(clanHall != null)
			player.sendPacket(new AgitDecoInfo(getClanHall()));
	}

	@Override
	public void broadcastDecoInfo()
	{
		for(L2Player player : L2World.getAroundPlayers(this))
			if(player != null)
				sendDecoInfo(player);
	}

	@Override
	protected int getPrivFunctions()
	{
		return L2Clan.CP_CH_SET_FUNCTIONS;
	}

	@Override
	protected int getPrivDismiss()
	{
		return L2Clan.CP_CH_DISMISS;
	}

	@Override
	protected int getPrivDoors()
	{
		return L2Clan.CP_CH_OPEN_DOOR;
	}
}