package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.serverpackets.StopMoveToLocationInVehicle;
import com.lineage.util.Location;

/**
 * [C] 5D CannotMoveAnymoreInVehicle
 * <b>Format:<b/> cddddd
 * @author Felixx
 */
public class CannotMoveAnymoreInVehicle extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);
	private int _boatid;

	@Override
	public void readImpl()
	{
		_boatid = readD();
		_loc.x = readD();
		_loc.y = readD();
		_loc.z = readD();
		_loc.h = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(activeChar.isInBoat())
			if(activeChar.getBoat().getObjectId() == _boatid)
			{
				activeChar.setInBoatPosition(_loc);
				activeChar.setHeading(_loc.h);
				StopMoveToLocationInVehicle msg = new StopMoveToLocationInVehicle(activeChar, _boatid);
				activeChar.broadcastPacket(msg);
			}
	}
}