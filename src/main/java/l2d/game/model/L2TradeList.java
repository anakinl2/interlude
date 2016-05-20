package l2d.game.model;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.util.Log;

public class L2TradeList
{
	private static Logger _log = Logger.getLogger(L2TradeList.class.getName());

	private final FastList<L2ItemInstance> _items = FastList.newInstance();
	private int _listId;
	private boolean _confirmed;
	private String _buyStoreName, _sellStoreName;

	private String _npcId;

	public L2TradeList(int listId)
	{
		_listId = listId;
		_confirmed = false;
	}

	public L2TradeList()
	{
		this(0);
	}

	public void setNpcId(String id)
	{
		_npcId = id;
	}

	public String getNpcId()
	{
		return _npcId;
	}

	public void addItem(L2ItemInstance item)
	{
		synchronized (_items)
		{
			_items.add(item);
		}
	}

	public void removeAll()
	{
		_items.clear();
	}

	/**
	 * @return Returns the listId.
	 */
	public int getListId()
	{
		return _listId;
	}

	public void setSellStoreName(String name)
	{
		_sellStoreName = name;
	}

	public String getSellStoreName()
	{
		return _sellStoreName;
	}

	public void setBuyStoreName(String name)
	{
		_buyStoreName = name;
	}

	public String getBuyStoreName()
	{
		return _buyStoreName;
	}

	/**
	 * @return Returns the items.
	 */
	public FastList<L2ItemInstance> getItems()
	{
		return _items;
	}

	public int getPriceForItemId(int itemId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getItemId() == itemId)
					return item.getPriceToSell();
		}
		return -1;
	}

	public L2ItemInstance getItemByItemId(int itemId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getItemId() == itemId)
					return item;
		}
		return null;
	}

	public L2ItemInstance getItem(int ObjectId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getObjectId() == ObjectId)
					return item;
		}
		return null;
	}

	public synchronized void setConfirmedTrade(boolean x)
	{
		_confirmed = x;
	}

	public synchronized boolean hasConfirmed()
	{
		return _confirmed;
	}

	public boolean contains(int objId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getObjectId() == objId)
					return true;
		}
		return false;
	}

	public boolean validateTrade(L2Player player)
	{
		Inventory playersInv = player.getInventory();
		L2ItemInstance playerItem;
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
			{
				playerItem = playersInv.getItemByObjectId(item.getObjectId());
				if(playerItem == null || playerItem.getIntegerLimitedCount() < item.getIntegerLimitedCount() || playerItem.isEquipped())
					return false;
			}
		}
		return true;
	}

	/**
	 * Call validate before this
	 * Обычный трейд между игроками
	 */
	// synchronized не трогать - CME фикс!
	public synchronized void tradeItems(L2Player player, L2Player reciever)
	{
		Inventory playersInv = player.getInventory();
		Inventory recieverInv = reciever.getInventory();
		L2ItemInstance recieverItem, TransferItem;

		for(L2ItemInstance temp : _items)
		{
			// If player trades the enchant scroll he was using remove its effect
			if(player.getEnchantScroll() != null && temp.getObjectId() == player.getEnchantScroll().getObjectId())
				player.setEnchantScroll(null);

			L2ItemInstance oldItem = playersInv.getItemByObjectId(temp.getObjectId());
			if(oldItem == null)
			{
				_log.warning("Warning: null trade item, player " + player);
				continue;
			}

			oldItem.setWhFlag(true);
			TransferItem = playersInv.dropItem(temp.getObjectId(), temp.getIntegerLimitedCount());
			oldItem.setWhFlag(false);

			Log.LogItem(player, reciever, Log.TradeGive, TransferItem);

			if(TransferItem == null)
			{
				_log.warning("Warning: null trade transfer item, player " + player);
				continue;
			}

			temp.setLastChange((byte) oldItem.getLastChange());

			recieverItem = recieverInv.addItem(TransferItem);
			TransferItem.setWhFlag(false);

			Log.LogItem(player, reciever, Log.TradeGet, recieverItem);
		}

		player.sendChanges();
		reciever.sendChanges();
	}

	public void updateSellList(L2Player player, ConcurrentLinkedQueue<TradeItem> list)
	{
		Inventory playersInv = player.getInventory();
		L2ItemInstance item;
		for(L2ItemInstance temp : _items)
		{
			item = playersInv.getItemByObjectId(temp.getObjectId());
			if(item == null || item.getCount() <= 0)
			{
				for(TradeItem i : list)
					if(i.getObjectId() == temp.getItemId())
					{
						list.remove(i);
						break;
					}
			}
			else if(item.getCount() < temp.getCount())
				temp.setCount(item.getCount());
		}
	}

	public synchronized void buySellItems(L2Player buyer, ConcurrentLinkedQueue<TradeItem> listToBuy, L2Player seller, ConcurrentLinkedQueue<TradeItem> listToSell)
	{
		Inventory sellerInv = seller.getInventory();
		Inventory buyerInv = buyer.getInventory();

		TradeItem sellerTradeItem = null;
		L2ItemInstance sellerInventoryItem = null;
		L2ItemInstance TransferItem = null;
		L2ItemInstance temp;
		ConcurrentLinkedQueue<TradeItem> unsold = new ConcurrentLinkedQueue<TradeItem>();
		unsold.addAll(listToSell);

		int cost = 0;
		long amount = 0;

		for(TradeItem buyerTradeItem : listToBuy)
		{
			sellerTradeItem = null;

			for(TradeItem unsoldItem : unsold)
				if(unsoldItem.getItemId() == buyerTradeItem.getItemId() && unsoldItem.getOwnersPrice() == buyerTradeItem.getOwnersPrice())
				{
					sellerTradeItem = unsoldItem;
					break;
				}

			if(sellerTradeItem == null)
				continue;

			sellerInventoryItem = sellerInv.getItemByObjectId(sellerTradeItem.getObjectId());

			unsold.remove(sellerTradeItem);

			if(sellerInventoryItem == null)
				continue;

			int buyerItemCount = buyerTradeItem.getCount();
			int sellerItemCount = sellerTradeItem.getCount();

			if(sellerItemCount > sellerInventoryItem.getIntegerLimitedCount())
				sellerItemCount = sellerInventoryItem.getIntegerLimitedCount();

			if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL || seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
			{
				if(buyerItemCount > sellerItemCount)
					buyerTradeItem.setCount(sellerItemCount);
				if(buyerItemCount > sellerInventoryItem.getIntegerLimitedCount())
					buyerTradeItem.setCount(sellerInventoryItem.getIntegerLimitedCount());
				buyerItemCount = buyerTradeItem.getCount();
				amount = buyerItemCount;
				cost = (int) (amount * sellerTradeItem.getOwnersPrice());
			}

			if(buyer.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
			{
				if(sellerItemCount > buyerItemCount)
					sellerTradeItem.setCount(buyerItemCount);
				if(sellerItemCount > sellerInventoryItem.getIntegerLimitedCount())
					sellerTradeItem.setCount(sellerInventoryItem.getIntegerLimitedCount());
				sellerItemCount = sellerTradeItem.getCount();
				amount = sellerItemCount;
				cost = (int) (amount * buyerTradeItem.getOwnersPrice());
			}

			long sum = (long) buyerItemCount * buyerTradeItem.getOwnersPrice();
			if(sum > Integer.MAX_VALUE)
			{
				_log.warning("Integer Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
				_log.warning(buyer.getName() + " try to use exploit, ban this player!");
				seller.sendMessage(new CustomMessage("l2d.game.model.L2TradeList.BuyerExploit", seller));
				return;
			}
			sum = (long) sellerItemCount * sellerTradeItem.getOwnersPrice();
			if(sum > Integer.MAX_VALUE)
			{
				_log.warning("Integer Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
				_log.warning(seller.getName() + " try to use exploit, ban this player!");
				buyer.sendMessage(new CustomMessage("l2d.game.model.L2TradeList.SellerExploit", buyer));
				return;
			}

			if(buyer.getAdena() < cost)
			{
				_log.warning("buy item without full adena sum " + buyer.getName() + " and " + seller.getName() + ".");
				return;
			}

			TransferItem = sellerInv.dropItem(sellerInventoryItem.getObjectId(), amount);
			Log.LogItem(seller, buyer, Log.PrivateStoreSell, TransferItem, amount);
			buyer.reduceAdena(cost);
			seller.addAdena(cost);

			int tax = (int) (cost * Config.SERVICES_TRADE_TAX / 100);
			if(ZoneManager.getInstance().checkIfInZone(L2Zone.ZoneType.offshore, seller.getX(), seller.getY()))
				tax = (int) (cost * Config.SERVICES_OFFSHORE_TRADE_TAX / 100);
			if(Config.SERVICES_TRADE_TAX_ONLY_OFFLINE && !seller.isInOfflineMode())
				tax = 0;
			if(tax > 0)
			{
				seller.reduceAdena(tax);
				L2World.addTax(tax);
				seller.sendMessage(new CustomMessage("trade.HavePaidTax", seller).addNumber(tax));
			}

			temp = buyerInv.addItem(TransferItem);
			Log.LogItem(buyer, seller, Log.PrivateStoreBuy, TransferItem, amount);

			if(!temp.isStackable())
			{
				if(temp.getEnchantLevel() > 0)
				{
					seller.sendPacket(new SystemMessage(SystemMessage._S2S3_HAS_BEEN_SOLD_TO_S1_AT_THE_PRICE_OF_S4_ADENA).addString(buyer.getName()).addNumber(temp.getEnchantLevel()).addItemName(sellerInventoryItem.getItemId()).addNumber(cost));
					buyer.sendPacket(new SystemMessage(SystemMessage._S2S3_HAS_BEEN_PURCHASED_FROM_S1_AT_THE_PRICE_OF_S4_ADENA).addString(seller.getName()).addNumber(temp.getEnchantLevel()).addItemName(sellerInventoryItem.getItemId()).addNumber(cost));
				}
				else
				{
					seller.sendPacket(new SystemMessage(SystemMessage.S2_IS_SOLD_TO_S1_AT_THE_PRICE_OF_S3_ADENA).addString(buyer.getName()).addItemName(sellerInventoryItem.getItemId()).addNumber(cost));
					buyer.sendPacket(new SystemMessage(SystemMessage.S2_HAS_BEEN_PURCHASED_FROM_S1_AT_THE_PRICE_OF_S3_ADENA).addString(seller.getName()).addItemName(sellerInventoryItem.getItemId()).addNumber(cost));
				}
			}
			else
			{
				seller.sendPacket(new SystemMessage(SystemMessage.S2_S3_HAVE_BEEN_SOLD_TO_S1_FOR_S4_ADENA).addString(buyer.getName()).addItemName(sellerInventoryItem.getItemId()).addNumber(amount).addNumber(cost));
				buyer.sendPacket(new SystemMessage(SystemMessage.S3_S2_HAS_BEEN_PURCHASED_FROM_S1_FOR_S4_ADENA).addString(seller.getName()).addItemName(sellerInventoryItem.getItemId()).addNumber(amount).addNumber(cost));
			}

			sellerInventoryItem = null;
		}

		seller.sendChanges();
		buyer.sendChanges();

		HashSet<TradeItem> tmp;

		if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
			seller.setSellList(null);

		// update seller's sell list
		if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL)
		{
			tmp = new HashSet<TradeItem>();
			tmp.addAll(seller.getSellList());

			for(TradeItem sl : listToSell)
				for(TradeItem bl : listToBuy)
					if(sl.getItemId() == bl.getItemId() && sl.getOwnersPrice() == bl.getOwnersPrice())
					{
						L2ItemInstance inst = seller.getInventory().getItemByObjectId(sl.getObjectId());
						if(inst == null || inst.getIntegerLimitedCount() <= 0)
						{
							tmp.remove(sl);
							continue;
						}
						if(inst.isStackable())
						{
							sl.setCount(sl.getCount() - bl.getCount());
							if(sl.getCount() <= 0)
							{
								tmp.remove(sl);
								continue;
							}
							if(inst.getIntegerLimitedCount() < sl.getCount())
								sl.setCount(inst.getIntegerLimitedCount());
						}
					}

			ConcurrentLinkedQueue<TradeItem> newlist = new ConcurrentLinkedQueue<TradeItem>();
			newlist.addAll(tmp);
			seller.setSellList(newlist);
		}

		// update buyer's buy list
		if(buyer.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
		{
			tmp = new HashSet<TradeItem>();
			tmp.addAll(buyer.getBuyList());

			for(TradeItem bl : listToBuy)
				for(TradeItem sl : listToSell)
					if(sl.getItemId() == bl.getItemId() && sl.getOwnersPrice() == bl.getOwnersPrice())
						if(!ItemTable.getInstance().getTemplate(bl.getItemId()).isStackable())
							tmp.remove(bl);
						else
						{
							bl.setCount(bl.getCount() - sl.getCount());
							if(bl.getCount() <= 0)
								tmp.remove(bl);
						}

			ConcurrentLinkedQueue<TradeItem> newlist = new ConcurrentLinkedQueue<TradeItem>();
			newlist.addAll(tmp);
			buyer.setBuyList(newlist);
		}
	}
}