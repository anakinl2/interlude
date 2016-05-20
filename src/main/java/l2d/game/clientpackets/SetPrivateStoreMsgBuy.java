package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.serverpackets.PrivateStoreMsgBuy;

public class SetPrivateStoreMsgBuy extends L2GameClientPacket
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
			tradeList.setBuyStoreName(_storename);
			sendPacket(new PrivateStoreMsgBuy(activeChar));
		}
	}
}