package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Player.TransactionType;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestExMPCCAcceptJoin extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _response, _unk;

	/*
	 * format: chdd
	 */
	@Override
	public void readImpl()
	{
		_response = _buf.hasRemaining() ? readD() : 0;
		_unk = _buf.hasRemaining() ? readD() : 0;
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

		if(activeChar.getTransactionType() != TransactionType.CHANNEL || activeChar.getTransactionType() != requestor.getTransactionType())
			return;

		if(!requestor.isInParty() || !activeChar.isInParty() || !requestor.getParty().isInCommandChannel() || activeChar.getParty().isInCommandChannel())
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.NO_USER_HAS_BEEN_INVITED_TO_THE_COMMAND_CHANNEL));
			return;
		}

		if(_response == 1 && activeChar.isTeleporting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_JOIN_A_COMMAND_CHANNEL_WHILE_TELEPORTING));
			requestor.sendPacket(new SystemMessage(SystemMessage.NO_USER_HAS_BEEN_INVITED_TO_THE_COMMAND_CHANNEL));
			return;
		}

		if(_response == 1)
			requestor.getParty().getCommandChannel().addParty(activeChar.getParty());
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DECLINED_THE_CHANNEL_INVITATION).addString(activeChar.getName()));
	}
}