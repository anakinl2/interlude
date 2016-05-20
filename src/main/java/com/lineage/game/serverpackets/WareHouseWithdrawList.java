package com.lineage.game.serverpackets;

import java.util.NoSuchElementException;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.Warehouse.WarehouseType;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2ItemInstance.ItemClass;
import com.lineage.game.templates.L2Item;

public class WareHouseWithdrawList extends L2GameServerPacket
{
	public static final int PRIVATE = 1;
	public static final int CLAN = 2;
	public static final int CASTLE = 3;
	public static final int FREIGHT = 4;

	private int _money;
	private L2ItemInstance[] _items;
	private int _type;
	private boolean can_writeImpl = false;

	public WareHouseWithdrawList(L2Player cha, WarehouseType type, ItemClass clss)
	{
		if(cha == null)
			return;

		_money = cha.getAdena();
		_type = type.getPacketValue();
		switch(type)
		{
			case PRIVATE:
				_items = cha.getWarehouse().listItems(clss);
				break;
			case CLAN:
			case CASTLE:
				_items = cha.getClan().getWarehouse().listItems(clss);
				break;
			/*
			 case CASTLE:
			 items = _cha.getClan().getCastleWarehouse().listItems();
			 break;
			 */
			case FREIGHT:
				_items = cha.getFreight().listItems(clss);
				break;
			default:
				throw new NoSuchElementException("Invalid value of 'type' argument");
		}

		if(_items.length == 0)
		{
			cha.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE));
			return;
		}

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x42);
		writeH(_type);
		writeD(_money);
		writeH(_items.length);
		for(L2ItemInstance temp : _items)
		{
			L2Item item = temp.getItem();
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getIntegerLimitedCount());
			writeH(item.getType2ForPackets());
			writeH(temp.getCustomType1());
			writeD(temp.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeH(0); // ?
			writeD(temp.getObjectId()); // return value for define item (object_id)
			writeD(temp.getAugmentationId() & 0x0000FFFF);
			writeD(temp.getAugmentationId() >> 16);
		}
	}
}