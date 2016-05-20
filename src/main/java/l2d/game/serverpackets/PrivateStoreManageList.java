package l2d.game.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.model.TradeItem;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

public class PrivateStoreManageList extends L2GameServerPacket
{
	private int seller_id, seller_adena;
	private boolean _package = false;
	private ConcurrentLinkedQueue<TradeItem> _sellList;
	private ConcurrentLinkedQueue<TradeItem> _haveList;

	/**
	 * Окно управления личным магазином покупки
	 * @param seller
	 */
	public PrivateStoreManageList(L2Player seller, boolean pkg)
	{
		seller_id = seller.getObjectId();
		seller_adena = seller.getAdena();
		_package = pkg;

		// Проверяем список вещей в инвентаре, если вещь остутствует - убираем из списка продажи
		_sellList = new ConcurrentLinkedQueue<TradeItem>();
		for(TradeItem i : seller.getSellList())
		{
			L2ItemInstance inst = seller.getInventory().getItemByObjectId(i.getObjectId());
			if(i.getCount() <= 0 || inst == null || !inst.canBeTraded(seller))
				continue;
			if(inst.getIntegerLimitedCount() < i.getCount())
				i.setCount(inst.getIntegerLimitedCount());
			_sellList.add(i);
		}

		L2TradeList _list = new L2TradeList(0);
		// Строим список вещей, годных для продажи имеющихся в инвентаре
		for(L2ItemInstance item : seller.getInventory().getItemsList())
			if(item != null && item.canBeTraded(seller) && item.getItemId() != 57)
				_list.addItem(item);

		_haveList = new ConcurrentLinkedQueue<TradeItem>();

		// Делаем список для собственно передачи с учетом количества
		for(L2ItemInstance item : _list.getItems())
		{
			TradeItem ti = new TradeItem();
			ti.setObjectId(item.getObjectId());
			ti.setItemId(item.getItemId());
			ti.setCount(item.getIntegerLimitedCount());
			ti.setEnchantLevel(item.getEnchantLevel());
			_haveList.add(ti);
		}

		//Убираем совпадения между списками, в сумме оба списка должны совпадать с содержимым инвентаря
		if(_sellList.size() > 0)
			for(TradeItem itemOnSell : _sellList)
			{
				_haveList.remove(itemOnSell);
				boolean added = false;
				for(TradeItem itemInInv : _haveList)
					if(itemInInv.getObjectId() == itemOnSell.getObjectId())
					{
						added = true;
						itemOnSell.setCount(Math.min(itemOnSell.getCount(), itemInInv.getCount()));
						if(itemOnSell.getCount() == itemInInv.getCount())
							_haveList.remove(itemInInv);
						else if(itemOnSell.getCount() > 0)
							itemInInv.setCount(itemInInv.getCount() - itemOnSell.getCount());
						else
							_sellList.remove(itemOnSell);
						break;
					}
				if(!added)
					_sellList.remove(itemOnSell);
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9a);
		//section 1
		writeD(seller_id);
		writeD(_package ? 1 : 0);
		writeD(seller_adena);

		//Список имеющихся вещей
		writeD(_haveList.size());
		for(TradeItem temp : _haveList)
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(temp.getItemId());
			writeD(tempItem.getType2());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(0);
			writeH(temp.getEnchantLevel());//enchant lvl
			writeH(0);
			writeD(tempItem.getBodyPart());
			writeD(tempItem.getReferencePrice()); //store price
		}

		//Список вещей уже поставленых на продажу
		writeD(_sellList.size());
		for(TradeItem temp2 : _sellList)
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(temp2.getItemId());
			writeD(tempItem.getType2());
			writeD(temp2.getObjectId());
			writeD(temp2.getItemId());
			writeD(temp2.getCount());
			writeH(0);
			writeH(temp2.getEnchantLevel());//enchant lvl
			writeH(0);
			writeD(tempItem.getBodyPart());
			writeD(temp2.getOwnersPrice());//your price
			writeD(temp2.getStorePrice()); //store price
		}
	}
}