package com.lineage.game.clientpackets;

public class RequestExWithdrawMpccRoom extends L2GameClientPacket
{
	@Override
	public void runImpl()
	{
		System.out.println(getType());
	}

	@Override
	public void readImpl()
	{}
}