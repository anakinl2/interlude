package events.korean;

import static com.lineage.game.model.L2Zone.ZoneType.OlympiadStadia;

import java.util.concurrent.ScheduledFuture;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.Announcements;
import com.lineage.game.instancemanager.TownManager;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.serverpackets.ExEventMatchMessage;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.Say2;
import com.lineage.game.tables.DoorTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.GArray;
import com.lineage.util.GCSArray;

public class KoreanEvent extends Functions implements ScriptFile
{
	//////////////rewardai///////////////////////////////
	private static final int KoreanPrizeId = 6393;
	private static final int KoreanPrizeCount = 10;
	//////////////////////////////////////////////////////
	

	public static L2Object self;
	private static GCSArray<Integer> _players_list = new GCSArray<Integer>();
	private static GCSArray<Integer> _Blue = new GCSArray<Integer>();
	private static GCSArray<Integer> _Red = new GCSArray<Integer>();

	private static int BlueDead;
	private static int RedDead;
	private static int BlueTotal;
	private static int RedTotal;

	public static L2Player pl1 = null;
	public static L2Player pl2 = null;

	private static ScheduledFuture<?> _checknulls;
	private static ScheduledFuture<?> _eventEnd;

	private static L2Zone _zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.battle_zone, 4, true);

	public static void sayToAll(final String message)
	{
		for(L2Player pl : getPlayers(_players_list))
			pl.sendPacket(new Say2(0, 18, "[Korean]", "[Korean] - "+message));
	}

	public static void say1(int i)
	{
		for(L2Player pl : getPlayers(_players_list))
				pl.sendPacket(new ExEventMatchMessage(i, "Mldc moki sniffint packetus ;D!"));
	}
	public static void sayToScrean(final String message)
	{
		for(L2Player pl : getPlayers(_players_list))
				pl.sendPacket(new ExEventMatchMessage(0, message));
	}

	public static void question()
	{
		for(L2Player player : L2World.getAllPlayers())
		{
			if(player != null && player.getLevel() > 69)
				player.scriptRequest("Do you want join a korean style pvp event?", "events.korean.KoreanEvent:addPlayer", new Object[0]);
		}
	}

	public static void init()
	{
		Announcements.getInstance().announceToAll("Registration to \"Korean\" event is now open..");
		question();
		executeTask("events.korean.KoreanEvent", "begin", new Object[0], 20000);
	}

	public static void begin()
	{
		if(_players_list.size() <2)
		{
			sayToAll("Korean Event canceled!");
			_players_list.clear();
			return;
		}
		clearArena();
		_eventEnd = addTask("end", 10 * 60 * 1000);
		SaveCoords();
		closeColiseumDoors();
		checkforDcs();
		sayToAll("Korean event begins!!");
		teleport();
		executeTask("events.korean.KoreanEvent", "paralyzePlayers", new Object[0], 2000);
		executeTask("events.korean.KoreanEvent", "begin1", new Object[0], 10000);
		sayToAll("Plzzz Wait 10s for players with wooden pc's ;D!!");
		BlueTotal = _Blue.size();
		RedTotal = _Red.size();
	}

	public static void begin1()
	{
		nextopponentFromTeam1();
		nextopponentFromTeam2();
		sayToScrean(pl1.getName() + " vs " + pl2.getName());
	}

	public static boolean teleport()
	{
		int i = 0;

		for(L2Player pl : getPlayers(_players_list))
		{
			if(_Blue.size() == _Red.size())
				_Blue.add((Integer)pl.getObjectId());
			else
				_Red.add((Integer)pl.getObjectId());

			if(pl.isDead())
				pl.doRevive();
		}

		for(L2Player t1 : getPlayers(_Blue))
		{
			t1.setTeam(1,true);
			t1.teleToLocation(150056 + i, 45912, -3437);
			i += 100;
		}

		i = 0;
		for(L2Player t2 : getPlayers( _Red))
		{
			t2.setTeam(1,true);
			t2.teleToLocation(148856 + i, 47640, -3437);
			i += 100;
		}
		sayToAll("Porting...");
		return true;
	}

	public static void startmatch(boolean team)
	{
		if(!checkwinner())
		{
			if(team)
			{
				pl1 = null;
				nextopponentFromTeam1();
			}
			else
			{
				pl2 = null;
				nextopponentFromTeam2();
			}
			sayToScrean(pl1.getName() + " vs " + pl2.getName());
		}
		else
			end();
	}

	private static void end()
	{
		say1(3);
		sayToAll("Thank you for joing our event!");
		_checknulls.cancel(true);
		_eventEnd.cancel(true);

		openColiseumDoors();

		for(L2Player pl : getPlayers(_players_list))
		{
				if(pl.isDead())
					pl.doRevive();
				TeleportPlayerToSaveCoords(pl);
		}

		_Blue.clear();
		_Red.clear();
		_players_list.clear();

		BlueDead = 0;
		RedDead = 0;
		BlueTotal = 0;
		RedTotal = 0;
		pl1 = null;
		pl2 = null;
	}

	public static boolean checkwinner()
	{
		if(RedDead == RedTotal)
		{
			for(L2Player pl : getPlayers(_Blue))
				addItem(pl, KoreanPrizeId, KoreanPrizeCount);
			sayToScrean("Blue wins!");
			return true;
		}
		if(BlueDead == BlueTotal)
		{
			for(L2Player pl : getPlayers(_Red))
				addItem(pl, KoreanPrizeId, KoreanPrizeCount);
			sayToScrean("Red wins!");
			return true;
		}
		return false;
	}

	public static void checkforDcs()
	{
		_checknulls = addTask("checkforDcs", 2000);

		for(Integer ObjId : _Red)
		{
			L2Player player =  L2World.getPlayer(ObjId);
			if(player == null)
			{
				RedDead++;
				_Red.remove(ObjId);
				_players_list.remove(ObjId);
				RedTotal =- 1;
				if(pl2.getObjectId() == ObjId)
				{
					pl2 = null;
					nextopponentFromTeam2();
				}		
			}
		}
		
		for(Integer ObjId : _Blue)
		{
			L2Player player =  L2World.getPlayer(ObjId);
			if(player == null)
			{
				BlueDead++;
				_Blue.remove(ObjId);
				_players_list.remove(ObjId);
				BlueTotal =- 1;
				if(pl1.getObjectId() == ObjId)
				{
					pl1 = null;
					nextopponentFromTeam1();
				}		
			}
		}
	}

	public static void nextopponentFromTeam1()
	{
		if(checkwinner())
		{
			end();
			return;
		}
		for(L2Player t1 : getPlayers(_Blue))
		{
			if(!t1.isDead() && t1 != pl1)
				pl1 = t1;
		}
		pl1.setTeam(2,true);
		unparalyze(pl1);
		pl1.setCurrentHpMp(pl1.getMaxHp(), pl1.getMaxMp());
		pl1.setCurrentCp(pl1.getMaxCp());
		pl1.broadcastPacket(new MagicSkillUse(pl1, pl1, 1217, 1, 100, 0));
	}

	public static void nextopponentFromTeam2()
	{
		if(checkwinner())
		{
			end();
			return;
		}

		for(L2Player t2 : getPlayers(_Red))
		{
			if(!t2.isDead() && t2 != pl2)
				pl2 = t2;
		}
		pl2.setTeam(2,true);
		unparalyze(pl2);
		pl2.setCurrentHpMp(pl2.getMaxHp(), pl2.getMaxMp());
		pl2.setCurrentCp(pl2.getMaxCp());
		pl2.broadcastPacket(new MagicSkillUse(pl2, pl2, 1217, 1, 100, 0));
	}

	public void OnDie(final L2Object self, final L2Character killer)
	{
		if(self != null && self.isPlayer() && _players_list.contains((Integer)self.getObjectId()))
		{
			if(_Blue.contains((Integer)self.getObjectId()))
			{
				BlueDead++;
				sayToAll(((L2Player) self).getName() + " is dead now...");
				startmatch(true);
			}
			if(_Red.contains((Integer)self.getObjectId()))
			{
				RedDead++;
				sayToAll(((L2Player) self).getName() + " is dead now...");
				startmatch(false);
			}
			((L2Player) self).setTeam(0,false);
		}
	}

	public static void paralyzePlayers()
	{
		 L2Skill revengeSkill = SkillTable.getInstance().getInfo(9098, 1);
		for( L2Player player : getPlayers(_players_list))
		{
				revengeSkill.getEffects(player, player, false, false);
				if(player.getPet() != null)
					revengeSkill.getEffects(player, player.getPet(), false, false);
			}
	}

	public static void unparalyze(L2Player pl)
	{
		if(pl != null)
			pl.getEffectList().stopAllEffects();
		if(pl.getPet() != null)
			pl.getPet().getEffectList().stopAllEffects();
	}

	public static void SaveCoords()
	{
		for(L2Player player : getPlayers(_players_list))
				player.setVar("EventBackCoords", player.getX() + " " + player.getY() + " " + player.getZ());
	}

	public static void TeleportPlayerToSaveCoords(L2Player player)
	{
		unparalyze(player);
		player.setTeam(0,false);

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

		if(_players_list.size() >= 20)
		{
			player.sendMessage("To late, maybe next time! :D");
			return;
		}

		if(_players_list.contains(player))
		{
			player.sendMessage("Dont Cheat!");
		}

		_players_list.add((Integer)player.getObjectId());
		player.sendMessage("Registred! HF!");
	}

	public static boolean checkPlayer(L2Player player)
	{
		if(player.getDuel() != null)
		{
			player.sendMessage("Leave duel!");
			return false;
		}

		if(player.isInZone(OlympiadStadia))
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

	public static ScheduledFuture<?> addTask(String task, Integer time)
	{
		return executeTask("events.korean.KoreanEvent", task, new Object[0], time);
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
	
	private static void openColiseumDoors()
	{
		DoorTable.getInstance().getDoor(24190001).openMe();
		DoorTable.getInstance().getDoor(24190002).openMe();
		DoorTable.getInstance().getDoor(24190003).openMe();
		DoorTable.getInstance().getDoor(24190004).openMe();
	}

	private static void closeColiseumDoors()
	{
		DoorTable.getInstance().getDoor(24190001).closeMe();
		DoorTable.getInstance().getDoor(24190002).closeMe();
		DoorTable.getInstance().getDoor(24190003).closeMe();
		DoorTable.getInstance().getDoor(24190004).closeMe();
	}

	private static GArray<L2Player> getPlayers(GCSArray<Integer> list)
	{
		GArray<L2Player> result = new GArray<L2Player>();
		for(Integer ObjId : list)
		{
			L2Player player =  L2World.getPlayer(ObjId);
			if(player != null)
				result.add(player);
		}
		return result;
	}

	@Override
	public void onLoad()
	{}

	@Override
	public void onReload()
	{}

	@Override
	public void onShutdown()
	{}
}
