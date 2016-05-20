package com.lineage.auth.gameservercon.gspackets;

import com.lineage.auth.gameservercon.lspackets.IpAction;
import com.lineage.auth.IpManager;
import com.lineage.auth.gameservercon.AttGS;
import com.lineage.auth.gameservercon.GSConnection;
import com.lineage.auth.gameservercon.lspackets.BanIPList;

/**
 * @author -Wooden-
 *
 */
public class UnbanIP extends ClientBasePacket
{
	public UnbanIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String ip = readS();
		IpManager.getInstance().UnbanIp(ip);

		GSConnection.getInstance().broadcastPacket(new BanIPList());
		GSConnection.getInstance().broadcastPacket(new IpAction(ip, false, ""));
	}
}