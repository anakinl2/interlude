package com.lineage.game.clientpackets;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Alliance;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;

public class RequestOustAlly extends L2GameClientPacket
{
	private String _clanName;

	@Override
	public void readImpl()
	{
		_clanName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Clan leaderClan = activeChar.getClan();
		if(leaderClan == null)
		{
			activeChar.sendActionFailed();
			return;
		}
		L2Alliance alliance = leaderClan.getAlliance();
		if(alliance == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			return;
		}

		L2Clan clan;

		if(!activeChar.isAllyLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}

		if(_clanName == null)
			return;

		clan = ClanTable.getInstance().getClanByName(_clanName);

		if(clan != null)
		{
			if(!alliance.isMember(clan.getClanId()))
			{
				activeChar.sendActionFailed();
				return;
			}

			if(alliance.getLeader().equals(clan))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_WITHDRAW_FROM_THE_ALLIANCE));
				return;
			}

			clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1).addString("Your clan has been expelled from " + alliance.getAllyName() + " alliance."));
			clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.A_CLAN_THAT_HAS_WITHDRAWN_OR_BEEN_EXPELLED_CANNOT_ENTER_INTO_AN_ALLIANCE_WITHIN_ONE_DAY_OF_WITHDRAWAL_OR_EXPULSION));
			clan.setAllyId(0);
			clan.setLeavedAlly();
			alliance.broadcastAllyStatus();
			alliance.removeAllyMember(clan.getClanId());
			alliance.setExpelledMember();
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestOustAlly.ClanDismissed", activeChar).addString(clan.getName()).addString(alliance.getAllyName()));
		}
	}
}