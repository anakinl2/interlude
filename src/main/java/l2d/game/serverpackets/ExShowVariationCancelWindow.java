package l2d.game.serverpackets;

public class ExShowVariationCancelWindow extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x51);
	}
}