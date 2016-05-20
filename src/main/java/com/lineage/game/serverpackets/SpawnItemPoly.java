package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Object;
import com.lineage.game.model.instances.L2ItemInstance;

/**
 * 15
 * ee cc 11 43 		object id
 * 39 00 00 00 		item id
 * 8f 14 00 00 		x
 * b7 f1 00 00 		y
 * 60 f2 ff ff 		z
 * 01 00 00 00 		show item count
 * 7a 00 00 00      count                                         .
 *
 * format  dddddddd
 */
public class SpawnItemPoly extends L2GameServerPacket
{
	private int _objectId;
	private int _itemId;
	private int _x, _y, _z;
	private int _stackable, _count;
	private long long_count;

	public SpawnItemPoly(L2Object object)
	{
		if(object instanceof L2ItemInstance)
		{
			L2ItemInstance item = (L2ItemInstance) object;
			_objectId = object.getObjectId();
			_itemId = object.getPolyid();
			_x = item.getX();
			_y = item.getY();
			_z = item.getZ();
			_stackable = item.isStackable() ? 0x01 : 0x00;
			_count = item.getIntegerLimitedCount();
			long_count = item.getCount();
		}
		else
		{
			_objectId = object.getObjectId();
			_itemId = object.getPolyid();
			_x = object.getX();
			_y = object.getY();
			_z = object.getZ();
			_stackable = 0x00;
			_count = 1;
			long_count = 1;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x0b);
		writeD(_objectId);
		writeD(_itemId);

		writeD(_x);
		writeD(_y);
		writeD(_z);
		// only show item count if it is a stackable item
		writeD(_stackable);
		writeD(_count);
		writeD(0x00); //c2
	}
}