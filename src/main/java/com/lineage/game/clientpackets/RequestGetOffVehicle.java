package com.lineage.game.clientpackets;

import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.instancemanager.BoatManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.serverpackets.GetOffVehicle;
import com.lineage.game.serverpackets.VehicleInfo;

public class RequestGetOffVehicle extends L2GameClientPacket
{
	// Format: cdddd
	private int _id, _x, _y, _z;

	@Override
	public void readImpl()
	{
		_id = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		L2BoatInstance boat = BoatManager.getInstance().GetBoat(_id);
		if(boat == null)
		{
			activeChar.sendActionFailed();
			return;
		}
		// Не даем слезть с лодки на ходу
		if(boat.isMoving)
		{
			activeChar.sendActionFailed();
			return;
		}
		activeChar.setXYZ(_x, _y, GeoEngine.getHeight(_x, _y, _z));
		activeChar.broadcastPacket(new GetOffVehicle(activeChar, boat, _x, _y, _z));
		activeChar.sendPacket(new VehicleInfo(boat));
	}
}