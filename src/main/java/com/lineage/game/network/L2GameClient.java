package com.lineage.game.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.network.MMOClient;
import com.lineage.ext.network.MMOConnection;
import com.lineage.game.loginservercon.LSConnection;
import com.lineage.game.loginservercon.SessionKey;
import com.lineage.game.loginservercon.gspackets.PlayerLogout;
import com.lineage.game.model.CharSelectInfoPackage;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.L2GameServerPacket;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;
import com.lineage.game.templates.StatsSet;

/**
 * Represents a client connected on Game Server
 */
public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>>
{
	protected static Logger _log = Logger.getLogger(L2GameClient.class.getName());

	public static int devCharId;
	public GameCrypt _crypt;
	private float _bonus = 1;
	private long _bonus_expire = 0;
	public GameClientState _state;
	private int _upTryes = 0, _upTryesTotal = 0;
	private long _upTryesRefresh = 0;

	public static enum GameClientState
	{
		CONNECTED,
		AUTHED,
		IN_GAME
	}

	private String _loginName;
	private L2Player _activeChar;
	private SessionKey _sessionId;
	private final MMOConnection<L2GameClient> _connection;

	//private byte[] _filter;

	private int revision = 0;
	private boolean _gameGuardOk = false;

	public boolean protect_used = false;
	public byte client_lang = -1;
	public String HWID = "";

	private ArrayList<Integer> _charSlotMapping = new ArrayList<Integer>();
	private PacketLogger pktLogger = null;
	private boolean pktLoggerMatch = false;
	public StatsSet account_fields = null;

	public L2GameClient(MMOConnection<L2GameClient> con, boolean offline)
	{
		super(con);
		_state = offline ? GameClientState.IN_GAME : GameClientState.CONNECTED;
		_connection = offline ? null : con;
		_sessionId = new SessionKey(-1, -1, -1, -1);
		_crypt = new GameCrypt();

		protect_used = Config.PROTECT_ENABLE;
		if(protect_used)
			protect_used = !Config.PROTECT_UNPROTECTED_IPS.isIpInNets(getIpAddr());

		if((Config.LOG_CLIENT_PACKETS || Config.LOG_SERVER_PACKETS) && !offline)
		{
			pktLogger = new PacketLogger(this, Config.PACKETLOGGER_FLUSH_SIZE);
			if(Config.PACKETLOGGER_IPS != null)
				if(Config.PACKETLOGGER_IPS.isIpInNets(getIpAddr()))
					pktLoggerMatch = true;
		}
	}

	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		this(con, false);
	}

	public void disconnectOffline()
	{
		onDisconnection();
	}

	@Override
	protected void onDisconnection()
	{
		if(pktLogger != null)
		{
			if(!pktLogger.assigned() && pktLoggerMatch)
				pktLogger.assign();
			pktLogger.close();
			pktLogger = null;
		}

		if(getLoginName() == null || getLoginName().equals("") || _state != GameClientState.IN_GAME && _state != GameClientState.AUTHED)
			return;
		try
		{
			if(_activeChar != null && _activeChar.isInOfflineMode())
				//LSConnection.getInstance().sendPacket(new PlayerLogout(getLoginName()));
				return;

			LSConnection.getInstance().removeAccount(this);
			L2Player player = _activeChar;
			_activeChar = null;

			if(player != null && !player.isLogoutStarted()) // this should only happen on connection loss
			{
				player.setLogoutStarted(true);
				player.prepareToLogout(false);
				saveCharToDisk(player);
				player.deleteMe();
				if(player.getNetConnection() != null)
				{
					player.getNetConnection().closeNow(false);
					player.setNetConnection(null);
				}
				player.setConnected(false);
				_activeChar = null;
			}

			setConnection(null);
		}
		catch(Exception e1)
		{
			_log.log(Level.WARNING, "error while disconnecting client", e1);
		}
		finally
		{
			LSConnection.getInstance().sendPacket(new PlayerLogout(getLoginName()));
		}
	}

	public static void saveCharToDisk(L2Player cha)
	{
		try
		{
			cha.getInventory().updateDatabase(true);
			cha.store(false);
		}
		catch(Exception e)
		{
			_log.warning("Error saving player character: " + e);
			e.printStackTrace();
		}
	}

	public void deleteFromClan(L2Player cha)
	{
		L2Clan clan = cha.getClan();
		if(clan != null)
			clan.removeClanMember(cha.getObjectId());
	}

	public static void deleteFromClan(int charId, int clanId)
	{
		if(clanId == 0)
			return;
		L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if(clan != null)
			clan.removeClanMember(charId);
	}

	public void markRestoredChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		if(getActiveChar() != null)
		{
			saveCharToDisk(getActiveChar());
			if(Config.DEBUG)
				_log.fine("active Char saved");
			_activeChar = null;
		}

		int objid = getObjectIdForSlot(charslot);
		if(objid < 0)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "data error on restore char:", e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void markToDeleteChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		if(getActiveChar() != null)
		{
			saveCharToDisk(getActiveChar());
			if(Config.DEBUG)
				_log.fine("active Char saved");
			_activeChar = null;
		}

		int objid = getObjectIdForSlot(charslot);
		if(objid < 0)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_id=?");
			statement.setLong(1, (int) (System.currentTimeMillis() / 1000));
			statement.setInt(2, objid);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "data error on update deletime char:", e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void deleteChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		if(getActiveChar() != null)
		{
			saveCharToDisk(getActiveChar());
			if(Config.DEBUG)
				_log.fine("active Char saved");
			_activeChar = null;
		}

		int objid = getObjectIdForSlot(charslot);
		if(objid == -1)
			return;

		deleteCharByObjId(objid);
	}

	public static void deleteCharByObjId(int objid)
	{
		if(objid < 0)
			return;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE pets FROM pets, items WHERE pets.item_obj_id=items.object_id AND items.owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_effects_save WHERE char_obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? or friend_id = ?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DatabaseUtils.closeStatement(statement);
			statement = null;
		}
		catch(Exception e)
		{
			_log.warning("data error on delete char:" + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public L2Player loadCharFromDisk(int charslot)
	{
		Integer objectId = getObjectIdForSlot(charslot);
		if(objectId == -1)
			return null;

		L2Object object = L2World.findObject(objectId);
		if(object != null)
			if(object.isPlayer())
			{
				L2Player player = (L2Player) object;
				//_log.warning(player.toFullString() + " tried to make a clone.");
				if(!player.isInOfflineMode())
				{
					player.sendPacket(new SystemMessage(SystemMessage.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT));
					LSConnection.getInstance().sendPacket(new PlayerLogout(getLoginName()));
					player.logout(false, false, true);
				}
				else
				{
					player.setOfflineMode(false);
					//player.logout(false, false, true);
					if(player.getNetConnection() != null)
						player.getNetConnection().onDisconnection();
					else
					{
						L2GameClient.saveCharToDisk(player);
						player.deleteMe();
					}
				}
			}

		L2Player character = L2Player.load(objectId);
		if(character != null)
		{
			// preinit some values for each login
			character.setRunning(); // running is default
			character.standUp(); // standing is default

			character.updateStats();
			character.setOnlineStatus(true);
			setActiveChar(character);
			character.restoreBonus();
			character.setVar("lang@", "en");

			if(protect_used && Config.PROTECT_GS_STORE_HWID && !HWID.equals(""))
				character.storeHWID(HWID);

			if(pktLogger != null)
				if(!pktLogger.assigned())
				{
					if(!pktLoggerMatch)
						if(Config.PACKETLOGGER_CHARACTERS != null)
						{
							String char_name = character.getName();
							for(int i = 0; i < Config.PACKETLOGGER_CHARACTERS.size(); i++)
							{
								String s_mask = Config.PACKETLOGGER_CHARACTERS.get(i);
								if(char_name.matches(s_mask))
								{
									pktLoggerMatch = true;
									break;
								}
							}
						}
					if(pktLoggerMatch)
						pktLogger.assign();
					else
						pktLogger = null;
				}
		}
		else
			_log.warning("could not restore obj_id: " + objectId + " in slot:" + charslot);

		return character;
	}

	public int getObjectIdForSlot(int charslot)
	{
		if(charslot < 0 || charslot >= _charSlotMapping.size())
		{
			_log.warning(getLoginName() + " tried to modify Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}
		return _charSlotMapping.get(charslot);
	}

	@Override
	public MMOConnection<L2GameClient> getConnection()
	{
		return _connection;
	}

	public L2Player getActiveChar()
	{
		return _activeChar;
	}

	/**
	 * @return Returns the sessionId.
	 */
	public SessionKey getSessionId()
	{
		return _sessionId;
	}

	public String getLoginName()
	{
		return _loginName;
	}

	private void logHWID()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(Config.PROTECT_GS_LOG_HWID_QUERY);
			statement.setString(1, _loginName);
			statement.setString(2, getIpAddr());
			statement.setString(3, HWID);
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warning("could not log HWID:" + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void setLoginName(String loginName)
	{
		_loginName = loginName;
		if(protect_used && Config.PROTECT_GS_LOG_HWID && getIpAddr() != "Disconnected")
			logHWID();

		if(pktLogger != null && !pktLoggerMatch && Config.PACKETLOGGER_ACCOUNTS != null)
			for(int i = 0; i < Config.PACKETLOGGER_ACCOUNTS.size(); i++)
			{
				String s_mask = Config.PACKETLOGGER_ACCOUNTS.get(i);
				if(loginName.matches(s_mask))
				{
					pktLoggerMatch = true;
					break;
				}
			}
	}

	public void setActiveChar(L2Player cha)
	{
		_activeChar = cha;
		if(cha != null)
			// we store the connection in the player object so that external
			// events can directly send events to the players client
			// might be changed later to use a central event management and distribution system
			_activeChar.setNetConnection(this);
	}

	public void setSessionId(SessionKey sessionKey)
	{
		_sessionId = sessionKey;
	}

	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping.clear();

		for(CharSelectInfoPackage element : chars)
		{
			int objectId = element.getObjectId();
			_charSlotMapping.add(objectId);
		}
	}

	public void setCharSelection(int c)
	{
		_charSlotMapping.clear();
		_charSlotMapping.add(c);
	}

	/**
	 * @return Returns the revision.
	 */
	public int getRevision()
	{
		return revision;
	}

	/**
	 * @param revision The revision to set.
	 */
	public void setRevision(int revision)
	{
		this.revision = revision;
	}

	public void setGameGuardOk(boolean gameGuardOk)
	{
		_gameGuardOk = gameGuardOk;
	}

	public boolean isGameGuardOk()
	{
		return _gameGuardOk;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		if(pktLogger != null && Config.LOG_SERVER_PACKETS)
			pktLogger.log_packet((byte) 1, buf, size);
		_crypt.encrypt(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
		return true;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		_crypt.decrypt(buf.array(), buf.position(), size);
		if(pktLogger != null && Config.LOG_CLIENT_PACKETS)
			pktLogger.log_packet((byte) 0, buf, size);
		return true;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		if(getConnection() == null)
			return;
		getConnection().sendPacket(gsp);
	}

	public void close(L2GameServerPacket gsp)
	{
		getConnection().close(gsp);
	}

	public String getIpAddr()
	{
		try
		{
			return _connection.getSocket().getInetAddress().getHostAddress();
		}
		catch(NullPointerException e)
		{
			return "Disconnected";
		}
	}

	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
		_crypt.setKey(key, protect_used);
		return key;
	}

	public float getBonus()
	{
		return _bonus;
	}

	public void setBonus(float bonus)
	{
		_bonus = bonus;
	}

	/**
	 * @return время окончания бонуса в unixtime
	 */
	public long getBonusExpire()
	{
		return _bonus_expire;
	}

	public void setBonusExpire(long time)
	{
		if(time < 0)
			return;
		if(time < System.currentTimeMillis() / 1000)
		{
			_bonus = 1;
			return;
		}
		_bonus_expire = time;
	}

	public GameClientState getState()
	{
		return _state;
	}

	public void setState(GameClientState state)
	{
		_state = state;
	}

	/**
	 * @return произведено ли отключение игрока
	 */
	public boolean onClientPacketFail()
	{
		if(isPacketsFailed())
			return true;

		if(_upTryesRefresh == 0)
			_upTryesRefresh = System.currentTimeMillis() + 5000;
		else if(_upTryesRefresh < System.currentTimeMillis())
		{
			_upTryesRefresh = System.currentTimeMillis() + 5000;
			_upTryes = 0;
		}

		_upTryes++;
		_upTryesTotal++;

		if(_upTryes > 4 || _upTryesTotal > 10)
		{
			_log.warning("Too many client packet fails, connection closed. IP: " + getIpAddr() + ", account:" + getLoginName());
			L2Player activeChar = getActiveChar();
			if(activeChar != null)
				activeChar.logout(false, false, true);
			else
				closeNow(true);
			_upTryesTotal = Integer.MAX_VALUE;
			return true;
		}

		return false;
	}

	public boolean isPacketsFailed()
	{
		return _upTryesTotal == Integer.MAX_VALUE;
	}
}