package com.lineage.game.serverpackets;

import javolution.util.FastList;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.model.TradeItem;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;

public class PrivateStoreManageListBuy extends L2GameServerPacket
{
	private FastList<BuyItemInfo> buylist = new FastList<BuyItemInfo>();
	private int buyer_id, buyer_adena;
	private L2TradeList _list;

	/**
	 * Окно управления личным магазином продажи
	 * @param buyer
	 */
	public PrivateStoreManageListBuy(L2Player buyer)
	{
		buyer_id = buyer.getObjectId();
		buyer_adena = buyer.getAdena();

		int _id, count, store_price, body_part, type2, owner_price;
		for(TradeItem e : buyer.getBuyList())
		{
			_id = e.getItemId();
			L2Item tempItem = ItemTable.getInstance().getTemplate(_id);
			if(tempItem == null)
				continue;

			count = e.getCount();
			store_price = e.getStorePrice();
			body_part = tempItem.getBodyPart();
			type2 = tempItem.getType2ForPackets();
			owner_price = e.getOwnersPrice();
			buylist.add(new BuyItemInfo(_id, count, store_price, body_part, type2, owner_price));
		}

		_list = new L2TradeList(0);
		for(L2ItemInstance item : buyer.getInventory().getItems())
			if(item != null && item.canBeTraded(buyer))
			{
				for(TradeItem ti : buyer.getBuyList())
					if(ti.getItemId() == item.getItemId())
						continue;
				_list.addItem(item);
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb7);
		//section 1
		writeD(buyer_id);
		writeD(buyer_adena);

		//section2
		writeD(_list.getItems().size());//for potential sells
		for(L2ItemInstance temp : _list.getItems())
		{
			writeD(temp.getItemId());
			writeH(0); //show enchant lvl as 0, as you can't buy enchanted weapons
			writeD(temp.getIntegerLimitedCount());
			writeD(temp.getPriceToSell());
			writeH(0);
			writeD(temp.getBodyPart());
			writeH(temp.getItem().getType2());
		}

		//section 3
		writeD(buylist.size());//count for any items already added for sell
		for(BuyItemInfo e : buylist)
		{
			writeD(e._id);
			writeH(0);
			writeD(e.count);
			writeD(e.store_price);
			writeH(0);
			writeD(e.body_part);
			writeH(e.type2);
			writeD(e.owner_price);//your price
			writeD(e.store_price);//fixed store price
		}
	}

	static class BuyItemInfo
	{
		public int _id, count, store_price, body_part, type2, owner_price;

		public BuyItemInfo(int __id, int _count, int _store_price, int _body_part, int _type2, int _owner_price)
		{
			_id = __id;
			count = _count; //TODO: long
			store_price = _store_price; //TODO: long
			body_part = _body_part;
			type2 = _type2;
			owner_price = _owner_price;
		}
	}
}