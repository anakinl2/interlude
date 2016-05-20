package bosses;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.SpawnTable;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.serverpackets.SocialAction;
import com.lineage.util.Rnd;
import com.lineage.util.Util;
import com.lineage.util.Location;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;

public class SailrenManager extends Functions implements ScriptFile
{
	public void onLoad()
	{
		init();
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	@SuppressWarnings("unused")
	private static String _questName;
	@SuppressWarnings("unused")
	private static long _lastAttackTime = 0;
	private static EpicBossState _state;
	private static SailrenManager _instance;

	// Teleport cube location.
	private final int _sailrenCubeLocation[][] = {{27734, -6838, -1982, 0}};
	protected List<L2Spawn> _sailrenCubeSpawn = new FastList<L2Spawn>();
	protected List<L2NpcInstance> _sailrenCube = new FastList<L2NpcInstance>();

	// Spawn data of monsters
	protected L2Spawn _velociraptorSpawn; // Velociraptor
	protected L2Spawn _pterosaurSpawn; // Pterosaur
	protected L2Spawn _tyrannoSpawn; // Tyrannosaurus
	protected L2Spawn _sailrenSapwn; // Sailren

	// Instance of monsters
	protected L2NpcInstance _velociraptor; // Velociraptor
	protected L2NpcInstance _pterosaur; // Pterosaur
	protected L2NpcInstance _tyranno; // Tyrannosaurus
	protected L2NpcInstance _sailren; // Sailren

	// Tasks
	protected ScheduledFuture<?> _cubeSpawnTask = null;
	protected ScheduledFuture<?> _sailrenSpawnTask = null;
	protected ScheduledFuture<?> _intervalEndTask = null;
	protected ScheduledFuture<?> _activityTimeEndTask = null;
	protected ScheduledFuture<?> _onPartyAnnihilatedTask = null;
	protected ScheduledFuture<?> _socialTask = null;

	// State of Sailren's lair.
	protected boolean _isAlreadyEnteredOtherParty = false;

	public static SailrenManager getInstance()
	{
		if(_instance == null)
			_instance = new SailrenManager();
		return _instance;
	}

	public SailrenManager()
	{
		@SuppressWarnings("unused")
		String _questName = "_1004_EntertoSailren";
		_state = new EpicBossState(29065);
	}

	public void init()
	{
		// Init state.
		_isAlreadyEnteredOtherParty = false;

		// Setting spawn data of monsters.
		try
		{
			L2NpcTemplate template1;

			// Velociraptor
			template1 = NpcTable.getTemplate(22218); // Velociraptor
			_velociraptorSpawn = new L2Spawn(template1);
			_velociraptorSpawn.setLocx(27852);
			_velociraptorSpawn.setLocy( -5536);
			_velociraptorSpawn.setLocz( -1983);
			_velociraptorSpawn.setHeading(44732);
			_velociraptorSpawn.setAmount(1);
			_velociraptorSpawn.setRespawnDelay(60 * 1000);
			SpawnTable.getInstance().addNewSpawn(_velociraptorSpawn, false);

			// Pterosaur
			template1 = NpcTable.getTemplate(22199); // Pterosaur
			_pterosaurSpawn = new L2Spawn(template1);
			_pterosaurSpawn.setLocx(27852);
			_pterosaurSpawn.setLocy( -5536);
			_pterosaurSpawn.setLocz( -1983);
			_pterosaurSpawn.setHeading(44732);
			_pterosaurSpawn.setAmount(1);
			_pterosaurSpawn.setRespawnDelay(60 * 1000);
			SpawnTable.getInstance().addNewSpawn(_pterosaurSpawn, false);

			// Tyrannosaurus
			template1 = NpcTable.getTemplate(22217); // Tyrannosaurus
			_tyrannoSpawn = new L2Spawn(template1);
			_tyrannoSpawn.setLocx(27852);
			_tyrannoSpawn.setLocy( -5536);
			_tyrannoSpawn.setLocz( -1983);
			_tyrannoSpawn.setHeading(44732);
			_tyrannoSpawn.setAmount(1);
			_tyrannoSpawn.setRespawnDelay(60 * 1000);
			SpawnTable.getInstance().addNewSpawn(_tyrannoSpawn, false);

			// Sailren
			template1 = NpcTable.getTemplate(29065); // Sailren
			_sailrenSapwn = new L2Spawn(template1);
			_sailrenSapwn.setLocx(27810);
			_sailrenSapwn.setLocy( -5655);
			_sailrenSapwn.setLocz( -1983);
			_sailrenSapwn.setHeading(44732);
			_sailrenSapwn.setAmount(1);
			_sailrenSapwn.setRespawnDelay(60 * 1000);
			SpawnTable.getInstance().addNewSpawn(_sailrenSapwn, false);

		}
		catch(Exception e)
		{}

		// Setting spawn data of teleporte cube.
		try
		{
			L2NpcTemplate cube = NpcTable.getTemplate(32107);
			L2Spawn spawnDat;

			for(int[] element : _sailrenCubeLocation)
			{
				spawnDat = new L2Spawn(cube);
				spawnDat.setAmount(1);
				spawnDat.setLocx(element[0]);
				spawnDat.setLocy(element[1]);
				spawnDat.setLocz(element[2]);
				spawnDat.setHeading(element[3]);
				spawnDat.setRespawnDelay(60);
				spawnDat.setLocation(0);
				SpawnTable.getInstance().addNewSpawn(spawnDat, false);
				_sailrenCubeSpawn.add(spawnDat);
			}
		}
		catch(Exception e)
		{}

		if( !_state.getState().equals(EpicBossState.State.NOTSPAWN))
			setIntervalEndTask();

	}

	// Whether it is permitted to enter the sailren's lair is confirmed.
	public int canIntoSailrenLair(L2Player pc)
	{
		if((pc.getParty() == null))
			return 4;
		else if(_isAlreadyEnteredOtherParty)
			return 2;
		else if(_state.getState().equals(EpicBossState.State.NOTSPAWN))
			return 0;
		else if(_state.getState().equals(EpicBossState.State.ALIVE) || _state.getState().equals(EpicBossState.State.DEAD))
			return 1;
		else if(_state.getState().equals(EpicBossState.State.INTERVAL))
			return 3;
		else
			return 0;

	}

	// Set Sailren spawn task.
	public void setSailrenSpawnTask(int npcId)
	{
		if(_sailrenSpawnTask == null)
		{
			_sailrenSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new SailrenSpawn(npcId), 60 * 1000);
		}
	}

	// Teleporting player to sailren's lair.
	public void entryToSailrenLair(L2Player pc)
	{
		int driftx;
		int drifty;

		if(canIntoSailrenLair(pc) != 0)
		{
			pc.sendMessage("Entrance was refused because it did not satisfy it.");
			_isAlreadyEnteredOtherParty = false;
			return;
		}

		if(pc.getParty() == null)
		{
			driftx = Rnd.get( -80, 80);
			drifty = Rnd.get( -80, 80);
			pc.teleToLocation(27734 + driftx, -6938 + drifty, -1982);
		}
		else
		{
			List<L2Character> members = new FastList<L2Character>(); // list of member of teleport candidate.
			for(L2Character mem : pc.getParty().getPartyMembers())
			{
				// teleporting it within alive and the range of recognition of the leader of the party.
				if( !mem.isDead() && Util.checkIfInRange(700, pc, mem, true))
				{
					members.add(mem);
				}
			}
			for(L2Character mem : members)
			{
				driftx = Rnd.get( -80, 80);
				drifty = Rnd.get( -80, 80);
				mem.teleToLocation(27734 + driftx, -6938 + drifty, -1982);
			}
		}
		_isAlreadyEnteredOtherParty = true;
	}

	// When annihilating or limit of time coming, the compulsion movement players from the sailren's lair.
	public void banishForeigners()
	{
		_isAlreadyEnteredOtherParty = false;
	}

	public static void setLastAttackTime()
	{
		_lastAttackTime = System.currentTimeMillis();
	}

	// Clean up Sailren's lair.
	public void setUnspawn()
	{
		// Eliminate players.
		banishForeigners();

		// Delete teleport cube.
		for(L2NpcInstance cube : _sailrenCube)
		{
			cube.getSpawn().stopRespawn();
			cube.deleteMe();
		}
		_sailrenCube.clear();

		// Not executed tasks is canceled.
		if(_cubeSpawnTask != null)
		{
			_cubeSpawnTask.cancel(true);
			_cubeSpawnTask = null;
		}
		if(_sailrenSpawnTask != null)
		{
			_sailrenSpawnTask.cancel(true);
			_sailrenSpawnTask = null;
		}
		if(_intervalEndTask != null)
		{
			_intervalEndTask.cancel(true);
			_intervalEndTask = null;
		}
		if(_activityTimeEndTask != null)
		{
			_activityTimeEndTask.cancel(true);
			_activityTimeEndTask = null;
		}

		// Init state of sailren's lair.
		_velociraptor = null;
		_pterosaur = null;
		_tyranno = null;
		_sailren = null;

		// Interval begin.
		setIntervalEndTask();
	}

	// Spawn teleport cube.
	public void spawnCube()
	{
		for(L2Spawn spawnDat : _sailrenCubeSpawn)
		{
			_sailrenCube.add(spawnDat.doSpawn(true));
		}
	}

	// Task of teleport cube spawn.
	public void setCubeSpawn()
	{
		_state.setState(EpicBossState.State.DEAD);
		_state.update();

		_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 10000);

	}

	// Task of interval of sailren spawn.
	public void setIntervalEndTask()
	{
		if( !_state.getState().equals(EpicBossState.State.INTERVAL))
		{
			_state.setRespawnDate(Rnd.get(1440, 1440 + 1440));
			_state.setState(EpicBossState.State.INTERVAL);
			_state.update();
		}

		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	// Spawn monster.
	private class SailrenSpawn implements Runnable
	{
		private int _npcId;
		private Location _pos = new Location(27628, -6109, -1982, 44732);

		public SailrenSpawn(int npcId)
		{
			_npcId = npcId;
		}

		public void run()
		{
			switch(_npcId)
			{
				case 22218: // Velociraptor
					_velociraptor = _velociraptorSpawn.doSpawn(true);
					_velociraptor.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, _pos);
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new Social(_velociraptor, 2), 6000);
					if(_activityTimeEndTask != null)
					{
						_activityTimeEndTask.cancel(true);
						_activityTimeEndTask = null;
					}
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(_velociraptor), 120);
					break;
				case 22199: // Pterosaur
					_velociraptorSpawn.stopRespawn();
					_pterosaur = _pterosaurSpawn.doSpawn(true);
					_pterosaur.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, _pos);
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new Social(_pterosaur, 2), 6000);
					if(_activityTimeEndTask != null)
					{
						_activityTimeEndTask.cancel(true);
						_activityTimeEndTask = null;
					}
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(_pterosaur), 120);
					break;
				case 22217: // Tyrannosaurus
					_pterosaurSpawn.stopRespawn();
					_tyranno = _tyrannoSpawn.doSpawn(true);
					_tyranno.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, _pos);
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new Social(_tyranno, 2), 6000);
					if(_activityTimeEndTask != null)
					{
						_activityTimeEndTask.cancel(true);
						_activityTimeEndTask = null;
					}
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(_tyranno), 120);
					break;
				case 29065: // Sailren
					_tyrannoSpawn.stopRespawn();
					_sailren = _sailrenSapwn.doSpawn(true);

					_state.setRespawnDate(Rnd.get(1440, 1440) + 1400);
					_state.setState(EpicBossState.State.ALIVE);
					_state.update();

					_sailren.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, _pos);
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new Social(_sailren, 2), 6000);
					if(_activityTimeEndTask != null)
					{
						_activityTimeEndTask.cancel(true);
						_activityTimeEndTask = null;
					}
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(_sailren), 120);
					break;
			}

			if(_sailrenSpawnTask != null)
			{
				_sailrenSpawnTask.cancel(true);
				_sailrenSpawnTask = null;
			}
		}
	}

	// Spawn teleport cube.
	private class CubeSpawn implements Runnable
	{
		public void run()
		{
			spawnCube();
		}
	}

	// Limit of time coming.
	private class ActivityTimeEnd implements Runnable
	{
		private L2NpcInstance _mob;

		public ActivityTimeEnd(L2NpcInstance npc)
		{
			_mob = npc;
		}

		public void run()
		{
			if( !_mob.isDead())
			{
				_mob.deleteMe();
				_mob.getSpawn().stopRespawn();
				_mob = null;
			}
			// clean up sailren's lair.
			setUnspawn();
		}
	}

	// Interval end.
	private class IntervalEnd implements Runnable
	{
		public void run()
		{
			_state.setState(EpicBossState.State.NOTSPAWN);
			_state.update();
		}
	}

	// Social.
	private class Social implements Runnable
	{
		private int _action;
		private L2NpcInstance _npc;

		public Social(L2NpcInstance npc, int actionId)
		{
			_npc = npc;
			_action = actionId;
		}

		public void run()
		{
			SocialAction sa = new SocialAction(_npc.getObjectId(), _action);
			_npc.broadcastPacket(sa);
		}
	}
}
