package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

/**
 * [C] 52 RequestReplySurrenderPledgeWar
 * opcode(id) = 0x08
 * 
 * @author Felixx
 */
public final class RequestReplySurrenderPledgeWar extends L2GameClientPacket
{
	private int _answer;

	@Override
	protected void readImpl()
	{
		@SuppressWarnings("unused")
		final String _reqName = readS();
		_answer = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		final L2Player requestor = activeChar.getTransactionRequester();
		if(requestor == null)
			return;

		if(_answer == 1)
		{
			// requestor.deathPenalty(false, false, false); TODO wtf
			//ClanTable.getInstance().deleteclanswars(requestor.getClanId(), activeChar.getClanId());
		}
		else
		{}

		activeChar.setTransactionRequester(null);
	}
}