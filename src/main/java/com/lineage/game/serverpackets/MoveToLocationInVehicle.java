package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.util.Location;

public class MoveToLocationInVehicle extends L2GameServerPacket
{
	private int char_id, boat_id;
	private Location _origin, _destination;

	public MoveToLocationInVehicle(L2Player cha, L2BoatInstance boat, Location origin, Location destination)
	{
		char_id = cha.getObjectId();
		boat_id = boat.getObjectId();
		_origin = origin;
		_destination = destination;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x71);
		writeD(char_id);
		writeD(boat_id);
		writeD(_destination.x);
		writeD(_destination.y);
		writeD(_destination.z);
		writeD(_origin.x);
		writeD(_origin.y);
		writeD(_origin.z);
	}
}