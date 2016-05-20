package l2d.auth.gameservercon.lspackets;

import l2d.Config;
import l2d.auth.GameServerTable;

public class AuthResponse extends ServerBasePacket
{
	public AuthResponse(int serverId)
	{
		writeC(0x02);
		writeC(serverId);
		writeS(GameServerTable.getInstance().getServerNameById(serverId));
		if(Config.SHOW_LICENCE)
			writeC(0);
	}
}