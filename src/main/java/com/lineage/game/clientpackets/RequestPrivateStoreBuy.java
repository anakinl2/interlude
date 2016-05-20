package com.lineage.game.clientpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.Config;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.TradeItem;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * Список покупаемого в приватном магазине продажи
 * см. также SendPrivateStoreBuyBuyList
 */
public class RequestPrivateStoreBuy extends L2GameClientPacket
{
	// format: cddb, b - array of (ddd)
	private int _sellerID;
	private int _count;
	private int[] _items; // count * 3

	@Override
	public void readImpl()
	{
		_sellerID = readD();
		_count = readD();
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new int[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD(); //object id
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
		if(_items == null)
			return;

		L2Player buyer = getClient().getActiveChar();
		if(buyer == null)
			return;

		if(!buyer.getPlayerAccess().UseTrade)
		{
			buyer.sendPacket(new SystemMessage(SystemMessage.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES));
			return;
		}

		ConcurrentLinkedQueue<TradeItem> buyerlist = new ConcurrentLinkedQueue<TradeItem>();

		L2Player seller = (L2Player) buyer.getVisibleObject(_sellerID);
		if(seller == null || seller.getPrivateStoreType() != L2Player.STORE_PRIVATE_SELL && seller.getPrivateStoreType() != L2Player.STORE_PRIVATE_SELL_PACKAGE || seller.getDistance3D(buyer) > L2Character.INTERACTION_DISTANCE)
		{
			buyer.sendActionFailed();
			return;
		}

		if(seller.getTradeList() == null)
		{
			cancelStore(seller);
			return;
		}

		ConcurrentLinkedQueue<TradeItem> sellerlist = seller.getSellList();
		int cost = 0;

		if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
		{
			buyerlist = new ConcurrentLinkedQueue<TradeItem>();
			buyerlist.addAll(sellerlist);
			for(TradeItem ti : buyerlist)
				cost += ti.getOwnersPrice() * ti.getCount();
		}
		else
			for(int i = 0; i < _count; i++)
			{
				int objectId = _items[i * 3 + 0];
				int count = _items[i * 3 + 1];
				int price = _items[i * 3 + 2];

				for(TradeItem si : sellerlist)
					if(si.getObjectId() == objectId)
					{
						if(count > si.getCount() || price != si.getOwnersPrice())
						{
							buyer.sendActionFailed();
							return;
						}

						L2ItemInstance sellerItem = seller.getInventory().getItemByObjectId(objectId);
						if(sellerItem == null || sellerItem.getIntegerLimitedCount() < count)
						{
							buyer.sendActionFailed();
							return;
						}

						TradeItem temp = new TradeItem();
						temp.setObjectId(si.getObjectId());
						temp.setItemId(sellerItem.getItemId());
						temp.setCount(count);
						temp.setOwnersPrice(si.getOwnersPrice());

						cost += temp.getOwnersPrice() * temp.getCount();
						buyerlist.add(temp);
					}
			}

		if(buyer.getAdena() < cost || cost > Integer.MAX_VALUE || cost < 0)
		{
			buyer.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			buyer.sendActionFailed();
			return;
		}

		seller.getTradeList().buySellItems(buyer, buyerlist, seller, sellerlist);
		buyer.sendChanges();

		seller.saveTradeList();

		// на всякий случай немедленно сохраняем все изменения
		for(L2ItemInstance i : buyer.getInventory().getItemsList())
			i.updateDatabase(true);

		for(L2ItemInstance i : seller.getInventory().getItemsList())
			i.updateDatabase(true);

		if(seller.getSellList().isEmpty())
			cancelStore(seller);

		seller.sendChanges();
		buyer.sendActionFailed();
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
			if(activeChar.getNetConnection() != null)
				activeChar.getNetConnection().disconnectOffline();
		}
	}
}