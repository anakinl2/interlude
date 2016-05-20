package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.ItemList;

public class RequestItemList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || !activeChar.getPlayerAccess().UseInventory || activeChar.isInvetoryDisabled())
			return;
		sendPacket(new ItemList(activeChar, true));
	}
}