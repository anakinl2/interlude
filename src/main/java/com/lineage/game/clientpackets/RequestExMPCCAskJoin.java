package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Player.TransactionType;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.ExAskJoinMPCC;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 */
public class RequestExMPCCAskJoin extends L2GameClientPacket
{
	private String _name;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null || !activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
			return;

		L2Player target = L2World.getPlayer(_name);

		// Чар с таким имененм не найден в мире
		if(target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE));
			return;
		}

		// Сам себя нельзя
		if(activeChar == target)
			return;

		L2Party activeParty = activeChar.getParty();

		// Приглашать в СС может только лидер CC
		if(activeParty == null || !activeParty.isInCommandChannel() || activeParty.getCommandChannel().getChannelLeader() != activeChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL));
			return;
		}

		// Нельзя приглашать безпартийных и не лидеров партий
		if(!target.isInParty() || !target.getParty().isLeader(target))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_INVITED_WRONG_TARGET));
			return;
		}

		if(target.getParty().isInCommandChannel())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL).addString(_name));
			return;
		}

		// Чувак уже отвечает на какое-то приглашение
		if(target.isTransactionInProgress())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER));
			return;
		}

		activeChar.setTransactionType(TransactionType.CHANNEL);
		target.setTransactionRequester(activeChar, System.currentTimeMillis() + 30000);
		target.setTransactionType(TransactionType.CHANNEL);
		target.sendPacket(new ExAskJoinMPCC(activeChar.getName()));
		activeChar.sendMessage("You invited " + target.getName() + " to your Command Channel.");
	}
}