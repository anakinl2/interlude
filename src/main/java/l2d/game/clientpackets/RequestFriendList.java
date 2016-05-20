package l2d.game.clientpackets;

import l2d.game.serverpackets.L2FriendList;

public class RequestFriendList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		sendPacket(new L2FriendList(getClient().getActiveChar()));
	}
}