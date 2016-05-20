package com.lineage.game.instancemanager;

import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.lineage.game.idfactory.IdFactory;
import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.siege.SiegeSpawn;
import com.lineage.game.model.entity.siege.clanhall.ClanHallSiege;
import com.lineage.game.model.instances.L2ClanHallMessengerInstance;
import com.lineage.game.tables.NpcTable;

public class ClanHallSiegeManager extends SiegeManager
{
	protected static Logger _log = Logger.getLogger(CastleSiegeManager.class.getName());

	private static FastMap<Integer, FastList<SiegeSpawn>> _siegeBossSpawnList;
	private static FastMap<Integer, FastList<L2ClanHallMessengerInstance>> _messengersList;

	private static int _defenderRespawnDelay = 20000;
	private static int _siegeClanMinLevel = 4;
	private static int _siegeLength = 60;

	public static void load()
	{
		try
		{
			InputStream is = Config.class.getClassLoader().getResourceAsStream(Config.SIEGE_CLANHALL_CONFIGURATION_FILE);
			Properties siegeSettings = new Properties();
			siegeSettings.load(is);
			is.close();

			// Siege spawns settings
			_siegeBossSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();
			_messengersList = new FastMap<Integer, FastList<L2ClanHallMessengerInstance>>();

			for(ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
			{
				if(clanhall.getSiege() == null)
					continue;

				FastList<SiegeSpawn> _siegeBossSpawns = new FastList<SiegeSpawn>();
				FastList<L2ClanHallMessengerInstance> _messengers = new FastList<L2ClanHallMessengerInstance>();

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty("N" + clanhall.getId() + "SiegeBoss" + i, "");

					if(_spawnParams.length() == 0)
						break;

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int heading = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());

						_siegeBossSpawns.add(new SiegeSpawn(clanhall.getId(), x, y, z, heading, npc_id));
					}
					catch(Exception e)
					{
						_log.warning("Error while loading Siege Boss for " + clanhall.getName());
					}
				}

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty("N" + clanhall.getId() + "Messenger" + i, "");

					if(_spawnParams.length() == 0)
						break;

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int heading = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());

						L2ClanHallMessengerInstance messenger = new L2ClanHallMessengerInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(npc_id));
						messenger.setCurrentHpMp(messenger.getMaxHp(), messenger.getMaxMp(), true);
						messenger.setXYZInvisible(x, y, GeoEngine.getHeight(x, y, z));
						messenger.setSpawnedLoc(messenger.getLoc());
						messenger.setHeading(heading);
						messenger.spawnMe();

						_messengers.add(messenger);
					}
					catch(Exception e)
					{
						_log.warning("Error while loading Messenger(s) for " + clanhall.getName());
					}
				}

				_siegeBossSpawnList.put(clanhall.getId(), _siegeBossSpawns);
				_messengersList.put(clanhall.getId(), _messengers);

				if(_siegeBossSpawns.size() == 0)
					_log.warning("Not found Siege Boss for " + clanhall.getName());
				if(_messengersList.size() == 0)
					_log.warning("Not found Messengers for " + clanhall.getName());

				clanhall.getSiege().setDefenderRespawnDelay(_defenderRespawnDelay);
				clanhall.getSiege().setSiegeClanMinLevel(_siegeClanMinLevel);
				clanhall.getSiege().setSiegeLength(_siegeLength);

				if(clanhall.getSiege().getZone() != null)
					clanhall.getSiege().getZone().setActive(false);
				else
					_log.warning("Not found Zone for " + clanhall.getName());

				clanhall.getSiege().startAutoTask();
			}
		}
		catch(Exception e)
		{
			System.err.println("Error while loading siege data.");
			e.printStackTrace();
		}
	}

	public static FastList<SiegeSpawn> getSiegeBossSpawnList(int siegeUnitId)
	{
		return _siegeBossSpawnList.get(siegeUnitId);
	}

	public static ClanHallSiege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY());
	}

	public static ClanHallSiege getSiege(int x, int y)
	{
		for(ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
			if(clanhall.getSiege() != null && clanhall.getSiege().checkIfInZone(x, y, false))
				return clanhall.getSiege();
		return null;
	}

	public static int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}

	public static int getSiegeLength()
	{
		return _siegeLength;
	}
}