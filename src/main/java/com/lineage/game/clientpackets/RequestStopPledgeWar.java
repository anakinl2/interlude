package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2ClanMember;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;

public class RequestStopPledgeWar extends L2GameClientPacket
{
	//Format: cS
	private static Logger _log = Logger.getLogger(RequestStopPledgeWar.class.getName());

	String _pledgeName;

	@Override
	public void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestStopPledgeWar");

		L2Clan playerClan = activeChar.getClan();
		if(playerClan == null)
			return;

		if(!((activeChar.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) == L2Clan.CP_CL_PLEDGE_WAR))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			activeChar.sendActionFailed();
			return;
		}

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

		if(clan == null)
		{
			activeChar.sendMessage(new CustomMessage("com.lineage.game.clientpackets.RequestStopPledgeWar.NoSuchClan", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		if(!playerClan.isAtWarWith(clan.getClanId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_NOT_DECLARED_A_CLAN_WAR_TO_S1_CLAN));
			activeChar.sendActionFailed();
			return;
		}

		for(L2ClanMember mbr : clan.getMembers())
			if(mbr.isOnline() && mbr.getPlayer().isInCombat())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.A_CEASE_FIRE_DURING_A_CLAN_WAR_CAN_NOT_BE_CALLED_WHILE_MEMBERS_OF_YOUR_CLAN_ARE_ENGAGED_IN_BATTLE));
				activeChar.sendActionFailed();
				return;
			}

		_log.info("RequestStopPledgeWar: By player: " + activeChar.getName() + " of clan: " + playerClan.getName() + " to clan: " + _pledgeName);

		ClanTable.getInstance().stopClanWar(playerClan, clan);
	}
}