package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Alliance;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Player.TransactionType;
import com.lineage.game.serverpackets.SystemMessage;

/**
 *  format  c(d)
 */
public class RequestAnswerJoinAlly extends L2GameClientPacket
{
	private int _response;

	@Override
	public void readImpl()
	{
		_response = _buf.remaining() >= 4 ? readD() : 0;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar != null)
		{
			L2Player requestor = activeChar.getTransactionRequester();

			activeChar.setTransactionRequester(null);

			if(requestor == null)
				return;

			requestor.setTransactionRequester(null);

			if(requestor.getAlliance() == null)
				return;

			if(activeChar.getTransactionType() != TransactionType.ALLY || activeChar.getTransactionType() != requestor.getTransactionType())
				return;

			if(_response == 1)
			{
				L2Alliance ally = requestor.getAlliance();
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ACCEPTED_THE_ALLIANCE));
				activeChar.getClan().setAllyId(requestor.getAllyId());
				activeChar.getClan().updateClanInDB();
				ally.addAllyMember(activeChar.getClan(), true);
				ally.broadcastAllyStatus();
			}
			else
				requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_CLAN_INTO_THE_ALLIANCE));
		}
	}
}