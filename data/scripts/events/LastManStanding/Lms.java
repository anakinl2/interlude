package events.LastManStanding;

import static com.lineage.game.model.L2Zone.ZoneType.OlympiadStadia;
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
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class Lms extends Functions implements ScriptFile
{
	//////////////rewardai///////////////////////////////
	private static final int LmsPrizeId = 6393;
	private static final int LmsPrizeforKill = 10;
	private static final int LmsPrizeforLastKill = 20;
	//////////////////////////////////////////////////////
	
	
	public static L2Object self;
		private static GCSArray<Integer> _players_list = new GCSArray<Integer>();
		private static GCSArray<Integer> _liveplayers = new GCSArray<Integer>();

		static boolean started = false;
		static boolean reg = false;

		private static L2Zone _zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.battle_zone, 4, true);

		public static void say(String message)
		{
			for(L2Player pl : getPlayers(_players_list))
				pl.sendPacket(new Say2(0, 18, "[TvT]", "[Lms] - "+message));
		}
		
		public static void say1(int i)
		{
			for(L2Player pl : getPlayers(_players_list))
					pl.sendPacket(new ExEventMatchMessage(i, "Mldc moki sniffint packetus ;D!"));
		}

		public static void say2(String msg)
		{
			for(L2Player pl : getPlayers(_players_list))
					pl.sendPacket(new ExEventMatchMessage(0, msg));
		}

		public static void question()
		{
			reg = true;
			for(L2Player player : L2World.getAllPlayers())
			{
				if(player != null && player.getLevel() > 69)
					player.scriptRequest("Do you want join a \"Last Man Standing \" event?", "events.LastManStanding.Lms:addPlayer", new Object[0]);
			}
		}
		
		public static void init()
		{
			Announcements.getInstance().announceToAll("Registration to \"Last Man Standing\" event is now open..");
			question();
			executeTask("events.LastManStanding.Lms", "begin", new Object[0], 20000);
		}
		
		public static void begin()
		{
			Announcements.getInstance().announceToAll("Registration to \"Last Man Standing\" event is now closed..");
			reg = false;
			started = true;

			SaveCoords();
			portPlayers();
			setBlue();
			canceleffects();
			sleep(2);
			paralyzePlayers();
			_liveplayers.addAll(_players_list);
			clearArena();
			closeColiseumDoors();
			executeTask("events.LastManStanding.Lms", "run", new Object[0], 30000);
			say("Plzzz Wait 30s for players with wooden pc's ;D!!");
			sleep(30);
			say("10s left to start!!");
			say2("10s left to start!!");
			sleep(10);
			beginCountdown();
			say("FIGHT");
			setRed();
			canceleffects();
			killnotinbattle();
			say1(2);
			time();	
			reward();
			end();
		}

		private static boolean time()
		{
			for (int i = 6; i > 0; i--)
			{
				if(_liveplayers.size()<2)
					break;
	
				say2(_liveplayers.size()+" Live players left! "+i*30+"s. left!");
				sleep(30);
			}
			return true;
		}
		
		
	private static boolean reward()
	{
		for(L2Player player : getPlayers(_liveplayers))
		{
		addItem(player, LmsPrizeId, LmsPrizeforLastKill);
		say2(player.getName()+" was last!");
		say(player.getName()+" was last!");
		}
		return true;
	}

	
	public static void setBlue()
	{
		for(L2Player player : getPlayers(_players_list))
			player.setTeam(1,true);
	}
	
	public static void setRed()
	{
		for(L2Player player : getPlayers(_players_list))
			player.setTeam(2,true);
	}
	
		public static void paralyzePlayers()
		{
			L2Skill revengeSkill = SkillTable.getInstance().getInfo(9098, 1);
			for(L2Player player : getPlayers(_players_list))
			{
					revengeSkill.getEffects(player, player, false, false);
					if(player.getPet() != null)
						revengeSkill.getEffects(player, player.getPet(), false, false);
			}
		}
		
	public static void canceleffects()
	{
		for(L2Player player : getPlayers(_players_list))
		{
			player.getEffectList().stopAllEffects();

			if(player.getPet() != null)
				player.getPet().getEffectList().stopAllEffects();

			if(player.getParty() != null)
				player.getParty().oustPartyMember(player);
		}
	}

		private static void end()
		{
			say1(3);
			say("Thank you for joing our event!");
			for(L2Player pl : getPlayers(_players_list))
			{
					if(pl.isDead())
						pl.doRevive();
					TeleportPlayerToSaveCoords(pl);
					pl.setTeam(0,false);
			}

			_liveplayers.clear();
			_players_list.clear();
			
			openColiseumDoorsAll();
			started = false;
		}

		public static boolean beginCountdown()
		{
			for (int i = 5; i > 0; i--)
			{
				say1(i+3);
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

		for(L2Player pl : getPlayers(_players_list))
		{
			Location pos = Rnd.coordsRandomize(149464, 46760, -3438, 0, 0, 500);
			pl.teleToLocation(pos, 0);
		}
	}

		public static void healPlayers()
		{
			for(L2Player pl : getPlayers(_players_list))
			{
					pl.setCurrentHpMp(pl.getMaxHp(), pl.getMaxMp());
					pl.setCurrentCp(pl.getMaxCp());
					pl.broadcastPacket(new MagicSkillUse(pl, pl, 1217, 1, 500, 0));
			}
		}

		public static void resPlayers()
		{
			for(L2Player pl :getPlayers( _players_list))
					if(pl.isDead())
						pl.doRevive();
		}

		private static boolean sleep(int sec)
		{
			try
			{Thread.sleep(sec*1000);}
			catch(InterruptedException e)
			{e.printStackTrace();}	
			return true;
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

			if(_players_list.size() >= 20)
			{
				player.sendMessage("To late, maybe next time! :D");
				return;
			}

			if(_players_list.contains(player))
			{
				player.sendMessage("Dont Cheat!");
			}

			_players_list.add(player.getObjectId());
			player.sendMessage("Registred! HF!");
		}

		public static boolean checkPlayer(L2Player player)
		{
			if(!reg)
			{
				player.sendMessage("Too late..");
				return false;
			}
			
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
		

		public void OnDie(L2Object self, L2Character killer)
		{
			if(self != null && self.isPlayer() && _players_list.contains((Integer)self.getObjectId()))
			{
				
				_liveplayers.remove((Integer)self.getObjectId());
				
				if(killer != null && killer.isPlayer())
						addItem((L2Player) killer, LmsPrizeId, LmsPrizeforKill);
			}
		}
		
		public static void clearArena()
		{
			for(L2Object obj : _zone.getObjects())
				if(obj != null)
				{
					L2Player player = obj.getPlayer();
					if(player != null && !_players_list.contains(player.getObjectId()))
						player.teleToLocation(TownManager.getInstance().getTown(11).getSpawn());
				}
		}
		
		private static GArray<L2Player> getPlayers(GCSArray<Integer> list)
		{
			GArray<L2Player> result = new GArray<L2Player>();
			for(Integer ObjId : list)
			{
				L2Player player =  L2World.getPlayer(ObjId);
				if(player != null)
					result.add(player);
				else
				{
					_players_list.remove(ObjId);
					if(_liveplayers.contains(ObjId))
					_liveplayers.remove(ObjId);
				}
			}
			return result;
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
		
		@Override
		public void onLoad()
		{}
		@Override
		public void onReload()
		{end();}
		@Override
		public void onShutdown()
		{}

}
