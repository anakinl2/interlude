package events.DeathMatch;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.Announcements;
import l2d.game.ThreadPoolManager;
import l2d.game.serverpackets.Revive;
import l2d.game.serverpackets.SocialAction;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2World;
import l2d.game.model.L2Zone;
import static l2d.game.model.L2Zone.ZoneType.OlympiadStadia;
import l2d.game.tables.DoorTable;
import l2d.game.tables.SkillTable;
import com.lineage.util.Rnd;
import com.lineage.util.Location;
import com.lineage.Config;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DeathMatch extends Functions implements ScriptFile
{
	public L2Object self;
	private static ConcurrentLinkedQueue<L2Player> players_list = new ConcurrentLinkedQueue<L2Player>();
	private static L2Zone _zone = ZoneManager.getInstance().getZoneByIndex(L2Zone.ZoneType.battle_zone, 4, true);
	private static int seconds = 1000;
	private static int minutes = 60000;

	public class StartTask implements Runnable
	{
		public void run()
		{
			Start();
		}
	}

	public static void sayToAll(final String message)
	{
		Announcements.getInstance().announceToAll(message);
	}

	public void Start()
	{
		sayToAll("Регистрация на Эвент Смертельный Матч началась");
		sayToAll("Эвент проводится для игроков "+Config.DM_MinLevel+"-"+Config.DM_MaxLevel+" уровня");
		executeTask("events.DeathMatch.DeathMatch", "Registered", new Object[0], seconds);
		executeTask("events.DeathMatch.DeathMatch", "Registered", new Object[0], minutes);
		executeTask("events.DeathMatch.DeathMatch", "Registered", new Object[0], 2 * minutes);
		executeTask("events.DeathMatch.DeathMatch", "SaveCoords", new Object[0], 3 * minutes);
		executeTask("events.DeathMatch.DeathMatch", "StartBattle", new Object[0], 3 * minutes + 5 * seconds);
		executeTask("events.DeathMatch.DeathMatch", "EndBattle", new Object[0], 8 * minutes + 5 * seconds);
	}

	public static void StartBattle()
	{
		closeColiseumDoors();
		clearArena();
		if(players_list.size() < 10)
		{
			sayToAll("Эвент отменен из за малого кол-ва участников");
			executeTask("events.DeathMatch.DeathMatch", "End", new Object[0], 4 * seconds);
			return;
		}
		sayToAll("Телепортация игроков...");
		executeTask("events.DeathMatch.DeathMatch", "ressurectPlayers", new Object[0], 1 * seconds);
		executeTask("events.DeathMatch.DeathMatch", "healPlayers", new Object[0], 2 * seconds);
		executeTask("events.DeathMatch.DeathMatch", "paralyzePlayers", new Object[0], 4 * seconds);
		executeTask("events.DeathMatch.DeathMatch", "TeleportToColiseum", new Object[0], 6 * seconds);
		executeTask("events.DeathMatch.DeathMatch", "DeleteAllBuff", new Object[0], 66 * seconds);
	}

	public static void SaveCoords()
	{
		for(final L2Player player : players_list)
			if(player != null)
				player.setVar("DeathMatchBackCoords", player.getX() + " " + player.getY() + " " + player.getZ() + " " + player.getReflection().getId());
	}

	public static void TeleportToSaveCoords()
	{
		for(final L2Player player : players_list)
			if(player != null)
			{
				final String var = player.getVar("DeathMatchBackCoords");
				if(var == null || var.equals(""))
					continue;
				final String[] coords = var.split(" ");
				if(coords.length != 4)
					continue;
				player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
				player.unsetVar("DeathMatchBackCoords");
			}
	}

	public static void TeleportPlayerToSaveCoords(L2Player player)
	{
		players_list.remove(player);
		final String var = player.getVar("DeathMatchBackCoords");
		final String[] coords = var.split(" ");
		player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
		player.unsetVar("DeathMatchBackCoords");
	}

	public void End()
	{
		players_list.clear();
		sayToAll("Эвент Смертельный Матч закончился.");
		if(Config.DM_Interval * minutes != 0)
			ThreadPoolManager.getInstance().scheduleEffect(new StartTask(), Config.DM_Interval * minutes);
	}

	public static void EndBattle()
	{
		MessageToPlayer("Сейчас Вы будете вылечены и телепортированы обратно.");
		executeTask("events.DeathMatch.DeathMatch", "ressurectPlayers", new Object[0], seconds);
		executeTask("events.DeathMatch.DeathMatch", "healPlayers", new Object[0], 2 * seconds);
		executeTask("events.DeathMatch.DeathMatch", "TeleportToSaveCoords", new Object[0], 3 * seconds);
		openColiseumDoors();
		executeTask("events.DeathMatch.DeathMatch", "End", new Object[0], 4 * seconds);
	}

	public static void Registered()
	{
		for(final L2Player player : L2World.getAllPlayers())
			if(player != null && !players_list.contains(player) && player.getLevel() >= Config.DM_MinLevel && player.getLevel() <= Config.DM_MaxLevel)
			{
				player.scriptRequest("Будете участвовать в эвенте Смертельный матч ?", "events.DeathMatch.DeathMatch:addPlayer", new Object[0]);
				player.unsetVar("DeathMatchBackCoords");
			}
	}

	public void addPlayer()
	{
		final L2Player player = (L2Player) self;
		if(player == null || !checkPlayer(player))
			return;

		if(players_list.size() < Config.DM_MaxPlayer)
		{
			players_list.add(player);
			player.sendMessage("Вы зарегестрировались !");
		}
		else
		{
			player.sendMessage("Кол-во участников достигло максимальной отметки, попробуйте в другой раз.");
		}
	}

	public static void MessageToPlayer(final String text)
	{
		for(final L2Player player : players_list)
			if(player != null)
				player.sendMessage(text);
	}

	public static void TeleportToGiran(final L2Character character)
	{
		if(character != null)
			character.teleToLocation(83464, 148616, -3431);
	}

	public static void TeleportToColiseum()
	{
		for(final L2Player player : players_list)
			if(player != null)
			{
				final Location pos = Rnd.coordsRandomize(149505, 46719, -3417, 0, 0, 500);
				player.teleToLocation(pos, 0);
			}
		MessageToPlayer("Старт через 60 секунд приготовьтесь !!!");
	}

	public static void clearArena()
	{
		for(final L2Object obj : _zone.getObjects())
			if(obj != null)
			{
				final L2Player player = obj.getPlayer();
				if(player != null && !players_list.contains(player))
					TeleportToGiran(player);
			}
	}

	private static void closeColiseumDoors()
	{
		DoorTable.getInstance().getDoor(24190001).closeMe();
		DoorTable.getInstance().getDoor(24190002).closeMe();
		DoorTable.getInstance().getDoor(24190003).closeMe();
		DoorTable.getInstance().getDoor(24190004).closeMe();
	}

	private static void openColiseumDoors()
	{
		DoorTable.getInstance().getDoor(24190001).openMe();
		DoorTable.getInstance().getDoor(24190002).openMe();
		DoorTable.getInstance().getDoor(24190003).openMe();
		DoorTable.getInstance().getDoor(24190004).openMe();
	}

	public static void paralyzePlayers()
	{
		final L2Skill revengeSkill = SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_CURSE, 1);
		for(final L2Player player : players_list)
			if(player != null)
			{
				revengeSkill.getEffects(player, player, false, false);
				if(player.getPet() != null)
					revengeSkill.getEffects(player, player.getPet(), false, false);
			}
	}

	public static void DeleteAllBuff()
	{
		for(final L2Player player : players_list)
			if(player != null)
				player.getEffectList().stopAllEffects();
		MessageToPlayer("---=== Старт ===---");
	}

	public static void ressurectPlayers()
	{
		for(final L2Player player : players_list)
			if(player != null && player.isDead())
			{
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp(), true);
				player.restoreExp();
				player.broadcastPacket(new SocialAction(player.getObjectId(), 15));
				player.broadcastPacket(new Revive(player));
				player.doRevive();
			}
	}

	public static void healPlayers()
	{
		for(final L2Player player : players_list)
			if(player != null)
			{
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
			}
	}

	public static void checkLive()
	{
		if(players_list.size() <= 1)
			EndBattle();
	}

	public static void OnDie(final L2Object self, final L2Character killer)
	{
		if(self != null && self.isPlayer() && players_list.contains(self))
		{
			((L2Player) self).sendMessage("Вы проиграли.");
			TeleportPlayerToSaveCoords((L2Player) self);
			checkLive();
			if(killer != null && killer.isPlayer())
				addItem((L2Player) killer, Config.DM_Id, Config.DM_Count);
		}
	}

	public static boolean checkPlayer(L2Player player)
	{
		if(player.getDuel() != null)
		{
			player.sendMessage("Для регистрации выйдите из дуэли");
			return false;
		}

		if(player.isInZone(OlympiadStadia))
		{
			player.sendMessage("Участники олимпиады не могут принимать участие в эвенте");
			return false;
		}

		if(player.isInParty() && player.getParty().isInDimensionalRift())
		{
			player.sendMessage("Участники Dimensional Rift не могут принимать участие в эвенте");
			return false;
		}

		if(player.isTeleporting())
		{
			player.sendMessage("Во время телепортации нельзя регестрироваться");
			return false;
		}
		return true;
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}
}