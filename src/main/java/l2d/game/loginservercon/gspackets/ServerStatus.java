package l2d.game.loginservercon.gspackets;

import javolution.util.FastList;
import l2d.game.loginservercon.Attribute;

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