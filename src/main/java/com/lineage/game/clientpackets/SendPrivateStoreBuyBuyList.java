package com.lineage.game.clientpackets;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.TradeItem;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Util;

/**
 * format: cddb, b - array of (ddhhdd)
 * Список продаваемого в приватный магазин покупки
 * см. также RequestPrivateStoreBuy
 */
public class SendPrivateStoreBuyBuyList extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(SendPrivateStoreBuyBuyList.class.getName());
	private int _buyerID;
	private L2Player _buyer;
	private L2Player _seller;
	private int _count;
	private int _sumPrice = 0;
	private ConcurrentLinkedQueue<TradeItem> _sellerlist = new ConcurrentLinkedQueue<TradeItem>();
	private ConcurrentLinkedQueue<TradeItem> _buyerlist = new ConcurrentLinkedQueue<TradeItem>();

	private boolean _fail = false;
	private boolean seller_fail = false;

	@Override
	public void readImpl()
	{
		L2GameClient client = getClient();
		_buyerID = readD();
		_seller = client.getActiveChar();
		_buyer = (L2Player) _seller.getVisibleObject(_buyerID);
		_count = readD();

		if(_count * 20 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			seller_fail = true;
			return;
		}

		if(_seller == null || _buyer == null || _buyer.getTradeList() == null || _seller.getDistance3D(_buyer) > L2Character.INTERACTION_DISTANCE)
		{
			_fail = true;
			return;
		}

		if(!_seller.getPlayerAccess().UseTrade)
		{
			_seller.sendPacket(new SystemMessage(SystemMessage.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES));
			_fail = true;
			return;
		}

		_buyerlist = _buyer.getBuyList();

		long totalPrice = 0;
		long totalCount = 0;

		TradeItem temp;
		for(int i = 0; i < _count; i++)
		{
			temp = new TradeItem();

			readD(); // ObjectId, не работает

			temp.setItemId(readD());

			readH();
			readH();

			int itemcount = readD();

			temp.setOwnersPrice(readD());

			L2ItemInstance SIItem = _seller.getInventory().getItemByItemId(temp.getItemId());

			if(SIItem == null)
			{
				// if(Config.DEBUG)
				_log.warning("Player " + _seller.getName() + " tries to sell to PSB:" + _buyer.getName() + " item not in inventory");
				continue;
			}

			if(SIItem.isEquipped())
			{
				_seller.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_SELL_HAS_FAILED));
				_fail = true;
				return;
			}

			temp.setObjectId(SIItem.getObjectId());

			if(itemcount > SIItem.getIntegerLimitedCount())
				itemcount = SIItem.getIntegerLimitedCount();

			temp.setCount(itemcount);
			temp.setEnchantLevel(SIItem.getEnchantLevel());

			totalPrice += temp.getOwnersPrice() * temp.getCount();
			totalCount += temp.getCount();
			_sumPrice += temp.getOwnersPrice() * temp.getCount();

			_sellerlist.add(temp);
		}

		if(totalPrice < 0 || totalPrice > Integer.MAX_VALUE || totalCount < 0 || totalCount > Integer.MAX_VALUE)
		{
			Util.handleIllegalPlayerAction(_seller, "SendPrivateStoreBuyBuyList[47]", "tried an overflow exploit totalPrice: " + totalPrice + " totalCount: " + totalCount, 0);
			_fail = true;
			return;
		}

		_fail = false;
	}

	@Override
	public void runImpl()
	{
		if(seller_fail)
		{
			if(_seller != null)
				_seller.sendActionFailed();
			return;
		}

		if(_buyer == null)
		{
			if(_seller != null)
				_seller.sendActionFailed();
			return;
		}

		if(_fail)
		{
			cancelStore(_buyer);
			return;
		}

		if(_buyer.getAdena() < _sumPrice || _buyer.getPrivateStoreType() != L2Player.STORE_PRIVATE_BUY)
		{
			cancelStore(_buyer);
			return;
		}

		_buyer.getTradeList().buySellItems(_buyer, _buyerlist, _seller, _sellerlist);
		_buyer.saveTradeList();

		// на всякий случай немедленно сохраняем все изменения
		for(L2ItemInstance i : _buyer.getInventory().getItemsList())
			i.updateDatabase(true);

		for(L2ItemInstance i : _seller.getInventory().getItemsList())
			i.updateDatabase(true);

		if(_buyer.getBuyList().isEmpty())
			cancelStore(_buyer);

		_buyer.updateStats();
		_seller.sendActionFailed();
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