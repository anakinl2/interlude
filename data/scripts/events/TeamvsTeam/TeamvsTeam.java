package events.TeamvsTeam;

import static com.lineage.game.model.L2Zone.ZoneType.OlympiadStadia;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.clientpackets.Say2C;
import com.lineage.game.instancemanager.TownManager;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2World;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.ExEventMatchMessage;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.serverpackets.Say2;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.DoorTable;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.SpawnTable;
import com.lineage.util.GArray;
import com.lineage.util.GCSArray;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

/**
 * 
 * @author Midnex
 *
 */
public class TeamvsTeam extends Functions implements ScriptFile
{
	private static GCSArray<Integer> _players_list;
	private static GCSArray<Integer> _Blue;
	private static GCSArray<Integer> _Red;

	private static GCSArray<Integer> _BlueLive;
	private static GCSArray<Integer> _RedLive;
	
	private static GCSArray<Location> _buffers;
	private static GCSArray<L2Spawn> _buffers_spawn;

	private static GArray<teamBalance> _balance;
	
	private static int Bluewins;
	private static int Redwins;

	private static ScheduledFuture<?> _checknulls;

	static boolean started = false;
	static boolean reg = false;

	private static L2Zone _zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.battle_zone, 4, true);
	private static L2Zone _zone1Peace = ZoneManager.getInstance().getZoneById(ZoneType.peace_zone, 115, true);
	private static L2Zone _zone2Peace = ZoneManager.getInstance().getZoneById(ZoneType.peace_zone, 116, true);
	
	
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
		EventScheduler.setEventName(1);
		
		for(L2Player player : L2World.getAllPlayers())
		{
			if(player != null && player.getLevel() >= 76)
			{
				player.sendPacket(new MagicSkillUse(player, player, 5006, 1, 0, 0));
				player.scriptRequest("Register to the event : [ Impulse Teamfight ]", "events.TeamvsTeam.TeamvsTeam:addPlayer", new Object[0]);
			}
		}
	}

	public static void init()
	{
		_players_list = new GCSArray<Integer>();
		_Blue = new GCSArray<Integer>();
		_Red = new GCSArray<Integer>();
		_BlueLive = new GCSArray<Integer>();
		_RedLive = new GCSArray<Integer>();
		_balance = new GArray<teamBalance>();
		
		_balance.add(new teamBalance(0));
		_balance.add(new teamBalance(1));

		
		Bluewins = 0;
		Redwins = 0;

		sayAll("Registration to [ Impulse Teamfight ] event is now open! Type '.join' to register.");
		question();
		executeTask("events.TeamvsTeam.TeamvsTeam", "begin", new Object[0], 31000);
	}

	public static void begin()
	{
		if(_players_list.size() < 4)
		{
			sayAll("[ Impulse Teamfight ] - Canceled due lack of players.");
			_players_list = new GCSArray<Integer>();
			reg = false;
			EventScheduler.setEvent(false);
			return;
		}
		
		sayAll("[ Impulse Teamfight ] is starting now.");

		reg = false;
		EventScheduler.setNextEventTime(-1);
		started = true;

		clearArena();
		checkforDcs();
		closeColiseumDoors();
		executeTask("events.TeamvsTeam.TeamvsTeam", "run", new Object[0], 10000);
		SaveCoords();
		portPlayers();
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
			balancePlayer(pl, player.getActiveClassId());
		}
		_balance = null;
		for(int rounds = 1; rounds <= 3; rounds++)
			round(rounds);
		winner(false, 0);
		end();
	}

	private static boolean round(int round)
	{
		_BlueLive = new GCSArray<Integer>();
		_RedLive = new GCSArray<Integer>();
		_BlueLive.addAll(_Blue);
		_RedLive.addAll(_Red);
		resPlayers();
		healPlayers();
		portPlayers();

		sleep(round == 1 ? 15 : 5);
		say("40 seconds to buff up.");
		sleep(2);
		spawnBuffers();
		sleep(30);
		say("20 seconds to buff up.");
		sleep(9);
		healPlayers();
		sleep(1);
		unspawnBuffers();
		sleep(1);
		beginCountdown();
		say1(0,"FIGHT");
		openColiseumDoors();
		say("Blue " + Bluewins + " : " + Redwins + " Red");
		sleep(15);
		closeColiseumDoors();
		killnotinbattle();
		say("Round will last 2 minutes.");
		sleep(60);
		say("1 minute till the round ends.");
		sleep(30);
		say("30 seconds till the round ends.");
		sleep(20);
		say("10 seconds till the round ends.");
		sleep(10);
		winner(true, round);
		say("Blue " + Bluewins + " : " + Redwins + " Red");
		return true;
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
		_BlueLive = new GCSArray<Integer>();
		_RedLive = new GCSArray<Integer>();
		Bluewins = 0;
		Redwins = 0;
		openColiseumDoorsAll();
		started = false;
		EventScheduler.setEvent(false);
	}

	private static boolean winner(boolean roundwin, int round)
	{
		if(roundwin)
		{
			if(_RedLive.size() > _BlueLive.size())
			{
				if(round == 1)
					say("Red team won the 1st round.");
				if(round == 2)
					say("Red team won the 2nd round.");
				Redwins++;
			}
			else if (_RedLive.size() < _BlueLive.size())
			{
				if(round == 1)
					say("Blue team won the 1st round.");
				if(round == 2)
					say("Blue team won the 2nd round.");
				Bluewins++;
			}
			else
			{
				if(Rnd.chance(50))
				{
					if(round == 1)
						say("Blue team won the 1st round.");
					if(round == 2)
						say("Blue team won the 2nd round.");
					Bluewins++;
				}
				else
				{
					if(round == 1)
						say("Red team won the 1st round.");
					if(round == 2)
						say("Red team won the 2nd round.");
					Redwins++;
				}
			}
		}
		else
		{
			boolean weapons = Rnd.chance(50);
			if(Bluewins > Redwins)
			{
				for(L2Player pl : getPlayers(_Blue))
					addItem(pl, weapons ? 959 : 960, 1);
				sayAll("Blue has won this impulse Teamfight.");
			}
			else
			{
				for(L2Player pl : getPlayers(_Red))
					addItem(pl, weapons ? 959 : 960, 1);
				sayAll("Red has won this impulse Teamfight.");
			}
			if(Bluewins == 1)
			{
				for(L2Player pl : getPlayers(_Blue))
					addItem(pl, weapons ? 729 : 730, 1);
			}
			if(Redwins == 1)
			{
				for(L2Player pl : getPlayers(_Red))
					addItem(pl, weapons ? 729 : 730, 1);
			}

			for(L2Player all : getPlayers(_players_list))
			{
				addItem(all, 7917, 1);
			}
			
			Object[] players = getPlayers(_players_list).toArray();
			Arrays.sort(players, TOP.getInstance());
			
			int placea = 0;
			sayAll("Top 5 in current event:");
			for(Object place : players)
			{
				placea++;
				if(placea==6)
					break;
				
				L2Player player = (L2Player)place;
				if(player != null)
					sayAll(placea+"# "+player.getName()+" [kills : "+player.getEventPoints()+"]");
			}
		}
		return true;
	}

	public static boolean beginCountdown()
	{
		for(int i = 10; i > 0; i--)
		{
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

	public static void portPlayers()
	{
		for(L2Player t2 : getPlayers(_Red))
		{
			Location pos = Rnd.coordsRandomize(147496, 46712, -3434, 0, 10, 100);
			t2.teleToLocation(pos);
			t2.setTeam(2,true);
			t2.getEffectList().stopAllEffects();
		}

		for(L2Player t1 : getPlayers(_Blue))
		{
			Location pos = Rnd.coordsRandomize(151480, 46728, -3434, 0, 10, 100);
			t1.teleToLocation(pos);
			t1.setTeam(1,true);
			t1.getEffectList().stopAllEffects();
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
				_RedLive.remove(ObjId);
				_Red.remove(ObjId);
				_players_list.remove(ObjId);
			}
		}

		for(Integer ObjId : _Blue)
		{
			L2Player player = L2World.getPlayer(ObjId);
			if(player == null)
			{
				_BlueLive.remove(ObjId);
				_Blue.remove(ObjId);
				_players_list.remove(ObjId);
			}
		}
	}

	public static ScheduledFuture<?> addTask(String task, Integer time)
	{
		return executeTask("events.TeamvsTeam.TeamvsTeam", task, new Object[0], time);
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
		player.sendMessage("[ Impulse Teamfight ] will start in a few moments.");
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
		if(killer == null || !started || killer.getTeam() == 0)
			return;
		
		if(self != null && self.isPlayer() && _players_list.contains((Integer) self.getObjectId()))
		{
			if(killer.getObjectId() != self.getObjectId())
				killer.getPlayer().increaseEventPoints();
			if(_Blue.contains((Integer) self.getObjectId()))
				_BlueLive.remove((Integer) self.getObjectId());
			if(_Red.contains((Integer) self.getObjectId()))
				_RedLive.remove((Integer) self.getObjectId());
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
		for(L2Object obj : _zone1Peace.getObjects())
			if(obj != null)
			{
				L2Player player = obj.getPlayer();
				if(player != null)
					player.teleToLocation(TownManager.getInstance().getTown(11).getSpawn());
			}
		for(L2Object obj : _zone2Peace.getObjects())
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


	private static void openColiseumDoors()
	{
		DoorTable.getInstance().getDoor(24190002).openMe();
		DoorTable.getInstance().getDoor(24190003).openMe();
	}

	private static void closeColiseumDoors()
	{
		DoorTable.getInstance().getDoor(24190001).closeMe();
		DoorTable.getInstance().getDoor(24190002).closeMe();
		DoorTable.getInstance().getDoor(24190003).closeMe();
		DoorTable.getInstance().getDoor(24190004).closeMe();
	}

	private static void openColiseumDoorsAll()
	{
		DoorTable.getInstance().getDoor(24190001).openMe();
		DoorTable.getInstance().getDoor(24190002).openMe();
		DoorTable.getInstance().getDoor(24190003).openMe();
		DoorTable.getInstance().getDoor(24190004).openMe();
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
		_buffers.add(new Location(147784, 46568, -3400));
		_buffers.add(new Location(151656, 46872, -3400));
		_buffers.add(new Location(151432, 46872, -3400));
		_buffers.add(new Location(151208, 46872, -3400));
		_buffers.add(new Location(151656, 46568, -3400));
		_buffers.add(new Location(151432, 46568, -3400));
		_buffers.add(new Location(151208, 46568, -3400));
		_buffers.add(new Location(147352, 46872, -3400));
		_buffers.add(new Location(147560, 46872, -3400));
		_buffers.add(new Location(147784, 46872, -3400));
		_buffers.add(new Location(147352, 46568, -3400));
		_buffers.add(new Location(147560, 46568, -3400));
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

		public teamBalance(int i)
		{
		}

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
