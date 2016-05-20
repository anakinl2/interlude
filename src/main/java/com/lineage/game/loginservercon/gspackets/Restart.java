package com.lineage.game.loginservercon.gspackets;

public class Restart extends GameServerBasePacket
{
	public Restart()
	{
		writeC(0x09);
	}
}