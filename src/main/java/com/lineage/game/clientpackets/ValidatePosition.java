package com.lineage.game.clientpackets;

import com.lineage.Config;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.util.Location;

public class ValidatePosition extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);
	@SuppressWarnings("unused")
	private int _data;
	private double _diff;
	private Location _lastClientPosition;
	private Location _lastServerPosition;

	/**
	 * packet type id 0x48
	 * format: cddddd
	 */
	@Override
	public void readImpl()
	{
		_loc.x = readD();
		_loc.y = readD();
		_loc.z = readD();
		_loc.h = readD();
		_data = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.isTeleporting() || activeChar.inObserverMode())
			return;

		_lastClientPosition = activeChar.getLastClientPosition();
		_lastServerPosition = activeChar.getLastServerPosition();

		if(_lastClientPosition == null)
			_lastClientPosition = activeChar.getLoc();
		if(_lastServerPosition == null)
			_lastServerPosition = activeChar.getLoc();

		if(activeChar.getX() == 0 && activeChar.getY() == 0 && activeChar.getZ() == 0)
		{
			correctPosition(activeChar);
			return;
		}

		int dz = _lastClientPosition.z - _loc.z;

		if(!activeChar.inObserverMode() && Config.DAMAGE_FROM_FALLING && dz >= 333 && !activeChar.isInZone(ZoneType.water))
			activeChar.falling(dz);

		_diff = activeChar.getDistance(_loc.x, _loc.y);

		/*
		 * if(activeChar.isInBoat())
		 * activeChar.setXYZ(activeChar.getBoat().getX(), activeChar.getBoat().getY(), activeChar.getBoat().getZ());
		 * else if(activeChar.isFlying())
		 * activeChar.setXYZ(_loc.x, _loc.y, _loc.z);
		 * else if(activeChar.isSwimming())
		 * activeChar.setXYZ(_loc.x, _loc.y, _loc.z);
		 * else
		 */if(dz < 333 && Math.abs(_loc.z - activeChar.getZ()) >= 333)
		{
			if(activeChar.getIncorrectValidateCount() >= 3)
				activeChar.teleToClosestTown();
			else if(Config.GEODATA_ENABLE)
			{
				activeChar.teleToLocation(activeChar.getLoc());
				activeChar.setIncorrectValidateCount(activeChar.getIncorrectValidateCount() + 1);
			}
		}
		else if(_loc.z < -15000 || _loc.z > 15000)
		{
			if(activeChar.getIncorrectValidateCount() >= 3)
				activeChar.teleToClosestTown();
			else
			{
				correctPosition(activeChar);
				activeChar.setIncorrectValidateCount(activeChar.getIncorrectValidateCount() + 1);
			}
		}
		else if(_diff > 1000)
		{
			if(activeChar.getIncorrectValidateCount() >= 3)
				activeChar.teleToClosestTown();
			else if(Config.GEODATA_ENABLE)
			{
				activeChar.teleToLocation(activeChar.getLoc());
				activeChar.setIncorrectValidateCount(activeChar.getIncorrectValidateCount() + 1);
			}
		}
		else if(_diff > 128) // old: activeChar.getMoveSpeed() * 2
			// && !activeChar.isFlying() && !activeChar.isInBoat() && !activeChar.isSwimming()
			// TODO реализовать NetPing и вычислять предельное отклонение исходя из пинга по формуле: 16 + (ping * activeChar.getMoveSpeed()) / 1000
			activeChar.validateLocation(false);
		/*
		 * if(activeChar.isMoving)
		 * activeChar.broadcastPacket(new CharMoveToLocation(activeChar));
		 * else
		 * activeChar.broadcastPacket(new ValidateLocation(activeChar));
		 */
		else
			activeChar.setIncorrectValidateCount(0);

		activeChar.checkWaterState();

		if(activeChar.getPet() != null && !activeChar.getPet().isInRange())
			activeChar.getPet().teleportToOwner();

		activeChar.setLastClientPosition(_loc.setH(activeChar.getHeading()));
		activeChar.setLastServerPosition(activeChar.getLoc());
	}

	private void correctPosition(L2Player activeChar)
	{
		if(_lastServerPosition.x != 0 && _lastServerPosition.y != 0 && _lastServerPosition.z != 0)
		{
			if(GeoEngine.getNSWE(_lastServerPosition.x, _lastServerPosition.y, _lastServerPosition.z) == 15)
				activeChar.teleToLocation(_lastServerPosition);
			else
				activeChar.teleToClosestTown();
		}
		else if(_lastClientPosition.x != 0 && _lastClientPosition.y != 0 && _lastClientPosition.z != 0)
		{
			if(GeoEngine.getNSWE(_lastClientPosition.x, _lastClientPosition.y, _lastClientPosition.z) == 15)
				activeChar.teleToLocation(_lastClientPosition);
			else
				activeChar.teleToClosestTown();
		}
		else
			activeChar.teleToClosestTown();
	}
}