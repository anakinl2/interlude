package l2d.game.clientpackets;

import l2d.game.model.L2Alliance;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;

/**
 * format: c
 */
public class RequestWithdrawAlly extends L2GameClientPacket
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

		L2Clan clan = activeChar.getClan();
		if(clan == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_MAY_APPLY_FOR_WITHDRAWAL_FROM_THE_ALLIANCE));
			return;
		}

		if(clan.getAlliance() == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			return;
		}

		if(clan.equals(clan.getAlliance().getLeader()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.ALLIANCE_LEADERS_CANNOT_WITHDRAW));
			return;
		}

		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOU_HAVE_WITHDRAWN_FROM_THE_ALLIANCE));
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.A_CLAN_THAT_HAS_WITHDRAWN_OR_BEEN_EXPELLED_CANNOT_ENTER_INTO_AN_ALLIANCE_WITHIN_ONE_DAY_OF_WITHDRAWAL_OR_EXPULSION));
		L2Alliance alliance = clan.getAlliance();
		clan.setAllyId(0);
		clan.setLeavedAlly();
		alliance.broadcastAllyStatus();
		alliance.removeAllyMember(clan.getClanId());
	}
}