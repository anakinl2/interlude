package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.BoatManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.serverpackets.MoveToLocationInVehicle;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Location;

public class RequestMoveToLocationInVehicle extends L2GameClientPacket
{
	private Location _pos = new Location(0, 0, 0);
	private Location _originPos = new Location(0, 0, 0);
	private int _boatId;

	/**
	 * format: cddddddd
	 */
	@Override
	public void readImpl()
	{
		_boatId = readD(); //objectId of boat
		_pos.x = readD();
		_pos.y = readD();
		_pos.z = readD();
		_originPos.x = readD();
		_originPos.y = readD();
		_originPos.z = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.getPet() != null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.BECAUSE_PET_OR_SERVITOR_MAY_BE_DROWNED_WHILE_THE_BOAT_MOVES_PLEASE_RELEASE_THE_SUMMON_BEFORE_DEPARTURE));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isMovementDisabled() || activeChar.isSitting())
		{
			activeChar.sendActionFailed();
			return;
		}

		L2BoatInstance boat = BoatManager.getInstance().GetBoat(_boatId);
		if(boat == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.isInBoat() || activeChar.getBoat() != boat)
			activeChar.setBoat(boat);

		activeChar.setInBoatPosition(_pos);
		activeChar.broadcastPacket(new MoveToLocationInVehicle(activeChar, boat, _originPos, _pos));
	}
}