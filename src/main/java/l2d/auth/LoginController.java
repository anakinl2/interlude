package l2d.auth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import javolution.util.FastCollection.Record;
import javolution.util.FastMap;
import javolution.util.FastSet;
import l2d.Base64;
import l2d.Config;
import l2d.auth.crypt.Crypt;
import l2d.auth.crypt.ScrambledKeyPair;
import l2d.auth.gameservercon.AttGS;
import l2d.auth.gameservercon.GameServerInfo;
import l2d.auth.serverpackets.LoginFail.LoginFailReason;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.db.mysql;
import l2d.game.templates.StatsSet;
import l2d.util.Log;
import l2d.util.NetList;
import l2d.util.Rnd;

public class LoginController
{
	protected static Logger _log = Logger.getLogger(LoginController.class.getName());

	private static LoginController _instance;

	/** Time before kicking the client if he didnt logged yet */
	private final static int LOGIN_TIMEOUT = 60 * 1000;

	/** Clients that are on the LS but arent assocated with a account yet*/
	protected final FastSet<L2LoginClient> _clients = new FastSet<L2LoginClient>();

	/** Authed Clients on LoginServer*/
	protected final FastMap<String, L2LoginClient> _loginServerClients = new FastMap<String, L2LoginClient>().setShared(true);

	private Map<InetAddress, BanInfo> _bannedIps = new FastMap<InetAddress, BanInfo>().setShared(true);

	private Map<InetAddress, FailedLoginAttempt> _hackProtection;

	protected ScrambledKeyPair[] _keyPairs;

	protected byte[][] _blowfishKeys;

	public static Crypt DEFAULT_CRYPT;
	public static Crypt[] LEGACY_CRYPT;

	public static enum State
	{
		VALID,
		WRONG,
		NOT_PAID,
		BANNED,
		IN_USE,
		IP_ACCESS_DENIED
	}

	public class Status
	{
		public float bonus = 1;
		public int bonus_expire = 0;
		//public boolean proxy = false;
		public State state;

		public void setBonus(float value)
		{
			bonus = value;
		}

		public void setBonusExpire(int value)
		{
			bonus_expire = value;
		}

		//public void setProxy(boolean value)
		//{
		//	proxy = value;
		//}

		public Status setState(State value)
		{
			state = value;
			return this;
		}
	}

	public static void load() throws GeneralSecurityException
	{
		if(_instance == null)
			_instance = new LoginController();
		else
			throw new IllegalStateException("LoginController can only be loaded a single time.");
	}

	public static LoginController getInstance()
	{
		return _instance;
	}

	private LoginController() throws GeneralSecurityException
	{
		_log.info("Loading LoginController...");

		try
		{
			DEFAULT_CRYPT = (Crypt) Class.forName("l2d.auth.crypt." + Config.DEFAULT_PASSWORD_ENCODING).getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
			ArrayList<Crypt> legacy = new ArrayList<Crypt>();
			for(String method : Config.LEGACY_PASSWORD_ENCODING.split(";"))
				if(!method.equalsIgnoreCase(Config.DEFAULT_PASSWORD_ENCODING))
					legacy.add((Crypt) Class.forName("l2d.auth.crypt." + method).getMethod("getInstance", new Class[0]).invoke(null, new Object[0]));
			LEGACY_CRYPT = legacy.toArray(new Crypt[legacy.size()]);
		}
		catch(ClassNotFoundException e)
		{
			_log.info("Unable to load password crypt, method not found, check config!");
			e.printStackTrace();
		}
		catch(Exception e)
		{
			_log.info("Unable to load password crypt!");
			e.printStackTrace();
		}

		_log.info("Loaded " + DEFAULT_CRYPT.getClass().getSimpleName() + " as default crypt.");

		_hackProtection = new FastMap<InetAddress, FailedLoginAttempt>();

		_keyPairs = new ScrambledKeyPair[Config.LOGIN_RSA_KEYPAIRS];

		KeyPairGenerator keygen;

		keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		//generate the initial set of keys
		for(int i = 0; i < _keyPairs.length; i++)
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		_log.info("Cached " + _keyPairs.length + " KeyPairs for RSA communication");

		testCipher((RSAPrivateKey) _keyPairs[0]._pair.getPrivate());

		// Store keys for blowfish communication
		generateBlowFishKeys();
	}

	/**
	 * This is mostly to force the initialization of the Crypto Implementation, avoiding it being done on runtime when its first needed.<BR>
	 * In short it avoids the worst-case execution time on runtime by doing it on loading.
	 * @param key Any private RSA Key just for testing purposes.
	 * @throws GeneralSecurityException if a underlying exception was thrown by the Cipher
	 */
	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// avoid worst-case execution, KenM
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[Config.LOGIN_BLOWFISH_KEYS][16];

		for(int i = 0; i < _blowfishKeys.length; i++)
			for(int j = 0; j < _blowfishKeys[i].length; j++)
				_blowfishKeys[i][j] = (byte) (Rnd.get(255) + 1);
		_log.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication");
	}

	/**
	 * @return Returns a random key
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[Rnd.get(_blowfishKeys.length)];
	}

	public void addLoginClient(L2LoginClient client)
	{
		synchronized (_clients)
		{
			_clients.add(client);
		}
	}

	public void removeLoginClient(L2LoginClient client)
	{
		synchronized (_clients)
		{
			_clients.remove(client);
		}
	}

	public SessionKey assignSessionKeyToClient()
	{
		SessionKey key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
		return key;
	}

	public void addAuthedLoginClient(String account, L2LoginClient client)
	{
		synchronized (_loginServerClients)
		{
			_loginServerClients.put(account, client);
		}
	}

	public L2LoginClient removeAuthedLoginClient(String account)
	{
		synchronized (_loginServerClients)
		{
			return _loginServerClients.remove(account);
		}
	}

	public boolean isAccountInLoginServer(String account)
	{
		synchronized (_loginServerClients)
		{
			return _loginServerClients.containsKey(account);
		}
	}

	public L2LoginClient getAuthedClient(String account)
	{
		synchronized (_loginServerClients)
		{
			return _loginServerClients.get(account);
		}
	}

	public Status tryAuthLogin(String account, String password, L2LoginClient client)
	{
		Status ret = new Status().setState(State.WRONG);

		ret = loginValid(account, password, client);
		if(ret.state != State.VALID)
			return ret;

		if(!isAccountInLoginServer(account) && !isAccountInAnyGameServer(account))
		{
			// dont allow 2 simultaneous login
			synchronized (_loginServerClients)
			{
				if(!_loginServerClients.containsKey(account))
					addAuthedLoginClient(account, client);
				else
					ret.state = State.IN_USE;
			}

			// was login successful?
			if(ret.state == State.VALID)
				// remove him from the non-authed list
				removeLoginClient(client);
		}
		else
			ret.state = State.IN_USE;
		return ret;
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 *
	 * @param address The Address to be banned.
	 * @param expiration Timestamp in miliseconds when this ban expires
	 * @throws UnknownHostException if the address is invalid.
	 */
	public void addBanForAddress(String address, long expiration) throws UnknownHostException
	{
		InetAddress netAddress = InetAddress.getByName(address);
		_bannedIps.put(netAddress, new BanInfo(netAddress, expiration));
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 *
	 * @param address The Address to be banned.
	 * @param duration is miliseconds
	 */
	public void addBanForAddress(InetAddress address, long duration)
	{
		_bannedIps.put(address, new BanInfo(address, System.currentTimeMillis() + duration));
	}

	public boolean isBannedAddress(InetAddress address)
	{
		BanInfo bi = _bannedIps.get(address);
		if(bi != null)
		{
			if(bi.hasExpired())
			{
				_bannedIps.remove(address);
				return false;
			}
			return true;
		}
		return false;
	}

	public Map<InetAddress, BanInfo> getBannedIps()
	{
		return _bannedIps;
	}

	/**
	 * Remove the specified address from the ban list
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip
	 */
	public boolean removeBanForAddress(InetAddress address)
	{
		return _bannedIps.remove(address) != null;
	}

	/**
	 * Remove the specified address from the ban list
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip or the address was invalid.
	 */
	public boolean removeBanForAddress(String address)
	{
		try
		{
			return this.removeBanForAddress(InetAddress.getByName(address));
		}
		catch(UnknownHostException e)
		{
			return false;
		}
	}

	public SessionKey getKeyForAccount(String account)
	{
		L2LoginClient client;
		synchronized (_loginServerClients)
		{
			client = _loginServerClients.get(account);
		}
		if(client != null)
			return client.getSessionKey();
		return null;
	}

	public int getOnlinePlayerCount(int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		if(gsi != null && gsi.isAuthed())
			return gsi.getCurrentPlayerCount();
		return 0;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for(GameServerInfo gsi : serverList)
		{
			AttGS gst = gsi.getGameServer();
			if(gst != null && gst.isAccountInGameServer(account))
				return true;
		}
		return false;
	}

	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for(GameServerInfo gsi : serverList)
		{
			AttGS gst = gsi.getGameServer();
			if(gst != null && gst.isAccountInGameServer(account))
				return gsi;
		}
		return null;
	}

	public int getTotalOnlinePlayerCount()
	{
		int total = 0;
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for(GameServerInfo gsi : serverList)
			if(gsi.isAuthed())
				total += gsi.getCurrentPlayerCount();
		return total;
	}

	public int getMaxAllowedOnlinePlayers(int id)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(id);
		if(gsi != null)
			return gsi.getMaxPlayers();
		return 0;
	}

	public boolean isLoginPossible(L2LoginClient client, int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		int access = client.getAccessLevel();
		boolean loginOk = gsi != null && gsi.isAuthed() && (gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() || access >= 50);
		if(loginOk && client.getLastServer() != serverId)
			mysql.set("UPDATE accounts SET lastServer = " + serverId + " WHERE login = '" + client.getAccount() + "'");
		return loginOk;
	}

	public void setAccountAccessLevel(String user, int banLevel, String comments, int banTime)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			String stmt = "UPDATE accounts SET access_level = ?, comments = ?, banExpires = ? WHERE login=?";
			statement = con.prepareStatement(stmt);
			statement.setInt(1, banLevel);
			statement.setString(2, comments);
			statement.setInt(3, banTime);
			statement.setString(4, user);
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warning("Could not set accessLevel: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public boolean isGM(String user)
	{
		boolean ok = false;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if(rset.next())
			{
				int accessLevel = rset.getInt(1);
				if(accessLevel >= 100)
					ok = true;
			}
		}
		catch(Exception e)
		{
			//_log.warning("could not check gm state:"+e);
			ok = false;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		return ok;
	}

	/**
	 * <p>This method returns one of the cached {@link ScrambledKeyPair ScrambledKeyPairs} for communication with Login Clients.</p>
	 * @return a scrambled keypair
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.get(_keyPairs.length)];
	}

	public static final String[] account_field_columns = { "pay_stat", "access_level", "bonus", "bonus_expire", "lastServer", };

	public Status loginValid(String user, String password, L2LoginClient client)// throws HackingException
	{
		Status ok = new Status().setState(State.WRONG);
		InetAddress address = client.getConnection().getSocket().getInetAddress();
		Log.add("'" + (user == null ? "null" : user) + "' " + (address == null ? "null" : address.getHostAddress()), "logins_ip");

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			int lastServer = 1;
			boolean paid = false;
			boolean banned = false;
			String phash = "";

			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT password,pay_stat,access_level,bonus,bonus_expire,banExpires,lastServer,AllowIPs FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if(rset.next())
			{
				client.account_fields = new StatsSet(rset, account_field_columns);
				String allowedIps = rset.getString("AllowIPs");
				if(allowedIps != null && !allowedIps.isEmpty() && !allowedIps.equals("*"))
				{
					NetList allowedList = new NetList();
					allowedList.LoadFromString(allowedIps, ",");
					if(!allowedList.isIpInNets(client.getIpAddress()))
						return new Status().setState(State.IP_ACCESS_DENIED);
				}
				phash = rset.getString("password");
				if(phash.equals(""))
					return new Status().setState(State.WRONG);
				paid = rset.getInt(2) == 1;
				banned = rset.getInt(3) < 0;
				long banTime = rset.getLong("banExpires");
				int bonusTime = rset.getInt("bonus_expire");
				ok.setBonus(bonusTime > System.currentTimeMillis() / 1000 || bonusTime < 0 ? rset.getFloat("bonus") : 1);
				if(ok.bonus > 1)
					ok.setBonusExpire(bonusTime);
				//ok.setProxy(rset.getInt("proxy") == 1);
				if(banTime == -1)
					banned = true;
				else if(banTime > 0)
					if(banTime < System.currentTimeMillis() / 1000)
						unBanAcc(user);
					else
						banned = true;
				lastServer = Math.max(rset.getInt("lastServer"), 1);
				if(Config.LOGIN_DEBUG)
					_log.fine("account exists");
			}
			DatabaseUtils.closeDatabaseSR(statement, rset);
			if(phash.equals(""))
			{
				if(Config.AUTO_CREATE_ACCOUNTS)
				{
					if(user != null && user.length() >= 2 && user.length() <= 14)
					{
						statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level,lastIP,comments) values(?,?,?,?,?,?)");
						statement.setString(1, user);
						statement.setString(2, DEFAULT_CRYPT.encrypt(password));
						statement.setLong(3, System.currentTimeMillis() / 1000);
						statement.setInt(4, 0);
						statement.setString(5, address != null ? address.getHostAddress() : "");
						statement.setString(6, "");
						statement.execute();
						DatabaseUtils.closeStatement(statement);
						if(Config.LOGIN_DEBUG)
							_log.fine("created new account for " + user);
						return new Status().setState(State.VALID);

					}
					if(Config.LOGIN_DEBUG)
						_log.fine("Invalid username creation/use attempt: " + user);
					return new Status().setState(State.WRONG);
				}
				if(Config.LOGIN_DEBUG)
					_log.fine("account missing for user " + user);
				return new Status().setState(State.WRONG);
			}

			ok.setState(State.VALID);

			// проверяем не зашифрован ли пароль одним из устаревших но поддерживаемых алгоритмов
			boolean oldcrypt = false;
			for(Crypt c : LEGACY_CRYPT)
				if(c.compare(password, phash)) // если да то заменяем на стандартный
				{
					statement = con.prepareStatement("UPDATE accounts SET password=? WHERE login=?");
					statement.setString(1, DEFAULT_CRYPT.encrypt(password));
					statement.setString(2, user);
					statement.execute();
					DatabaseUtils.closeStatement(statement);
					oldcrypt = true;
					break;
				}

			// если старые алгоритмы не подошли проверяем стандартным
			if(!oldcrypt && !DEFAULT_CRYPT.compare(password, phash))
				return new Status().setState(State.WRONG);

			if(!paid)
				return new Status().setState(State.NOT_PAID);
			if(banned)
				return new Status().setState(State.BANNED);
			if(ok.state == State.VALID)
			{
				statement = con.prepareStatement("UPDATE accounts SET lastactive=?, lastIP=? WHERE login=?");
				statement.setLong(1, System.currentTimeMillis() / 1000);
				statement.setString(2, address != null ? address.getHostAddress() : "");
				statement.setString(3, user);
				statement.execute();
				client.setLastServer(lastServer);
			}
		}
		catch(Exception e)
		{
			_log.warning("Could not check password:" + e);
			e.printStackTrace();
			ok.setState(State.WRONG);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		if(ok.state != State.VALID)
		{
			Log.add("'" + user + "':'" + password + "' " + (address != null ? address.getHostAddress() : ""), "logins_ip_fails");

			if(address != null)
			{
				FailedLoginAttempt failedAttempt = _hackProtection.get(address);
				int failedCount;
				if(failedAttempt == null)
				{
					_hackProtection.put(address, new FailedLoginAttempt(address, password));
					failedCount = 1;
				}
				else
				{
					failedAttempt.increaseCounter(password);
					failedCount = failedAttempt.getCount();
				}

				if(failedCount >= Config.LOGIN_TRY_BEFORE_BAN)
					this.addBanForAddress(address, Config.LOGIN_TRY_BEFORE_BAN_TIME * 60 * 1000);
			}
		}
		else
		{
			if(address != null)
				_hackProtection.remove(address);
			Log.add("'" + user + "' " + (address != null ? address.getHostAddress() : ""), "logins_ip");
		}

		return ok;
	}

	public void unBanAcc(String name)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET access_level = ?, banExpires = ? WHERE login = ?");
			statement.setInt(1, 0);
			statement.setInt(2, 0);
			statement.setString(3, name);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warning("Cant unban acc " + name + ", " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public boolean loginBanned(String user)
	{
		boolean ok = false;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if(rset.next())
			{
				int accessLevel = rset.getInt(1);
				if(accessLevel < 0)
					ok = true;
			}
		}
		catch(Exception e)
		{
			// digest algo not found ??
			// out of bounds should not be possible
			_log.warning("could not check ban state:" + e);
			ok = false;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		return ok;
	}

	class FailedLoginAttempt
	{
		//private InetAddress _ipAddress;
		private int _count;
		private long _lastAttempTime;
		private String _lastPassword;

		public FailedLoginAttempt(InetAddress address, String lastPassword)
		{
			//_ipAddress = address;
			_count = 1;
			_lastAttempTime = System.currentTimeMillis();
			_lastPassword = lastPassword;
		}

		public void increaseCounter(String password)
		{
			if(!_lastPassword.equals(password))
			{
				// check if theres a long time since last wrong try
				if(System.currentTimeMillis() - _lastAttempTime < 300 * 1000)
					_count++;
				else
					// restart the status
					_count = 1;
				_lastPassword = password;
				_lastAttempTime = System.currentTimeMillis();
			}
			else
				_lastAttempTime = System.currentTimeMillis();
		}

		public int getCount()
		{
			return _count;
		}
	}

	public class BanInfo
	{
		private InetAddress _ipAddress;
		// Expiration
		private long _expiration;

		public BanInfo(InetAddress ipAddress, long expiration)
		{
			_ipAddress = ipAddress;
			_expiration = expiration;
		}

		public InetAddress getAddress()
		{
			return _ipAddress;
		}

		public boolean hasExpired()
		{
			return System.currentTimeMillis() > _expiration;
		}
	}

	class PurgeThread extends Thread
	{
		@Override
		public void run()
		{
			while(true)
			{
				synchronized (_clients)
				{
					for(Record e = _clients.head(), end = _clients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = _clients.valueOf(e);
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
					}
				}

				synchronized (_loginServerClients)
				{
					for(FastMap.Entry<String, L2LoginClient> e = _loginServerClients.head(), end = _loginServerClients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = e.getValue();
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
					}
				}

				try
				{
					Thread.sleep(2 * LOGIN_TIMEOUT);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public boolean ipBlocked(String ipAddress)
	{
		int tries = 0;
		InetAddress ia;
		try
		{
			ia = InetAddress.getByName(ipAddress);
		}
		catch(UnknownHostException e)
		{
			return false;
		}

		if(_hackProtection.containsKey(ia))
			tries = _hackProtection.get(ia).getCount();

		if(tries > Config.LOGIN_TRY_BEFORE_BAN)
		{
			_hackProtection.remove(ia);
			_log.warning("Removed host from hacklist! IP number: " + ipAddress);
			return true;
		}
		return false;
	}

	public boolean setPassword(String account, String password)
	{
		boolean updated = true;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			statement = con.prepareStatement("UPDATE accounts SET password=? WHERE login=?");
			statement.setString(1, Base64.encodeBytes(hash));
			statement.setString(2, account);
			statement.execute();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			updated = false;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		return updated;
	}
}