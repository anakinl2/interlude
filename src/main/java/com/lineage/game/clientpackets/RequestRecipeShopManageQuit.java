package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

public class RequestRecipeShopManageQuit extends L2GameClientPacket
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

		if(activeChar.getDuel() != null)
		{
			activeChar.sendActionFailed();
			return;
		}

		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		activeChar.broadcastUserInfo(true);
		activeChar.standUp();
	}
}