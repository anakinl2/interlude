package l2d.auth.gameservercon;

import java.io.IOException;
import java.util.logging.Logger;

import l2d.auth.gameservercon.gspackets.AuthRequest;
import l2d.auth.gameservercon.gspackets.BanIP;
import l2d.auth.gameservercon.gspackets.BlowFishKey;
import l2d.auth.gameservercon.gspackets.ChangeAccessLevel;
import l2d.auth.gameservercon.gspackets.ChangePassword;
import l2d.auth.gameservercon.gspackets.ClientBasePacket;
import l2d.auth.gameservercon.gspackets.LockAccountIP;
import l2d.auth.gameservercon.gspackets.MoveCharToAcc;
import l2d.auth.gameservercon.gspackets.PlayerAuthRequest;
import l2d.auth.gameservercon.gspackets.PlayerInGame;
import l2d.auth.gameservercon.gspackets.PlayerLogout;
import l2d.auth.gameservercon.gspackets.Restart;
import l2d.auth.gameservercon.gspackets.ServerStatus;
import l2d.auth.gameservercon.gspackets.TestConnectionResponse;
import l2d.auth.gameservercon.gspackets.UnbanIP;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 19:05:16
 */
public class PacketHandler
{
	private static Logger log = Logger.getLogger(PacketHandler.class.getName());

	public static ClientBasePacket handlePacket(byte[] data, AttGS gameserver)
	{
		ClientBasePacket packet = null;
		try
		{
			data = gameserver.decrypt(data);
			int packetType = data[0] & 0xff;

			if(!gameserver.isAuthed() && packetType > 1)
			{
				log.severe("Packet id[" + packetType + "] from not authed server.");
				return null;
			}

			switch(packetType)
			{
				case 0x00:
					new BlowFishKey(data, gameserver).run();
					break;
				case 0x01:
					new AuthRequest(data, gameserver).run();
					break;
				case 0x02:
					packet = new PlayerInGame(data, gameserver);
					break;
				case 0x03:
					packet = new PlayerLogout(data, gameserver);
					break;
				case 0x04:
					packet = new ChangeAccessLevel(data, gameserver);
					break;
				case 0x05:
					packet = new PlayerAuthRequest(data, gameserver);
					break;
				case 0x06:
					packet = new ServerStatus(data, gameserver);
					break;
				case 0x07:
					packet = new BanIP(data, gameserver);
					break;
				case 0x08:
					packet = new ChangePassword(data, gameserver);
					break;
				case 0x09:
					packet = new Restart(data, gameserver);
					break;
				case 0x0a:
					packet = new UnbanIP(data, gameserver);
					break;
				case 0x0b:
					packet = new LockAccountIP(data, gameserver);
					break;
				case 0x0c:
					packet = new MoveCharToAcc(data, gameserver);
					break;
				case 0x0d:
					packet = new TestConnectionResponse(data, gameserver);
					break;
				default:
					log.severe("Unknown packet from GS: " + packetType);

			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return packet;
	}
}