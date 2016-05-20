package com.lineage.game.serverpackets;

public class SunRise extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x1c);
	}
}