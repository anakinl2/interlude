package com.lineage.game.loginservercon.gspackets;

import com.lineage.game.loginservercon.Attribute;
import javolution.util.FastList;

public class ServerStatus extends GameServerBasePacket
{

	public ServerStatus(FastList<Attribute> attributes)
	{
		writeC(0x06);
		writeD(attributes.size());
		for(Attribute temp : attributes)
		{
			writeD(temp.id);
			writeD(temp.value);
		}

		FastList.recycle(attributes);
	}
}