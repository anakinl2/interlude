package l2d.game.serverpackets;

import l2d.game.model.L2Player;

public class RecipeShopMsg extends L2GameServerPacket
{
	private int _chaObjectId;
	private String _chaStoreName;

	public RecipeShopMsg(L2Player player)
	{
		if(player.getCreateList() == null || player.getCreateList().getStoreName() == null)
			return;
		_chaObjectId = player.getObjectId();
		_chaStoreName = player.getCreateList().getStoreName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xdb);
		writeD(_chaObjectId);
		writeS(_chaStoreName);
	}
}