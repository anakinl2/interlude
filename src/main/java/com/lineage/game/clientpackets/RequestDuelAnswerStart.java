package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.Duel;
import com.lineage.game.serverpackets.SystemMessage;

/**
 *  format  chddd
 */

public class RequestDuelAnswerStart extends L2GameClientPacket
{
	private int _response;
	private int _duelType;
	@SuppressWarnings("unused")
	private int _unk1;

	@Override
	public void readImpl()
	{
		_duelType = readD();
		_unk1 = readD();
		_response = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		L2Player requestor = player.getTransactionRequester();
		if(requestor == null)
			return;

		if(_response == 1)
		{
			SystemMessage msg1, msg2;
			if(_duelType == 1)
			{
				msg1 = new SystemMessage(SystemMessage.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_PARTY_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
				msg1.addString(requestor.getName());

				msg2 = new SystemMessage(SystemMessage.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_DUEL_AGAINST_THEIR_PARTY_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
				msg2.addString(player.getName());
			}
			else
			{
				msg1 = new SystemMessage(SystemMessage.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
				msg1.addString(requestor.getName());

				msg2 = new SystemMessage(SystemMessage.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
				msg2.addString(player.getName());
			}

			player.sendPacket(msg1);
			requestor.sendPacket(msg2);

			Duel.createDuel(requestor, player, _duelType);
		}
		else
		{
			SystemMessage msg;
			if(_duelType == 1)
				msg = new SystemMessage(SystemMessage.THE_OPPOSING_PARTY_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
			else
			{
				msg = new SystemMessage(SystemMessage.S1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
				msg.addString(player.getName());
			}
			requestor.sendPacket(msg);
		}

		player.setTransactionRequester(null);
		requestor.setTransactionRequester(null);
	}
}