package com.lineage.game.instancemanager;

import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.entity.siege.SiegeSpawn;
import com.lineage.game.model.instances.L2ArtefactInstance;
import com.lineage.game.model.instances.L2ControlTowerInstance;
import com.lineage.game.tables.NpcTable;

public class CastleSiegeManager extends SiegeManager
{
	private static Logger _log = Logger.getLogger(CastleSiegeManager.class.getName());

	private static FastMap<Integer, FastList<SiegeSpawn>> _controlTowerSpawnList;
	private static FastMap<Integer, FastList<SiegeSpawn>> _artefactSpawnList;

	private static int _controlTowerLosePenalty = 20000;
	private static int _defenderRespawnDelay = 20000;
	private static int _siegeClanMinLevel = 5;
	private static int _siegeLength = 120;

	public static void load()
	{
		try
		{
			InputStream is =Config.class.getClassLoader().getResourceAsStream(Config.SIEGE_CASTLE_CONFIGURATION_FILE);
			Properties siegeSettings = new Properties();
			siegeSettings.load(is);
			is.close();

			// Siege spawns settings
			_controlTowerSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();
			_artefactSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();

			for(Castle castle : CastleManager.getInstance().getCastles().values())
			{
				FastList<SiegeSpawn> _controlTowersSpawns = new FastList<SiegeSpawn>();

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "ControlTower" + Integer.toString(i), "");

					if(_spawnParams.length() == 0)
						break;

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());
						int hp = Integer.parseInt(st.nextToken());

						_controlTowersSpawns.add(new SiegeSpawn(castle.getId(), x, y, z, 0, npc_id, hp));
					}
					catch(Exception e)
					{
						_log.warning("Error while loading control tower(s) for " + castle.getName() + " castle.");
					}
				}

				FastList<SiegeSpawn> _artefactSpawns = new FastList<SiegeSpawn>();

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "Artefact" + Integer.toString(i), "");

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

						_artefactSpawns.add(new SiegeSpawn(castle.getId(), x, y, z, heading, npc_id));
					}
					catch(Exception e)
					{
						_log.warning("Error while loading artefact(s) for " + castle.getName() + " castle.");
					}
				}

				_controlTowerSpawnList.put(castle.getId(), _controlTowersSpawns);
				_artefactSpawnList.put(castle.getId(), _artefactSpawns);

				castle.getSiege().setControlTowerLosePenalty(_controlTowerLosePenalty);
				castle.getSiege().setDefenderRespawnDelay(_defenderRespawnDelay);
				castle.getSiege().setSiegeClanMinLevel(_siegeClanMinLevel);
				castle.getSiege().setSiegeLength(_siegeLength);

				spawnArtifacts(castle);
				spawnControlTowers(castle);

				castle.getSiege().getZone().setActive(false);
				castle.getSiege().startAutoTask();
			}
		}
		catch(Exception e)
		{
			System.err.println("Error while loading siege data.");
			e.printStackTrace();
		}
	}

	public static FastList<SiegeSpawn> getControlTowerSpawnList(int _castleId)
	{
		if(_controlTowerSpawnList.containsKey(_castleId))
			return _controlTowerSpawnList.get(_castleId);
		return null;
	}

	public static void spawnArtifacts(Castle castle)
	{
		for(SiegeSpawn _sp : _artefactSpawnList.get(castle.getId()))
		{
			L2ArtefactInstance art = new L2ArtefactInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(_sp.getNpcId()));
			art.setCurrentHpMp(art.getMaxHp(), art.getMaxMp(), true);
			art.setHeading(_sp.getLoc().h);
			art.spawnMe(_sp.getLoc().changeZ(50));
			castle.getSiege().addArtifact(art);
		}
	}

	/**
	 * Spawn control tower.
	 * @param Сastle
	 */
	public static void spawnControlTowers(Castle castle)
	{
		for(SiegeSpawn sp : getControlTowerSpawnList(castle.getId()))
		{
			L2ControlTowerInstance tower = new L2ControlTowerInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(sp.getNpcId()), castle.getSiege(), sp.getHp());
			tower.setCurrentHpMp(tower.getMaxHp(), tower.getMaxMp(), true);
			tower.setHeading(sp.getLoc().h);
			tower.spawnMe(sp.getLoc());
			castle.getSiege().addControlTower(tower);
		}
	}

	public static int getControlTowerLosePenalty()
	{
		return _controlTowerLosePenalty;
	}

	public static int getDefenderRespawnDelay()
	{
		return _defenderRespawnDelay;
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