package com.lineage.game.serverpackets;

import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.util.Location;

public class VehicleInfo extends L2GameServerPacket
{
	private int _boatObjId;
	private Location _loc;

	public VehicleInfo(L2BoatInstance boat)
	{
		_boatObjId = boat.getObjectId();
		_loc = boat.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x59);
		writeD(_boatObjId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h);
	}
}