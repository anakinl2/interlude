package com.lineage.game.serverpackets;

public class NormalCamera extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xc8);
	}
}