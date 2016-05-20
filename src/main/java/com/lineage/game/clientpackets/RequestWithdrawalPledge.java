package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2ClanMember;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgeShowMemberListDelete;
import com.lineage.game.serverpackets.PledgeShowMemberListDeleteAll;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestWithdrawalPledge extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestWithdrawalPledge");

		//is the guy in a clan  ?
		if(activeChar.getClanId() == 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.ONE_CANNOT_LEAVE_ONES_CLAN_DURING_COMBAT));
			return;
		}

		L2Clan clan = activeChar.getClan();
		if(clan == null)
			return;

		L2ClanMember member = clan.getClanMember(activeChar.getObjectId());
		if(member == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(member.isClanLeader())
		{
			activeChar.sendMessage("A clan leader may not be dismissed.");
			return;
		}

		// this also updated the database
		clan.removeClanMember(activeChar.getObjectId());

		//player withdrawed.
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(activeChar.getName()));

		// Remove the Player From the Member list
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(activeChar.getName()));

		activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS));

		activeChar.setClan(null);

		if(!activeChar.isNoble())
			activeChar.setTitle("");

		activeChar.setLeaveClanCurTime();

		activeChar.broadcastUserInfo(true);

		// disable clan tab
		activeChar.sendPacket(new PledgeShowMemberListDeleteAll());
	}
}