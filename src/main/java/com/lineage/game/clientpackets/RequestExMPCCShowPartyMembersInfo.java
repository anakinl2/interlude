package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.ExMPCCShowPartyMemberInfo;

/**
 * Format: ch d
 * Пример пакета:
 * D0 2E 00 4D 90 00 10
 * @author SYS
 */
public class RequestExMPCCShowPartyMembersInfo extends L2GameClientPacket
{
	private int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null || !activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
			return;

		L2Player partyLeader = (L2Player) L2World.findObject(_objectId);
		if(partyLeader != null)
			activeChar.sendPacket(new ExMPCCShowPartyMemberInfo(partyLeader));
	}
}