package l2d.game.serverpackets;

public class ExEventMatchCreate extends L2GameServerPacket
{

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x04);
		writeC(2);
		writeS("HIIII");
	}
}