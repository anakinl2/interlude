package com.lineage.auth.gameservercon.lspackets;

import com.lineage.auth.GameServerTable;
import com.lineage.Config;

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