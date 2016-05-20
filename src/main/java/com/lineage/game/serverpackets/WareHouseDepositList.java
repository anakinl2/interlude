package com.lineage.game.serverpackets;

import java.util.ArrayList;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.Warehouse.WarehouseType;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.templates.L2Item;

public class WareHouseDepositList extends L2GameServerPacket
{
	private int _whtype, char_adena;
	private ArrayList<L2ItemInstance> _itemslist = new ArrayList<L2ItemInstance>();

	public WareHouseDepositList(L2Player cha, WarehouseType whtype)
	{
		_whtype = whtype.getPacketValue();
		char_adena = cha.getAdena();
		for(L2ItemInstance item : cha.getInventory().getItems())
			if(item != null && item.canBeStored(cha, _whtype == 1))
				_itemslist.add(item);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x41);
		writeH(_whtype);
		writeD(char_adena);
		writeH(_itemslist.size());
		for(L2ItemInstance temp : _itemslist)
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
			writeH(0x00); // ? 200
			writeD(temp.getObjectId()); // return value for define item (object_id)
			writeD(temp.getAugmentationId() & 0x0000FFFF);
			writeD(temp.getAugmentationId() >> 16);
		}
	}
}