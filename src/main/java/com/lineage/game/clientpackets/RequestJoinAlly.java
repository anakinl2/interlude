package com.lineage.game.clientpackets;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Player.TransactionType;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.AskJoinAlliance;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestJoinAlly extends L2GameClientPacket
{
	// format: cd

	private int _id;

	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || activeChar.getClan() == null || activeChar.getAlliance() == null)
			return;

		if(activeChar.getAlliance().getMembersCount() >= Config.ALT_MAX_ALLY_SIZE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_CLAN_INTO_THE_ALLIANCE));
			return;
		}

		L2Player target = (L2Player) L2World.findObject(_id);
		if(target == null)
		{
			activeChar.sendPacket(Msg.THAT_PLAYER_IS_NOT_ONLINE);
			return;
		}
		if(target.getClan() == null)
		{
			activeChar.sendActionFailed();
			return;
		}
		if(!activeChar.isAllyLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}
		if(target.getAlliance() != null || activeChar.getAlliance().isMember(target.getClan().getClanId()))
		{
			//same or another alliance - no need to invite
			SystemMessage sm = new SystemMessage(SystemMessage.S1_CLAN_IS_ALREADY_A_MEMBER_OF_S2_ALLIANCE);
			sm.addString(target.getClan().getName());
			sm.addString(target.getAlliance().getAllyName());
			activeChar.sendPacket(sm);
			return;
		}
		if(!target.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_A_CLAN_LEADER).addString(target.getName()));
			return;
		}
		if(activeChar.isAtWarWith(target.getClanId()) > 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_ALLY_WITH_A_CLAN_YOU_ARE_AT_BATTLE_WITH));
			return;
		}
		if(!target.getClan().canJoinAlly())
		{
			SystemMessage sm = new SystemMessage(SystemMessage.S1_CLAN_CANNOT_JOIN_THE_ALLIANCE_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_IT_LEFT_ANOTHER_ALLIANCE);
			sm.addString(target.getClan().getName());
			activeChar.sendPacket(sm);
			return;
		}
		if(!activeChar.getAlliance().canInvite())
			activeChar.sendMessage(new CustomMessage("com.lineage.game.clientpackets.RequestJoinAlly.InvitePenalty", activeChar));
		if(activeChar.isTransactionInProgress())
		{
			activeChar.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		if(target.isTransactionInProgress())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));
			return;
		}
		target.setTransactionRequester(activeChar, System.currentTimeMillis() + 10000);
		target.setTransactionType(TransactionType.ALLY);
		activeChar.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		activeChar.setTransactionType(TransactionType.ALLY);
		//leader of alliance request an alliance.
		SystemMessage sm = new SystemMessage(SystemMessage.S2_THE_LEADER_OF_S1_HAS_REQUESTED_AN_ALLIANCE);
		sm.addString(activeChar.getAlliance().getAllyName());
		sm.addString(activeChar.getName());
		target.sendPacket(sm);
		target.sendPacket(new AskJoinAlliance(activeChar.getObjectId(), activeChar.getName(), activeChar.getAlliance().getAllyName()));
		return;
	}
}