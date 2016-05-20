package l2d.game.clientpackets;

import l2d.game.instancemanager.BoatManager;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2BoatInstance;
import l2d.game.serverpackets.GetOnVehicle;
import l2d.util.Location;

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