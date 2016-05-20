package com.lineage.auth.gameservercon.gspackets;

import com.lineage.auth.IpManager;
import com.lineage.auth.gameservercon.AttGS;
import com.lineage.auth.gameservercon.GSConnection;
import com.lineage.auth.gameservercon.lspackets.BanIPList;
import com.lineage.auth.gameservercon.lspackets.IpAction;

public class BanIP extends ClientBasePacket
{

	public BanIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String ip = readS();
		String admin = readS();

		IpManager.getInstance().BanIp(ip, admin, 0, "");
		GSConnection.getInstance().broadcastPacket(new BanIPList());
		GSConnection.getInstance().broadcastPacket(new IpAction(ip, true, admin));
	}
}