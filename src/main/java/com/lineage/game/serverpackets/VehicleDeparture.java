package com.lineage.game.serverpackets;

import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.util.Location;

public class VehicleDeparture extends L2GameServerPacket
{
	private int _moveSpeed, _rotationSpeed;
	private int _boatObjId;
	private Location _loc;

	public VehicleDeparture(L2BoatInstance boat)
	{
		_boatObjId = boat.getObjectId();
		_moveSpeed = (int) boat.getMoveSpeed();
		_rotationSpeed = boat.getRotationSpeed();
		_loc = boat.getDestination();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x5a);
		writeD(_boatObjId);
		writeD(_moveSpeed);
		writeD(_rotationSpeed);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
	}
}