package com.lineage.game.model.entity.SevenSignsFestival;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2World;
import com.lineage.game.model.SpawnListener;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.PledgeShowInfoUpdate;
import com.lineage.game.serverpackets.PledgeStatusChanged;
import com.lineage.game.serverpackets.Say2;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.GArray;

/**
 * Seven Signs Festival of Darkness Engine
 */
public class SevenSignsFestival implements SpawnListener
{
	private static Logger _log = Logger.getLogger(SevenSignsFestival.class.getName());
	private static SevenSignsFestival _instance;
	private static final SevenSigns _signsInstance = SevenSigns.getInstance();
	public static final String GET_CLAN_NAME = "SELECT clan_name FROM clan_data WHERE clan_id = (SELECT clanid FROM characters WHERE char_name = ?)";
	public static final int FESTIVAL_MANAGER_START = 120000; // 2 mins
	public static final int FESTIVAL_LENGTH = 1080000; // 18 mins
	public static final int FESTIVAL_CYCLE_LENGTH = 2280000; // 38 mins
	public static final int FESTIVAL_SIGNUP_TIME = FESTIVAL_CYCLE_LENGTH - FESTIVAL_LENGTH;
	public static final int FESTIVAL_FIRST_SPAWN = 120000; // 2 mins
	public static final int FESTIVAL_FIRST_SWARM = 300000; // 5 mins
	public static final int FESTIVAL_SECOND_SPAWN = 540000; // 9 mins
	public static final int FESTIVAL_SECOND_SWARM = 720000; // 12 mins
	public static final int FESTIVAL_CHEST_SPAWN = 900000; // 15 mins
	// Key Constants
	public static final int FESTIVAL_MAX_OFFSET = 230;
	public static final int FESTIVAL_DEFAULT_RESPAWN = 60; // Specify in seconds!
	public static final int FESTIVAL_COUNT = 5;
	public static final int FESTIVAL_LEVEL_MAX_31 = 0;
	public static final int FESTIVAL_LEVEL_MAX_42 = 1;
	public static final int FESTIVAL_LEVEL_MAX_53 = 2;
	public static final int FESTIVAL_LEVEL_MAX_64 = 3;
	public static final int FESTIVAL_LEVEL_MAX_NONE = 4;
	public static final int[] FESTIVAL_LEVEL_SCORES = { 60, 70, 100, 120, 150 }; // 500 maximum possible score
	public static final short FESTIVAL_OFFERING_ID = 5901;
	public static final short FESTIVAL_OFFERING_VALUE = 5;

	public static enum FestivalStatus
	{
		Begining,
		Signup,
		Started,
		FirstSpawn,
		FirstSwarm,
		SecondSpawn,
		SecondSwarm,
		ChestSpawn,
		Ending
	}

	private static FestivalManager _managerInstance;
	private static ScheduledFuture<?> _managerScheduledTask;
	private static long _nextFestivalCycleStart;
	private static long _nextFestivalStart;
	private static boolean _festivalInitialized;
	private static boolean _festivalInProgress;
	private static List<Integer> _accumulatedBonuses; // The total bonus available (in Ancient Adena)
	private static L2NpcInstance _dawnChatGuide;
	private static L2NpcInstance _duskChatGuide;
	private static Map<Integer, GArray<L2Player>> _dawnFestivalParticipants;
	private static Map<Integer, GArray<L2Player>> _duskFestivalParticipants;
	private static Map<Integer, GArray<L2Player>> _dawnPreviousParticipants;
	private static Map<Integer, GArray<L2Player>> _duskPreviousParticipants;
	private static Map<Integer, Integer> _dawnFestivalScores;
	private static Map<Integer, Integer> _duskFestivalScores;
	private static Map<Integer, L2DarknessFestival> _festivalInstances;

	/**
	 * _festivalData is essentially an instance of the seven_signs_festival table and
	 * should be treated as such.
	 * Data is initially accessed by the related Seven Signs cycle, with _signsCycle representing data for the current round of Festivals.
	 * The actual table data is stored as a series of StatsSet constructs. These are accessed by the use of an offset based on the number of festivals, thus:
	 * offset = FESTIVAL_COUNT + festivalId
	 * (Data for Dawn is always accessed by offset > FESTIVAL_COUNT)
	 */
	private Map<Integer, Map<Integer, StatsSet>> _festivalData;

	public SevenSignsFestival()
	{
		_accumulatedBonuses = new FastList<Integer>();
		_dawnFestivalParticipants = new FastMap<Integer, GArray<L2Player>>();
		_dawnPreviousParticipants = new FastMap<Integer, GArray<L2Player>>();
		_dawnFestivalScores = new FastMap<Integer, Integer>();
		_duskFestivalParticipants = new FastMap<Integer, GArray<L2Player>>();
		_duskPreviousParticipants = new FastMap<Integer, GArray<L2Player>>();
		_duskFestivalScores = new FastMap<Integer, Integer>();
		_festivalData = new FastMap<Integer, Map<Integer, StatsSet>>();
		restoreFestivalData();
		L2Spawn.addSpawnListener(this);
		if(_signsInstance.isSealValidationPeriod())
		{
			_log.info("SevenSignsFestival: Initialization bypassed due to Seal Validation in effect.");
			return;
		}
		startFestivalManager();
	}

	public static SevenSignsFestival getInstance()
	{
		if(_instance == null)
			_instance = new SevenSignsFestival();
		return _instance;
	}

	/**
	 * Returns the associated name (level range) to a given festival ID.
	 * 
	 * @param int festivalID
	 * @return String festivalName
	 */
	public static String getFestivalName(final int festivalID)
	{
		switch(festivalID)
		{
			case FESTIVAL_LEVEL_MAX_31:
				return "Level 31 or lower";
			case FESTIVAL_LEVEL_MAX_42:
				return "Level 42 or lower";
			case FESTIVAL_LEVEL_MAX_53:
				return "Level 53 or lower";
			case FESTIVAL_LEVEL_MAX_64:
				return "Level 64 or lower";
			default:
				return "No Level Limit";
		}
	}

	/**
	 * Returns the maximum allowed player level for the given festival type.
	 * 
	 * @param festivalId
	 * @return int maxLevel
	 */
	public static int getMaxLevelForFestival(final int festivalId)
	{
		switch(festivalId)
		{
			case SevenSignsFestival.FESTIVAL_LEVEL_MAX_31:
				return 31;
			case SevenSignsFestival.FESTIVAL_LEVEL_MAX_42:
				return 42;
			case SevenSignsFestival.FESTIVAL_LEVEL_MAX_53:
				return 53;
			case SevenSignsFestival.FESTIVAL_LEVEL_MAX_64:
				return 64;
			default:
				return Experience.getMaxLevel();
		}
	}

	/**
	 * Used to produced a delimited string for an array of string elements.
	 * (Similar to implode() in PHP)
	 * 
	 * @param strArray
	 * @param delimiter
	 * @return String implodedString
	 */
	public static String implodeString(final List<String> strArray, final String delimiter)
	{
		final StringBuilder sb = new StringBuilder();
		for(final String strValue : strArray)
			sb.append(strValue + delimiter);
		sb.delete(sb.length() - delimiter.length(), sb.length() - 1); // remove last delimiter
		return sb.toString();
	}

	/**
	 * Used to start the Festival Manager, if the current period is not Seal Validation.
	 */
	public void startFestivalManager()
	{
		// Start the Festival Manager for the first time after the server has started at the specified time, then invoke it automatically after every cycle.
		_managerInstance = new FestivalManager(FestivalStatus.Begining);
		setNextFestivalStart(FESTIVAL_MANAGER_START + FESTIVAL_SIGNUP_TIME);
		_managerScheduledTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(_managerInstance, FESTIVAL_MANAGER_START, FESTIVAL_CYCLE_LENGTH + 1000);
		_log.info("SevenSignsFestival: The first Festival of Darkness cycle begins in " + FESTIVAL_MANAGER_START / 1000 / 60 + " minute(s).");
	}

	public void stopFestivalManager()
	{
		if(_managerScheduledTask != null)
			_managerScheduledTask.cancel(false);
	}

	/**
	 * Restores saved festival data, basic settings from the properties file
	 * and past high score data from the database.
	 */
	private void restoreFestivalData()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT festivalId, cabal, cycle, date, score, members FROM seven_signs_festival");
			rset = statement.executeQuery();
			while(rset.next())
			{
				final int cycle = _signsInstance.getCurrentCycle();
				int festivalId = rset.getInt("festivalId");
				final int cabal = SevenSigns.getCabalNumber(rset.getString("cabal"));
				final StatsSet festivalDat = new StatsSet();
				festivalDat.set("festivalId", festivalId);
				festivalDat.set("cabal", cabal);
				festivalDat.set("cycle", cycle);
				festivalDat.set("date", rset.getString("date"));
				festivalDat.set("score", rset.getInt("score"));
				festivalDat.set("members", rset.getString("members"));
				if(cabal == SevenSigns.CABAL_DAWN)
					festivalId += FESTIVAL_COUNT;
				Map<Integer, StatsSet> tempData = _festivalData.get(cycle);
				if(tempData == null)
					tempData = new FastMap<Integer, StatsSet>();
				tempData.put(festivalId, festivalDat);
				_festivalData.put(cycle, tempData);
			}
			DatabaseUtils.closeDatabaseSR(statement, rset);
			final StringBuffer query = new StringBuffer("SELECT festival_cycle, ");
			for(int i = 0; i < FESTIVAL_COUNT - 1; i++)
				query.append("accumulated_bonus" + String.valueOf(i) + ", ");
			query.append("accumulated_bonus" + String.valueOf(FESTIVAL_COUNT - 1) + " ");
			query.append("FROM seven_signs_status");
			statement = con.prepareStatement(query.toString());
			rset = statement.executeQuery();
			while(rset.next())
				for(int i = 0; i < FESTIVAL_COUNT; i++)
					_accumulatedBonuses.add(i, rset.getInt("accumulated_bonus" + String.valueOf(i)));
		}
		catch(final SQLException e)
		{
			_log.severe("SevenSignsFestival: Failed to load configuration: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/**
	 * Stores current festival data, basic settings to the properties file
	 * and past high score data to the database.
	 * 
	 * @param updateSettings
	 * @throws Exception
	 */
	public synchronized void saveFestivalData(final boolean updateSettings)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			for(final Map<Integer, StatsSet> currCycleData : _festivalData.values())
				for(final StatsSet festivalDat : currCycleData.values())
				{
					final int festivalCycle = festivalDat.getInteger("cycle");
					final int festivalId = festivalDat.getInteger("festivalId");
					final String cabal = SevenSigns.getCabalShortName(festivalDat.getInteger("cabal"));
					// Try to update an existing record.
					statement = con.prepareStatement("UPDATE seven_signs_festival SET date=?, score=?, members=? WHERE cycle=? AND cabal=? AND festivalId=?");
					statement.setLong(1, Long.valueOf(festivalDat.getString("date")));
					statement.setInt(2, festivalDat.getInteger("score"));
					statement.setString(3, festivalDat.getString("members"));
					statement.setInt(4, festivalCycle);
					statement.setString(5, cabal);
					statement.setInt(6, festivalId);
					// If there was no record to update, assume it doesn't exist and add a new one, otherwise continue with the next record to store.
					if(statement.executeUpdate() > 0)
					{
						statement.close();
						continue;
					}
					DatabaseUtils.closeStatement(statement);
					final FiltredPreparedStatement statement2 = con.prepareStatement("INSERT INTO seven_signs_festival (festivalId, cabal, cycle, date, score, members) VALUES (?,?,?,?,?,?)");
					statement2.setInt(1, festivalId);
					statement2.setString(2, cabal);
					statement2.setInt(3, festivalCycle);
					statement2.setLong(4, Long.valueOf(festivalDat.getString("date")));
					statement2.setInt(5, festivalDat.getInteger("score"));
					statement2.setString(6, festivalDat.getString("members"));
					statement2.execute();
					DatabaseUtils.closeStatement(statement2);
				}
		}
		catch(final SQLException e)
		{
			_log.severe("SevenSignsFestival: Failed to save configuration: " + e);
		}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}
		// Updates Seven Signs DB data also, so call only if really necessary.
		if(updateSettings)
			_signsInstance.saveSevenSignsData(null, true);
	}

	/**
	 * If a clan member is a member of the highest-ranked party in the Festival of Darkness, 100 points are added per member
	 */
	public void rewardHighestRanked()
	{
		String[] partyMembers;
		StatsSet overallData = getOverallHighestScoreData(FESTIVAL_LEVEL_MAX_31);
		if(overallData != null)
		{
			partyMembers = overallData.getString("members").split(",");
			for(final String partyMemberName : partyMembers)
				addReputationPointsForPartyMemberClan(partyMemberName);
		}
		overallData = getOverallHighestScoreData(FESTIVAL_LEVEL_MAX_42);
		if(overallData != null)
		{
			partyMembers = overallData.getString("members").split(",");
			for(final String partyMemberName : partyMembers)
				addReputationPointsForPartyMemberClan(partyMemberName);
		}
		overallData = getOverallHighestScoreData(FESTIVAL_LEVEL_MAX_53);
		if(overallData != null)
		{
			partyMembers = overallData.getString("members").split(",");
			for(final String partyMemberName : partyMembers)
				addReputationPointsForPartyMemberClan(partyMemberName);
		}
		overallData = getOverallHighestScoreData(FESTIVAL_LEVEL_MAX_64);
		if(overallData != null)
		{
			partyMembers = overallData.getString("members").split(",");
			for(final String partyMemberName : partyMembers)
				addReputationPointsForPartyMemberClan(partyMemberName);
		}
		overallData = getOverallHighestScoreData(FESTIVAL_LEVEL_MAX_NONE);
		if(overallData != null)
		{
			partyMembers = overallData.getString("members").split(",");
			for(final String partyMemberName : partyMembers)
				addReputationPointsForPartyMemberClan(partyMemberName);
		}
	}

	private void addReputationPointsForPartyMemberClan(final String partyMemberName)
	{
		final L2Player player = L2World.getPlayer(partyMemberName);
		if(player != null)
		{
			if(player.getClan() != null)
			{
				player.getClan().incReputation(100, true, "SevenSignsFestival");
				final SystemMessage sm = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_AN_ACTIVE_MEMBER_OF_THE_HIGHEST_RANKED_PARTY_IN_THE_FESTIVAL_OF_DARKNESS_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE);
				sm.addString(partyMemberName);
				sm.addNumber(100);
				player.getClan().broadcastToOnlineMembers(sm);
				player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
				player.getClan().broadcastToOnlineMembers(new PledgeStatusChanged(player.getClan()));
			}
		}
		else
		{
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(GET_CLAN_NAME);
				statement.setString(1, partyMemberName);
				final ResultSet rset = statement.executeQuery();
				if(rset.next())
				{
					final String clanName = rset.getString("clan_name");
					if(clanName != null)
					{
						final L2Clan clan = ClanTable.getInstance().getClanByName(clanName);
						if(clan != null)
						{
							clan.incReputation(100, true, "SevenSignsFestival");
							clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
							clan.broadcastToOnlineMembers(new PledgeStatusChanged(clan));
							final SystemMessage sm = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_AN_ACTIVE_MEMBER_OF_THE_HIGHEST_RANKED_PARTY_IN_THE_FESTIVAL_OF_DARKNESS_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE);
							sm.addString(partyMemberName);
							sm.addNumber(100);
							clan.broadcastToOnlineMembers(sm);

						}
					}
				}
				DatabaseUtils.closeResultSet(rset);
				DatabaseUtils.closeStatement(statement);
			}
			catch(final Exception e)
			{
				_log.warning("could not get clan name of " + partyMemberName + ": " + e);
			}
			finally
			{
				DatabaseUtils.closeConnection(con);
			}
		}
	}

	/**
	 * Used to reset all festival data at the beginning of a new quest event period.
	 */
	public void resetFestivalData(final boolean updateSettings)
	{
		// Set all accumulated bonuses back to 0.
		for(int i = 0; i < FESTIVAL_COUNT; i++)
			_accumulatedBonuses.set(i, 0);
		_dawnPreviousParticipants.clear();
		_duskPreviousParticipants.clear();
		_dawnFestivalParticipants.clear();
		_duskFestivalParticipants.clear();
		_dawnFestivalScores.clear();
		_duskFestivalScores.clear();
		// Set up a new data set for the current cycle of festivals
		final Map<Integer, StatsSet> newData = new FastMap<Integer, StatsSet>();
		for(int i = 0; i < FESTIVAL_COUNT * 2; i++)
		{
			int festivalId = i;
			if(i >= FESTIVAL_COUNT)
				festivalId -= FESTIVAL_COUNT;
			// Create a new StatsSet with "default" data for Dusk
			final StatsSet tempStats = new StatsSet();
			tempStats.set("festivalId", festivalId);
			tempStats.set("cycle", _signsInstance.getCurrentCycle());
			tempStats.set("date", "0");
			tempStats.set("score", 0);
			tempStats.set("members", "");
			if(i >= FESTIVAL_COUNT)
				tempStats.set("cabal", SevenSigns.CABAL_DAWN);
			else
				tempStats.set("cabal", SevenSigns.CABAL_DUSK);
			newData.put(i, tempStats);
		}
		// Add the newly created cycle data to the existing festival data, and subsequently save it to the database.
		_festivalData.put(_signsInstance.getCurrentCycle(), newData);
		saveFestivalData(updateSettings);
		// Remove any unused blood offerings from online players.
		for(final L2Player onlinePlayer : L2World.getAllPlayers())
		{
			final L2ItemInstance bloodOfferings = onlinePlayer.getInventory().getItemByItemId(FESTIVAL_OFFERING_ID);
			if(bloodOfferings != null)
				onlinePlayer.getInventory().destroyItem(bloodOfferings, bloodOfferings.getIntegerLimitedCount(), true);
		}
		_log.info("SevenSignsFestival: Reinitialized engine for next competition period.");
	}

	public boolean isFestivalInitialized()
	{
		return _festivalInitialized;
	}

	public static void setFestivalInitialized(final boolean isInitialized)
	{
		_festivalInitialized = isInitialized;
	}

	public boolean isFestivalInProgress()
	{
		return _festivalInProgress;
	}

	public static void setFestivalInProgress(final boolean inProgress)
	{
		_festivalInProgress = inProgress;
	}

	public static void setNextCycleStart()
	{
		_nextFestivalCycleStart = System.currentTimeMillis() + FESTIVAL_CYCLE_LENGTH;
	}

	public static void setNextFestivalStart(final long milliFromNow)
	{
		_nextFestivalStart = System.currentTimeMillis() + milliFromNow;
	}

	public int getMinsToNextCycle()
	{
		if(_signsInstance.isSealValidationPeriod())
			return -1;
		return Math.round((_nextFestivalCycleStart - System.currentTimeMillis()) / 1000 / 60);
	}

	public static int getMinsToNextFestival()
	{
		if(_signsInstance.isSealValidationPeriod())
			return -1;
		return Math.round((_nextFestivalStart - System.currentTimeMillis()) / 1000 / 60) + 1;
	}

	public String getTimeToNextFestivalStr()
	{
		if(_signsInstance.isSealValidationPeriod())
			return "<font color=\"FF0000\">This is the Seal Validation period. Festivals will resume next week.</font>";
		return "<font color=\"FF0000\">The next festival will begin in " + getMinsToNextFestival() + " minute(s).</font>";
	}

	/**
	 * Returns the current festival ID and oracle ID that the specified player is in,
	 * but will return the default of {-1, -1} if the player is not found as a participant.
	 * 
	 * @param player
	 * @return int[] playerFestivalInfo
	 */
	public int[] getFestivalForPlayer(final L2Player player)
	{
		final int[] playerFestivalInfo = { -1, -1 };
		int festivalId = 0;
		while(festivalId < FESTIVAL_COUNT)
		{
			GArray<L2Player> participants = getDawnFestivalParticipants().get(festivalId);
			// If there are no participants in this festival, move on to the next.
			if(participants != null && participants.contains(player))
			{
				playerFestivalInfo[0] = SevenSigns.CABAL_DAWN;
				playerFestivalInfo[1] = festivalId;
				return playerFestivalInfo;
			}
			festivalId++;
			participants = getDuskFestivalParticipants().get(festivalId);
			if(participants != null && participants.contains(player))
			{
				playerFestivalInfo[0] = SevenSigns.CABAL_DUSK;
				playerFestivalInfo[1] = festivalId;
				return playerFestivalInfo;
			}
			festivalId++;
		}
		// Return default data if the player is not found as a participant.
		return playerFestivalInfo;
	}

	public boolean isParticipant(final L2Player player)
	{
		if(_signsInstance.isSealValidationPeriod())
			return false;
		if(_managerInstance == null)
			return false;
		for(final GArray<L2Player> participants : getDawnFestivalParticipants().values())
			if(participants.contains(player))
				return true;
		for(final GArray<L2Player> participants : getDuskFestivalParticipants().values())
			if(participants.contains(player))
				return true;
		return false;
	}

	public GArray<L2Player> getParticipants(final int oracle, final int festivalId)
	{
		if(oracle == SevenSigns.CABAL_DAWN)
			return getDawnFestivalParticipants().get(festivalId);
		return getDuskFestivalParticipants().get(festivalId);
	}

	public GArray<L2Player> getPreviousParticipants(final int oracle, final int festivalId)
	{
		if(oracle == SevenSigns.CABAL_DAWN)
			return getDawnPreviousParticipants().get(festivalId);
		return getDuskPreviousParticipants().get(festivalId);
	}

	public void setParticipants(final int oracle, final int festivalId, final L2Party festivalParty)
	{
		GArray<L2Player> participants = null;
		if(festivalParty != null)
			participants = festivalParty.getPartyMembers();
		if(oracle == SevenSigns.CABAL_DAWN)
			getDawnFestivalParticipants().put(festivalId, participants);
		else
			getDuskFestivalParticipants().put(festivalId, participants);
	}

	public void updateParticipants(final L2Player player, final L2Party festivalParty)
	{
		if(!isParticipant(player))
			return;
		final int[] playerFestInfo = getFestivalForPlayer(player);
		final int oracle = playerFestInfo[0];
		final int festivalId = playerFestInfo[1];
		if(festivalId > -1)
		{
			if(_festivalInitialized)
			{
				final L2DarknessFestival festivalInst = getFestivalInstance(oracle, festivalId);
				if(festivalParty == null)
					for(final L2Player partyMember : getParticipants(oracle, festivalId))
						festivalInst.relocatePlayer(partyMember, true);
				else
					festivalInst.relocatePlayer(player, true);
			}
			setParticipants(oracle, festivalId, festivalParty);
		}
	}

	public int getHighestScore(final int oracle, final int festivalId)
	{
		return getHighestScoreData(oracle, festivalId).getInteger("score");
	}

	/**
	 * Returns a stats set containing the highest score <b>this cycle</b> for the
	 * the specified cabal and associated festival ID.
	 * 
	 * @param oracle
	 * @param festivalId
	 * @return StatsSet festivalDat
	 */
	public StatsSet getHighestScoreData(final int oracle, final int festivalId)
	{
		int offsetId = festivalId;
		if(oracle == SevenSigns.CABAL_DAWN)
			offsetId += 5;
		// Attempt to retrieve existing score data (if found), otherwise create a new blank data set and display a console warning.
		StatsSet currData = null;
		try
		{
			currData = _festivalData.get(_signsInstance.getCurrentCycle()).get(offsetId);
		}
		catch(final Exception e)
		{
			_log.config("SSF: Error while getting scores");
			_log.config("oracle=" + oracle + " festivalId=" + festivalId + " offsetId" + offsetId + " _signsCycle" + _signsInstance.getCurrentCycle());
			_log.config("_festivalData=" + _festivalData.toString());
			e.printStackTrace();
		}
		if(currData == null)
		{
			currData = new StatsSet();
			currData.set("score", 0);
			currData.set("members", "");
			_log.warning("SevenSignsFestival: Data missing for " + SevenSigns.getCabalName(oracle) + ", FestivalID = " + festivalId + " (Current Cycle " + _signsInstance.getCurrentCycle() + ")");
		}
		return currData;
	}

	/**
	 * Returns a stats set containing the highest ever recorded
	 * score data for the specified festival.
	 * 
	 * @param festivalId
	 * @return StatsSet result
	 */
	public StatsSet getOverallHighestScoreData(final int festivalId)
	{
		StatsSet result = null;
		int highestScore = 0;
		for(final Map<Integer, StatsSet> currCycleData : _festivalData.values())
			for(final StatsSet currFestData : currCycleData.values())
			{
				final int currFestID = currFestData.getInteger("festivalId");
				final int festivalScore = currFestData.getInteger("score");
				if(currFestID != festivalId)
					continue;
				if(festivalScore > highestScore)
				{
					highestScore = festivalScore;
					result = currFestData;
				}
			}
		return result;
	}

	/**
	 * Set the final score details for the last participants of the specified festival data.
	 * Returns <b>true</b> if the score is higher than that previously recorded <b>this cycle</b>.
	 * 
	 * @param player
	 * @param oracle
	 * @param festivalId
	 * @param offeringScore
	 * @return boolean isHighestScore
	 */
	public boolean setFinalScore(final L2Player player, final int oracle, final int festivalId, final int offeringScore)
	{
		List<String> partyMembers;
		final int currDawnHighScore = getHighestScore(SevenSigns.CABAL_DAWN, festivalId);
		final int currDuskHighScore = getHighestScore(SevenSigns.CABAL_DUSK, festivalId);
		int thisCabalHighScore = 0;
		int otherCabalHighScore = 0;
		if(oracle == SevenSigns.CABAL_DAWN)
		{
			thisCabalHighScore = currDawnHighScore;
			otherCabalHighScore = currDuskHighScore;
			_dawnFestivalScores.put(festivalId, offeringScore);
		}
		else
		{
			thisCabalHighScore = currDuskHighScore;
			otherCabalHighScore = currDawnHighScore;
			_duskFestivalScores.put(festivalId, offeringScore);
		}
		final StatsSet currFestData = getHighestScoreData(oracle, festivalId);
		// Check if this is the highest score for this level range so far for the player's cabal.
		if(offeringScore > thisCabalHighScore)
		{
			// If the current score is greater than that for the other cabal, then they already have the points from this festival.
			if(thisCabalHighScore > otherCabalHighScore)
				return false;
			partyMembers = new FastList<String>();
			final GArray<L2Player> prevParticipants = getPreviousParticipants(oracle, festivalId);
			// Record a string list of the party members involved.
			for(final L2Player partyMember : prevParticipants)
				partyMembers.add(partyMember.getName());
			// Update the highest scores and party list.
			currFestData.set("date", String.valueOf(System.currentTimeMillis()));
			currFestData.set("score", offeringScore);
			currFestData.set("members", implodeString(partyMembers, ","));
			// Only add the score to the cabal's overall if it's higher than the other cabal's score.
			if(offeringScore > otherCabalHighScore)
			{
				final int contribPoints = FESTIVAL_LEVEL_SCORES[festivalId];
				// Give this cabal the festival points, while deducting them from the other.
				_signsInstance.addFestivalScore(oracle, contribPoints);
			}
			saveFestivalData(true);
			return true;
		}
		return false;
	}

	public int getAccumulatedBonus(final int festivalId)
	{
		return _accumulatedBonuses.get(festivalId);
	}

	public void addAccumulatedBonus(final int festivalId, final int stoneType, final int stoneAmount)
	{
		int eachStoneBonus = 0;
		switch(stoneType)
		{
			case SevenSigns.SEAL_STONE_BLUE_ID:
				eachStoneBonus = SevenSigns.SEAL_STONE_BLUE_VALUE;
				break;
			case SevenSigns.SEAL_STONE_GREEN_ID:
				eachStoneBonus = SevenSigns.SEAL_STONE_GREEN_VALUE;
				break;
			case SevenSigns.SEAL_STONE_RED_ID:
				eachStoneBonus = SevenSigns.SEAL_STONE_RED_VALUE;
				break;
		}
		final int newTotalBonus = _accumulatedBonuses.get(festivalId) + stoneAmount * eachStoneBonus;
		_accumulatedBonuses.set(festivalId, newTotalBonus);
	}

	/**
	 * Calculate and return the proportion of the accumulated bonus for the festival
	 * where the player was in the winning party, if the winning party's cabal won the event.
	 * The accumulated bonus is then updated, with the player's share deducted.
	 * 
	 * @param player
	 * @return playerBonus (the share of the bonus for the party)
	 */
	public int distribAccumulatedBonus(final L2Player player)
	{
		int playerBonus = 0;
		final String playerName = player.getName();
		final int playerCabal = _signsInstance.getPlayerCabal(player);
		if(playerCabal != _signsInstance.getCabalHighestScore())
			return 0;
		for(final StatsSet festivalData : _festivalData.get(_signsInstance.getCurrentCycle()).values())
			if(festivalData.getString("members").indexOf(playerName) > -1)
			{
				final int festivalId = festivalData.getInteger("festivalId");
				final int numPartyMembers = festivalData.getString("members").split(",").length;
				final int totalAccumBonus = _accumulatedBonuses.get(festivalId);
				playerBonus = totalAccumBonus / numPartyMembers;
				_accumulatedBonuses.set(festivalId, totalAccumBonus - playerBonus);
				break;
			}
		return playerBonus;
	}

	/**
	 * Used to send a "shout" message to all players currently present in an Oracle.
	 * Primarily used for Festival Guide and Witch related speech.
	 * 
	 * @param senderName
	 * @param message
	 */
	public static void sendMessageToAll(final String senderName, final String message)
	{
		if(_dawnChatGuide == null || _duskChatGuide == null)
			return;
		_dawnChatGuide.broadcastPacket(new Say2(_dawnChatGuide.getObjectId(), 1, senderName, message));
		_duskChatGuide.broadcastPacket(new Say2(_duskChatGuide.getObjectId(), 1, senderName, message));
	}

	/**
	 * Basically a wrapper-call to signal to increase the challenge of the specified festival.
	 * 
	 * @param oracle
	 * @param festivalId
	 * @return boolean isChalIncreased
	 */
	public boolean increaseChallenge(final int oracle, final int festivalId)
	{
		if(getFestivalInstance(oracle, festivalId) == null)
			return false;
		return getFestivalInstance(oracle, festivalId).increaseChallenge();
	}

	/**
	 * Used with the SpawnListener, to update the required "chat guide" instances,
	 * for use with announcements in the oracles.
	 * 
	 * @param npc
	 */
	@Override
	public void npcSpawned(final L2NpcInstance npc)
	{
		if(npc == null)
			return;
		// If the spawned NPC ID matches the ones we need, assign their instances.
		if(npc.getNpcId() == 31127)
			_dawnChatGuide = npc;
		if(npc.getNpcId() == 31137)
			_duskChatGuide = npc;
	}

	/**
	 * Returns the running instance of a festival for the given Oracle and festivalID.
	 * <BR>
	 * A <B>null</B> value is returned if there are no participants in that festival.
	 * <BR><BR>
	 * Compute the offset if a Dusk instance is required.<BR>
	 * ID: 0 1 2 3 4 <BR>
	 * Dusk 1: 10 11 12 13 14 <BR>
	 * Dawn 2: 20 21 22 23 24 <BR>
	 * 
	 * @param oracle
	 * @param festivalId
	 * @return L2DarknessFestival festivalInst
	 */
	public final L2DarknessFestival getFestivalInstance(final int oracle, final int festivalId)
	{
		if(!_festivalInitialized)
			return null;
		return _festivalInstances.get(festivalId + (oracle == SevenSigns.CABAL_DUSK ? 10 : 20));
	}

	public void setDawnChat(final L2NpcInstance npc)
	{
		_dawnChatGuide = npc;
	}

	public void setDuskChat(final L2NpcInstance npc)
	{
		_duskChatGuide = npc;
	}

	public static Map<Integer, GArray<L2Player>> getDawnFestivalParticipants()
	{
		return _dawnFestivalParticipants;
	}

	public static Map<Integer, GArray<L2Player>> getDuskFestivalParticipants()
	{
		return _duskFestivalParticipants;
	}

	public static Map<Integer, GArray<L2Player>> getDawnPreviousParticipants()
	{
		return _dawnPreviousParticipants;
	}

	public static Map<Integer, GArray<L2Player>> getDuskPreviousParticipants()
	{
		return _duskPreviousParticipants;
	}

	public static Map<Integer, L2DarknessFestival> getFestivalInstances()
	{
		return _festivalInstances;
	}

	public static void setFestivalInstances(final Map<Integer, L2DarknessFestival> instances)
	{
		_festivalInstances = instances;
	}

	public static void setManagerInstance(final FestivalManager instance)
	{
		_managerInstance = instance;
	}
}