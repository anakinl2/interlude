package com.lineage.game.serverpackets;

public class CharacterDeleteSuccess extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x23);
	}
}