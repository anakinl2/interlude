package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SendTradeDone;

public class RequestPrivateStoreQuitBuy extends L2GameClientPacket
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
		if(activeChar.getTradeList() != null)
		{
			activeChar.getTradeList().removeAll();
			activeChar.sendPacket(new SendTradeDone(0));
			activeChar.setTradeList(null);
			activeChar.setTransactionRequester(null);
		}
		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		activeChar.standUp();
		activeChar.broadcastUserInfo(true);
	}
}