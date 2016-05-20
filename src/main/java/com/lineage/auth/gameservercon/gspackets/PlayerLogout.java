package com.lineage.auth.gameservercon.gspackets;

import java.util.logging.Logger;

import com.lineage.auth.GameServerTable;
import com.lineage.auth.LoginController;
import com.lineage.auth.gameservercon.AttGS;
import com.lineage.Config;

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