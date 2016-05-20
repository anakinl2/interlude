package com.lineage.game.clientpackets;

public class RequestExFishRanking extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		System.out.println("Unfinished packet: " + getType());
	}
}