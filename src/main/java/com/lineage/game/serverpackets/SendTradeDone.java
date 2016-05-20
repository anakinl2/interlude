package com.lineage.game.serverpackets;

public class SendTradeDone extends L2GameServerPacket
{
	private int _num;

	public SendTradeDone(int num)
	{
		_num = num;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x22);
		writeD(_num);
	}
}