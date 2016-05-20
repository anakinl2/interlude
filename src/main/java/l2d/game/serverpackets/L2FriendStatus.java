package l2d.game.serverpackets;

import l2d.game.model.L2Player;

public class L2FriendStatus extends L2GameServerPacket
{
	private String char_name;
	private boolean _login = false;
	private L2Player _friend;

	public L2FriendStatus(L2Player player, boolean login)
	{
		if(player == null)
			return;
		_login = login;
		char_name = player.getName();
		_friend = player;

	}

	@Override
	protected final void writeImpl()
	{
		if(char_name == null)
			return;
		writeC(0xFC);
		writeD(_login ? 1 : 0); //Logged in 1 logged off 0
		writeS(char_name);
		writeD(_login ? _friend.getObjectId() : 0x00); // online
	}
}