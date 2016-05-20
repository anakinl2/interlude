package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

public class RequestExCancelEnchantItem extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar != null)
			activeChar.setEnchantScroll(null);
	}
}