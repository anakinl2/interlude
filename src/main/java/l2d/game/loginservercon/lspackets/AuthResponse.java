package l2d.game.loginservercon.lspackets;

import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.loginservercon.AttLS;
import l2d.game.loginservercon.Attribute;
import l2d.game.loginservercon.gspackets.PlayerInGame;
import l2d.game.loginservercon.gspackets.ServerStatus;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.tables.FakePlayersTable;

public class AuthResponse extends LoginServerBasePacket
{
	private static final Logger log = Logger.getLogger(AuthResponse.class.getName());

	private int _serverId;
	private String _serverName;

	public AuthResponse(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		_serverId = readC();
		_serverName = readS();
		getLoginServer().setLicenseShown(readC() == 1);

		log.info("Registered on login as Server " + _serverId + " : " + _serverName);

		FastList<Attribute> attributes = FastList.newInstance();

		attributes.add(new Attribute(Attribute.SERVER_LIST_SQUARE_BRACKET, Config.SERVER_LIST_BRACKET ? Attribute.ON : Attribute.OFF));
		attributes.add(new Attribute(Attribute.SERVER_LIST_CLOCK, Config.SERVER_LIST_CLOCK ? Attribute.ON : Attribute.OFF));
		attributes.add(new Attribute(Attribute.TEST_SERVER, Config.SERVER_LIST_TESTSERVER ? Attribute.ON : Attribute.OFF));
		attributes.add(new Attribute(Attribute.SERVER_LIST_STATUS, Config.SERVER_GMONLY ? Attribute.STATUS_GM_ONLY : Attribute.STATUS_AUTO));

		sendPacket(new ServerStatus(attributes));

		if(L2World.getAllPlayersCount() > 0)
		{
			FastList<String> playerList = FastList.newInstance();
			for(L2Player player : L2World.getAllPlayers())
			{
				if(player.isInOfflineMode())
					continue;
				if(player.getAccountName().isEmpty())
				{
					log.warning("AuthResponse: empty accname for " + player);
					continue;
				}
				playerList.add(player.getAccountName());
				getLoginServer().getCon().addAccountInGame(player.getNetConnection());
			}

			int size = L2World.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount();

			sendPacket(new PlayerInGame(null, size));
			for(String name : playerList)
				sendPacket(new PlayerInGame(name, size));
		}
	}

	/**
	 * @return Returns the serverId.
	 */
	public int getServerId()
	{
		return _serverId;
	}

	/**
	 * @return Returns the serverName.
	 */
	public String getServerName()
	{
		return _serverName;
	}
}
