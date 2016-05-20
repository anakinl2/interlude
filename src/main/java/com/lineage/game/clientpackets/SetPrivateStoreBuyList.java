package com.lineage.game.clientpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.Config;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.TradeItem;
import com.lineage.game.serverpackets.ChangeWaitType;
import com.lineage.game.serverpackets.PrivateStoreMsgBuy;
import com.lineage.game.serverpackets.SystemMessage;

public class SetPrivateStoreBuyList extends L2GameClientPacket
{
	// format: cdb, b - array of (dhhdd)
	private int _count;
	private int[] _items; // count * 3

	@Override
	public void readImpl()
	{
		_count = readD();
		if(_count * 16 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new int[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD(); //item id
			readH();// don't know what this is
			readH();
			_items[i * 3 + 1] = readD(); //count
			_items[i * 3 + 2] = readD(); //price
			if(_items[i * 3 + 1] < 0)
			{
				_items = null;
				break;
			}
		}
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(_items == null || !activeChar.checksForShop(false))
		{
			cancelStore(activeChar);
			return;
		}

		int maxSlots = activeChar.getTradeLimit();

		if(_count > maxSlots)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
			cancelStore(activeChar);
			return;
		}

		TradeItem temp;
		ConcurrentLinkedQueue<TradeItem> listbuy = new ConcurrentLinkedQueue<TradeItem>();
		int totalCost = 0;

		for(int x = 0; x < _count; x++)
		{
			if(_items[x * 3 + 1] < 1)
			{
				_count--;
				continue;
			}
			temp = new TradeItem();
			temp.setItemId(_items[x * 3 + 0]);
			temp.setCount(_items[x * 3 + 1]);
			temp.setOwnersPrice(_items[x * 3 + 2]);
			totalCost += temp.getOwnersPrice() * temp.getCount();
			if(temp.getOwnersPrice() < 0 || temp.getCount() < 0)
			{
				cancelStore(activeChar);
				return;
			}
			listbuy.add(temp);
		}

		if(totalCost > activeChar.getAdena())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_PURCHASE_PRICE_IS_HIGHER_THAN_THE_AMOUNT_OF_MONEY_THAT_YOU_HAVE_AND_SO_YOU_CANNOT_OPEN_A_PERSONAL_STORE));
			cancelStore(activeChar);
			return;
		}

		if(_count > 0)
		{
			activeChar.setBuyList(listbuy);
			activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_BUY);
			activeChar.broadcastPacket(new ChangeWaitType(activeChar, ChangeWaitType.WT_SITTING));
			activeChar.broadcastUserInfo(true);
			activeChar.broadcastPacket(new PrivateStoreMsgBuy(activeChar));
			activeChar.sitDown();
			return;
		}

		cancelStore(activeChar);
	}

	private void cancelStore(L2Player activeChar)
	{
		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		activeChar.broadcastUserInfo(true);
		activeChar.getBuyList().clear();
		if(activeChar.isInOfflineMode() && Config.SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING)
		{
			activeChar.setOfflineMode(false);
			activeChar.logout(false, false, true);
			activeChar.getNetConnection().disconnectOffline();
		}
	}
}