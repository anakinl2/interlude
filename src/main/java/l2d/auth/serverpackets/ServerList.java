package l2d.auth.serverpackets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javolution.util.FastList;
import l2d.auth.GameServerTable;
import l2d.auth.L2LoginClient;
import l2d.auth.gameservercon.GameServerInfo;
import l2d.auth.gameservercon.gspackets.ServerStatus;
import l2d.game.loginservercon.AdvIP;
import l2d.util.Util;

/**
 * ServerList
 * Format: cc [cddcchhcdc]
 * c: server list size (number of servers)
 * c: last server
 * [ (repeat for each servers)
 * c: server id (ignored by client?)
 * d: server ip
 * d: server port
 * c: age limit (used by client?)
 * c: pvp or not (used by client?)
 * h: current number of players
 * h: max number of players
 * c: 0 if server is down
 * d: 2nd bit: clock
 * 3rd bit: wont dsiplay server name
 * 4th bit: test server (used by client?)
 * c: 0 if you dont want to display brackets in front of sever name
 * ]
 * Server will be considered as Good when the number of online players
 * is less than half the maximum. as Normal between half and 4/5
 * and Full when there's more than 4/5 of the maximum number of players
 */
public final class ServerList extends L2LoginServerPacket
{
	private List<ServerData> _servers;
	private int _lastServer;

	class ServerData
	{
		String ip;
		int port;
		boolean pvp;
		int currentPlayers;
		int maxPlayers;
		boolean testServer;
		boolean brackets;
		boolean clock;
		int status;
		public int server_id;

		ServerData(final String pIp, final int pPort, final boolean pPvp, final boolean pTestServer, final int pCurrentPlayers, final int pMaxPlayers, final boolean pBrackets, final boolean pClock, final int pStatus, final int pServer_id)
		{
			ip = pIp;
			port = pPort;
			pvp = pPvp;
			testServer = pTestServer;
			currentPlayers = pCurrentPlayers;
			maxPlayers = pMaxPlayers;
			brackets = pBrackets;
			clock = pClock;
			status = pStatus;
			server_id = pServer_id;
		}
	}

	public ServerList(final L2LoginClient client)
	{
		_servers = new FastList<ServerData>();
		_lastServer = client.getLastServer();
		for(GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
		{
			// addServer("192.168.66.55", gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(),
			// gsi.getStatus(), gsi.getId());
			// addServer("10.101.53.80", gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(),
			// gsi.getStatus(), gsi.getId());
			// addServer("10.50.4.222", gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(),
			// gsi.getStatus(), gsi.getId());

			Boolean added = false;
			if(client.getIpAddress().equals("Null IP"))
				continue;
			String ipAddr = Util.isInternalIP(client.getIpAddress()) ? gsi.getInternalHost() : gsi.getExternalHost();
			if(ipAddr == null || ipAddr.equals("Null IP"))
				continue;

			if(gsi.getStatus() == ServerStatus.STATUS_GM_ONLY && client.getAccessLevel() > 0)
			{
				// Если сервер только для ГМов, и клиент ГМ.
				if(gsi.getAdvIP() != null)
					for(final AdvIP ip : gsi.getAdvIP())
						if(!added && GameServerTable.getInstance().CheckSubNet(client.getConnection().getSocket().getInetAddress().getHostAddress(), ip))
						{
							added = true;
							addServer(ip.ipadress, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
						}
				if(!added)
					if(ipAddr.equals("*"))
						addServer(client.getConnection().getSocket().getLocalAddress().getHostAddress(), gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
					else
						addServer(ipAddr, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
			}
			else if(gsi.getStatus() != ServerStatus.STATUS_GM_ONLY)
			{
				// Если сервер для всех.
				if(gsi.getAdvIP() != null)
					for(final AdvIP ip : gsi.getAdvIP())
						if(!added && GameServerTable.getInstance().CheckSubNet(client.getConnection().getSocket().getInetAddress().getHostAddress(), ip))
						{
							added = true;
							addServer(ip.ipadress, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
						}
				if(!added)
					if(ipAddr.equals("*"))
						addServer(client.getConnection().getSocket().getLocalAddress().getHostAddress(), gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
					else
						addServer(ipAddr, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
			}
			else
			{
				// Если сервер для ГМов и клиент не ГМ
				if(gsi.getAdvIP() != null)
					for(final AdvIP ip : gsi.getAdvIP())
						if(!added && GameServerTable.getInstance().CheckSubNet(client.getConnection().getSocket().getInetAddress().getHostAddress(), ip))
						{
							added = true;
							addServer(ip.ipadress, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
						}
				if(!added)
					if(ipAddr.equals("*"))
						addServer(client.getConnection().getSocket().getLocalAddress().getHostAddress(), gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
					else
						addServer(ipAddr, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), ServerStatus.STATUS_DOWN, gsi.getId());
			}
		}
	}

	public void addServer(final String ip, final int port, final boolean pvp, final boolean testServer, final int currentPlayer, final int maxPlayer, final boolean brackets, final boolean clock, final int status, final int server_id)
	{
		_servers.add(new ServerData(ip, port, pvp, testServer, currentPlayer, maxPlayer, brackets, clock, status, server_id));
	}

	@Override
	public void write()
	{
		writeC(0x04);
		writeC(_servers.size());
		writeC(_lastServer);
		for(final ServerData server : _servers)
		{
			writeC(server.server_id); // server id

			try
			{
				final InetAddress i4 = InetAddress.getByName(server.ip);
				final byte[] raw = i4.getAddress();
				writeC(raw[0] & 0xff);
				writeC(raw[1] & 0xff);
				writeC(raw[2] & 0xff);
				writeC(raw[3] & 0xff);
			}
			catch(final UnknownHostException e)
			{
				e.printStackTrace();
				writeC(127);
				writeC(0);
				writeC(0);
				writeC(1);
			}

			writeD(server.port);
			writeC(0x00); // age limit
			writeC(server.pvp ? 0x01 : 0x00);
			writeH(server.currentPlayers);
			writeH(server.maxPlayers);
			writeC(server.status == ServerStatus.STATUS_DOWN ? 0x00 : 0x01);
			int bits = 0;
			if(server.testServer)
				bits |= 0x04;
			if(server.clock)
				bits |= 0x02;
			writeD(bits);
			writeC(server.brackets ? 0x01 : 0x00);
		}
	}
}