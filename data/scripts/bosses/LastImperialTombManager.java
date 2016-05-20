package bosses;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.ext.listeners.L2ZoneEnterLeaveListener;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.ThreadPoolManager;
import l2d.game.clientpackets.Say2C;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Object;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.Say2;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.DoorTable;
import com.lineage.util.Rnd;

public class LastImperialTombManager extends Functions implements ScriptFile
{
	private static final Logger _log = Logger.getLogger(LastImperialTombManager.class.getName());

	private static LastImperialTombManager _instance;

	private static boolean _isInvaded = false;

	// Instance list of monsters.
	protected static List<L2NpcInstance> _hallAlarmDevices = new FastList<L2NpcInstance>();
	protected static List<L2NpcInstance> _darkChoirPlayers = new FastList<L2NpcInstance>();
	protected static List<L2NpcInstance> _darkChoirCaptains = new FastList<L2NpcInstance>();
	protected static List<L2NpcInstance> _room1Monsters = new FastList<L2NpcInstance>();
	protected static List<L2NpcInstance> _room2InsideMonsters = new FastList<L2NpcInstance>();
	protected static List<L2NpcInstance> _room2OutsideMonsters = new FastList<L2NpcInstance>();

	// Instance list of doors.
	protected static List<L2DoorInstance> _room1Doors = new FastList<L2DoorInstance>();
	protected static List<L2DoorInstance> _room2InsideDoors = new FastList<L2DoorInstance>();
	protected static List<L2DoorInstance> _room2OutsideDoors = new FastList<L2DoorInstance>();
	protected static L2DoorInstance _room3Door = null;

	// Instance list of players.
	protected static List<L2Player> _partyLeaders = new FastList<L2Player>();
	protected static List<L2Player> _registedPlayers = new FastList<L2Player>();
	protected static L2Player _commander = null;

	// Frintezza's Magic Force Field Removal Scroll.
	private final int SCROLL = 8073;

	// Player does reach to HallofFrintezza
	private boolean _isReachToHall = false;

	// Location of invade.
	private final int[][] _invadeLoc = { {173235, -76884, -5107}, {175003, -76933, -5107}, {174196, -76190, -5107}, {174013, -76120, -5107}, {173263, -75161, -5107}};

	// Task
	protected ScheduledFuture<?> _InvadeTask = null;
	protected ScheduledFuture<?> _RegistrationTimeInfoTask = null;
	protected ScheduledFuture<?> _Room1SpawnTask = null;
	protected ScheduledFuture<?> _Room2InsideDoorOpenTask = null;
	protected ScheduledFuture<?> _Room2OutsideSpawnTask = null;
	protected ScheduledFuture<?> _CheckTimeUpTask = null;

	private static String _varName;
	private static L2Zone _zone;
	private static ZoneListener _zoneListener = new ZoneListener();

	private static int LIT_REGISTRATION_MODE = 0;
	private static int LIT_REGISTRATION_TIME = 10;
	private static int LIT_MIN_PARTY_CNT = 4;
	private static int LIT_MAX_PARTY_CNT = 5;
	private static int LIT_MIN_PLAYER_CNT = 7;
	private static int LIT_MAX_PLAYER_CNT = 45;
	private static int LIT_TIME_LIMIT = 35;

	public static class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{}
	}

	// Constructor
	public LastImperialTombManager()
	{}

	// Instance.
	public static LastImperialTombManager getInstance()
	{
		if(_instance == null)
			_instance = new LastImperialTombManager();
		return _instance;
	}

	public int getRandomRespawnDate()
	{
		return 0;
	}

	// Load monsters and close doors.
	public void init()
	{
		_instance = this;
		_varName = "LastImperialTomb";
		_zone = ZoneManager.getInstance().getZoneById(ZoneType.epic, 702002, false);
		_zone.getListenerEngine().addMethodInvokedListener(_zoneListener);

		LastImperialTombSpawnlist.getInstance().clear();
		LastImperialTombSpawnlist.getInstance().fill();
		initDoors();

		_log.info("LastImperialTombManager: Init The Last Imperial Tomb.");
	}

	// Setting list of doors and close doors.
	private void initDoors()
	{
		_room1Doors.clear();
		_room1Doors.add(DoorTable.getInstance().getDoor(25150042));

		for(int i = 25150051; i <= 25150058; i++)
		{
			_room1Doors.add(DoorTable.getInstance().getDoor(i));
		}

		_room2InsideDoors.clear();
		for(int i = 25150061; i <= 25150070; i++)
		{
			_room2InsideDoors.add(DoorTable.getInstance().getDoor(i));
		}
		_room2OutsideDoors.clear();
		_room2OutsideDoors.add(DoorTable.getInstance().getDoor(25150043));
		_room2OutsideDoors.add(DoorTable.getInstance().getDoor(25150045));

		_room3Door = DoorTable.getInstance().getDoor(25150046);

		for(L2DoorInstance door : _room1Doors)
		{
			door.closeMe();
		}

		for(L2DoorInstance door : _room2InsideDoors)
		{
			door.closeMe();
		}

		for(L2DoorInstance door : _room2OutsideDoors)
		{
			door.closeMe();
		}

		_room3Door.closeMe();
	}

	// Return true,tomb was already invaded by players.
	public boolean isInvaded()
	{
		return _isInvaded;
	}

	public boolean isReachToHall()
	{
		return _isReachToHall;
	}

	// RegistrationMode = command channel.
	public boolean tryRegistrationCc(L2Player pc)
	{
		if( !FrintezzaManager.isEnableEnterToLair())
		{
			pc.sendMessage("Currently no entry possible.");
			return false;
		}

		if(isInvaded())
		{
			pc.sendMessage("Another group is already fighting inside the imperial tomb.");
			return false;
		}

		if(_commander == null)
		{
			if(pc.getParty() != null)
			{
				if(pc.getParty().getCommandChannel() != null)
				{
					if(pc.getParty().getCommandChannel().getChannelLeader() == pc && pc.getParty().getCommandChannel().getParties().size() >= LIT_MIN_PARTY_CNT && pc.getParty().getCommandChannel().getParties().size() <= LIT_MAX_PARTY_CNT && pc.getInventory().getCountOf(SCROLL) >= 1)
						return true;
				}
			}
			pc.sendMessage("You must be the commander of a party channel and possess a \"Frintezza's Magic Force Field Removal Scroll\"."); // Retail messages?
			return false;
		}
		else
		{
			pc.sendMessage("There can only one command channel register at the same time."); // Retail messages?
			return false;
		}
	}

	// RegistrationMode = party.
	public boolean tryRegistrationPt(L2Player pc)
	{
		if( !FrintezzaManager.isEnableEnterToLair())
		{
			pc.sendMessage("Currently no entry possible."); // Retail messages?
			return false;
		}

		if(isInvaded())
		{
			pc.sendMessage("Another group is already fighting inside the imperial tomb."); // Retail messages?
			return false;
		}

		if(_partyLeaders.size() < LIT_MAX_PARTY_CNT)
		{
			if(pc.getParty() != null)
			{
				if(pc.getParty().getPartyLeader() == pc && pc.getInventory().getCountOf(SCROLL) >= 1)
					return true;
			}
		}

		pc.sendMessage("You must be the leader of a party and possess a \"Frintezza's Magic Force Field Removal Scroll\"."); // Retail messages?
		return false;
	}

	public void unregisterPt(L2Party pt)
	{
		if(_partyLeaders.contains(pt.getPartyLeader()))
		{
			_partyLeaders.remove(pt.getPartyLeader());
			pt.getPartyLeader().sendMessage("Warning: Unregistered from last imperial tomb invasion."); // Retail messages?
		}
	}

	public void unregisterPc(L2Player pc)
	{
		if(_registedPlayers.contains(pc))
		{
			_registedPlayers.remove(pc);
			pc.sendMessage("Warning: Unregistered from last imperial tomb invasion."); // Retail messages?
		}
	}

	// RegistrationMode = single.
	public boolean tryRegistrationPc(L2Player pc)
	{
		if( !FrintezzaManager.isEnableEnterToLair())
		{
			pc.sendMessage("Currently no entry possible."); // Retail messages?
			return false;
		}

		if(_registedPlayers.contains(pc))
		{
			pc.sendMessage("You are already registered."); // Retail messages?
			return false;
		}

		if(isInvaded())
		{
			pc.sendMessage("Another group is already fighting inside the imperial tomb."); // Retail messages?
			return false;
		}

		if(_registedPlayers.size() < LIT_MAX_PLAYER_CNT)
			if(pc.getInventory().getCountOf(SCROLL) >= 1)
				return true;

		pc.sendMessage("You have to possess a \"Frintezza's Magic Force Field Removal Scroll\"."); // Retail messages?
		return false;
	}

	// Registration to enter to tomb.
	public synchronized void registration(L2Player pc, L2NpcInstance npc)
	{
		switch(LIT_REGISTRATION_MODE)
		{
			case 0:
			{
				if(_commander != null)
					return;
				_commander = pc;
				if(_InvadeTask != null)
					_InvadeTask.cancel(true);
				_InvadeTask = ThreadPoolManager.getInstance().scheduleGeneral(new Invade(), 10000);
				break;
			}
			case 1:
			{
				if(_partyLeaders.contains(pc))
					return;
				_partyLeaders.add(pc);

				if(_partyLeaders.size() == 1)
					_RegistrationTimeInfoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AnnouncementRegstrationInfo(npc, LIT_REGISTRATION_TIME * 60000), 1000);
				break;
			}
			case 2:
			{
				if(_registedPlayers.contains(pc))
					return;
				_registedPlayers.add(pc);
				if(_registedPlayers.size() == 1)
					_RegistrationTimeInfoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AnnouncementRegstrationInfo(npc, LIT_REGISTRATION_TIME * 60000), 1000);
				break;
			}
			default:
				_log.warning("LastImperialTombManager: Invalid Registration Mode!");
		}
	}

	// Announcement of remaining time of registration to players.
	protected void doAnnouncementRegstrationInfo(L2NpcInstance npc, int remaining)
	{
		Say2 cs = null;

		if(remaining == (LIT_REGISTRATION_TIME * 60000))
		{
			cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "Entrance is now possible.");
			npc.broadcastPacket(cs);
		}

		if(remaining >= 10000)
		{
			if(remaining > 60000)
			{
				cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), (remaining / 60000) + " minute(s) left for entrance.");
				npc.broadcastPacket(cs);
				remaining = remaining - 60000;

				switch(LIT_REGISTRATION_MODE)
				{
					case 1:
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "For entrance, at least " + LIT_MIN_PARTY_CNT + " parties are needed.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), LIT_MAX_PARTY_CNT + " is the maximum party count.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "The current number of registered parties is " + _partyLeaders.size() + ".");
						npc.broadcastPacket(cs);
						break;
					case 2:
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "For entrance, at least " + LIT_MIN_PLAYER_CNT + " people are needed.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), LIT_MAX_PLAYER_CNT + " is the capacity.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), _registedPlayers.size() + " people are currently registered.");
						npc.broadcastPacket(cs);
						break;
				}

				if(_RegistrationTimeInfoTask != null)
					_RegistrationTimeInfoTask.cancel(true);
				_RegistrationTimeInfoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AnnouncementRegstrationInfo(npc, remaining), 60000);
			}
			else
			{
				cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), (remaining / 60000) + " minute(s) left for entrance.");
				npc.broadcastPacket(cs);
				remaining = remaining - 10000;

				switch(LIT_REGISTRATION_MODE)
				{
					case 1:
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "For entrance, at least " + LIT_MIN_PARTY_CNT + " parties are needed.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), LIT_MAX_PARTY_CNT + " is the maximum party count.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "The current number of registered parties is " + _partyLeaders.size() + ".");
						npc.broadcastPacket(cs);
						break;
					case 2:
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "For entrance, at least " + LIT_MIN_PLAYER_CNT + " people are needed.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), LIT_MAX_PLAYER_CNT + " is the capacity.");
						npc.broadcastPacket(cs);
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), _registedPlayers.size() + " people are currently registered.");
						npc.broadcastPacket(cs);
						break;
				}

				if(_RegistrationTimeInfoTask != null)
					_RegistrationTimeInfoTask.cancel(true);
				_RegistrationTimeInfoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AnnouncementRegstrationInfo(npc, remaining), 10000);
			}
		}
		else
		{
			cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "Entrance period ended.");
			npc.broadcastPacket(cs);

			switch(LIT_REGISTRATION_MODE)
			{
				case 1:
					if((_partyLeaders.size() < LIT_MIN_PARTY_CNT) || (_partyLeaders.size() > LIT_MAX_PARTY_CNT))
					{
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "Since the conditions were not met, the entrance was refused.");
						npc.broadcastPacket(cs);
						return;
					}
					break;
				case 2:
					if((_registedPlayers.size() < LIT_MIN_PARTY_CNT) || (_registedPlayers.size() > LIT_MAX_PLAYER_CNT))
					{
						cs = new Say2(npc.getObjectId(), Say2C.SHOUT, npc.getName(), "Since the conditions were not met, the entrance was refused.");
						npc.broadcastPacket(cs);
						return;
					}
					break;
			}

			if(_RegistrationTimeInfoTask != null)
				_RegistrationTimeInfoTask.cancel(true);

			if(_InvadeTask != null)
				_InvadeTask.cancel(true);
			_InvadeTask = ThreadPoolManager.getInstance().scheduleGeneral(new Invade(), 10000);
		}
	}

	// Invade to tomb.
	public void doInvade()
	{
		initDoors();

		switch(LIT_REGISTRATION_MODE)
		{
			case 0:
				doInvadeCc();
				break;
			case 1:
				doInvadePt();
				break;
			case 2:
				doInvadePc();
				break;
			default:
				_log.warning("LastImperialTombManager: Invalid Registration Mode!");
				return;
		}

		_isInvaded = true;

		if(_Room1SpawnTask != null)
			_Room1SpawnTask.cancel(true);
		_Room1SpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnRoom1Mobs1st(), 15000);

		if(_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);
		_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(LIT_TIME_LIMIT * 60000), 15000);
	}

	private void doInvadeCc()
	{
		int locId = 0;

		if(_commander.getInventory().getCountOf(SCROLL) < 1)
		{
			_commander.sendPacket(new SystemMessage(SystemMessage.INCORRECT_ITEM_COUNT));

			_commander.sendMessage("Since the conditions were not met, the entrance was refused.");

			return;
		}

		_commander.getInventory().destroyItemByItemId(SCROLL, 1, true);

		for(L2Party pt : _commander.getParty().getCommandChannel().getParties())
		{
			if(locId >= 5)
				locId = 0;

			for(L2Player pc : pt.getPartyMembers())
			{
				pc.teleToLocation(_invadeLoc[locId][0] + Rnd.get(50), _invadeLoc[locId][1] + Rnd.get(50), _invadeLoc[locId][2]);
			}

			locId++;
		}
	}

	private void doInvadePt()
	{
		int locId = 0;
		boolean isReadyToInvade = true;

		SystemMessage sm = new SystemMessage(SystemMessage.S1);
		sm.addString("Since the conditions were not met, the entrance was refused.");
		for(L2Player ptl : _partyLeaders)
		{
			if(ptl.getInventory().getCountOf(SCROLL) < 1)
			{
				ptl.sendPacket(new SystemMessage(SystemMessage.INCORRECT_ITEM_COUNT));
				ptl.sendPacket(sm);

				isReadyToInvade = false;
			}
		}

		if( !isReadyToInvade)
		{
			for(L2Player ptl : _partyLeaders)
			{
				ptl.sendPacket(sm);
			}

			return;
		}

		for(L2Player ptl : _partyLeaders)
		{
			ptl.getInventory().destroyItemByItemId(SCROLL, 1, true);
		}

		for(L2Player ptl : _partyLeaders)
		{
			if(locId >= 5)
				locId = 0;

			for(L2Player pc : ptl.getParty().getPartyMembers())
			{
				pc.teleToLocation(_invadeLoc[locId][0] + Rnd.get(50), _invadeLoc[locId][1] + Rnd.get(50), _invadeLoc[locId][2]);
			}

			locId++;
		}
	}

	private void doInvadePc()
	{
		int locId = 0;
		boolean isReadyToInvade = true;

		for(L2Player pc : _registedPlayers)
		{
			if(pc.getInventory().getCountOf(SCROLL) < 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.INCORRECT_ITEM_COUNT);
				pc.sendPacket(sm);

				sm = new SystemMessage(SystemMessage.S1);
				sm.addString("Since the conditions were not met, the entrance was refused.");
				pc.sendPacket(sm);

				isReadyToInvade = false;
			}
		}

		if( !isReadyToInvade)
		{
			for(L2Player pc : _registedPlayers)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S1);
				sm.addString("Since the conditions were not met, the entrance was refused.");
				pc.sendPacket(sm);
			}

			return;
		}

		for(L2Player pc : _registedPlayers)
		{
			pc.getInventory().destroyItemByItemId(SCROLL, 1, true);
		}

		for(L2Player pc : _registedPlayers)
		{
			if(locId >= 5)
				locId = 0;

			pc.teleToLocation(_invadeLoc[locId][0] + Rnd.get(50), _invadeLoc[locId][1] + Rnd.get(50), _invadeLoc[locId][2]);

			locId++;
		}
	}

	public void onKillHallAlarmDevice()
	{
		int killCnt = 0;

		for(L2NpcInstance HallAlarmDevice : _hallAlarmDevices)
		{
			if(HallAlarmDevice.isDead())
				killCnt++;
		}

		switch(killCnt)
		{
			case 1:
				if(Rnd.get(100) < 10)
				{
					openRoom1Doors();
					openRoom2OutsideDoors();
					spawnRoom2InsideMob();
				}
				else
				{
					if(_Room1SpawnTask != null)
						_Room1SpawnTask.cancel(true);

					_Room1SpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnRoom1Mobs2nd(), 3000);
				}
				break;
			case 2:
				if(Rnd.get(100) < 20)
				{
					openRoom1Doors();
					openRoom2OutsideDoors();
					spawnRoom2InsideMob();
				}
				else
				{
					if(_Room1SpawnTask != null)
						_Room1SpawnTask.cancel(true);

					_Room1SpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnRoom1Mobs3rd(), 3000);
				}
				break;
			case 3:
				if(Rnd.get(100) < 30)
				{
					openRoom1Doors();
					openRoom2OutsideDoors();
					spawnRoom2InsideMob();
				}
				else
				{
					if(_Room1SpawnTask != null)
						_Room1SpawnTask.cancel(true);

					_Room1SpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnRoom1Mobs4th(), 3000);
				}
				break;
			case 4:
				openRoom1Doors();
				openRoom2OutsideDoors();
				spawnRoom2InsideMob();
				break;
			default:
				break;
		}
	}

	public void onKillDarkChoirPlayer()
	{
		int killCnt = 0;

		for(L2NpcInstance DarkChoirPlayer : _room2InsideMonsters)
		{
			if(DarkChoirPlayer.isDead())
				killCnt++;
		}

		if(_room2InsideMonsters.size() <= killCnt)
		{
			if(_Room2InsideDoorOpenTask != null)
				_Room2InsideDoorOpenTask.cancel(true);
			if(_Room2OutsideSpawnTask != null)
				_Room2OutsideSpawnTask.cancel(true);

			_Room2InsideDoorOpenTask = ThreadPoolManager.getInstance().scheduleGeneral(new OpenRoom2InsideDoors(), 3000);
			_Room2OutsideSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnRoom2OutsideMobs(), 4000);
		}
	}

	public void onKillDarkChoirCaptain()
	{
		int killCnt = 0;

		for(L2NpcInstance DarkChoirCaptain : _darkChoirCaptains)
		{
			if(DarkChoirCaptain.isDead())
				killCnt++;
		}

		if(_darkChoirCaptains.size() <= killCnt)
		{
			openRoom2OutsideDoors();

			for(L2NpcInstance mob : _room2OutsideMonsters)
			{
				mob.deleteMe();
				mob.getSpawn().stopRespawn();
			}

			for(L2NpcInstance DarkChoirCaptain : _darkChoirCaptains)
			{
				DarkChoirCaptain.deleteMe();
				DarkChoirCaptain.getSpawn().stopRespawn();
			}
		}
	}

	private void openRoom1Doors()
	{
		for(L2NpcInstance npc : _hallAlarmDevices)
		{
			npc.deleteMe();
			npc.getSpawn().stopRespawn();
		}

		for(L2NpcInstance npc : _room1Monsters)
		{
			npc.deleteMe();
			npc.getSpawn().stopRespawn();
		}

		for(L2DoorInstance door : _room1Doors)
		{
			door.openMe();
		}
	}

	protected void openRoom2InsideDoors()
	{
		for(L2DoorInstance door : _room2InsideDoors)
		{
			door.openMe();
		}
	}

	protected void openRoom2OutsideDoors()
	{
		for(L2DoorInstance door : _room2OutsideDoors)
		{
			door.openMe();
		}
		_room3Door.openMe();
	}

	protected void closeRoom2OutsideDoors()
	{
		for(L2DoorInstance door : _room2OutsideDoors)
		{
			door.closeMe();
		}
		_room3Door.closeMe();
	}

	private void spawnRoom2InsideMob()
	{
		L2NpcInstance mob;
		for(L2Spawn spawn : LastImperialTombSpawnlist.getInstance().getRoom2InsideSpawnList())
		{
			mob = spawn.doSpawn(true);
			mob.getSpawn().stopRespawn();
			_room2InsideMonsters.add(mob);
		}
	}

	public void setReachToHall()
	{
		_isReachToHall = true;
	}

	protected void doCheckTimeUp(int remaining)
	{
		if(_isReachToHall)
			return;

		Say2 cs = null;
		int timeLeft;
		int interval;

		if(remaining > 300000)
		{
			timeLeft = remaining / 60000;
			interval = 300000;
			cs = new Say2(0, Say2C.ALLIANCE, "Notice", timeLeft + " minutes left.");
			remaining = remaining - 300000;
		}
		else if(remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
			cs = new Say2(0, Say2C.ALLIANCE, "Notice", timeLeft + " minutes left.");
			remaining = remaining - 60000;
		}
		else if(remaining > 30000)
		{
			timeLeft = remaining / 1000;
			interval = 30000;
			cs = new Say2(0, Say2C.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining = remaining - 30000;
		}
		else
		{
			timeLeft = remaining / 1000;
			interval = 10000;

			cs = new Say2(0, Say2C.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining = remaining - 10000;
		}

		for(L2Player pc : getPlayersInside())
		{
			pc.sendPacket(cs);
		}

		if(_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);
		if(remaining >= 10000)
			_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(remaining), interval);
		else
			_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), interval);
	}

	protected void cleanUpTomb()
	{
		initDoors();
		cleanUpMobs();
		banishForeigners();
		cleanUpRegister();
		_isInvaded = false;
		_isReachToHall = false;

		if(_InvadeTask != null)
			_InvadeTask.cancel(true);
		if(_RegistrationTimeInfoTask != null)
			_RegistrationTimeInfoTask.cancel(true);
		if(_Room1SpawnTask != null)
			_Room1SpawnTask.cancel(true);
		if(_Room2InsideDoorOpenTask != null)
			_Room2InsideDoorOpenTask.cancel(true);
		if(_Room2OutsideSpawnTask != null)
			_Room2OutsideSpawnTask.cancel(true);
		if(_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);

		_InvadeTask = null;
		_RegistrationTimeInfoTask = null;
		_Room1SpawnTask = null;
		_Room2InsideDoorOpenTask = null;
		_Room2OutsideSpawnTask = null;
		_CheckTimeUpTask = null;
	}

	// Delete all mobs from tomb.
	private void cleanUpMobs()
	{
		for(L2NpcInstance mob : _hallAlarmDevices)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_hallAlarmDevices.clear();

		for(L2NpcInstance mob : _darkChoirPlayers)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_darkChoirPlayers.clear();

		for(L2NpcInstance mob : _darkChoirCaptains)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_darkChoirCaptains.clear();

		for(L2NpcInstance mob : _room1Monsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_room1Monsters.clear();

		for(L2NpcInstance mob : _room2InsideMonsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_room2InsideMonsters.clear();

		for(L2NpcInstance mob : _room2OutsideMonsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_room2OutsideMonsters.clear();
	}

	private void cleanUpRegister()
	{
		_commander = null;
		_partyLeaders.clear();
		_registedPlayers.clear();
	}

	private class SpawnRoom1Mobs1st implements Runnable
	{
		public void run()
		{
			L2NpcInstance mob;
			for(L2Spawn spawn : LastImperialTombSpawnlist.getInstance().getRoom1SpawnList1st())
			{
				if(spawn.getNpcId() == 18328)
				{
					mob = spawn.doSpawn(true);
					mob.getSpawn().stopRespawn();
					_hallAlarmDevices.add(mob);
				}
				else
				{
					mob = spawn.doSpawn(true);
					mob.getSpawn().stopRespawn();
					_room1Monsters.add(mob);
				}
			}
		}
	}

	private class SpawnRoom1Mobs2nd implements Runnable
	{
		public void run()
		{
			L2NpcInstance mob;
			for(L2Spawn spawn : LastImperialTombSpawnlist.getInstance().getRoom1SpawnList2nd())
			{
				mob = spawn.doSpawn(true);
				mob.getSpawn().stopRespawn();
				_room1Monsters.add(mob);
			}
		}
	}

	private class SpawnRoom1Mobs3rd implements Runnable
	{
		public void run()
		{
			L2NpcInstance mob;
			for(L2Spawn spawn : LastImperialTombSpawnlist.getInstance().getRoom1SpawnList3rd())
			{
				mob = spawn.doSpawn(true);
				mob.getSpawn().stopRespawn();
				_room1Monsters.add(mob);
			}
		}
	}

	private class SpawnRoom1Mobs4th implements Runnable
	{
		public void run()
		{
			L2NpcInstance mob;
			for(L2Spawn spawn : LastImperialTombSpawnlist.getInstance().getRoom1SpawnList4th())
			{
				mob = spawn.doSpawn(true);
				mob.getSpawn().stopRespawn();
				_room1Monsters.add(mob);
			}
		}
	}

	private class OpenRoom2InsideDoors implements Runnable
	{
		public void run()
		{
			closeRoom2OutsideDoors();
			openRoom2InsideDoors();
		}
	}

	private class SpawnRoom2OutsideMobs implements Runnable
	{
		public void run()
		{
			for(L2Spawn spawn : LastImperialTombSpawnlist.getInstance().getRoom2OutsideSpawnList())
			{
				if(spawn.getNpcId() == 18334)
				{
					L2NpcInstance mob = spawn.doSpawn(true);
					mob.getSpawn().stopRespawn();
					_darkChoirCaptains.add(mob);
				}
				else
				{
					L2NpcInstance mob = spawn.doSpawn(true);
					mob.getSpawn().startRespawn();
					_room2OutsideMonsters.add(mob);
				}
			}
		}
	}

	private class AnnouncementRegstrationInfo implements Runnable
	{
		private L2NpcInstance _npc = null;
		private final int _remaining;

		public AnnouncementRegstrationInfo(L2NpcInstance npc, int remaining)
		{
			_npc = npc;
			_remaining = remaining;
		}

		public void run()
		{
			doAnnouncementRegstrationInfo(_npc, _remaining);
		}
	}

	private class Invade implements Runnable
	{
		public void run()
		{
			doInvade();
		}
	}

	private class CheckTimeUp implements Runnable
	{
		private final int _remaining;

		public CheckTimeUp(int remaining)
		{
			_remaining = remaining;
		}

		public void run()
		{
			doCheckTimeUp(_remaining);
		}
	}

	private class TimeUp implements Runnable
	{
		public void run()
		{
			cleanUpTomb();
		}
	}

	// When the party is annihilated, they are banished.
	public void checkAnnihilated()
	{
		if(isPlayersAnnihilated())
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					cleanUpTomb();
				}
			}, 5000);
		}
	}

	private static void banishForeigners()
	{
		for(L2Player player : getPlayersInside())
		{
			if(_varName != null)
			{
				QuestState qs = player.getQuestState(_varName);
				if(qs != null && qs.getInt("ok") == 1)
					qs.exitCurrentQuest(true);
			}
			player.teleToClosestTown();
		}
	}

	private static List<L2Player> getPlayersInside()
	{
		return getZone().getInsidePlayers();
	}

	public synchronized boolean isPlayersAnnihilated()
	{
		for(L2Player pc : getPlayersInside())
		{
			if( !pc.isDead())
				return false;
		}
		return true;
	}

	public static L2Zone getZone()
	{
		return _zone;
	}

	@Override
	public void onLoad()
	{
		init();
	}

	@Override
	public void onReload()
	{}

	@Override
	public void onShutdown()
	{}
}
