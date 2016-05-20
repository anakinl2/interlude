package l2d.game.clientpackets;

import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.serverpackets.JoinPledge;
import l2d.game.serverpackets.PledgeShowInfoUpdate;
import l2d.game.serverpackets.PledgeShowMemberListAdd;
import l2d.game.serverpackets.PledgeShowMemberListAll;
import l2d.game.serverpackets.SystemMessage;

public class RequestAnswerJoinPledge extends L2GameClientPacket
{
	//Format: cd
	private int _response;

	@Override
	public void readImpl()
	{
		_response = _buf.hasRemaining() ? readD() : 0;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestAnswerJoinPledge");

		L2Player requestor = activeChar.getTransactionRequester();

		activeChar.setTransactionRequester(null);

		if(requestor == null)
			return;

		requestor.setTransactionRequester(null);

		if(requestor.getClan() == null)
			return;

		if(activeChar.getTransactionType() != TransactionType.CLAN || activeChar.getTransactionType() != requestor.getTransactionType())
			return;

		if(_response == 1)
		{
			if(activeChar.canJoinClan())
			{
				activeChar.sendPacket(new JoinPledge(requestor.getClanId()));

				L2Clan clan = requestor.getClan();

				clan.addClanMember(activeChar);
				activeChar.setClan(clan);
				clan.getClanMember(activeChar.getName()).setPlayerInstance(activeChar);

				if(clan.isAcademy(activeChar.getPledgeType()))
					activeChar.setLvlJoinedAcademy(activeChar.getLevel());

				clan.getClanMember(activeChar.getName()).setPowerGrade(clan.getAffiliationRank(activeChar.getPledgeType()));

				activeChar.sendPacket(new SystemMessage(SystemMessage.ENTERED_THE_CLAN));
				clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_HAS_JOINED_THE_CLAN).addString(activeChar.getName()));
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(clan.getClanMember(activeChar.getName())), activeChar);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));

				// this activates the clan tab on the new member
				activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
				activeChar.setLeaveClanTime(0);
				activeChar.updatePledgeClass();
				clan.addAndShowSkillsToPlayer(activeChar);
				activeChar.broadcastUserInfo(true);

				activeChar.store(false);
			}
			else
			{
				requestor.sendPacket(new SystemMessage(SystemMessage.AFTER_A_CLAN_MEMBER_IS_DISMISSED_FROM_A_CLAN_THE_CLAN_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_ACCEPTING_A_NEW_MEMBER));
				activeChar.sendPacket(new SystemMessage(SystemMessage.AFTER_LEAVING_OR_HAVING_BEEN_DISMISSED_FROM_A_CLAN_YOU_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_JOINING_ANOTHER_CLAN));
				activeChar.setPledgeType(0);
			}
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_REFUSED_TO_JOIN_THE_CLAN).addString(activeChar.getName()));
			activeChar.setPledgeType(0);
		}
	}
}