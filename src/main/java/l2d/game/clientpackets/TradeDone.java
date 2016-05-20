package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.serverpackets.SendTradeDone;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.GmListTable;
import l2d.util.Log;

public class TradeDone extends L2GameClientPacket
{
	//Format: cd
	private static Logger _log = Logger.getLogger(TradeDone.class.getName());

	private int _response;

	@Override
	public void readImpl()
	{
		_response = readD();
	}

	@Override
	public void runImpl()
	{
		synchronized (getClient())
		{
			L2Player activeChar = getClient().getActiveChar();
			if(activeChar == null)
				return;

			L2Player requestor = activeChar.getTransactionRequester();

			if(requestor == null || requestor == activeChar)
			{
				activeChar.sendPacket(new SendTradeDone(0));
				activeChar.setTradeList(null);
				activeChar.setTransactionRequester(null);
				activeChar.sendActionFailed();
				return;
			}

			if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || requestor.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			{
				activeChar.sendPacket(new SendTradeDone(0));
				activeChar.setTradeList(null);
				activeChar.setTransactionRequester(null);
				activeChar.sendActionFailed();
				activeChar.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
				requestor.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
				return;
			}

			L2Player requestor_partner = requestor.getTransactionRequester();

			L2TradeList activeChar_list = activeChar.getTradeList();
			L2TradeList requestor_list = requestor.getTradeList();

			if(requestor_partner != null && requestor_partner == activeChar && activeChar_list != null && requestor_list != null)
			{
				if(_response == 1)
				{
					if(!activeChar_list.hasConfirmed())
					{
						// first party accepted the trade
						requestor.sendPacket(new SystemMessage(SystemMessage.C1_HAS_CONFIRMED_THE_TRADE).addString(activeChar.getName()));
						activeChar.sendActionFailed();
						activeChar_list.setConfirmedTrade(true);

						//notify clients that "OK" button has been pressed.
						activeChar.sendPacket(Msg.TradePressOwnOk);
						requestor.sendPacket(Msg.TradePressOtherOk);
					}

					//Check for dual confirmation
					if(!requestor_list.hasConfirmed())
					{
						activeChar.sendActionFailed();
						return;
					}

					//Can't exchange on a big distance
					if(!activeChar.isInRange(requestor, 1000))
					{
						activeChar.sendPacket(new SendTradeDone(0));
						activeChar.sendPacket(new SystemMessage(SystemMessage.C1_HAS_CANCELLED_THE_TRADE).addString(requestor.getName()));
						requestor.sendPacket(new SendTradeDone(0));
						requestor.sendPacket(new SystemMessage(SystemMessage.C1_HAS_CANCELLED_THE_TRADE).addString(activeChar.getName()));

						activeChar.setTradeList(null);
						requestor.setTradeList(null);
						requestor.setTransactionRequester(null);
						activeChar.setTransactionRequester(null);
						return;
					}

					boolean trade1Valid = activeChar_list.validateTrade(activeChar);
					boolean trade2Valid = requestor_list.validateTrade(requestor);
					if(trade1Valid && trade2Valid)
					{
						if(activeChar_list.getItems() == null)
						{
							_log.warning("TradeDone: empty player tradelist?");
							activeChar.sendActionFailed();
							return;
						}

						if(requestor_list.getItems() == null)
						{
							_log.warning("TradeDone: empty requestor tradelist?");
							activeChar.sendActionFailed();
							return;
						}

						activeChar_list.tradeItems(activeChar, requestor);
						requestor_list.tradeItems(requestor, activeChar);
					}
					requestor.sendPacket(new SendTradeDone(1));
					activeChar.sendPacket(new SendTradeDone(1));

					if(trade1Valid && trade2Valid)
					{
						requestor.sendPacket(Msg.YOUR_TRADE_IS_SUCCESSFUL);
						activeChar.sendPacket(Msg.YOUR_TRADE_IS_SUCCESSFUL);
					}
					else
					{
						if(!trade2Valid)
						{
							String msgToSend = requestor.getName() + " tried a trade dupe [!trade2Valid]";
							Log.add(msgToSend, "illegal-actions");
							GmListTable.broadcastMessageToGMs(msgToSend);
							activeChar.sendPacket(Msg.THE_ATTEMPT_TO_TRADE_HAS_FAILED);
							requestor.sendPacket(Msg.THE_ATTEMPT_TO_TRADE_HAS_FAILED);
						}

						if(!trade1Valid)
						{
							String msgToSend = activeChar.getName() + " tried a trade dupe [!trade1Valid]";
							Log.add(msgToSend, "illegal-actions");
							GmListTable.broadcastMessageToGMs(msgToSend);
							activeChar.sendPacket(Msg.THE_ATTEMPT_TO_TRADE_HAS_FAILED);
							requestor.sendPacket(Msg.THE_ATTEMPT_TO_TRADE_HAS_FAILED);
						}
					}
				}
				else
				{
					activeChar.sendPacket(new SendTradeDone(0));
					requestor.sendPacket(new SendTradeDone(0));
					requestor.sendPacket(new SystemMessage(SystemMessage.C1_HAS_CANCELLED_THE_TRADE).addString(activeChar.getName()));
				}

				activeChar.setTradeList(null);
				requestor.setTradeList(null);

				// clear transaction flag
				requestor.setTransactionRequester(null);
				activeChar.setTransactionRequester(null);
			}
			else
			{
				// trade partner logged off. trade is canceled
				activeChar.sendPacket(new SendTradeDone(0));
				activeChar.sendPacket(Msg.THAT_PLAYER_IS_NOT_ONLINE);
				activeChar.setTransactionRequester(null);
				requestor.setTradeList(null);
				activeChar.setTradeList(null);
			}
		}
	}
}