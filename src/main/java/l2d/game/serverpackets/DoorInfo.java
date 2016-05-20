package l2d.game.serverpackets;

import l2d.game.model.instances.L2DoorInstance;

/**
 * 60
 * d6 6d c0 4b		door id
 * 8f 14 00 00 		x
 * b7 f1 00 00 		y
 * 60 f2 ff ff 		z
 * 00 00 00 00 		??
 *
 * format  dddd    rev 377  ID:%d X:%d Y:%d Z:%d
 *         ddddd   rev 419
 */
public class DoorInfo extends L2GameServerPacket
{
	private int _id, obj_id;
	private int p2, p3, p4, p6, p7, p8, p9;

	public DoorInfo(L2DoorInstance door)
	{
		_id = door.getDoorId();
		obj_id = door.getObjectId();
		p2 = 1;
		p3 = 1;
		p4 = door.isOpen() ? 0 : 1; //opened 0 /closed 1
		p6 = (int) door.getCurrentHp();
		p7 = door.getMaxHp();
		p8 = door.isHPVisible() ? 1 : 0;
		p9 = door.getDamage();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4c);
		writeD(obj_id);
		writeD(_id);
		writeD(p2);
		writeD(p3);
		writeD(p4);
		writeD(p6);
		writeD(p7);
		writeD(p8);
		writeD(p9);
	}
}