package com.lineage.game.clientpackets;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.network.L2GameClient.GameClientState;
import com.lineage.game.serverpackets.CharacterSelectionInfo;
import com.lineage.game.serverpackets.RestartResponse;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestRestart extends L2GameClientPacket
{
	/**
	 * packet type id 0x57
	 * format:      c
	 */

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestRestart.Olympiad", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.inObserverMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.OBSERVERS_CANNOT_PARTICIPATE));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_RESTART_WHILE_IN_COMBAT));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isFishing())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isBlocked() && !activeChar.isFlying()) // Разрешаем выходить из игры если используется сервис HireWyvern. Вернет в начальную точку.
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestRestart.OutOfControl", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		// Prevent player from restarting if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if(activeChar.isFestivalParticipant())
		{
			if(SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestRestart.Festival", activeChar));
				activeChar.sendActionFailed();
				return;
			}
			L2Party playerParty = activeChar.getParty();

			if(playerParty != null)
				playerParty.broadcastMessageToPartyMembers(activeChar.getName() + " has been removed from the upcoming festival.");
		}

		if(getClient() != null)
			getClient().setState(GameClientState.AUTHED);
		activeChar.logout(false, true, false);
		sendPacket(new RestartResponse());
		// send char list
		CharacterSelectionInfo cl = new CharacterSelectionInfo(getClient().getLoginName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
}