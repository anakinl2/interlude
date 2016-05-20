package l2d.game.clientpackets;

import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.serverpackets.JoinParty;
import l2d.game.serverpackets.SystemMessage;

public class RequestAnswerJoinParty extends L2GameClientPacket
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

		if(requestor.getParty() == null)
			return;

		if(activeChar.getTransactionType() != TransactionType.PARTY || activeChar.getTransactionType() != requestor.getTransactionType())
			return;

		requestor.sendPacket(new JoinParty(_response));

		if(_response == 1)
		{
			if(requestor.getParty().getMemberCount() >= L2Party.MAX_SIZE)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
				requestor.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
				return;
			}

			if(requestor.getParty().isInReflection() && requestor.getParty().getMemberCount() >= L2Party.MAX_INSTANCE_SIZE)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
				requestor.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
				return;
			}

			activeChar.joinParty(requestor.getParty());
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.THE_PLAYER_DECLINED_TO_JOIN_YOUR_PARTY));
			//activate garbage collection if there are no other members in party (happens when we were creating new one)
			if(requestor.getParty() != null && requestor.getParty().getMemberCount() == 1)
				requestor.setParty(null);
		}
	}
}