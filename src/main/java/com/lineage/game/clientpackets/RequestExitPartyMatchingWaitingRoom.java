package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.model.L2Player;

/**
 * Format: (ch)
 */
public class RequestExitPartyMatchingWaitingRoom extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		PartyRoomManager.getInstance().removeFromWaitingList(activeChar);
	}
}