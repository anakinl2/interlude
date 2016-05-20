package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Character;

public class FinishRotation extends L2GameServerPacket
{
	private int _heading;
	private int _charObjId;

	public FinishRotation(L2Character cha)
	{
		_charObjId = cha.getObjectId();
		_heading = cha.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x63);
		writeD(_charObjId);
		writeD(_heading);
	}
}