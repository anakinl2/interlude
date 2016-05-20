package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ClanTable;

/**
 * [C] 50 RequestReplyStopPledgeWar <p>
 * opcode(id) = 0x06
 * 
 * @author Felixx
 */
public final class RequestReplyStopPledgeWar extends L2GameClientPacket
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
			ClanTable.getInstance().stopClanWar(requestor.getClan(), activeChar.getClan());
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.REQUEST_TO_END_WAR_HAS_BEEN_DENIED));

		activeChar.setTransactionRequester(null);
	}
}