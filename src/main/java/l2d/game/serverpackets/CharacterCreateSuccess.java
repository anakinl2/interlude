package l2d.game.serverpackets;

/**
 * [S] 0f CharacterCreateSuccess
 * @author Felixx
 *
 */
public class CharacterCreateSuccess extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x19);
		writeD(0x01);
	}
}