package l2d.game.clientpackets;

import l2d.game.geodata.GeoEngine;
import l2d.game.instancemanager.BoatManager;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2BoatInstance;
import l2d.game.serverpackets.GetOffVehicle;
import l2d.game.serverpackets.VehicleInfo;

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