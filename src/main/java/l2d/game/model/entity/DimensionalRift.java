package l2d.game.model.entity;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javolution.util.FastList;
import l2d.Config;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.DimensionalRiftManager;
import l2d.game.instancemanager.DimensionalRiftManager.DimensionalRiftRoom;
import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.Reflection;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestState;
import l2d.util.Location;
import l2d.util.Rnd;

public class DimensionalRift extends Reflection
{
	private Integer _roomType;
	L2Party _party;
	FastList<Integer> _completedRooms = new FastList<Integer>();
	private long seconds_5 = 5000L;
	private final int MILLISECONDS_IN_MINUTE = 60000;
	int jumps_current = 0;

	private Timer teleporterTimer;
	private TimerTask teleporterTimerTask;
	private Timer spawnTimer;
	private TimerTask spawnTimerTask;
	private Timer killRiftTimer;
	private TimerTask killRiftTimerTask;

	int _choosenRoom = -1;
	private boolean _hasJumped = false;
	FastList<Integer> deadPlayers = new FastList<Integer>();
	FastList<Integer> revivedInWaitingRoom = new FastList<Integer>();
	private boolean isBossRoom = false;

	public DimensionalRift(L2Party party, int type, int room)
	{
		super(IdFactory.getInstance().getNextId());
		startCollapseTimer(7200000); // 120 минут таймер, для защиты от утечек памяти
		_roomType = type;
		_party = party;
		_choosenRoom = room;
		checkBossRoom(_choosenRoom);

		Location coords = getRoomCoord(room);

		for(L2Player p : party.getPartyMembers())
		{
			teleToLocation(p, coords.rnd(0, 50));
			p.setReflection(this);
		}

		party.setDimensionalRift(this);

		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}

	public int getType()
	{
		return _roomType;
	}

	public int getCurrentRoom()
	{
		return _choosenRoom;
	}

	private void createTeleporterTimer(final boolean reasonTP)
	{
		if(teleporterTimerTask != null)
		{
			teleporterTimerTask.cancel();
			teleporterTimerTask = null;
		}

		if(teleporterTimer != null)
		{
			teleporterTimer.cancel();
			teleporterTimer = null;
		}

		teleporterTimer = new Timer();
		teleporterTimerTask = new TimerTask(){
			@Override
			public void run()
			{
				if(reasonTP && jumps_current < getMaxJumps() && _party.getMemberCount() > deadPlayers.size())
				{
					jumps_current++;

					_completedRooms.add(_choosenRoom);
					_choosenRoom = -1;

					for(L2Player p : _party.getPartyMembers())
						if(!revivedInWaitingRoom.contains(p.getObjectId()))
							teleportToNextRoom(p);
					createTeleporterTimer(true);
					createSpawnTimer(_choosenRoom);
				}
				else
				{
					for(L2Player p : _party.getPartyMembers())
						if(!revivedInWaitingRoom.contains(p.getObjectId()))
							teleportToWaitingRoom(p);
					killRift();
					cancel();

				}
			}
		};
		if(reasonTP)
			teleporterTimer.schedule(teleporterTimerTask, calcTimeToNextJump()); //Teleporter task, 8-10 minutes
		else
			teleporterTimer.schedule(teleporterTimerTask, seconds_5); //incorrect party member invited.
	}

	public void createSpawnTimer(int room)
	{
		if(spawnTimerTask != null)
		{
			spawnTimerTask.cancel();
			spawnTimerTask = null;
		}

		if(spawnTimer != null)
		{
			spawnTimer.cancel();
			spawnTimer = null;
		}

		final DimensionalRiftRoom riftRoom = DimensionalRiftManager.getInstance().getRoom(_roomType, room);

		spawnTimer = new Timer();
		spawnTimerTask = new TimerTask(){
			@Override
			public void run()
			{
				for(L2Spawn s : riftRoom.getSpawns())
				{
					L2Spawn sp = s.clone();
					sp.setReflection(_id);
					addSpawn(sp);
					if(!isBossRoom)
						sp.startRespawn();
					for(int i = 0; i < sp.getAmount(); i++)
						sp.doSpawn(true);
				}
				Quest.addSpawnToInstance(31865, riftRoom.getTeleportCoords(), false, _id);
			}
		};

		spawnTimer.schedule(spawnTimerTask, Config.RIFT_SPAWN_DELAY);
	}

	public void createNewKillRiftTimer()
	{
		if(killRiftTimerTask != null)
		{
			killRiftTimerTask.cancel();
			killRiftTimerTask = null;
		}

		if(killRiftTimer != null)
		{
			killRiftTimer.cancel();
			killRiftTimer = null;
		}

		killRiftTimer = new Timer();
		killRiftTimerTask = new TimerTask(){
			@Override
			public void run()
			{
				for(L2Player p : _party.getPartyMembers())
					if(!revivedInWaitingRoom.contains(p.getObjectId()))
						DimensionalRiftManager.getInstance().teleportToWaitingRoom(p);
				killRift();
			}
		};

		killRiftTimer.schedule(killRiftTimerTask, 100);
	}

	public void partyMemberInvited()
	{
		createTeleporterTimer(false);
	}

	public void partyMemberExited(L2Player player)
	{
		if(deadPlayers.contains(player.getObjectId()))
			deadPlayers.remove(new Integer(player.getObjectId()));

		revivedInWaitingRoom.remove(new Integer(player.getObjectId()));

		if(_party.getMemberCount() < Config.RIFT_MIN_PARTY_SIZE || _party.getMemberCount() == 1)
			createNewKillRiftTimer();
	}

	public void manualTeleport(L2Player player, L2NpcInstance npc)
	{
		if(!player.isInParty() || !player.getParty().isInDimensionalRift())
			return;

		if(player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/rift/NotPartyLeader.htm", npc);
			return;
		}

		if(!isBossRoom)
		{
			if(_hasJumped)
			{
				DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/rift/AllreadyTeleported.htm", npc);
				return;
			}
			_hasJumped = true;
		}

		_completedRooms.add(_choosenRoom);
		_choosenRoom = -1;

		for(L2Player p : _party.getPartyMembers())
			teleportToNextRoom(p);

		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}

	public void manualExitRift(L2Player player, L2NpcInstance npc)
	{
		if(!player.isInParty() || !player.getParty().isInDimensionalRift())
			return;

		if(player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/rift/NotPartyLeader.htm", npc);
			return;
		}

		createNewKillRiftTimer();
	}

	void teleportToNextRoom(L2Player player)
	{
		// Дергается один раз, по идее надо куда-то вынести из цикла
		if(_choosenRoom == -1)
		{
			for(L2Spawn s : getSpawns())
			{
				s.despawnAll();
				s.stopRespawn();
			}

			_choosenRoom = Rnd.get(1, 9);

			// Do not tp in the same room
			ArrayList<Integer> notCompletedRooms = new ArrayList<Integer>();
			for(int i = 1; i <= 9; i++)
				if(!_completedRooms.contains(i))
					notCompletedRooms.add(i);
			if(notCompletedRooms.size() > 0)
				_choosenRoom = notCompletedRooms.get(Rnd.get(notCompletedRooms.size()));

			checkBossRoom(_choosenRoom);
		}

		teleToLocation(player, getRoomCoord(_choosenRoom).rnd(0, 50));
	}

	protected void teleportToWaitingRoom(L2Player player)
	{
		DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
		Quest riftQuest = QuestManager.getQuest(635);
		if(riftQuest != null)
		{
			QuestState qs = player.getQuestState(riftQuest.getName());
			if(qs != null && qs.getInt("cond") == 1)
				qs.set("cond", "0");
		}
	}

	public void teleToLocation(L2Player player, Location loc)
	{
		DimensionalRiftManager.teleToLocation(player, loc.x, loc.y, loc.z, this);
	}

	public void killRift()
	{
		_completedRooms = null;

		if(_party != null)
			_party.setDimensionalRift(null);

		_party = null;
		revivedInWaitingRoom = null;
		deadPlayers = null;
		DimensionalRiftManager.getInstance().killRift(this);
	}

	public Timer getTeleportTimer()
	{
		return teleporterTimer;
	}

	public TimerTask getTeleportTimerTask()
	{
		return teleporterTimerTask;
	}

	public Timer getSpawnTimer()
	{
		return spawnTimer;
	}

	public TimerTask getSpawnTimerTask()
	{
		return spawnTimerTask;
	}

	public Timer getKillRiftTimer()
	{
		return killRiftTimer;
	}

	public TimerTask getKillRiftTimerTask()
	{
		return killRiftTimerTask;
	}

	public void setTeleportTimer(Timer t)
	{
		teleporterTimer = t;
	}

	public void setTeleportTimerTask(TimerTask tt)
	{
		teleporterTimerTask = tt;
	}

	public void setSpawnTimer(Timer t)
	{
		spawnTimer = t;
	}

	public void setSpawnTimerTask(TimerTask st)
	{
		spawnTimerTask = st;
	}

	public void setKillRiftTimer(Timer t)
	{
		killRiftTimer = t;
	}

	public void setKillRiftTimerTask(TimerTask st)
	{
		killRiftTimerTask = st;
	}

	private long calcTimeToNextJump()
	{
		if(isBossRoom)
			return 60 * MILLISECONDS_IN_MINUTE;
		return Config.RIFT_AUTO_JUMPS_TIME * MILLISECONDS_IN_MINUTE + Rnd.get(Config.RIFT_AUTO_JUMPS_TIME_RAND);
	}

	public void memberDead(L2Player player)
	{
		if(!deadPlayers.contains(player.getObjectId()))
			deadPlayers.add(player.getObjectId());

		if(_party.getMemberCount() <= deadPlayers.size())
			createNewKillRiftTimer();
	}

	public void memberRessurected(L2Player player)
	{
		if(deadPlayers.contains(player.getObjectId()))
			deadPlayers.remove(new Integer(player.getObjectId()));
	}

	public void usedTeleport(L2Player player)
	{
		if(!revivedInWaitingRoom.contains(player.getObjectId()))
			revivedInWaitingRoom.add(player.getObjectId());

		if(!deadPlayers.contains(player.getObjectId()))
			deadPlayers.add(player.getObjectId());

		if(_party.getMemberCount() - revivedInWaitingRoom.size() < Config.RIFT_MIN_PARTY_SIZE)
			createNewKillRiftTimer();
	}

	public FastList<Integer> getRevivedAtWaitingRoom()
	{
		return revivedInWaitingRoom;
	}

	public void checkBossRoom(int room)
	{
		isBossRoom = DimensionalRiftManager.getInstance().getRoom(_roomType, room).isBossRoom();
	}

	public Location getRoomCoord(int room)
	{
		return DimensionalRiftManager.getInstance().getRoom(_roomType, room).getTeleportCoords();
	}

	public int getMaxJumps()
	{
		if(Config.RIFT_MAX_JUMPS <= 8 && Config.RIFT_MAX_JUMPS >= 1)
			return Config.RIFT_MAX_JUMPS;
		return 4;
	}

	@Override
	public boolean canChampions()
	{
		return true;
	}
}
