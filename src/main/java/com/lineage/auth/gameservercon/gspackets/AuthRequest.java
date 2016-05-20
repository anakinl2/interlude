package com.lineage.auth.gameservercon.gspackets;

import java.util.Arrays;
import java.util.logging.Logger;

import com.lineage.auth.GameServerTable;
import javolution.util.FastList;
import com.lineage.auth.gameservercon.AttGS;
import com.lineage.auth.gameservercon.GameServerInfo;
import com.lineage.auth.gameservercon.lspackets.AuthResponse;
import com.lineage.auth.gameservercon.lspackets.BanIPList;
import com.lineage.auth.gameservercon.lspackets.LoginServerFail;
import l2d.game.loginservercon.AdvIP;

/**
 * Format: cccddbd(sss)
 * c desired ID
 * c accept alternative ID
 * c reserve Host
 * s ExternalHostName
 * s InetranlHostName
 * d max players
 * d hexid size
 * b hexid
 * d size of AdvIP
 * (sss) Ip, IpMask, BitMask
 *
 * @author -Wooden-
 *
 */
public class AuthRequest extends ClientBasePacket
{
	protected static Logger log = Logger.getLogger(AuthRequest.class.getName());

	public AuthRequest(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		int requestId = readC();
		readC();
		readC();
		String externalIp = readS();
		String internalIp = readS();
		int port = readH();
		int maxOnline = readD();
		int hexIdLenth = readD();
		byte[] hexId = readB(hexIdLenth);
		int advIpsSize = readD();

		FastList<AdvIP> advIpList = FastList.newInstance();

		for(int i = 0; i < advIpsSize; i++)
		{
			AdvIP ip = new AdvIP();
			ip.ipadress = readS();
			ip.ipmask = readS();
			ip.bitmask = readS();
			advIpList.add(ip);
		}

		log.info("Trying to register server: " + requestId + ", " + getGameServer().getConnectionIpAddress());

		GameServerTable gameServerTable = GameServerTable.getInstance();

		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(requestId);
		// is there a gameserver registered with this id?
		if(gsi != null)
		{
			// does the hex id match?
			if(Arrays.equals(gsi.getHexId(), hexId))
				// check to see if this GS is already connected
				synchronized (gsi)
				{
					if(gsi.isAuthed())
						sendPacket(new LoginServerFail(LoginServerFail.REASON_ALREADY_LOGGED8IN));
					else
					{
						getGameServer().setGameServerInfo(gsi);
						gsi.setGameServer(getGameServer());
						gsi.setPort(port);
						gsi.setGameHosts(externalIp, internalIp, advIpList);
						gsi.setMaxPlayers(maxOnline);
						gsi.setAuthed(true);
					}
				}
			else

				// server id is already taken, and we cant get a new one for you
				sendPacket(new LoginServerFail(LoginServerFail.REASON_WRONG_HEXID));
		}
		else
			sendPacket(new LoginServerFail(LoginServerFail.REASON_WRONG_HEXID));

		if(gsi != null && gsi.isAuthed())
		{
			AuthResponse ar = new AuthResponse(gsi.getId());
			getGameServer().setAuthed(true);
			getGameServer().setServerId(gsi.getId());
			sendPacket(ar);
			sendPacket(new BanIPList());
			log.info("Server registration successful.");
		}
		else
			log.info("Server registration failed.");
	}
}
