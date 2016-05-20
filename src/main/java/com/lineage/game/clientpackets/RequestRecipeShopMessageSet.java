package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

public class RequestRecipeShopMessageSet extends L2GameClientPacket
{
	// format: cS
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.getDuel() != null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getCreateList() != null)
			activeChar.getCreateList().setStoreName(_name);
	}
}