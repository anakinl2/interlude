package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2BoatInstance;

public class GetOffVehicle extends L2GameServerPacket
{
	private int _x, _y, _z, char_obj_id, boat_obj_id;

	public GetOffVehicle(L2Player activeChar, L2BoatInstance boat, int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
		char_obj_id = activeChar.getObjectId();
		boat_obj_id = boat.getObjectId();

		activeChar.setBoat(null);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6f);
		writeD(char_obj_id);
		writeD(boat_obj_id);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}
}