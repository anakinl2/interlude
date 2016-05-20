package com.lineage.game.serverpackets;

public class SetupGauge extends L2GameServerPacket
{
	public static final int BLUE = 0;
	public static final int RED = 1;
	public static final int CYAN = 2;

	private int _dat1;
	private int _time;

	public SetupGauge(int dat1, int time)
	{
		_dat1 = dat1;// color  0-blue   1-red  2-cyan  3-
		_time = time;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6d);
		writeD(_dat1);
		writeD(_time);

		writeD(_time); //c2
	}
}