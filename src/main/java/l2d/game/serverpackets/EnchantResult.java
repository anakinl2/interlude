package l2d.game.serverpackets;

public class EnchantResult extends L2GameServerPacket
{
	private int _unknown;

	public EnchantResult(int unknown)
	{
		_unknown = unknown;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x81);
		writeD(_unknown);
	}
}