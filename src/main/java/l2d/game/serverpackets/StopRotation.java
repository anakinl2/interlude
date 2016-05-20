package l2d.game.serverpackets;

public class StopRotation extends L2GameServerPacket
{
	private int _charObjId;
	private int _degree;
	private int _speed;

	public StopRotation(int objectid, int degree, int speed)
	{
		_charObjId = objectid;
		_degree = degree;
		_speed = speed;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x63);
		writeD(_charObjId);
		writeD(_degree);
		writeD(_speed);
	}
}
