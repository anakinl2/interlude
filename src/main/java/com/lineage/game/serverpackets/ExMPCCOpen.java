package com.lineage.game.serverpackets;

/**
 * Opens the CommandChannel Information window
 */
public class ExMPCCOpen extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x25);
	}
}