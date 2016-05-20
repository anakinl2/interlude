package l2d.game.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import l2d.game.model.L2Player;
import l2d.game.model.TradeItem;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

public class PrivateStoreListBuy extends L2GameServerPacket
{
	private int buyer_id, seller_adena;
	private ConcurrentLinkedQueue<TradeItem> _buyerslist;

	/**
	 * Список вещей в личном магазине покупки, показываемый продающему
	 * @param seller
	 * @param storePlayer
	 */
	public PrivateStoreListBuy(L2Player seller, L2Player storePlayer)
	{
		seller_adena = seller.getAdena();
		buyer_id = storePlayer.getObjectId();

		ConcurrentLinkedQueue<L2ItemInstance> sellerItems = seller.getInventory().getItemsList();
		_buyerslist = new ConcurrentLinkedQueue<TradeItem>();
		_buyerslist.addAll(storePlayer.getBuyList());

		for(TradeItem buyListItem : _buyerslist)
			buyListItem.setTempValue(0);

		for(L2ItemInstance sellerItem : sellerItems)
			for(TradeItem buyListItem : _buyerslist)
				if(sellerItem.getItemId() == buyListItem.getItemId() && sellerItem.canBeTraded(seller))
				{
					buyListItem.setTempValue(Math.min(buyListItem.getCount(), sellerItem.getIntegerLimitedCount()));
					continue;
				}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb8);

		writeD(buyer_id);
		writeD(seller_adena);
		writeD(_buyerslist.size());
		for(TradeItem buyersitem : _buyerslist)
		{
			L2Item tmp = ItemTable.getInstance().getTemplate(buyersitem.getItemId());
			writeD(buyersitem.getObjectId());
			writeD(buyersitem.getItemId());
			writeH(buyersitem.getEnchantLevel());
			writeD(buyersitem.getTempValue());

			writeD(tmp.getReferencePrice());
			writeH(0);

			writeD(tmp.getBodyPart());
			writeH(tmp.getType2());
			writeD(buyersitem.getOwnersPrice());

			writeD(buyersitem.getCount());

		}
	}
}