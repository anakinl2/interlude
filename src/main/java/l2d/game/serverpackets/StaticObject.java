package l2d.game.serverpackets;

import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2StaticObjectInstance;

public class StaticObject extends L2GameServerPacket
{
	private int _id, obj_id;

	public StaticObject(L2StaticObjectInstance StaticObject)
	{
		_id = StaticObject.getStaticObjectId();
		obj_id = StaticObject.getObjectId();
	}

	public StaticObject(L2DoorInstance door)
	{
		_id = door.getDoorId();
		obj_id = door.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x99);
		writeD(_id);
		writeD(obj_id);
	}
}