package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.serverpackets.PrivateStoreManageListBuy;
import l2d.game.serverpackets.SendTradeDone;

public final class RequestPrivateStoreManageBuy extends L2GameClientPacket
{
	private static final String _C__90_REQUESTPRIVATESTOREMANAGEBUY = "[C] 90 RequestPrivateStoreManageBuy";

	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if(activeChar.isAlikeDead())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.checksForShop(false))
		{
			activeChar.sendActionFailed();
			return;
		}

		activeChar.standUp();
		if(activeChar.getTradeList() != null)
		{
			activeChar.getTradeList().removeAll();
			activeChar.sendPacket(new SendTradeDone(0));
			activeChar.setTransactionRequester(null);
		}
		else
			activeChar.setTradeList(new L2TradeList(0));
		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		activeChar.broadcastUserInfo(true);
		if(!activeChar.checksForShop(false))
		{
			activeChar.sendActionFailed();
			return;
		}
		activeChar.sendPacket(new PrivateStoreManageListBuy(activeChar));
	}

	@Override
	public String getType()
	{
		return _C__90_REQUESTPRIVATESTOREMANAGEBUY;
	}
}
