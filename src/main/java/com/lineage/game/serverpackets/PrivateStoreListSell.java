package com.lineage.game.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.TradeItem;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;

public class PrivateStoreListSell extends L2GameServerPacket
{
	private int seller_id, buyer_adena;
	private final boolean _package;
	private ConcurrentLinkedQueue<TradeItem> _sellList;

	/**
	 * Список вещей в личном магазине продажи, показываемый покупателю
	 * @param buyer
	 * @param seller
	 */
	public PrivateStoreListSell(L2Player buyer, L2Player seller)
	{
		seller_id = seller.getObjectId();
		buyer_adena = buyer.getAdena();
		_package = seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE;
		_sellList = seller.getSellList();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9b);
		writeD(seller_id);
		writeD(_package ? 1 : 0);
		writeD(buyer_adena);

		writeD(_sellList.size());
		for(TradeItem ti : _sellList)
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(ti.getItemId());

			writeD(tempItem.getType2());
			writeD(ti.getObjectId());
			writeD(ti.getItemId());
			writeD(ti.getCount());
			writeH(0);
			writeH(ti.getEnchantLevel());
			writeH(0x00);
			writeD(tempItem.getBodyPart());
			writeD(ti.getOwnersPrice());//your price
			writeD(ti.getStorePrice()); //store price

		}
	}
}