package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Player.TransactionType;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * [C] D0:15 AnswerJoinPartyRoom <p>
 * <b>Format:</b> (ch)d
 * @author Felixx
 */
public class AnswerJoinPartyRoom extends L2GameClientPacket
{
	private int _response;

	@Override
	public void readImpl()
	{
		if(_buf.hasRemaining())
			_response = readD();
		else
			_response = 0;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Player requestor = activeChar.getTransactionRequester();

		activeChar.setTransactionRequester(null);

		if(requestor == null)
			return;

		requestor.setTransactionRequester(null);

		if(activeChar.getTransactionType() != TransactionType.PARTY_ROOM || activeChar.getTransactionType() != requestor.getTransactionType())
			return;

		if(_response == 1)
		{
			if(requestor.getPartyRoom() <= 0)
			{
				activeChar.sendActionFailed();
				return;
			}
			if(activeChar.getPartyRoom() > 0)
			{
				activeChar.sendActionFailed();
				return;
			}
			PartyRoomManager.getInstance().joinPartyRoom(activeChar, requestor.getPartyRoom());
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.THE_PLAYER_DECLINED_TO_JOIN_YOUR_PARTY));

		//TODO проверить на наличие пакета ДОБАВЛЕНИЯ в список, в другом случае отсылать весь список всем мемберам
	}
}