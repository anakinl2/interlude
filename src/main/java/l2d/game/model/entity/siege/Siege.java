package l2d.game.model.entity.siege;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.instancemanager.SiegeGuardManager;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.entity.residence.Residence;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.RelationChanged;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ClanTable;
import l2d.game.tables.MapRegion;

public abstract class Siege
{
	private int _defenderRespawnDelay = 20000;
	private int _siegeClanMinLevel = 5;
	private int _siegeLength = 120;
	private int _controlTowerLosePenalty = 20000;

	protected FastMap<Integer, SiegeClan> _attackerClans = new FastMap<Integer, SiegeClan>();
	protected FastMap<Integer, SiegeClan> _defenderClans = new FastMap<Integer, SiegeClan>();
	protected FastMap<Integer, SiegeClan> _defenderWaitingClans = new FastMap<Integer, SiegeClan>();
	protected FastMap<Integer, SiegeClan> _defenderRefusedClans = new FastMap<Integer, SiegeClan>();

	protected Residence _siegeUnit;
	protected SiegeDatabase _database;
	protected SiegeGuardManager _siegeGuardManager;

	protected boolean _isInProgress = false;
	protected boolean _isRegistrationOver = false;

	protected int _ownerBeforeStart;
	protected int _defenderRespawnPenalty;

	protected Calendar _siegeDate;
	protected Calendar _siegeEndDate;
	protected Calendar _siegeRegistrationEndDate;

	protected ScheduledFuture _siegeStartTask;

	public Siege(Residence siegeUnit)
	{
		_siegeUnit = siegeUnit;
		_siegeDate = Calendar.getInstance();
	}

	public L2Zone getZone()
	{
		return ZoneManager.getInstance().getZoneByIndex(ZoneType.Siege, getSiegeUnit().getId(), false);
	}

	public L2Zone getResidenseZone()
	{
		return ZoneManager.getInstance().getZoneByIndex(ZoneType.siege_residense, getSiegeUnit().getId(), false);
	}

	/**
	 * When siege starts<BR><BR>
	 */
	public abstract void startSiege();

	/**
	 * When control of castle changed during siege<BR><BR>
	 */
	public abstract void midVictory();

	/** Display list of registered clans */
	public abstract void listRegisterClan(L2Player player);

	public abstract void endSiege();

	public abstract void Engrave(L2Clan clan, int objId);

	public abstract void startAutoTask();

	protected abstract void setNextSiegeDate();

	protected abstract void correctSiegeDateTime();

	protected abstract void saveSiege();

	public Residence getSiegeUnit()
	{
		return _siegeUnit;
	}

	public SiegeGuardManager getSiegeGuardManager()
	{
		return _siegeGuardManager;
	}

	public SiegeDatabase getDatabase()
	{
		return _database;
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y, boolean onlyActive)
	{
		return (isInProgress() || !onlyActive) && (getSiegeUnit().checkIfInZone(x, y) || getZone().checkIfInZone(x, y));
	}

	/**
	 * Announce to player.<BR><BR>
	 * 
	 * @param message
	 *            The String of the message to send to player
	 * @param inAreaOnly
	 *            The boolean flag to show message to players in area only.
	 */
	public static void announceToPlayer(SystemMessage message, boolean inAreaOnly)
	{
		for(L2Player player : L2World.getAllPlayers())
			if(player != null && (!inAreaOnly || player.isInZone(ZoneType.Siege)))
				player.sendPacket(message);
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		L2Clan clan;
		for(SiegeClan siegeClan : getAttackerClans().values())
		{
			clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			for(L2Player member : clan.getOnlineMembers(0))
			{
				if(clear)
					member.setSiegeState(0);
				else
					member.setSiegeState(1);

				for(L2Player pl : L2World.getAroundPlayers(member))
					pl.sendPacket(new RelationChanged(member, member.isAutoAttackable(pl), member.getRelation(pl)));
				member.sendUserInfo(false);
			}
		}
		for(SiegeClan siegeclan : getDefenderClans().values())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for(L2Player member : clan.getOnlineMembers(0))
			{
				if(clear)
					member.setSiegeState(0);
				else
					member.setSiegeState(2);

				for(L2Player pl : L2World.getAroundPlayers(member))
					pl.sendPacket(new RelationChanged(member, member.isAutoAttackable(pl), member.getRelation(pl)));
				member.sendUserInfo(false);
			}
		}
	}

	/** Return list of L2Player in the zone. */
	public List<L2Player> getPlayersInZone()
	{
		List<L2Player> players = new FastList<L2Player>();
		for(L2Object object : getZone().getObjects())
			if(object.isPlayer())
				players.add((L2Player) object);
		return players;
	}

	/**
	 * Teleport players
	 */
	public void teleportPlayer(TeleportWhoType teleportWho, MapRegion.TeleportWhereType teleportWhere)
	{
		List<L2Player> players = new FastList<L2Player>();
		int ownerId = getSiegeUnit().getOwnerId();
		switch(teleportWho)
		{
			case Owner:
				if(ownerId > 0)
					for(L2Player player : getPlayersInZone())
						if(player.getClan() != null && player.getClan().getClanId() == ownerId)
							players.add(player);
				break;
			case Attacker:
				for(L2Player player : getPlayersInZone())
					if(player.getClan() != null && getAttackerClan(player.getClan()) != null)
						players.add(player);
				break;
			case Defender:
				for(L2Player player : getPlayersInZone())
					if(player.getClan() != null && player.getClan().getClanId() != ownerId && getDefenderClan(player.getClan()) != null)
						players.add(player);
				break;
			case Spectator:
				for(L2Player player : getPlayersInZone())
					if(player.getClan() == null || getAttackerClan(player.getClan()) == null && getDefenderClan(player.getClan()) == null)
						players.add(player);
				break;
			default:
				players = getPlayersInZone();
		}
		for(L2Player player : players)
			if(player != null && !player.isGM())
			{
				if(player.getCastingSkill() != null && player.getCastingSkill().getId() == 246)
					player.abortCast();
				if(teleportWho == TeleportWhoType.Defender && teleportWhere == MapRegion.TeleportWhereType.Castle)
				{
					player.teleToLocation(getSiegeUnit().getZone().getSpawn());
					continue;
				}
				player.teleToLocation(MapRegion.getTeleTo(player, teleportWhere));
			}
	}

	/**
	 * Set siege date time<BR><BR>
	 * 
	 * @param siegeDateTime
	 *            The long of date time in millisecond
	 */
	public void setSiegeDateTime(long siegeDateTime)
	{
		_siegeDate.setTimeInMillis(siegeDateTime); // Set siege date
	}

	/**
	 * Return true if the player can register.<BR><BR>
	 * 
	 * @param player
	 *            The L2Player of the player trying to register
	 * @return true if the player can register.
	 */
	private boolean checkIfCanRegister(L2Player player)
	{
		if(isRegistrationOver())
			player.sendMessage(new CustomMessage("l2d.game.model.entity.siege.Siege.DeadlinePassed", player).addString(getSiegeUnit().getName()));
		else if(isInProgress())
			player.sendMessage(new CustomMessage("l2d.game.model.entity.siege.Siege.NotTimeToCancel", player));
		else if(player.getClan() == null || player.getClan().getLevel() < getSiegeClanMinLevel())
			player.sendMessage(new CustomMessage("l2d.game.model.entity.siege.Siege.ClanLevelToSmall", player).addNumber(getSiegeClanMinLevel()));
		else if(player.getClan().getHasCastle() > 0)
			player.sendPacket(new SystemMessage(SystemMessage.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE));
		else if(player.getClan().getClanId() == getSiegeUnit().getOwnerId())
			player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_THAT_OWNS_THE_CASTLE_IS_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE));
		else if(SiegeDatabase.checkIsRegistered(player.getClan(), getSiegeUnit().getId()))
			player.sendMessage(new CustomMessage("l2d.game.model.entity.siege.Siege.AlreadyRegistered", player));
		else
			return true;
		return false;
	}

	/**
	 * Register clan as attacker<BR><BR>
	 * 
	 * @param player
	 *            The L2Player of the player trying to register
	 */
	public void registerAttacker(L2Player player)
	{
		registerAttacker(player, false);
	}

	public void registerAttacker(L2Player player, boolean force)
	{
		if(player.getClan() == null)
			return;
		int allyId = 0;
		if(getSiegeUnit().getOwnerId() != 0)
		{
			L2Clan castleClan = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId());
			if(castleClan != null)
				allyId = castleClan.getAllyId();
		}
		if(allyId != 0)
			if(player.getClan().getAllyId() == allyId && !force)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_REGISTER_ON_THE_ATTACKING_SIDE_BECAUSE_YOU_ARE_PART_OF_AN_ALLIANCE_WITH_THE_CLAN_THAT_OWNS_THE_CASTLE));
				return;
			}
		if(force || checkIfCanRegister(player))
			_database.saveSiegeClan(player.getClan(), 1, false); // Save to database
	}

	/**
	 * Register clan as defender<BR><BR>
	 * 
	 * @param player
	 *            The L2Player of the player trying to register
	 */
	public void registerDefender(L2Player player)
	{
		registerDefender(player, false);
	}

	public void registerDefender(L2Player player, boolean force)
	{
		if(getSiegeUnit().getOwnerId() <= 0)
			player.sendMessage(new CustomMessage("l2d.game.model.entity.siege.Siege.OwnedByNPC", player).addString(getSiegeUnit().getName()));
		else if(force || checkIfCanRegister(player))
			_database.saveSiegeClan(player.getClan(), 2, false); // Save to database
	}

	protected void addDefender(SiegeClan sc, SiegeClanType type)
	{
		if(sc == null)
			return;
		sc.setTypeId(type);
		if(type == SiegeClanType.DEFENDER_PENDING)
			_defenderWaitingClans.put(sc.getClanId(), sc);
		else if(type == SiegeClanType.DEFENDER_REFUSED)
			_defenderRefusedClans.put(sc.getClanId(), sc);
		else
			_defenderClans.put(sc.getClanId(), sc);
	}

	protected void addAttacker(SiegeClan sc)
	{
		if(sc == null)
			return;
		sc.setTypeId(SiegeClanType.ATTACKER);
		_attackerClans.put(sc.getClanId(), sc);
	}

	/**
	 * Add clan as attacker<BR><BR>
	 * 
	 * @param clanId
	 *            The int of clan's id
	 */
	public void addAttacker(int clanId)
	{
		addAttacker(new SiegeClan(clanId, SiegeClanType.ATTACKER));
	}

	/**
	 * <p>Add clan as defender with the specified type</p>
	 * 
	 * @param clanId
	 *            The int of clan's id
	 * @param type
	 *            the type of the clan
	 */
	public void addDefender(int clanId, SiegeClanType type)
	{
		addDefender(new SiegeClan(clanId, type), type);
	}

	/**
	 * Add clan as defender waiting approval<BR><BR>
	 * 
	 * @param clanId
	 *            The int of clan's id
	 */
	public void addDefenderWaiting(int clanId)
	{
		addDefender(new SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING), SiegeClanType.DEFENDER_PENDING);
	}

	public void addDefenderRefused(int clanId)
	{
		addDefender(new SiegeClan(clanId, SiegeClanType.DEFENDER_REFUSED), SiegeClanType.DEFENDER_REFUSED);
	}

	protected void removeDefender(SiegeClan sc)
	{
		if(sc != null)
			getDefenderClans().remove(sc.getClanId());
	}

	protected void removeAttacker(SiegeClan sc)
	{
		if(sc != null)
			getAttackerClans().remove(sc.getClanId());
	}

	/**
	 * Remove clan from siege<BR><BR>
	 * 
	 * @param player
	 *            The L2Player of player/clan being removed
	 */
	public void removeSiegeClan(L2Player player)
	{
		removeSiegeClan(player.getClan());
	}

	/**
	 * Remove clan from siege<BR><BR>
	 * 
	 * @param clan
	 *            being removed
	 */
	public void removeSiegeClan(L2Clan clan)
	{
		if(clan == null || clan.getHasCastle() == getSiegeUnit().getId() || !SiegeDatabase.checkIsRegistered(clan, getSiegeUnit().getId()))
			return;
		_database.removeSiegeClan(clan.getClanId());
	}

	public SiegeClan getAttackerClan(L2Clan clan)
	{
		if(clan == null)
			return null;
		return getAttackerClan(clan.getClanId());
	}

	public SiegeClan getAttackerClan(int clanId)
	{
		return _attackerClans.get(clanId);
	}

	public FastMap<Integer, SiegeClan> getAttackerClans()
	{
		return _attackerClans;
	}

	public SiegeClan getDefenderClan(L2Clan clan)
	{
		if(clan == null)
			return null;
		return getDefenderClan(clan.getClanId());
	}

	public SiegeClan getDefenderClan(int clanId)
	{
		return _defenderClans.get(clanId);
	}

	public FastMap<Integer, SiegeClan> getDefenderClans()
	{
		return _defenderClans;
	}

	public SiegeClan getDefenderWaitingClan(L2Clan clan)
	{
		if(clan == null)
			return null;
		return getDefenderWaitingClan(clan.getClanId());
	}

	public SiegeClan getDefenderRefusedClan(L2Clan clan)
	{
		if(clan == null)
			return null;
		return getDefenderRefusedClan(clan.getClanId());
	}

	public SiegeClan getDefenderWaitingClan(int clanId)
	{
		return _defenderWaitingClans.get(clanId);
	}

	public FastMap<Integer, SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}

	public SiegeClan getDefenderRefusedClan(int clanId)
	{
		return _defenderRefusedClans.get(clanId);
	}

	public FastMap<Integer, SiegeClan> getDefenderRefusedClans()
	{
		return _defenderRefusedClans;
	}

	/**
	 * Approve clan as defender for siege<BR><BR>
	 * 
	 * @param clanId
	 *            The int of player's clan id
	 */
	public void approveSiegeDefenderClan(int clanId)
	{
		if(clanId <= 0)
			return;
		_database.saveSiegeClan(ClanTable.getInstance().getClan(clanId), 0, true);
		_database.loadSiegeClan();
	}

	public void refuseSiegeDefenderClan(int clanId)
	{
		if(clanId <= 0)
			return;
		_database.saveSiegeClan(ClanTable.getInstance().getClan(clanId), 3, true);
		_database.loadSiegeClan();
	}

	protected void defendersUpdate(boolean end)
	{
		for(SiegeClan clan : getDefenderClans().values())
			if(end)
			{
				clan.getClan().setSiege(null);
				clan.getClan().setDefender(false);
				clan.getClan().setAttacker(false);
			}
			else
			{
				clan.getClan().setSiege(this);
				clan.getClan().setDefender(true);
				clan.getClan().setAttacker(false);
			}
	}

	protected void attackersUpdate(boolean end)
	{
		for(SiegeClan clan : getAttackerClans().values())
			if(end)
			{
				clan.getClan().setSiege(null);
				clan.getClan().setDefender(false);
				clan.getClan().setAttacker(false);
			}
			else
			{
				clan.getClan().setSiege(this);
				clan.getClan().setDefender(false);
				clan.getClan().setAttacker(true);
			}
	}

	/**
	 * Return true if clan is attacker<BR><BR>
	 * 
	 * @param clan
	 *            The L2Clan of the player
	 */
	public boolean checkIsAttacker(L2Clan clan)
	{
		return getAttackerClan(clan) != null;
	}

	/**
	 * Return true if clan is defender<BR><BR>
	 * 
	 * @param clan
	 *            The L2Clan of the player
	 */
	public boolean checkIsDefender(L2Clan clan)
	{
		return getDefenderClan(clan) != null;
	}

	/**
	 * Return true if clan is defender waiting approval<BR><BR>
	 * 
	 * @param clan
	 *            The L2Clan of the player
	 */
	public boolean checkIsDefenderWaiting(L2Clan clan)
	{
		return getDefenderWaitingClan(clan) != null;
	}

	public boolean checkIsDefenderRefused(L2Clan clan)
	{
		return getDefenderRefusedClan(clan) != null;
	}

	/**
	 * Return true if clan is registered to the siege<BR><BR>
	 * 
	 * @param clanId
	 *            The clan id of the player
	 */
	public boolean checkIsClanRegistered(int clanId)
	{
		return getAttackerClan(clanId) != null || getDefenderClan(clanId) != null || getDefenderWaitingClan(clanId) != null || getDefenderRefusedClan(clanId) != null;
	}

	public int getDefenderRespawnTotal()
	{
		return _defenderRespawnDelay + _defenderRespawnPenalty;
	}

	public boolean isInProgress()
	{
		return _isInProgress;
	}

	public boolean isRegistrationOver()
	{
		return _isRegistrationOver;
	}

	public void setRegistrationOver(boolean value)
	{
		_isRegistrationOver = value;
	}

	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}

	public Calendar getSiegeEndDate()
	{
		return _siegeEndDate;
	}

	public long getTimeRemaining()
	{
		return getSiegeDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}

	private static final short[] SIEGE_SUMMONS = {
			1459,
			14768,
			14769,
			14770,
			14771,
			14772,
			14773,
			14774,
			14775,
			14776,
			14777,
			14778,
			14779,
			14780,
			14781,
			14782,
			14783,
			14784,
			14785,
			14786,
			14787,
			14788,
			14789,
			14790,
			14791,
			14792,
			14793,
			14794,
			14795,
			14796,
			14797,
			14798,
			14839 };

	protected void removeSiegeSummons()
	{
		for(L2Player player : getPlayersInZone())
			for(short id : SIEGE_SUMMONS)
				if(player.getPet() != null && id == player.getPet().getNpcId())
					player.getPet().unSummon();
	}

	/** Remove all Headquarters */
	protected void removeHeadquarters()
	{
		for(SiegeClan sc : getAttackerClans().values())
			if(sc != null)
				sc.removeHeadquarter();
		for(SiegeClan sc : getDefenderClans().values())
			if(sc != null)
				sc.removeHeadquarter();
	}

	public L2NpcInstance getHeadquarter(L2Clan clan)
	{
		if(clan != null)
		{
			SiegeClan sc = getAttackerClan(clan);
			if(sc != null)
				return sc.getHeadquarter();
		}
		return null;
	}

	/**
	 * Control Tower was killed
	 * Add respawn penalty to defenders for each control tower lose
	 */
	public void killedCT()
	{
		_defenderRespawnPenalty += getControlTowerLosePenalty();
	}

	public void sendTrapStatus(L2Player player, boolean enter)
	{}

	public Calendar getSiegeRegistrationEndDate()
	{
		return _siegeRegistrationEndDate;
	}

	public int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}

	public void setSiegeClanMinLevel(int siegeClanMinLevel)
	{
		_siegeClanMinLevel = siegeClanMinLevel;
	}

	public int getSiegeLength()
	{
		return _siegeLength;
	}

	public void setSiegeLength(int siegeLength)
	{
		_siegeLength = siegeLength;
	}

	public int getControlTowerLosePenalty()
	{
		return _controlTowerLosePenalty;
	}

	public void setControlTowerLosePenalty(int controlTowerLosePenalty)
	{
		_controlTowerLosePenalty = controlTowerLosePenalty;
	}

	public int getDefenderRespawnDelay()
	{
		return _defenderRespawnDelay;
	}

	public void setDefenderRespawnDelay(int respawnDelay)
	{
		_defenderRespawnDelay = respawnDelay;
	}

}