package l2d.auth.gameservercon.gspackets;

import l2d.auth.IpManager;
import l2d.auth.gameservercon.AttGS;
import l2d.auth.gameservercon.GSConnection;
import l2d.auth.gameservercon.lspackets.BanIPList;
import l2d.auth.gameservercon.lspackets.IpAction;

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