package com.lineage.game.instancemanager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.FiltredStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.db.mysql;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2RaidBossInstance;
import com.lineage.game.model.instances.L2ReflectionBossInstance;
import com.lineage.game.tables.GmListTable;
import com.lineage.game.tables.SpawnTable;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Log;
import com.lineage.util.PrintfFormat;
import com.lineage.util.Util;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class RaidBossSpawnManager
{

	private static Logger _log = Logger.getLogger(RaidBossSpawnManager.class.getName());

	private static RaidBossSpawnManager _instance;

	protected static Map<Integer, L2RaidBossInstance> _bosses;
	protected static Map<Integer, L2Spawn> _spawntable;
	protected static Map<Integer, StatsSet> _storedInfo;
	protected static Map<Integer, ScheduledFuture<?>> _schedules;

	protected static Map<Integer, FastMap<Integer, Integer>> _points;

	public static enum StatusEnum
	{
		ALIVE,
		DEAD,
		UNDEFINED
	}

	private RaidBossSpawnManager()
	{
		_instance = this;
		if(!Config.DONTLOADSPAWN)
			reloadBosses();
	}

	public void reloadBosses()
	{
		fillSpawnTable();
		fillPointsTable();

		calculateRanking();
	}

	public static RaidBossSpawnManager getInstance()
	{
		if(_instance == null)
			new RaidBossSpawnManager();
		return _instance;
	}

	private void fillPointsTable()
	{
		_points = new FastMap<Integer, FastMap<Integer, Integer>>();

		final FastList<Integer> _owners = new FastList<Integer>();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			// read raidboss points
			statement = con.prepareStatement("SELECT DISTINCT owner_id FROM `raidboss_points`");
			rset = statement.executeQuery();
			while(rset.next())
				_owners.add(rset.getInt("owner_id"));
			DatabaseUtils.closeDatabaseSR(statement, rset);

			statement = con.prepareStatement("SELECT * FROM `raidboss_points` WHERE `owner_id`=?");
			for(FastList.Node<Integer> n = _owners.head(), end = _owners.tail(); (n = n.getNext()) != end;)
			{
				final int ownerId = n.getValue();
				final FastMap<Integer, Integer> tmpScore = new FastMap<Integer, Integer>();
				statement.setInt(1, ownerId);
				rset = statement.executeQuery();
				while(rset.next())
					if(rset.getInt("boss_id") != -1 || rset.getInt("boss_id") != 0)
						tmpScore.put(rset.getInt("boss_id"), rset.getInt("points"));

				DatabaseUtils.closeResultSet(rset);
				_points.put(ownerId, tmpScore);
			}
		}
		catch(final SQLException e)
		{
			_log.warning(" ~ ERROR: Couldnt load raidboss points");
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	private void fillSpawnTable()
	{
		_bosses = new FastMap<Integer, L2RaidBossInstance>();
		_schedules = new FastMap<Integer, ScheduledFuture<?>>();
		_storedInfo = new FastMap<Integer, StatsSet>();
		_spawntable = new FastMap<Integer, L2Spawn>();

		ThreadConnection con = null;
		final FiltredStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			rset = con.createStatement().executeQuery("SELECT * FROM `raidboss_status`");
			while(rset.next())
			{
				final StatsSet info = new StatsSet();
				info.set("current_hp", rset.getDouble("current_hp"));
				info.set("current_mp", rset.getDouble("current_mp"));
				info.set("respawn_delay", rset.getInt("respawn_delay"));

				_storedInfo.put(rset.getInt("id"), info);
			}
		}
		catch(final SQLException e)
		{
			_log.warning(" ~ ERROR: Couldnt load raidboss statuses");
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		int npcId;
		for(final L2Spawn spawnDat : SpawnTable.getInstance()._raids)
		{
			npcId = spawnDat.getNpcId();
			spawnDat.setRespawnTime(_storedInfo.get(npcId) != null ? _storedInfo.get(npcId).getInteger("respawn_delay", 0) : 0);
			addNewSpawn(spawnDat, false);
		}

		_log.info("[Raid Boss Spawn Manager]");
		_log.info("~ Loaded: " + _storedInfo.size() + " Statuses");
		_log.info("~ Loaded: " + _bosses.size() + " Instances");
		_log.info("~ Loaded: " + _schedules.size() + " Instances");
		_log.info("[Raid Boss Spawn Manager]\n");
	}

	public void calculateRanking()
	{
		final FastMap<Integer, Integer> tmpRanking = new FastMap<Integer, Integer>();
		final FastMap<Integer, FastMap<Integer, Integer>> tmpPoints = new FastMap<Integer, FastMap<Integer, Integer>>();

		for(final int ownerId : _points.keySet())
		{
			FastMap<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
			tmpPoint = _points.get(ownerId);
			int totalPoints = 0;

			for(final int bossId : tmpPoint.keySet())
				if(bossId != -1 && bossId != 0)
					totalPoints += tmpPoint.get(bossId);

			// no need to store players w/o points
			if(totalPoints != 0)
			{
				tmpPoint.remove(0);
				tmpPoint.put(0, totalPoints);
				tmpPoints.put(ownerId, tmpPoint);

				tmpRanking.put(ownerId, totalPoints);
			}
		}

		final List<Map.Entry<Integer, Integer>> list = new Vector<Map.Entry<Integer, Integer>>(tmpRanking.entrySet());

		// descending
		Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>(){
			@Override
			public int compare(final Map.Entry<Integer, Integer> entry, final Map.Entry<Integer, Integer> entry1)
			{
				return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
			}
		});

		int ranking = 1;
		for(final Map.Entry<Integer, Integer> entry : list)
		{
			FastMap<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
			tmpPoint = tmpPoints.get(entry.getKey());

			tmpPoint.remove(-1);
			tmpPoint.put(-1, ranking);

			tmpPoints.remove(entry.getKey());
			tmpPoints.put(entry.getKey(), tmpPoint);

			ranking++;
		}

		_points.clear();
		_points = tmpPoints;
	}

	public void addPoints(final int ownerId, final int bossId, final int points)
	{
		FastMap<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
		tmpPoint = _points.get(ownerId);
		_points.remove(ownerId);

		if(tmpPoint == null || tmpPoint.isEmpty())
		{
			tmpPoint = new FastMap<Integer, Integer>();
			tmpPoint.put(bossId, points);
		}
		else
		{
			final int currentPoins = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId).intValue() : 0;

			tmpPoint.remove(bossId);
			tmpPoint.put(bossId, currentPoins == 0 ? points : currentPoins + points);
		}
		_points.put(ownerId, tmpPoint);
	}

	public void updateStatus(final L2RaidBossInstance raidboss, final boolean isBossDead)
	{
		if(raidboss.getNpcId() == 29020 || raidboss instanceof L2ReflectionBossInstance || raidboss.getName().equals("Anakazel"))
			return;

		if(_storedInfo.containsKey(raidboss.getNpcId()))
			_storedInfo.remove(raidboss.getNpcId());

		if(isBossDead)
		{
			raidboss.setRaidStatus(StatusEnum.DEAD);
			final int respawn_delay = raidboss.getSpawn().getRespawnDelayWithRnd();
			if(respawn_delay > 0)
			{
				final Calendar next_spawn = Calendar.getInstance();
				next_spawn.add(Calendar.SECOND, respawn_delay);

				// raidboss.getSpawn().updateInDb();
				raidboss.getSpawn().setRespawnTime((int) (next_spawn.getTimeInMillis() / 1000));
				// raidboss.getSpawn().updateInDb();
				final StatsSet info = new StatsSet();
				info.set("current_hp", raidboss.getMaxHp());
				info.set("current_mp", raidboss.getMaxMp());
				info.set("respawn_delay", (int) (next_spawn.getTimeInMillis() / 1000));
				_storedInfo.put(raidboss.getNpcId(), info);
				updateStatusDb(raidboss.getNpcId());

				// raidboss.getSpawn().updateInDb();

				final ScheduledFuture<?> futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnSchedule(raidboss.getNpcId()), respawn_delay * 1000);
				_schedules.put(raidboss.getNpcId(), futureSpawn);

				_log.info("[RaidBossSpawnManager]: Scheduled " + raidboss.getName() + " for respawn in " + Util.formatTime(respawn_delay));
				final String bosstype = raidboss.getClass().getSimpleName().replaceFirst("L2", "").replaceFirst("Instance", "");
				Log.add(PrintfFormat.LOG_BOSS_RESPAWN, new Object[] {
						bosstype,
						raidboss.getName(),
						raidboss.getNpcId(),
						Util.formatTime(respawn_delay),
						next_spawn.getTime() }, "bosses");
			}
		}
		else
		{
			raidboss.setRaidStatus(StatusEnum.ALIVE);

			if(raidboss.isCurrentHpFull() && raidboss.isCurrentMpFull())
				return;

			final StatsSet info = new StatsSet();
			info.set("current_hp", raidboss.getCurrentHp());
			info.set("current_mp", raidboss.getCurrentMp());

			_storedInfo.put(raidboss.getNpcId(), info);
		}
	}

	private class SpawnSchedule implements Runnable
	{
		private int bossId;

		public SpawnSchedule(final int npcId)
		{
			bossId = npcId;
		}

		@Override
		public void run()
		{
			L2RaidBossInstance raidboss = null;

			if(bossId == 25328)
				raidboss = DayNightSpawnManager.getInstance().handleNightBoss(_spawntable.get(bossId));
			else
				raidboss = (L2RaidBossInstance) _spawntable.get(bossId).doSpawn(true);

			if(raidboss != null)
			{
				raidboss.setRaidStatus(StatusEnum.ALIVE);
				GmListTable.broadcastMessageToGMs("Spawning RaidBoss " + raidboss.getName());

				_bosses.put(bossId, raidboss);
			}
			_schedules.remove(bossId);
		}
	}

	public void addNewSpawn(final L2Spawn spawnDat, final boolean storeInDb)
	{
		if(spawnDat == null)
			return;

		final int bossId = spawnDat.getNpcId();
		if(_spawntable.containsKey(bossId))
			return;

		SpawnTable.getInstance().addNewSpawn(spawnDat, storeInDb);

		if(System.currentTimeMillis() > spawnDat.getRespawnTime() * 1000L)
		{
			L2RaidBossInstance raidboss = null;

			if(bossId == 25328)
				raidboss = DayNightSpawnManager.getInstance().handleNightBoss(spawnDat);
			else
				raidboss = (L2RaidBossInstance) spawnDat.doSpawn(true);

			if(raidboss != null)
			{
				if(_storedInfo.containsKey(bossId))
				{
					final StatsSet info = _storedInfo.get(bossId);
					if(info.getDouble("current_hp") == raidboss.getMaxHp() && info.getDouble("current_mp") == raidboss.getMaxMp())
						_storedInfo.remove(bossId);
					else
					{
						raidboss.setCurrentHp(info.getDouble("current_hp"), false);
						raidboss.setCurrentMp(info.getDouble("current_mp"));
					}
				}
				raidboss.setRaidStatus(StatusEnum.ALIVE);

				_bosses.put(bossId, raidboss);
			}
		}
		else
		{
			final long spawnTime = spawnDat.getRespawnTime() * 1000L - System.currentTimeMillis();
			final ScheduledFuture<?> futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnSchedule(bossId), spawnTime);
			_schedules.put(bossId, futureSpawn);
		}

		_spawntable.put(bossId, spawnDat);
	}

	public void deleteSpawn(final L2Spawn spawnDat, final boolean updateDb)
	{
		if(spawnDat == null)
			return;
		if(!_spawntable.containsKey(spawnDat.getNpcId()))
			return;

		final int bossId = spawnDat.getNpcId();

		SpawnTable.getInstance().deleteSpawn(spawnDat, updateDb);
		_spawntable.remove(bossId);

		if(_bosses.containsKey(bossId))
			_bosses.remove(bossId);

		if(_schedules.containsKey(bossId))
		{
			final ScheduledFuture<?> f = _schedules.get(bossId);
			f.cancel(true);
			_schedules.remove(bossId);
		}

		if(_storedInfo.containsKey(bossId))
			_storedInfo.remove(bossId);

		if(updateDb)
		{
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM raidboss_status WHERE id=?");
				statement.setInt(1, bossId);
				statement.execute();
			}
			catch(final Exception e)
			{
				_log.warning(" ~ ERROR: Could not remove raidboss #" + bossId + " from DB: " + e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}
	}

	private void emptyStatusTable()
	{
		if(!mysql.set("DELETE FROM `raidboss_status` WHERE respawn_delay <= UNIX_TIMESTAMP()"))
			_log.warning(" ~ ERROR: Couldnt empty raidboss_status table");
	}

	private void emptyScoresTable()
	{
		if(!mysql.set("DELETE FROM `raidboss_points`"))
			_log.warning(" ~ ERROR: Couldnt empty raidboss_points table");
	}

	private void updateStatusDb()
	{
		updateStatusDb(0);
	}

	private void updateStatusDb(final int force_id)
	{
		if(force_id == 0)
			emptyStatusTable();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;

		for(final int bossId : _bosses.keySet())
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();

				if(force_id > 0 && bossId != force_id)
					continue;

				final L2RaidBossInstance raidboss = _bosses.get(bossId);

				if(raidboss == null)
					continue;

				if(force_id == 0 && raidboss.getRaidStatus().equals(StatusEnum.ALIVE))
					updateStatus(raidboss, false);

				final StatsSet info = _storedInfo.get(bossId);

				if(info == null)
					continue;

				statement = con.prepareStatement("REPLACE INTO `raidboss_status` (id, current_hp, current_mp, respawn_delay) VALUES (?,?,?,?)");
				statement.setInt(1, bossId);
				statement.setDouble(2, info.getDouble("current_hp"));
				statement.setDouble(3, info.getDouble("current_mp"));
				statement.setInt(4, info.getInteger("respawn_delay", 0));
				statement.execute();

				_log.info("[RaidBossSpawnManager]: Saved status for raidboss " + raidboss.getName());
			}
			catch(final SQLException e)
			{
				_log.warning(" ~ ERROR: Couldnt update raidboss_status table");
			}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
	}

	private void updatePointsDb()
	{
		emptyScoresTable();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;

		for(final int ownerId : _points.keySet())
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();

				final FastMap<Integer, Integer> tmpPoint = _points.get(ownerId);
				if(tmpPoint == null || tmpPoint.isEmpty())
					continue;

				for(final int bossId : tmpPoint.keySet())
				{
					if(bossId == -1 || bossId == 0)
						continue;

					final int points = tmpPoint.get(bossId);
					if(points == 0)
						continue;

					statement = con.prepareStatement("INSERT INTO `raidboss_points` (owner_id, boss_id, points) VALUES (?,?,?)");
					statement.setInt(1, ownerId);
					statement.setInt(2, bossId);
					statement.setInt(3, points);
					statement.execute();
					DatabaseUtils.closeStatement(statement);
				}
			}
			catch(final SQLException e)
			{
				_log.warning(" ~ ERROR: Couldnt update raidboss_points table");
			}
			finally
			{
				DatabaseUtils.closeConnection(con);
			}
	}

	public String[] getAllRaidBossStatus()
	{
		final String[] msg = new String[_bosses == null ? 0 : _bosses.size()];

		if(_bosses == null)
		{
			msg[0] = "None";
			return msg;
		}

		int index = 0;

		for(final Integer i : _bosses.keySet())
		{
			final L2RaidBossInstance raidboss = _bosses.get(i);

			msg[index] = raidboss.getName() + ": " + raidboss.getRaidStatus().name();
			index++;
		}

		return msg;
	}

	public String getRaidBossStatus(final int bossId)
	{
		String msg = "RaidBoss Status....\n";

		if(_bosses == null)
		{
			msg += "None";
			return msg;
		}

		if(_bosses.containsKey(bossId))
		{
			final L2RaidBossInstance raidboss = _bosses.get(bossId);

			msg += raidboss.getName() + ": " + raidboss.getRaidStatus().name();
		}

		return msg;
	}

	public StatusEnum getRaidBossStatusId(final int bossId)
	{
		if(_bosses.containsKey(bossId))
			return _bosses.get(bossId).getRaidStatus();
		else if(_schedules.containsKey(bossId))
			return StatusEnum.DEAD;
		else
			return StatusEnum.UNDEFINED;
	}

	public void notifySpawnNightBoss(final L2RaidBossInstance raidboss)
	{
		raidboss.setRaidStatus(StatusEnum.ALIVE);
		_bosses.put(raidboss.getNpcId(), raidboss);
		GmListTable.broadcastMessageToGMs("Spawning night RaidBoss " + raidboss.getName());
	}

	public boolean isDefined(final int bossId)
	{
		return _spawntable.containsKey(bossId);
	}

	public Map<Integer, L2RaidBossInstance> getBosses()
	{
		return _bosses;
	}

	public L2RaidBossInstance getBoss(final int bossId)
	{
		return _bosses.get(bossId);
	}

	public Map<Integer, L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	public Map<Integer, FastMap<Integer, Integer>> getPoints()
	{
		return _points;
	}

	public FastMap<Integer, Integer> getPointsByOwnerId(final int ownerId)
	{
		return _points.get(ownerId);
	}

	/**
	 * Saves all raidboss status and then clears all info from memory,
	 * including all schedules.
	 */
	public void cleanUp()
	{
		updateStatusDb();
		updatePointsDb();

		_bosses.clear();
		_storedInfo.clear();
		_spawntable.clear();
		_points.clear();

		if(_schedules != null)
			for(final Integer bossId : _schedules.keySet())
			{
				final ScheduledFuture<?> f = _schedules.get(bossId);
				f.cancel(true);
			}
		_schedules.clear();

		_log.fine("[RaidBossSpawnManager]: All raidboss info saved!");
	}

	public boolean isScheduled(final int bossId)
	{
		return _schedules.containsKey(bossId);
	}
}