package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

/**
 * [C] 4e RequestReplyStartPledgeWar <p>
 * opcode(id) = 0x04
 * 
 * @author Felixx
 */
public final class RequestReplyStartPledgeWar extends L2GameClientPacket
{
	private int _answer;
	@SuppressWarnings("unused")
	private String _reqName;

	@Override
	protected void readImpl()
	{
		_reqName = readS();
		_answer = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Player requestor = activeChar.getTransactionRequester();
		if(requestor == null)
			return;

		/*if(_answer == 1)
		{
			ClanTable.getInstance().restoreWars();
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.WAR_PROCLAMATION_HAS_BEEN_REFUSED));
		}*/
		activeChar.setTransactionRequester(null);
	}
}