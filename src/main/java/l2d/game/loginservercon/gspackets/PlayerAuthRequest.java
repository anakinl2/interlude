package l2d.game.loginservercon.gspackets;

import l2d.game.network.L2GameClient;

public class PlayerAuthRequest extends GameServerBasePacket
{
	public PlayerAuthRequest(L2GameClient client)
	{
		writeC(0x05);
		writeS(client.getLoginName());
		writeD(client.getSessionId().playOkID1);
		writeD(client.getSessionId().playOkID2);
		writeD(client.getSessionId().loginOkID1);
		writeD(client.getSessionId().loginOkID2);
	}
}