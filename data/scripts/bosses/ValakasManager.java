package bosses;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.ext.listeners.L2ZoneEnterLeaveListener;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.ThreadPoolManager;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2BossInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.SocialAction;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Log;
import com.lineage.util.Rnd;
import bosses.EpicBossState.State;

public class ValakasManager extends Functions implements ScriptFile
{
	private static class ValakasSpawn implements Runnable
	{
		private int _distance = 2550;
		private int _taskId = 0;
		private L2BossInstance _valakas = null;
		private List<L2Player> _players = getPlayersInside();

		ValakasSpawn(final int taskId, final L2BossInstance valakas)
		{
			_taskId = taskId;
			_valakas = valakas;
		}

		public void run()
		{
			int npcId;
			L2Spawn valakasSpawn = null;
			SocialAction sa = null;

			switch(_taskId)
			{
				case 1: // spawn.

					if(FWV_VALAKAS)
						npcId = 29028;
					Dying = false;
					// do spawn.
					valakasSpawn = _monsterSpawn.get(npcId);
					_valakas = (L2BossInstance) valakasSpawn.doSpawn(true);
					_monsters.add(_valakas);
					_valakas.setImobilised(true);

					_state.setRespawnDate(Rnd.get(FWV_FIXINTERVALOFVALAKAS, FWV_FIXINTERVALOFVALAKAS + FWV_RANDOMINTERVALOFVALAKAS));
					_state.setState(EpicBossState.State.ALIVE);
					_state.update();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(2, _valakas), 16);

					break;

				case 2:
					// set camera.
					for(final L2Player pc : _players)
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1800, 180, -1, 1500, 15000);
						}
						else
							pc.leaveMovieMode();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(3, _valakas), 3000);

					break;

				case 3:
					// do social.
					sa = new SocialAction(_valakas.getObjectId(), 1);
					_valakas.broadcastPacket(sa);

					// set camera.
					for(final L2Player pc : _players)
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1300, 180, -5, 3000, 15000);
						}
						else
							pc.leaveMovieMode();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(4, _valakas), 10000);

					break;

				case 4:
					// set camera.
					for(final L2Player pc : _players)
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 500, 180, -8, 600, 15000);
						}
						else
							pc.leaveMovieMode();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(5, _valakas), 200);

					break;

				case 5:
					// do social.
					sa = new SocialAction(_valakas.getObjectId(), 2);
					_valakas.broadcastPacket(sa);

					// set camera.
					for(final L2Player pc : _players)
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1200, 180, -5, 300, 15000);
						}
						else
							pc.leaveMovieMode();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(6, _valakas), 10800);

					break;

				case 6:
					// set camera.
					for(final L2Player pc : _players)
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 2800, 250, 70, 0, 15000);
						}
						else
							pc.leaveMovieMode();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(7, _valakas), 1900);

					break;

				case 7:
					// Set camera.
					for(final L2Player pc : _players)
					{
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 2600, 30, 60, 3400, 15000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// Set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(8, _valakas), 5700);

					break;

				case 8:
					// Set camera.
					for(final L2Player pc : _players)
					{
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 700, 150, -65, 0, 15000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// Set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(9, _valakas), 1400);
					break;

				case 9:
					// Set camera.
					for(final L2Player pc : _players)
					{
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1200, 150, -55, 2900, 15000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}
					// Set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(10, _valakas), 6700);
					break;

				case 10:
					// Set camera.
					for(final L2Player pc : _players)
					{
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 750, 170, -10, 1700, 5700);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// Set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(11, _valakas), 3700);
					break;

				case 11:
					// Set camera.
					for(final L2Player pc : _players)
					{
						if(pc.getDistance(_valakas) <= _distance)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 840, 170, -5, 1200, 2000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}
					// Set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(12, _valakas), 2000);

					break;
				case 12:
					_valakas.abortCast();
					// reset camera.
					for(final L2Player pc : _players)
						pc.leaveMovieMode();

					_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_valakas), 16);
					_valakas.setRunning();
					// move at random.
					if(FWV_MOVEATRANDOM)
					{
						final Location pos = new Location(Rnd.get(211080, 214909), Rnd.get( -115841, -112822), -1662, 0);
						_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_valakas, pos), 32);
					}

					_sleepCheckTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckLastAttack(), 600000);

					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					break;
			}
		}
	}

	public static class CheckLastAttack implements Runnable
	{
		public void run()
		{
			if(_state.getState().equals(EpicBossState.State.ALIVE))
				if(_lastAttackTime + FWV_LIMITUNTILSLEEP < System.currentTimeMillis())
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
			if(_selfDestructionTask != null)
			{
				_selfDestructionTask.cancel(true);
				_selfDestructionTask = null;
			}
			for(final L2Spawn spawnDat : _teleportCubeSpawn)
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

	// Move at random on after Valakas appears.
	public static class MoveAtRandom implements Runnable
	{
		private L2NpcInstance _npc;
		private Location _pos;

		public MoveAtRandom(final L2NpcInstance npc, final Location pos)
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

	// action is enabled the boss.
	public static class SetMobilised implements Runnable
	{
		private L2BossInstance _boss;

		public SetMobilised(final L2BossInstance boss)
		{
			_boss = boss;
		}

		public void run()
		{
			_boss.setImobilised(false);
			// _boss.setIsInSocialAction(false);

			// When it is possible to act, a social action is canceled.
			if(_socialTask != null)
			{
				_socialTask.cancel(true);
				_socialTask = null;
			}
		}
	}

	public static class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(final L2Zone zone, final L2Object object)
		{}

		@Override
		public void objectLeaved(final L2Zone zone, final L2Object object)
		{}
	}

	private static final int _teleportCubeId = 31759;
	private static final int _teleportCubeLocation[][] = { {214880, -116144, -1644, 0}, {213696, -116592, -1644, 0}, {212112, -116688, -1644, 0}, {211184, -115472, -1664, 0}, {210336, -114592, -1644, 0}, {211360, -113904, -1644, 0}, {213152, -112352, -1644, 0}, {214032, -113232, -1644, 0}, {214752, -114592, -1644, 0}, {209824, -115568, -1421, 0}, {210528, -112192, -1403, 0}, {213120, -111136, -1408, 0}, {215184, -111504, -1392, 0}, {215456, -117328, -1392, 0}, {213200, -118160, -1424, 0}

	};

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
	private static ScheduledFuture<?> _selfDestructionTask = null;
	private static ScheduledFuture<?> _moveAtRandomTask = null;
	private static ScheduledFuture<?> _sleepCheckTask = null;
	private static final int Valakas = 29028;
	private static String _questName;
	private static EpicBossState _state;
	private static L2Zone _zone;
	private static ZoneListener _zoneListener = new ZoneListener();
	private static long _lastAttackTime = 0;
	private static final boolean FWV_VALAKAS = true;
	private static final boolean FWV_MOVEATRANDOM = true;
	private static final int FWV_LIMITUNTILSLEEP = 30 * 60000;
	private static final int FWV_FIXINTERVALOFVALAKAS = 8 * 24 * 60 * 60000;
	private static final int FWV_RANDOMINTERVALOFVALAKAS = 6 * 24 * 60 * 60000;
	private static final int FWV_APPTIMEOFVALAKAS = 5 * 60000; // через 20 минут spawn
	private static final int VALAKAS = 29028;

	private static boolean Dying = false;

	private static void banishForeigners()
	{
		for(final L2Player player : getPlayersInside())
		{
			if(_questName != null)
			{
				final QuestState qs = player.getQuestState(_questName);
				if(qs != null && qs.getInt("ok") == 1)
					qs.exitCurrentQuest(true);
			}
			player.teleToClosestTown();
		}
	}

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
		return FWV_FIXINTERVALOFVALAKAS + Rnd.get(0, FWV_RANDOMINTERVALOFVALAKAS);
	}

	public static L2Zone getZone()
	{
		return _zone;
	}

	private static synchronized boolean isPlayersAnnihilated()
	{
		for(final L2Player pc : getPlayersInside())
			if( !pc.isDead())
				return false;
		return true;
	}

	private static void onValakasDie()
	{
		if(Dying)
			return;

		Dying = true;
		_state.setRespawnDate(getRespawnInterval());
		_state.setState(EpicBossState.State.INTERVAL);
		_state.update();

		Log.add("Valakas died", "bosses");

		_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 10000);
	}

	public static void OnDie(final L2Character self, final L2Character killer)
	{
		if(self == null)
			return;
		if(self.isPlayer() && _state != null && _state.getState() == State.ALIVE && _zone != null && _zone.checkIfInZone(self))
			checkAnnihilated();
		else if(self.isNpc() && (self.getNpcId() == VALAKAS))
			onValakasDie();
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

	// clean Valakas's lair.
	public static void setUnspawn()
	{
		// eliminate players.
		banishForeigners();

		// delete monsters.
		for(final L2NpcInstance mob : _monsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_monsters.clear();

		// delete teleport cube.
		for(final L2NpcInstance cube : _teleportCube)
		{
			cube.getSpawn().stopRespawn();
			cube.deleteMe();
		}
		_teleportCube.clear();

		// not executed tasks is canceled.
		if(_cubeSpawnTask != null)
		{
			_cubeSpawnTask.cancel(true);
			_cubeSpawnTask = null;
		}
		if(_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
		}
		if(_intervalEndTask != null)
		{
			_intervalEndTask.cancel(true);
			_intervalEndTask = null;
		}
		if(_socialTask != null)
		{
			_socialTask.cancel(true);
			_socialTask = null;
		}
		if(_mobiliseTask != null)
		{
			_mobiliseTask.cancel(true);
			_mobiliseTask = null;
		}
		if(_selfDestructionTask != null)
		{
			_selfDestructionTask.cancel(true);
			_selfDestructionTask = null;
		}
		if(_moveAtRandomTask != null)
		{
			_moveAtRandomTask.cancel(true);
			_moveAtRandomTask = null;
		}
		if(_sleepCheckTask != null)
		{
			_sleepCheckTask.cancel(true);
			_sleepCheckTask = null;
		}
	}

	public void init()
	{
		_questName = "_1003_EntertoValakas";
		_state = new EpicBossState(Valakas);
		_zone = ZoneManager.getInstance().getZoneById(ZoneType.epic, 702002, false);
		_zone.getListenerEngine().addMethodInvokedListener(_zoneListener);

		// setting spawn data of monsters.
		try
		{
			L2NpcTemplate template1;
			L2Spawn tempSpawn;

			// setting Valakas data.
			template1 = NpcTable.getTemplate(29028);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(212852);
			tempSpawn.setLocy( -114842);
			tempSpawn.setLocz( -1632);
			tempSpawn.setHeading(833);
			tempSpawn.setAmount(1);
			tempSpawn.stopRespawn();
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_monsterSpawn.put(29028, tempSpawn);
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}

		// setting spawn data of teleport cube.
		try
		{
			final L2NpcTemplate Cube = NpcTable.getTemplate(_teleportCubeId);
			L2Spawn spawnDat;
			for(final int[] element : _teleportCubeLocation)
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
		catch(final Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("ValakasManager : State of Valakas is " + _state.getState() + ".");
		if( !_state.getState().equals(EpicBossState.State.NOTSPAWN))
			setIntervalEndTask();

		final Date dt = new Date(_state.getRespawnDate());
		System.out.println("ValakasManager : Next spawn date of Valakas is " + dt + ".");
	}

	public static void setLastAttackTime()
	{
		_lastAttackTime = System.currentTimeMillis();
	}

	public void onLoad()
	{
		init();
	}

	public void onReload()
	{
		getZone().getListenerEngine().removeMethodInvokedListener(_zoneListener);
		sleep();
	}

	public void onShutdown()
	{}

	// setting Valakas spawn task.
	public static void setValakasSpawnTask()
	{
		if(_monsterSpawnTask == null)
		{
			Functions.npcShoutInRange("Valakas", "I will spawn in 5 minutes.", 2000);
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(1, null), FWV_APPTIMEOFVALAKAS);
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
}