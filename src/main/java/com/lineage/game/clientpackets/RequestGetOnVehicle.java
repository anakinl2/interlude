package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.BoatManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.serverpackets.GetOnVehicle;
import com.lineage.util.Location;

public class RequestGetOnVehicle extends L2GameClientPacket
{
	private int _id, _x, _y, _z;

	/**
	 * packet type id 0x53
	 * format:      cdddd
	 */
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
			return;
		activeChar.setBoat(boat);
		activeChar.setInBoatPosition(new Location(_x, _y, _z));
		activeChar.setLoc(boat.getLoc());
		activeChar.broadcastPacket(new GetOnVehicle(activeChar, boat, _x, _y, _z));
	}
}