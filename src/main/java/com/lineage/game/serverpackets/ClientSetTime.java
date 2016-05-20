package com.lineage.game.serverpackets;

import com.lineage.game.GameTimeController;

public class ClientSetTime extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xEC);
		writeD(GameTimeController.getInstance().getGameTime()); // time in client minutes
		writeD(6); //constant to match the server time( this determines the speed of the client clock)
	}
}