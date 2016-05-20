package l2d.auth.gameservercon.gspackets;

import l2d.Config;
import l2d.auth.L2LoginClient;
import l2d.auth.LoginController;
import l2d.auth.SessionKey;
import l2d.auth.gameservercon.AttGS;
import l2d.auth.gameservercon.lspackets.PlayerAuthResponse;

/**
 * @Author: Death
 * @Date: 15/11/2007
 * @Time: 12:30:08
 */
public class PlayerAuthRequest extends ClientBasePacket
{
	public PlayerAuthRequest(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		int playOkId1 = readD();
		int playOkId2 = readD();
		int loginOkId1 = readD();
		int loginOkId2 = readD();

		L2LoginClient client = LoginController.getInstance().getAuthedClient(account);

		if(client == null)
		{
			System.out.println("Client is null for account " + account);
			sendPacket(new PlayerAuthResponse(account));
			return;
		}

		SessionKey key = client.getSessionKey();

		int lPlayOk1 = key.playOkID1;
		int lPlayOk2 = key.playOkID2;
		int lLoginOk1 = key.loginOkID1;
		int lLoginOk2 = key.loginOkID2;

		boolean isAuthedOnLs;
		if(Config.SHOW_LICENCE)
			isAuthedOnLs = playOkId1 == lPlayOk1 && playOkId2 == lPlayOk2 && loginOkId1 == lLoginOk1 && loginOkId2 == lLoginOk2;
		else
			isAuthedOnLs = playOkId1 == lPlayOk1 && playOkId2 == lPlayOk2;

		sendPacket(new PlayerAuthResponse(client, isAuthedOnLs));
	}
}
