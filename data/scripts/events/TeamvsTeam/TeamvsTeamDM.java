package events.TeamvsTeam;

import static l2d.game.model.L2Zone.ZoneType.OlympiadStadia;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.clientpackets.Say2C;
import l2d.game.instancemanager.TownManager;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2World;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.Earthquake;
import l2d.game.serverpackets.ExEventMatchMessage;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.PlaySound;
import l2d.game.serverpackets.Say2;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.DoorTable;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SpawnTable;
import l2d.util.GArray;
import l2d.util.GCSArray;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * 
 * @author Midnex
 *
 */
public class TeamvsTeamDM extends Functions implements ScriptFile
{
	private static GCSArray<Integer> _players_list;
	private static GCSArray<Integer> _Blue;
	private static GCSArray<Integer> _Red;
	
	private static GCSArray<Location> _buffers;
	private static GCSArray<L2Spawn> _buffers_spawn;

	private static GCSArray<Location> _gates;
	private static GCSArray<L2Spawn> _gates_spawn;
	private static GCSArray<Location> _respawns;
	private static GArray<teamBalance> _balance;
	
	private static int _blue_score;
	private static int _red_score;

	private static ScheduledFuture<?> _checknulls;

	static boolean started = false;
	static boolean reg = false;
	
	private static L2Skill _nobless = SkillTable.getInstance().getInfo(1323, 1);


	private static L2Zone _zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.battle_zone, 5, true);

	public static void say(String message)
	{
		for(L2Player pl : getPlayers(_players_list))
			pl.sendPacket(new Say2(0, Say2C.ANNOUNCEMENT, "[TvT]", message));
	}
	
	public static void sayAll(String message)
	{
		for(L2Player pl : L2World.getAllPlayers())
			pl.sendPacket(new Say2(0, Say2C.ANNOUNCEMENT, "[TvT]", message));
	}

	public static void say1(int i, String message)
	{
		for(L2Player pl : getPlayers(_players_list))
		{
			if(i==0)
			{
				pl.sendPacket(new ExEventMatchMessage(i, message));
				if(message.contains("FIGHT"))
				  pl.sendPacket(new PlaySound("ItemSound3.sys_battle_start"));
				else
				  pl.sendPacket(new PlaySound("ItemSound3.sys_battle_count"));
			}
			pl.sendPacket(new ExEventMatchMessage(i, message));
		}
	}

	public static void question()
	{
		reg = true;
		EventScheduler.setNextEventTime(0);
		EventScheduler.setEventName(0);

		for(L2Player player : L2World.getAllPlayers())
		{
			if(player != null && player.getLevel() >= 76)
			{
				player.sendPacket(new MagicSkillUse(player, player, 5006, 1, 0, 0));
				//player.sendPacket(new PlaySound("ItemSound3.sys_pledge_join"));
				player.scriptRequest("Register to the event : [ Impulse TeamDeathMatch ]", "events.TeamvsTeam.TeamvsTeamDM:addPlayer", new Object[0]);
			}
		}
	}

	public static void init()
	{
		_players_list = new GCSArray<Integer>();
		_Blue = new GCSArray<Integer>();
		_Red = new GCSArray<Integer>();
		_balance = new GArray<teamBalance>();
		_gates_spawn = new GCSArray<L2Spawn>();
		
		_balance.add(new teamBalance());
		_balance.add(new teamBalance());
		
		_blue_score = 0;
		_red_score = 0;

		sayAll("Registration to [ Impulse TeamDeathMatch ] event is now open! Type '.join' to register.");
		question();
		executeTask("events.TeamvsTeam.TeamvsTeamDM", "begin", new Object[0], 31000);
	}

	public static void begin()
	{
		if(_players_list.size() < 4)
		{
			sayAll("[ Impulse TeamDeathMatch ] - Canceled due lack of players.");
			_players_list = new GCSArray<Integer>();
			reg = false;
			EventScheduler.setEvent(false);
			return;
		}
		
		sayAll("[ Impulse TeamDeathMatch ] is starting now.");

		reg = false;		
		EventScheduler.setNextEventTime(-1);
		started = true;

		clearArena();
		checkforDcs();
		executeTask("events.TeamvsTeam.TeamvsTeamDM", "run", new Object[0], 10000);
		SaveCoords();
		run();
	}

	private static void run()
	{
		for(Integer pl : _players_list)
		{
			L2Player player = L2World.getPlayer(pl);
			if(player == null)
				_players_list.remove(pl);
			player.resetEventPoints();
			player.leaveParty();
			balancePlayer(pl, player.getClassId().getId());
		}
		_balance = null;
		event();
		end();
	}
	
	private static boolean event()
	{
		closeGates();
		resPlayers();
		healPlayers();
		portPlayersToStart();
		sleep(20);
		say("20 seconds till buffers spawn.");
		sleep(20);
		spawnBuffers();
		say("30 seconds to buff up.");
		sleep(30);
		healPlayers();
		sleep(1);
		unspawnBuffers();
		sleep(1);
		beginCountdown();
		openGatesAnimation();
		sleep(1);
		openGates();
		say1(0,"FIGHT");
		return respawnLoop();
	}
	
	public static boolean respawnLoop()
	{
		int respawns = 0;
		say("Event will last 10 minutes.");
		while(started)
		{
			sleep(10);
			respawns++;
			respawnPlayers();

			if(respawns % 3 == 0)
			{
				say("Blue " + _blue_score + " : " + _red_score + " Red");
				if(respawns % 6 == 0)
				{
					say("Time left - "+(10-(respawns/6))+" minutes.");
					killnotinbattle();
				}
			}
			if(respawns == 60)
			{
				winner();
				end();
			}
		}
		return true;
	}
	
	public static void openGatesAnimation()
	{
		for(L2Spawn sp : _gates_spawn)
			sp.getLastSpawn().broadcastPacket(new MagicSkillUse(sp.getLastSpawn(), sp.getLastSpawn(), 347, 1, 0, 0));
	}


	public static void openGates()
	{
		for(L2Spawn sp : _gates_spawn)
		{			
			sp.getLastSpawn().broadcastPacket(new MagicSkillUse(sp.getLastSpawn(), sp.getLastSpawn(), 347, 1, 0, 0));
			sp.getLastSpawn().deleteMe();
		}
		_gates_spawn = null;
		DoorTable.getInstance().getDoor(25190013).openMe();
		DoorTable.getInstance().getDoor(25190014).openMe();
	}
	
	public static void closeGates()
	{
		for(Location loc : _gates)
		{
			try
			{
				L2Spawn _gatesSpawn;
				_gatesSpawn = new L2Spawn(NpcTable.getTemplate(21));
				_gatesSpawn.setAmount(1);
				_gatesSpawn.setLoc(loc);
				_gatesSpawn.stopRespawn();
				SpawnTable.getInstance().addNewSpawn(_gatesSpawn, false);
				_gates_spawn.add(_gatesSpawn);
				_gatesSpawn.doSpawn(true);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		DoorTable.getInstance().getDoor(25190013).closeMe();
		DoorTable.getInstance().getDoor(25190014).closeMe();
	}

	private static void end()
	{
		say1(3,"3");
		say("Thank you for joining our event!");
		
		if(_checknulls != null)
		{
			_checknulls.cancel(true);
			_checknulls = null;
		}
		for(L2Player pl : getPlayers(_players_list))
		{
			if(pl.isDead())
				pl.doRevive();
			TeleportPlayerToSaveCoords(pl);
			pl.setTeam(0,false);
		}

		_players_list = new GCSArray<Integer>();
		_Blue = new GCSArray<Integer>();
		_Red = new GCSArray<Integer>();
		_blue_score = 0;
		_red_score = 0;
		started = false;
		EventScheduler.setEvent(false);
	}

	private static boolean winner()
	{
		boolean weapons = Rnd.chance(50);
		if(_blue_score > _red_score)
		{
			for(L2Player pl : getPlayers(_Blue))
				addItem(pl, weapons ? 959 : 960, 1);
			sayAll("Blue has won this impulse TeamDeathMatch.");
		}
		else
		{
			if(_blue_score * 2 >= _red_score)
			{
				for(L2Player pl : getPlayers(_Blue))
					addItem(pl, weapons ? 729 : 730, 1);
			}
		}

		
		if(_blue_score < _red_score)
		{
			for(L2Player pl : getPlayers(_Red))
				addItem(pl, weapons ? 959 : 960, 1);
			sayAll("Red has won this impulse TeamDeathMatch.");
		}
		else
		{
			if(_red_score * 2 >= _blue_score)
			{
				for(L2Player pl : getPlayers(_Blue))
					addItem(pl, weapons ? 729 : 730, 1);
			}
		}
		
		
		for(L2Player all : getPlayers(_players_list))
			addItem(all, 7917, 1);

		Object[] players = getPlayers(_players_list).toArray();
		Arrays.sort(players, TOP.getInstance());

		int placea = 0;
		sayAll("Top 5 in current event:");
		for(Object place : players)
		{
			placea++;
			if(placea == 6)
				break;

			L2Player player = (L2Player) place;
			if(player != null)
				sayAll(placea + "# " + player.getName() + " [kills : " + player.getEventPoints() + "]");
		}
		return true;
	}

	public static boolean beginCountdown()
	{
		for(int i = 10; i > 0; i--)
		{
			if(i == 5)
			{
				for(L2Player player : getPlayers(_players_list))
					player.broadcastPacket(new Earthquake(player.getLoc(), 15, 8));
			}
			say1(0, " Gate opens in - " + i + " ");
			sleep(1);
		}
		return true;
	}

	private static void killnotinbattle()
	{
		for(L2Player pl : getPlayers(_players_list))
			if(!pl.isInZone(_zone))
				pl.doDie(pl);
	}

	public static void portPlayersToStart()
	{
		for(L2Player t2 : getPlayers(_Red))
		{
			Location pos = Rnd.coordsRandomize(92299,-123450,-4256, 0, 10, 100);
			t2.teleToLocation(pos);
			t2.setTeam(2,true);
			t2.getEffectList().stopAllEffects();
			_nobless.getEffects(t2, t2, false, false);
		}

		for(L2Player t1 : getPlayers(_Blue))
		{
			Location pos = Rnd.coordsRandomize(93044,-117838,-4160, 0, 10, 100);
			t1.teleToLocation(pos);
			t1.setTeam(1,true);
			t1.getEffectList().stopAllEffects();
			_nobless.getEffects(t1, t1, false, false);
		}
	}
	
	public static void respawnPlayers()
	{
		Location blueResp = _respawns.get(Rnd.get(_respawns.size()));
		Location redResp = _respawns.get(Rnd.get(_respawns.size()));
		for(L2Player player : getPlayers(_players_list))
		{
			if(!player.isDead())
				continue;
			
			Location pos = Rnd.coordsRandomize(player.getTeam() == 1 ? blueResp : redResp, 10, 100);
			player.teleToLocation(pos);
			player.setCurrentHp(player.getMaxHp(), true);
			player.doRevive();
			_nobless.getEffects(player, player, false, false);
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.cleanDebuffs();
		}
	}


	public static void healPlayers()
	{
		for(L2Player pl : getPlayers(_players_list))
		{
			pl.setCurrentHpMp(pl.getMaxHp(), pl.getMaxMp());
			pl.setCurrentCp(pl.getMaxCp());
			pl.resetSkillsReuse();

			boolean gotbuff = false;
			for(L2NpcInstance buff : pl.getAroundNpc(600, 100))
			{
				if(gotbuff)
					continue;

				gotbuff = true;
				pl.broadcastPacket(new MagicSkillUse(buff, pl, 1217, 1, 0, 0));
			}
		}
	}

	public static void resPlayers()
	{
		for(L2Player pl : getPlayers(_players_list))
			if(pl.isDead())
				pl.doRevive();
	}

	public static void checkforDcs()
	{
		_checknulls = addTask("checkforDcs", 2000);

		for(Integer ObjId : _Red)
		{
			L2Player player = L2World.getPlayer(ObjId);
			if(player == null)
			{
				_Red.remove(ObjId);
				_players_list.remove(ObjId);
			}
		}

		for(Integer ObjId : _Blue)
		{
			L2Player player = L2World.getPlayer(ObjId);
			if(player == null)
			{
				_Blue.remove(ObjId);
				_players_list.remove(ObjId);
			}
		}
	}

	public static ScheduledFuture<?> addTask(String task, Integer time)
	{
		return executeTask("events.TeamvsTeam.TeamvsTeamDM", task, new Object[0], time);
	}

	public static void SaveCoords()
	{
		for(L2Player player : getPlayers(_players_list))
			player.setVar("EventBackCoords", player.getX() + " " + player.getY() + " " + player.getZ());
	}

	public static void TeleportPlayerToSaveCoords(L2Player player)
	{
		final String var = player.getVar("EventBackCoords");
		final String[] coords = var.split(" ");

		player.abortCast();
		player.abortAttack();
		player.stopMove();

		player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));

		player.unsetVar("EventBackCoords");
	}

	public static void addPlayer()
	{
		L2Player player = (L2Player) self;
		if(player == null || !checkPlayer(player))
			return;

		if(_players_list.contains(player.getObjectId()))
		{
			player.sendMessage("You already joined.");
			return;
		}
		_players_list.add(player.getObjectId());
		player.sendPacket(new PlaySound("ItemSound.quest_accept"));
		player.sendMessage("[ Impulse TeamDeathMatch ] will start in a few moments.");
		player.broadcastPacket(new MagicSkillUse(player, player, 3089, 1, 0, 0));
	}

	public static boolean checkPlayer(L2Player player)
	{
		if(!reg)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_TOO_LATE_THE_REGISTRATION_PERIOD_IS_OVER));
			return false;
		}
		
		if(player.getLevel() < 76)
		{
			player.sendMessage("Your level must be atleast 76 to join events.");
			return false;
		}

		if(player.getDuel() != null)
		{
			player.sendMessage("Leave duel!");
			return false;
		}

		if(player.isInZone(OlympiadStadia) || player.isInOlympiadMode())
		{
			player.sendMessage("You cant join while in oly!");
			return false;
		}

		if(player.isInParty() && player.getParty().isInDimensionalRift())
		{
			player.sendMessage("You cant join from dimensional rift.");
			return false;
		}

		if(player.isTeleporting())
		{
			player.sendMessage("You are teleporting right now..");
			return false;
		}
		return true;
	}

	public void OnDie(L2Object self, L2Character killer)
	{
		if(!started || killer.getTeam()==0)
			return;

		if(self != null && self.isPlayer() && _players_list.contains((Integer) self.getObjectId()))
		{
			if(killer.getObjectId() != self.getObjectId())
			{
				if(killer.getTeam()==1)
					_blue_score++;
				else
					_red_score++;
				killer.getPlayer().increaseEventPoints();
			}
		}
	}

	public static void clearArena()
	{
		for(L2Object obj : _zone.getObjects())
			if(obj != null)
			{
				L2Player player = obj.getPlayer();
				if(player != null)
					player.teleToLocation(TownManager.getInstance().getTown(11).getSpawn());
			}
	}

	private static void spawnBuffers()
	{
		_buffers_spawn = new GCSArray<L2Spawn>();
		for(Location loc : _buffers)
		{
			try
			{
				L2Spawn _statueSpawn;
				_statueSpawn = new L2Spawn(NpcTable.getTemplate(20));
				_statueSpawn.setAmount(1);
				_statueSpawn.setLoc(loc);
				_statueSpawn.stopRespawn();
				SpawnTable.getInstance().addNewSpawn(_statueSpawn, false);
				_buffers_spawn.add(_statueSpawn);
				_statueSpawn.doSpawn(true);
				_statueSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_statueSpawn.getLastSpawn(), _statueSpawn.getLastSpawn(), 1049, 1, 0, 0));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static void unspawnBuffers()
	{
		for(L2Spawn sp : _buffers_spawn)
			sp.getLastSpawn().deleteMe();
		_buffers_spawn = null;
	}

	private static GArray<L2Player> getPlayers(GCSArray<Integer> list)
	{
		GArray<L2Player> result = new GArray<L2Player>();
		for(Integer ObjId : list)
		{
			L2Player player = L2World.getPlayer(ObjId);
			if(player != null)
				result.add(player);
		}
		return result;
	}

	@Override
	public void onLoad()
	{
		_buffers = new GCSArray<Location>();
		_buffers.add(new Location(92912, -123032, -4472));
		_buffers.add(new Location(92843, -122969, -4480));
		_buffers.add(new Location(92769, -122895, -4480));
		_buffers.add(new Location(92697, -122821, -4456));
		_buffers.add(new Location(92636, -122777, -4456));
		_buffers.add(new Location(92909, -118718, -4368));
		_buffers.add(new Location(92976, -118718, -4376));
		_buffers.add(new Location(93043, -118710, -4384));
		_buffers.add(new Location(93136, -118702, -4392));
		_buffers.add(new Location(93259, -118701, -4408));

		_respawns = new GCSArray<Location>();
		_respawns.add(new Location(92959, -119567, -4544));
		_respawns.add(new Location(95176, -119911, -4544));
		_respawns.add(new Location(96025, -120928, -4560));
		_respawns.add(new Location(95997, -121924, -4560));
		_respawns.add(new Location(94821, -122882, -4560));
		_respawns.add(new Location(93420, -122579, -4544));
		_respawns.add(new Location(92428, -121191, -4544));
		_respawns.add(new Location(94309, -121333, -4560));
		_respawns.add(new Location(92315, -123405, -4256));
		_respawns.add(new Location(93115, -118472, -4328));
		
		_gates = new GCSArray<Location>();
		_gates.add(new Location(93240, -118760, -4432, 49152));
		_gates.add(new Location(93144, -118760, -4416, 16384));
		_gates.add(new Location(93064, -118760, -4400, 16388));
		_gates.add(new Location(92872, -118760, -4384, 16384));
		_gates.add(new Location(91512, -123880, -4192, 32768));
		_gates.add(new Location(91512, -123624, -4192, 32768));
		_gates.add(new Location(92648, -122696, -4472, 40960));
		_gates.add(new Location(92760, -122808, -4480, 40960));
		_gates.add(new Location(92988, -123032, -4480, 40960));
		
	}

	public static class TOP implements Comparator<Object>
	{
		private static final TOP instance = new TOP();

		public final static TOP getInstance()
		{
			return instance;
		}

		@Override
		public int compare(Object player1, Object player2)
		{
			L2Player playerOne = (L2Player) player1;
			L2Player playerTwo = (L2Player) player2;

			if(playerOne == null || playerTwo == null)
				return -1;
			if(playerOne.getEventPoints() > playerTwo.getEventPoints())
				return -1;
			if(playerOne.getEventPoints() < playerTwo.getEventPoints())
				return 1;
			return 0;
		}
	}	
	
	public static void balancePlayer(Integer player, int playerclazz)
	{
		teamBalance blue = _balance.get(0);
		teamBalance red = _balance.get(1);

		String playertype = getType(playerclazz);
		if(blue.getPlayerTypeCount(playertype) > red.getPlayerTypeCount(playertype))
		{
			_Red.add(player);
			red.addToBalance(playertype);
		}
		else if(blue.getPlayerTypeCount(playertype) < red.getPlayerTypeCount(playertype))
		{
			_Blue.add(player);
			blue.addToBalance(playertype);
		}
		else if(blue.getPlayerTypeCount(playertype) == red.getPlayerTypeCount(playertype))
			if(blue.getSize() > red.getSize())
			{
				_Red.add(player);
				red.addToBalance(playertype);
			}
			else if(blue.getSize() < red.getSize())
			{
				_Blue.add(player);
				blue.addToBalance(playertype);
			}
			else if(Rnd.chance(50))
			{
				_Red.add(player);
				red.addToBalance(playertype);
			}
			else
			{
				_Blue.add(player);
				blue.addToBalance(playertype);
			}
	}
	
	public static String getType(int id)
	{
		switch(id)
		{
			case 93:
			case 101:
			case 108:
			case 132:
				return "Dagger";
			case 92:
			case 102:
			case 109:
			case 134:
				return "Archer";
			case 94:
			case 95:
			case 103:
			case 110:
			case 115:
			case 128:
			case 129:
				return "Mage";
			case 97:
			case 105:
			case 112:
				return "Healer";
			case 90:
			case 91:
			case 106:
				return "Tank";
			case 113:
			case 114:
			case 127:
				return "OtherGooddd";
			default:
				return "Other";
		}
	}
	
	static class teamBalance
	{
		//Test balance -------------------------------------------------------
		int _Dagger = 0;
		int _Archer = 0;
		int _Mage = 0;
		int _Healer = 0;
		int _Tank = 0;
		int _OtherGooddd = 0;
		int _Other = 0;
		int _all = 0;

		public int getPlayerTypeCount(String type)
		{
			if(type.equals("Dagger"))
				return _Dagger;
			else if(type.equals("Archer"))
				return _Archer;
			else if(type.equals("Mage"))
				return _Mage;
			else if(type.equals("Healer"))
				return _Healer;
			else if(type.equals("Tank"))
				return _Tank;
			else if(type.equals("OtherGooddd"))
				return _OtherGooddd;
			else if(type.equals("Other"))
				return _Other;
			return 0;
		}

		public int getSize()
		{
			return _all;
		}
		
		public void addToBalance(String type)
		{
			if(type.equals("Dagger"))
				_Dagger++;
			else if(type.equals("Archer"))
				_Archer++;
			else if(type.equals("Mage"))
				_Mage++;
			else if(type.equals("Healer"))
				_Healer++;
			else if(type.equals("Tank"))
				_Tank++;
			else if(type.equals("OtherGooddd"))
				_OtherGooddd++;
			else if(type.equals("Other"))
				_Other++;
			_all++;
		}
	}
	
	private static boolean sleep(int sec)
	{
		try
		{
			if(!started)
				return false;
			Thread.sleep(sec * 1000);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public void onReload()
	{
		if(started)
		{
			say("********************************");
			say("Something went wrong. Event aborted!");
			say("Players still will get 'Sancity Crystal'");
			say("********************************");
			for(L2Player all : getPlayers(_players_list))
				addItem(all, 7917, 1);
			end();
		}
	}

	@Override
	public void onShutdown()
	{}

}
