package com.lineage.game.serverpackets;

public class StartRotation extends L2GameServerPacket
{
	private int _charObjId;
	private int _degree;
	private int _side;
	private int _speed;

	public StartRotation(int objectid, int degree, int speed, int side)
	{
		_charObjId = objectid;
		_degree = degree;
		_speed = speed;
		_side = side;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x7a);
		writeD(_charObjId);
		writeD(_degree);
		writeD(_side);
		writeD(_speed);
	}
}