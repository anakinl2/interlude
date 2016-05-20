package com.lineage.game.serverpackets;

public class AutoAttackStart extends L2GameServerPacket
{
	// dh
	private int _targetId;

	public AutoAttackStart(int targetId)
	{
		_targetId = targetId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2b);
		writeD(_targetId);
	}
}