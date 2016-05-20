package l2d.auth.gameservercon.gspackets;

import l2d.auth.IpManager;
import l2d.auth.gameservercon.AttGS;
import l2d.auth.gameservercon.GSConnection;
import l2d.auth.gameservercon.lspackets.BanIPList;
import l2d.auth.gameservercon.lspackets.IpAction;

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