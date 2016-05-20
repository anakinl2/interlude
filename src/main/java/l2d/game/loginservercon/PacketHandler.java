package l2d.game.loginservercon;

import java.io.IOException;
import java.util.logging.Logger;

import l2d.game.loginservercon.lspackets.AuthResponse;
import l2d.game.loginservercon.lspackets.BanIPList;
import l2d.game.loginservercon.lspackets.ChangePasswordResponse;
import l2d.game.loginservercon.lspackets.IpAction;
import l2d.game.loginservercon.lspackets.KickPlayer;
import l2d.game.loginservercon.lspackets.LoginServerBasePacket;
import l2d.game.loginservercon.lspackets.LoginServerFail;
import l2d.game.loginservercon.lspackets.MoveCharToAccResponse;
import l2d.game.loginservercon.lspackets.PlayerAuthResponse;
import l2d.game.loginservercon.lspackets.RSAKey;
import l2d.game.loginservercon.lspackets.TestConnection;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 22:41:04
 */
public class PacketHandler
{
	private static final Logger log = Logger.getLogger(PacketHandler.class.getName());

	public static LoginServerBasePacket handlePacket(byte[] data, AttLS loginserver)
	{
		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: Processing packet from LS");

		LoginServerBasePacket packet = null;

		try
		{
			data = loginserver.decrypt(data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		int id = data[0] & 0xFF;

		switch(id)
		{
			case 0:
				packet = new RSAKey(data, loginserver);
				break;
			case 1:
				packet = new LoginServerFail(data, loginserver);
				break;
			case 2:
				packet = new AuthResponse(data, loginserver);
				break;
			case 3:
				packet = new PlayerAuthResponse(data, loginserver);
				break;
			case 4:
				packet = new KickPlayer(data, loginserver);
				break;
			case 5:
				packet = new BanIPList(data, loginserver);
				break;
			case 6:
				packet = new ChangePasswordResponse(data, loginserver);
				break;
			case 7:
				packet = new IpAction(data, loginserver);
				break;
			case 0x08:
				packet = new MoveCharToAccResponse(data, loginserver);
				break;
			case 0x09:
				packet = new TestConnection(data, loginserver);
				break;
			default:
				log.severe("LSConnection: Recieved unknown packet: " + id + ". Terminating connection.");
				//LSConnection.getInstance().shutdown();
				LSConnection.getInstance().restart();

		}

		return packet;
	}
}
