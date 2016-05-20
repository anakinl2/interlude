package l2d.game.tables;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.scripts.Scripts;
import l2d.game.instancemanager.CatacombSpawnManager;
import l2d.game.instancemanager.DayNightSpawnManager;
import l2d.game.instancemanager.RaidBossSpawnManager;
import l2d.game.model.L2Object;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.model.instances.L2SiegeGuardInstance;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Rnd;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class SpawnTable
{
	private static final Logger _log = Logger.getLogger(SpawnTable.class.getName());

	private static SpawnTable _instance;

	private HashMap<Integer, L2Spawn> _spawntable;
	private HashMap<Integer, ArrayList<L2Spawn>> _spawnsByNpcId;
	public ArrayList<L2Spawn> _raids;
	private int _npcSpawnCount;
	private int _spawnCount;

	private int _highestId;
	private int _spawnId;

	public static SpawnTable getInstance()
	{
		if(_instance == null)
			new SpawnTable();
		return _instance;
	}

	private SpawnTable()
	{
		_instance = this;
		NpcTable.getInstance().applyServerSideTitle();
		if(!Config.DONTLOADSPAWN)
			fillSpawnTable(true);
		else
		{
			_log.info("Spawn Correctly Disabled");
			Scripts.getInstance().callOnLoad();
		}
	}

	public HashMap<Integer, L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	private void fillSpawnTable(boolean scripts)
	{
		_spawntable = new HashMap<Integer, L2Spawn>();
		_spawnsByNpcId = new HashMap<Integer, ArrayList<L2Spawn>>();
		_raids = new ArrayList<L2Spawn>();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM spawnlist ORDER by npc_templateid");
			//TODO возможно в будующем понадобидся условие: WHERE npc_templateid NOT IN (SELECT bossId FROM epic_boss_spawn)
			rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;
			_npcSpawnCount = 0;
			_spawnCount = 0;
			_spawnId = 0;

			while(rset.next())
			{
				template1 = NpcTable.getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					if(template1.isInstanceOf(L2SiegeGuardInstance.class))
					{
						// Don't spawn Siege Guard
					}
					else if(Config.ALLOW_CLASS_MASTERS_LIST.isEmpty() && template1.name.equalsIgnoreCase("L2ClassMaster"))
					{
						// Dont' spawn class masters
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setAmount(rset.getInt("count") * (Config.ALT_DOUBLE_SPAWN && !template1.isRaid ? 2 : 1));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"), rset.getInt("respawn_delay_rnd"));
						spawnDat.setLocation(rset.getInt("loc_id"));
						if(template1.isRaid)
							_raids.add(spawnDat);
						else
						{
							spawnDat.setId(_spawnId);
							_spawnId++;
							spawnDat.setRespawnTime(0);

							if(template1.isInstanceOf(L2MonsterInstance.class))
								//FIXME: возможно наоборот
								if(template1.name.contains("Lilim") || template1.name.contains("Lith"))
									CatacombSpawnManager.getInstance().addDawnMob(spawnDat);
								else if(template1.name.contains("Nephilim") || template1.name.contains("Gigant"))
									CatacombSpawnManager.getInstance().addDuskMob(spawnDat);

							switch(rset.getInt("periodOfDay"))
							{
								case 0: // default
									_npcSpawnCount += spawnDat.init();
									_spawntable.put(spawnDat.getId(), spawnDat);
									ArrayList<L2Spawn> s = _spawnsByNpcId.get(spawnDat.getNpcId());
									if(s == null)
									{
										s = new ArrayList<L2Spawn>();
										_spawnsByNpcId.put(spawnDat.getNpcId(), s);
									}
									s.add(spawnDat);
									break;
								case 1: // Day
									DayNightSpawnManager.getInstance().addDayMob(spawnDat);
									break;
								case 2: // Night
									DayNightSpawnManager.getInstance().addNightMob(spawnDat);
									break;
							}
							_spawnCount++;

							if(_npcSpawnCount % 1000 == 0)
								_log.info("Spawned " + _npcSpawnCount + " npc");

							if(spawnDat.getId() > _highestId)
								_highestId = spawnDat.getId();
						}
					}
				}
				else
					_log.warning("mob data for id:" + rset.getInt("npc_templateid") + " missing in npc table");
			}
			DayNightSpawnManager.getInstance().notifyChangeMode();
			CatacombSpawnManager.getInstance().notifyChangeMode();
		}
		catch(Exception e1)
		{
			// problem with initializing spawn, go to next one
			_log.warning("spawn couldnt be initialized:" + e1);
			e1.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		_log.config("SpawnTable: Loaded " + _spawnCount + " Npc Spawn Locations. Total NPCs: " + _npcSpawnCount);
		if(Config.DEBUG)
			_log.fine("Spawning completed, total number of NPCs in the world: " + _npcSpawnCount);
		loadInventory();

		if(scripts)
			Scripts.getInstance().callOnLoad();
	}

	public L2Spawn getTemplate(int id)
	{
		return _spawntable.get(id);
	}

	public ArrayList<L2Spawn> getSpawnsByNpcId(int id)
	{
		return _spawnsByNpcId.get(id);
	}

	public void addNewSpawn(L2Spawn spawn, boolean storeInDb)
	{
		if(Config.DONTLOADSPAWN)
			return;
		_highestId++;
		spawn.setId(_highestId);
		_spawntable.put(new Integer(spawn.getId()), spawn);

		ArrayList<L2Spawn> s = _spawnsByNpcId.get(spawn.getNpcId());
		if(s == null)
		{
			s = new ArrayList<L2Spawn>();
			_spawnsByNpcId.put(spawn.getId(), s);
		}
		s.add(spawn);

		if(!storeInDb)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("INSERT INTO `spawnlist` (location,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id) values(?,?,?,?,?,?,?,?,?)");

			statement.setString(1, "");// spawn.getLocation());
			statement.setInt(2, spawn.getAmount());
			statement.setInt(3, spawn.getNpcId());
			statement.setInt(4, spawn.getLocx());
			statement.setInt(5, spawn.getLocy());
			statement.setInt(6, spawn.getLocz());
			statement.setInt(7, spawn.getHeading());
			statement.setInt(8, spawn.getRespawnDelay());
			statement.setInt(9, spawn.getLocation());
			statement.execute();
		}
		catch(Exception e1)
		{
			// problem with storing spawn
			_log.warning("spawn couldnt be stored in db:" + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void deleteSpawn(L2Spawn spawn, boolean updateDb)
	{
		ArrayList<L2Spawn> s = _spawnsByNpcId.get(spawn.getNpcId());
		if(s != null)
			s.remove(spawn);

		if(_spawntable.remove(new Integer(spawn.getId())) != null && updateDb)
		{
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM `spawnlist` WHERE `npc_templateid`=? AND `locx`=? AND `locy`=?");
				statement.setInt(1, spawn.getNpcId());
				statement.setInt(2, spawn.getLocx());
				statement.setInt(3, spawn.getLocy());
				statement.execute();
			}
			catch(Exception e1)
			{
				// problem with deleting spawn
				_log.warning("spawn couldnt be deleted in db:" + e1);
			}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}
	}

	//just wrapper
	public void reloadAll()
	{
		L2World.deleteVisibleNpcSpawns();
		CatacombSpawnManager.getInstance().cleanUp();
		DayNightSpawnManager.getInstance().cleanUp();
		fillSpawnTable(false);
		RaidBossSpawnManager.getInstance().reloadBosses();
	}

	public void loadInventory()
	{
		ConcurrentHashMap<Integer, L2Object> world = L2World.getAllObjects();
		int count = 0;
		HashSet<L2MonsterInstance> temp = null;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT owner_id, object_id, item_id, count, enchant_level FROM items WHERE loc = 'MONSTER'");
			rset = statement.executeQuery();

			while(rset.next())
			{
				count++;
				temp = new HashSet<L2MonsterInstance>();
				int id = rset.getInt("owner_id");
				// TODO: снести этот бред, ибо так все вещи будут у одного моба, а должны быть у рандомного
				for(L2Object tmp : world.values())
					if(tmp.isMonster() && ((L2MonsterInstance) tmp).getNpcId() == id)
						temp.add((L2MonsterInstance) tmp);
				if(temp.size() > 0)
				{
					L2MonsterInstance monster = (L2MonsterInstance) temp.toArray()[Rnd.get(temp.size())];
					L2ItemInstance item = L2ItemInstance.restoreFromDb(rset.getInt("object_id"));
					monster.giveItem(item, false);
				}
			}
		}
		catch(Exception e1)
		{
			e1.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		_log.info("Monsters inventory loaded, items: " + count);
	}

	public static void unload()
	{
		if(_instance != null)
			_instance = null;
	}
}