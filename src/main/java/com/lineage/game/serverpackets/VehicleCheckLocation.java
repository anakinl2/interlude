package com.lineage.game.serverpackets;

import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.util.Location;

public class VehicleCheckLocation extends L2GameServerPacket
{
	private int _boatObjId;
	private Location _loc;

	public VehicleCheckLocation(L2BoatInstance instance)
	{
		_boatObjId = instance.getObjectId();
		_loc = instance.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x73);
		writeD(_boatObjId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h);
	}
}