package l2d.game.serverpackets;

public class ObserverStart extends L2GameServerPacket
{
	// ddSS
	private int _x, _y, _z;

	public ObserverStart(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xdf);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeC(0x00);
		writeC(0xc0);
		writeC(0x00);
	}
}