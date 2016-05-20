package l2d.game.serverpackets;

import l2d.game.model.L2Player;

/**
 * format: d
 */
public class ChairSit extends L2GameServerPacket
{
	private L2Player _activeChar;
	private int _staticObjectId;

	public ChairSit(L2Player player, int staticObjectId)
	{
		_activeChar = player;
		_staticObjectId = staticObjectId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe1);
		writeD(_activeChar.getObjectId());
		writeD(_staticObjectId);
	}
}