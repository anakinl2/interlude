package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2ClanMember;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgeShowMemberListDelete;
import com.lineage.game.serverpackets.PledgeShowMemberListDeleteAll;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestOustPledgeMember extends L2GameClientPacket
{
	//Format: cS
	static Logger _log = Logger.getLogger(RequestOustPledgeMember.class.getName());

	private String _target;

	@Override
	public void readImpl()
	{
		_target = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null || !((activeChar.getClanPrivileges() & L2Clan.CP_CL_DISMISS) == L2Clan.CP_CL_DISMISS))
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestOustPledgeMember");

		L2Clan clan = activeChar.getClan();
		L2ClanMember member = clan.getClanMember(_target);
		if(member == null)
		{
			_log.warning("target is not member of the clan");
			return;
		}

		if(member.isOnline() && member.getPlayer().isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.A_CLAN_MEMBER_MAY_NOT_BE_DISMISSED_DURING_COMBAT));
			return;
		}

		if(member.isClanLeader())
		{
			activeChar.sendMessage("A clan leader may not be dismissed.");
			return;
		}

		clan.removeClanMember(member.getObjectId());
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED).addString(_target));
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(_target));
		clan.setExpelledMember();

		if(member.isOnline())
		{
			L2Player player = member.getPlayer();
			player.setClan(null);
			if(!player.isNoble())
				player.setTitle("");
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS));
			player.setLeaveClanCurTime();

			player.broadcastUserInfo(true);

			// disable clan tab
			player.sendPacket(new PledgeShowMemberListDeleteAll());
		}
	}
}