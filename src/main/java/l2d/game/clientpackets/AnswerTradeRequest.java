package l2d.game.clientpackets;

import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.model.L2TradeList;
import l2d.game.serverpackets.SendTradeDone;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.TradeStart;

/**
 * [C] 40 AnswerTradeRequest <p>
 * <b>Format:</b> cd
 * @author Felixx
 */
public class AnswerTradeRequest extends L2GameClientPacket
{
	// 
	private int _response;

	@Override
	public void readImpl()
	{
		_response = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		L2Player requestor = activeChar.getTransactionRequester();

		// trade partner logged off. trade is canceld
		if(requestor == null || requestor.getTransactionRequester() == null || requestor.getTransactionRequester() != activeChar)
		{
			if(_response != 0)
			{
				activeChar.sendPacket(new SendTradeDone(0));
				activeChar.sendPacket(Msg.THAT_PLAYER_IS_NOT_ONLINE);
			}
			activeChar.setTransactionRequester(null);
			return;
		}

		if(activeChar.getTransactionType() != TransactionType.TRADE || activeChar.getTransactionType() != requestor.getTransactionType())
		{
			activeChar.setTransactionRequester(null);
			requestor.setTransactionRequester(null);
			return;
		}

		if(_response != 1 || activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.C1_HAS_DENIED_YOUR_REQUEST_TO_TRADE).addString(activeChar.getName()));
			requestor.setTransactionRequester(null);
			activeChar.setTransactionRequester(null);
			if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
				activeChar.sendPacket(new SystemMessage(SystemMessage.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM));
			return;
		}

		requestor.sendPacket(new SystemMessage(SystemMessage.YOU_BEGIN_TRADING_WITH_C1).addString(activeChar.getName()));
		requestor.sendPacket(new TradeStart(requestor));
		requestor.setTradeList(new L2TradeList(0));

		activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_BEGIN_TRADING_WITH_C1).addString(requestor.getName()));
		activeChar.sendPacket(new TradeStart(activeChar));
		activeChar.setTradeList(new L2TradeList(0));

		activeChar.setTransactionRequester(requestor);
		requestor.setTransactionRequester(activeChar);
	}
}