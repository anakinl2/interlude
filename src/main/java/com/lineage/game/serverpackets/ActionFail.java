package com.lineage.game.serverpackets;

public class ActionFail extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x25);
	}
}