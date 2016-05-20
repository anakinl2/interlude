package com.lineage.game.serverpackets;

public class TutorialCloseHtml extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xa3);
	}
}