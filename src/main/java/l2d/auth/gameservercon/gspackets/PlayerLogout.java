package l2d.auth.gameservercon.gspackets;

import java.util.logging.Logger;

import l2d.Config;
import l2d.auth.GameServerTable;
import l2d.auth.LoginController;
import l2d.auth.gameservercon.AttGS;

/**
 * @author -Wooden-
 */
public class PlayerLogout extends ClientBasePacket
{
	public static final Logger log = Logger.getLogger(PlayerLogout.class.getName());

	public PlayerLogout(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();

		getGameServer().removeAccountFromGameServer(account);
		LoginController.getInstance().removeAuthedLoginClient(account);

		if(Config.LOGIN_DEBUG)
			log.info("Player " + account + " logged out from gameserver [" + getGameServer().getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getGameServer().getServerId()));
	}
}