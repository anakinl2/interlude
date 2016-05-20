package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.PartyRoom;

/**
 * format (ch) d
 */
public class RequestOustFromPartyRoom extends L2GameClientPacket
{
	private int _id;

	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		L2Player member = L2World.getPlayer(_id);
		if(activeChar == null || member == null)
			return;

		PartyRoom room = PartyRoomManager.getInstance().getRoom(member.getPartyRoom());
		if(room != null)
			room.removeMember(member, true);
	}
}