package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Character;
import com.lineage.util.Location;

/**
 * format   dddddd		(player id, target id, distance, startx, starty, startz)<p>
 */
public class ValidateLocation extends L2GameServerPacket
{
	private int _chaObjId;
	private Location _loc;

	public ValidateLocation(L2Character cha)
	{
		_chaObjId = cha.getObjectId();
		_loc = cha.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x61);

		writeD(_chaObjId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h);
	}
}