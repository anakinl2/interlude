package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.serverpackets.PrivateStoreMsgSell;

public class SetPrivateStoreMsgSell extends L2GameClientPacket
{
	private String _storename;

	@Override
	public void readImpl()
	{
		_storename = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2TradeList tradeList = activeChar.getTradeList();
		if(tradeList != null)
		{
			tradeList.setSellStoreName(_storename);
			sendPacket(new PrivateStoreMsgSell(activeChar, false));
		}
	}
}