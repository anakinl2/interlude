package l2d.auth.gameservercon.lspackets;

import javolution.util.FastList;
import l2d.auth.IpManager;
import l2d.util.BannedIp;

/**
 * @author -Wooden-
 *
 */
public class BanIPList extends ServerBasePacket
{
	// ID 0x00
	// format
	// d proto rev
	// d key size
	// b key

	public BanIPList()
	{
		FastList<BannedIp> baniplist = IpManager.getInstance().getBanList();
		writeC(0x05);
		writeD(baniplist.size());
		for(BannedIp ip : baniplist)
		{
			writeS(ip.ip);
			writeS(ip.admin);
		}

		FastList.recycle(baniplist);
	}
}
