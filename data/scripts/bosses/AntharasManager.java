package bosses;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.ext.listeners.L2ZoneEnterLeaveListener;
import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.ThreadPoolManager;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2BossInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.SocialAction;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Location;
import l2d.util.Log;
import l2d.util.Rnd;
import bosses.EpicBossState.State;

public class AntharasManager extends Functions implements ScriptFile
{
	private static class AntharasSpawn implements Runnable
	{
		private int _distance = 2550;
		private int _taskId = 0;
		private L2BossInstance _antharas = null;
		private List<L2Player> _players = getPlayersInside();

		AntharasSpawn(int taskId, L2BossInstance antharas)
		{
			_taskId = taskId;
			_antharas = antharas;
		}

		public void run()
		{
			int npcId;
			L2Spawn antharasSpawn = null;
			SocialAction sa = null;

			if(_socialTask != null)
			{
				_socialTask.cancel(false);
				_socialTask = null;
			}

			switch(_taskId)
			{
				case 1: // spawn.
					npcId = 29066; // old
					Dying = false;

					// do spawn.
					antharasSpawn = _monsterSpawn.get(npcId);
					_antharas = (L2BossInstance) antharasSpawn.doSpawn(true);
					_monsters.add(_antharas);
					_antharas.setImobilised(true);

					_state.setRespawnDate(Rnd.get(FWA_FIXINTERVALOFANTHARAS, FWA_FIXINTERVALOFANTHARAS));
					_state.setState(EpicBossState.State.ALIVE);
					_state.update();

					// setting 1st time of minions spawn task.
					if( !FWA_OLDANTHARAS)
					{
						int intervalOfBehemoth;
						int intervalOfBomber;

						// Interval of minions is decided by the number of players
						// that invaded the lair.
						intervalOfBehemoth = FWA_INTERVALOFBEHEMOTHONWEAK;
						intervalOfBomber = FWA_INTERVALOFBOMBERONWEAK;

						// spawn Behemoth.
						_behemothSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new BehemothSpawn(intervalOfBehemoth), 30000);

						// spawn Bomber.
						_bomberSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new BomberSpawn(intervalOfBomber), 30000);
					}

					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(2, _antharas), 16);
					break;
				case 2:
					// set camera.
					for(L2Player pc : _players)
						if(pc.getDistance(_antharas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 700, 13, -19, 0, 10000);
						}
						else
							pc.leaveMovieMode();

					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(3, _antharas), 3000);
					break;
				case 3:
					// do social.
					sa = new SocialAction(_antharas.getObjectId(), 1);
					_antharas.broadcastPacket(sa);

					// set camera.
					for(L2Player pc : _players)
						if(pc.getDistance(_antharas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 700, 13, 0, 6000, 10000);
						}
						else
							pc.leaveMovieMode();

					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(4, _antharas), 10000);
					break;
				case 4:
					// set camera.
					for(L2Player pc : _players)
						if(pc.getDistance(_antharas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 3800, 0, -3, 0, 10000);
						}
						else
							pc.leaveMovieMode();

					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(5, _antharas), 200);
					break;
				case 5:
					// do social.
					sa = new SocialAction(_antharas.getObjectId(), 2);
					_antharas.broadcastPacket(sa);

					// set camera.
					for(L2Player pc : _players)
						if(pc.getDistance(_antharas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 1200, 0, -3, 22000, 11000);
						}
						else
							pc.leaveMovieMode();

					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(6, _antharas), 10800);
					break;
				case 6:
					// set camera.
					for(L2Player pc : _players)
						if(pc.getDistance(_antharas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 1200, 0, -3, 300, 2000);
						}
						else
							pc.leaveMovieMode();

					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(7, _antharas), 1900);
					break;
				case 7:
					_antharas.abortCast();
					// reset camera.
					for(L2Player pc : _players)
						pc.leaveMovieMode();

					_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_antharas), 16);

					_antharas.setRunning();

					// move at random.
					if(FWA_MOVEATRANDOM)
					{
						Location pos = new Location(Rnd.get(175000, 178500), Rnd.get(112400, 116000), -7707, 0);
						_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_antharas, pos), 32);
					}

					_sleepCheckTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckLastAttack(), 600000);
					break;
			}
		}
	}

	// do spawn Behemoth.
	private static class BehemothSpawn implements Runnable
	{
		private int _interval;

		public BehemothSpawn(int interval)
		{
			_interval = interval;
		}

		public void run()
		{
			L2NpcTemplate template1;
			L2Spawn tempSpawn;

			try
			{
				// set spawn.
				template1 = NpcTable.getTemplate(29069);
				tempSpawn = new L2Spawn(template1);
				// allocates it at random in the lair of Antharas.
				tempSpawn.setLocx(Rnd.get(175000, 179900));
				tempSpawn.setLocy(Rnd.get(112400, 116000));
				tempSpawn.setLocz( -7709);
				tempSpawn.setHeading(0);
				tempSpawn.setAmount(1);
				tempSpawn.stopRespawn();
				SpawnTable.getInstance().addNewSpawn(tempSpawn, false);

				// do spawn.
				_monsters.add(tempSpawn.doSpawn(true));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			if(_behemothSpawnTask != null)
			{
				_behemothSpawnTask.cancel(false);
				_behemothSpawnTask = null;
			}

			// repeat.
			_behemothSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new BehemothSpawn(_interval), _interval);
		}
	}

	// do spawn Bomber.
	private static class BomberSpawn implements Runnable
	{
		private int _interval;

		public BomberSpawn(int interval)
		{
			_interval = interval;
		}

		public void run()
		{
			int npcId = Rnd.get(29070, 29076);
			L2NpcTemplate template1;
			L2Spawn tempSpawn;
			L2NpcInstance bomber = null;

			try
			{
				// set spawn.
				template1 = NpcTable.getTemplate(npcId);
				tempSpawn = new L2Spawn(template1);
				// allocates it at random in the lair of Antharas.
				tempSpawn.setLocx(Rnd.get(175000, 179900));
				tempSpawn.setLocy(Rnd.get(112400, 116000));
				tempSpawn.setLocz( -7709);
				tempSpawn.setHeading(0);
				tempSpawn.setAmount(1);
				tempSpawn.stopRespawn();
				SpawnTable.getInstance().addNewSpawn(tempSpawn, false);

				// do spawn.
				bomber = tempSpawn.doSpawn(true);
				_monsters.add(bomber);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			// set self destruction.
			if(bomber != null)
				_selfDestructionTask = ThreadPoolManager.getInstance().scheduleGeneral(new SelfDestructionOfBomber(bomber), 3000);

			if(_bomberSpawnTask != null)
			{
				_bomberSpawnTask.cancel(false);
				_bomberSpawnTask = null;
			}

			// repeat.
			_bomberSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new BomberSpawn(_interval), _interval);
		}
	}

	public static class CheckLastAttack implements Runnable
	{
		public void run()
		{
			if(_state.getState().equals(EpicBossState.State.ALIVE))
				if(_lastAttackTime + FWA_LIMITUNTILSLEEP < System.currentTimeMillis())
					sleep();
				else
					_sleepCheckTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckLastAttack(), 60000);
		}
	}

	// do spawn teleport cube.
	public static class CubeSpawn implements Runnable
	{
		public void run()
		{
			if(_behemothSpawnTask != null)
			{
				_behemothSpawnTask.cancel(false);
				_behemothSpawnTask = null;
			}
			if(_bomberSpawnTask != null)
			{
				_bomberSpawnTask.cancel(false);
				_bomberSpawnTask = null;
			}
			if(_selfDestructionTask != null)
			{
				_selfDestructionTask.cancel(false);
				_selfDestructionTask = null;
			}
			for(L2Spawn spawnDat : _teleportCubeSpawn)
				_teleportCube.add(spawnDat.doSpawn(true));
		}
	}

	// at end of interval.
	public static class IntervalEnd implements Runnable
	{
		public void run()
		{
			_state.setState(EpicBossState.State.NOTSPAWN);
			_state.update();
		}
	}

	// Move at random on after Antharas appears.
	public static class MoveAtRandom implements Runnable
	{
		private L2NpcInstance _npc;
		private Location _pos;

		public MoveAtRandom(L2NpcInstance npc, Location pos)
		{
			_npc = npc;
			_pos = pos;
		}

		public void run()
		{
			if(_npc.getAI().getIntention() == AI_INTENTION_ACTIVE)
				_npc.moveToLocation(_pos, 0, false);
		}
	}

	public static class onAnnihilated implements Runnable
	{
		public void run()
		{
			sleep();
		}
	}

	// do self destruction.
	private static class SelfDestructionOfBomber implements Runnable
	{
		private L2NpcInstance _bomber;

		public SelfDestructionOfBomber(L2NpcInstance bomber)
		{
			_bomber = bomber;
		}

		public void run()
		{
			L2Skill skill = null;
			switch(_bomber.getNpcId())
			{
				case 29070:
				case 29071:
				case 29072:
				case 29073:
				case 29074:
				case 29075:
					skill = SkillTable.getInstance().getInfo(5097, 1);
					break;
				case 29076:
					skill = SkillTable.getInstance().getInfo(5094, 1);
					break;
			}

			_bomber.doCast(skill, null, false);
		}
	}

	// action is enabled the boss.
	public static class SetMobilised implements Runnable
	{
		private L2BossInstance _boss;

		public SetMobilised(L2BossInstance boss)
		{
			_boss = boss;
		}

		public void run()
		{
			_boss.setImobilised(false);
		}
	}

	public static class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{}
	}

	// location of teleport cube.
	private static final int _teleportCubeId = 31859;
	private static final int _teleportCubeLocation[][] = {{177615, 114941, -7709, 0}};
	private static List<L2Spawn> _teleportCubeSpawn = new FastList<L2Spawn>();
	private static List<L2NpcInstance> _teleportCube = new FastList<L2NpcInstance>();

	// spawn data of monsters.
	private static Map<Integer, L2Spawn> _monsterSpawn = new FastMap<Integer, L2Spawn>();
	// instance of monsters.
	private static List<L2NpcInstance> _monsters = new FastList<L2NpcInstance>();

	// tasks.
	private static ScheduledFuture<?> _cubeSpawnTask = null;
	private static ScheduledFuture<?> _monsterSpawnTask = null;
	private static ScheduledFuture<?> _intervalEndTask = null;
	private static ScheduledFuture<?> _socialTask = null;
	private static ScheduledFuture<?> _mobiliseTask = null;
	private static ScheduledFuture<?> _behemothSpawnTask = null;
	private static ScheduledFuture<?> _bomberSpawnTask = null;
	private static ScheduledFuture<?> _selfDestructionTask = null;
	private static ScheduledFuture<?> _moveAtRandomTask = null;
	private static ScheduledFuture<?> _sleepCheckTask = null;

	private static final int Antharas = 29019;

	private static EpicBossState _state;
	private static L2Zone _zone;
	private static long _lastAttackTime = 0;

	private static final boolean FWA_OLDANTHARAS = false;
	private static final boolean FWA_MOVEATRANDOM = true;

	private static final int FWA_LIMITUNTILSLEEP = 30 * 60000;
	private static final int FWA_FIXINTERVALOFANTHARAS = 24 * 60 * 60000; // 11 суток
	private static final int FWA_APPTIMEOFANTHARAS = 5 * 60000; // 20 минут ожидание перед респом
	private static final int FWA_INTERVALOFBEHEMOTHONWEAK = 8 * 60000;
	private static final int FWA_INTERVALOFBOMBERONWEAK = 6 * 60000;

	private static final int ANTHARAS_OLD = 29019;
	private static final int ANTHARAS_WEAK = 29066;
	private static final int ANTHARAS_NORMAL = 29067;
	private static final int ANTHARAS_STRONG = 29068;

	private static boolean Dying = false;

/*	private static void banishForeigners()
	{
		for(L2Player player : getPlayersInside())
		{
			if(_questName != null)
			{
				QuestState qs = player.getQuestState(_questName);
				if(qs != null && qs.getInt("ok") == 1)
					qs.exitCurrentQuest(true);
			}
			player.teleToClosestTown();
		}
	}*/
	
	private static void checkAnnihilated()
	{
		if(isPlayersAnnihilated())
			ThreadPoolManager.getInstance().scheduleGeneral(new onAnnihilated(), 5000);
	}

	private static List<L2Player> getPlayersInside()
	{
		return getZone().getInsidePlayers();
	}

	private static int getRespawnInterval()
	{
		return FWA_FIXINTERVALOFANTHARAS;
	}

	public static L2Zone getZone()
	{
		return _zone;
	}

	private static synchronized boolean isPlayersAnnihilated()
	{
		for(L2Player pc : getPlayersInside())
			if( !pc.isDead())
				return false;
		return true;
	}

	private static void onAntharasDie()
	{
		if(Dying)
			return;

		Dying = true;
		_state.setRespawnDate(getRespawnInterval());
		_state.setState(EpicBossState.State.INTERVAL);
		_state.update();

		Log.add("Antharas died", "bosses");

		_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 10000);
	}

	public static void OnDie(L2Character self, L2Character killer)
	{
		if(self == null)
			return;
		if(self.isPlayer() && _state != null && _state.getState() == State.ALIVE && _zone != null && _zone.checkIfInZone(self))
			checkAnnihilated();
		else if(self.isNpc() && (self.getNpcId() == ANTHARAS_OLD || self.getNpcId() == ANTHARAS_WEAK || self.getNpcId() == ANTHARAS_NORMAL || self.getNpcId() == ANTHARAS_STRONG))
			onAntharasDie();
	}

	private static void setIntervalEndTask()
	{
		setUnspawn();

		if( !_state.getState().equals(EpicBossState.State.INTERVAL))
		{
			_state.setRespawnDate(getRespawnInterval());
			_state.setState(EpicBossState.State.INTERVAL);
			_state.update();
		}

		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	public static void sleep()
	{
		setUnspawn();
		if(_state.getState().equals(EpicBossState.State.ALIVE))
		{
			_state.setState(EpicBossState.State.NOTSPAWN);
			_state.update();
		}
	}

	// clean Antharas's lair.
	public static void setUnspawn()
	{
		// eliminate players.
		//banishForeigners();

		// delete monsters.
		for(L2NpcInstance mob : _monsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_monsters.clear();

		// delete teleport cube.
		for(L2NpcInstance cube : _teleportCube)
		{
			cube.getSpawn().stopRespawn();
			cube.deleteMe();
		}
		_teleportCube.clear();

		// not executed tasks is canceled.
		if(_cubeSpawnTask != null)
		{
			_cubeSpawnTask.cancel(false);
			_cubeSpawnTask = null;
		}
		if(_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(false);
			_monsterSpawnTask = null;
		}
		if(_intervalEndTask != null)
		{
			_intervalEndTask.cancel(false);
			_intervalEndTask = null;
		}
		if(_socialTask != null)
		{
			_socialTask.cancel(false);
			_socialTask = null;
		}
		if(_mobiliseTask != null)
		{
			_mobiliseTask.cancel(false);
			_mobiliseTask = null;
		}
		if(_behemothSpawnTask != null)
		{
			_behemothSpawnTask.cancel(false);
			_behemothSpawnTask = null;
		}
		if(_bomberSpawnTask != null)
		{
			_bomberSpawnTask.cancel(false);
			_bomberSpawnTask = null;
		}
		if(_selfDestructionTask != null)
		{
			_selfDestructionTask.cancel(false);
			_selfDestructionTask = null;
		}
		if(_moveAtRandomTask != null)
		{
			_moveAtRandomTask.cancel(false);
			_moveAtRandomTask = null;
		}
		if(_sleepCheckTask != null)
		{
			_sleepCheckTask.cancel(false);
			_sleepCheckTask = null;
		}
	}

	public void init()
	{
		_state = new EpicBossState(Antharas);
		_zone = ZoneManager.getInstance().getZoneById(ZoneType.epic, 702002, false);

		// setting spawn data of monsters.
		try
		{
			L2NpcTemplate template1;
			L2Spawn tempSpawn;

			// old Antharas.
			template1 = NpcTable.getTemplate(29019);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz( -7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.stopRespawn();
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_monsterSpawn.put(29019, tempSpawn);

			// weak Antharas.
			template1 = NpcTable.getTemplate(29066);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz( -7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.stopRespawn();
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_monsterSpawn.put(29066, tempSpawn);

			// normal Antharas.
			template1 = NpcTable.getTemplate(29067);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz( -7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.stopRespawn();
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_monsterSpawn.put(29067, tempSpawn);

			// strong Antharas.
			template1 = NpcTable.getTemplate(29068);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz( -7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.stopRespawn();
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_monsterSpawn.put(29068, tempSpawn);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// setting spawn data of teleport cube.
		try
		{
			L2NpcTemplate Cube = NpcTable.getTemplate(_teleportCubeId);
			L2Spawn spawnDat;
			for(int[] element : _teleportCubeLocation)
			{
				spawnDat = new L2Spawn(Cube);
				spawnDat.setAmount(1);
				spawnDat.setLocx(element[0]);
				spawnDat.setLocy(element[1]);
				spawnDat.setLocz(element[2]);
				spawnDat.setHeading(element[3]);
				spawnDat.setRespawnDelay(60);
				spawnDat.setLocation(0);
				SpawnTable.getInstance().addNewSpawn(spawnDat, false);
				_teleportCubeSpawn.add(spawnDat);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Log.add("AntharasManager : State of Antharas is " + _state.getState() + ".","bosses");
		if( !_state.getState().equals(EpicBossState.State.NOTSPAWN))
			setIntervalEndTask();

		Date dt = new Date(_state.getRespawnDate());
		Log.add("AntharasManager : Next spawn date of Antharas is " + dt + ".","bosses");
	}

	public static void setLastAttackTime()
	{
		_lastAttackTime = System.currentTimeMillis();
	}

	// setting Antharas spawn task.
	public static void setAntharasSpawnTask()
	{
		if(_monsterSpawnTask == null)
		{
			Functions.npcShoutInRange("Antharas", "I will spawn in 5 minutes.", 2000);
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(1, null), FWA_APPTIMEOFANTHARAS);
		}
	}

	public static boolean isEnableEnterToLair()
	{
		return _state.getState() == EpicBossState.State.NOTSPAWN;
	}
	
	public static boolean isSpawnSheduled()
	{
		if(isEnableEnterToLair() && _monsterSpawnTask != null)
			return true;
		return false;
	}

	public void onLoad()
	{
		init();
	}

	public void onReload()
	{
		sleep();
	}

	public void onShutdown()
	{}
}