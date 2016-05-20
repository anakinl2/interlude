package events.coins;

import java.util.ArrayList;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.Announcements;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Files;
import l2d.util.Rnd;
import events.Helper;

public class coins extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private static int MOUSE_COIN_CHANCE = 70;

	private static int MOUSE_COIN = 10639;
	private static int BASE_COIN_AFTER_RB = 1000;

	@SuppressWarnings("unused")
	private static ArrayList<L2Spawn> _spawns = new ArrayList<L2Spawn>();
	private static boolean _active = false;

	public void onLoad()
	{
		if(isActive())
		{
			_active = true;
			System.out.println("Loaded Event: L2Coins [state: activated]");
		}
		else
			System.out.println("Loaded Event: L2Coins [state: deactivated]");
	}

	/**
	 * Читает статус эвента из базы.
	 */
	private static boolean isActive()
	{
		return Helper.IsActive("L2Coins");
	}

	/**
	 * Запускает эвент
	 */
	public void startEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(Helper.SetActive("L2Coins", true))
		{
			System.out.println("Event 'L2Coins' started.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.coins.coins.AnnounceEventStarted", null);
		}
		else
			player.sendMessage("Event 'L2Coins' already started.");

		_active = true;

		show(Files.read("data/html/admin/events.htm", player), player);
	}

	/**
	 * Останавливает эвент
	 */
	public void stopEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(Helper.SetActive("L2Coins", false))
		{
			System.out.println("Event 'L2Coins' stopped.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.coins.coins.AnnounceEventStoped", null);
		}
		else
			player.sendMessage("Event 'L2Coins' not started.");

		_active = false;

		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public static void OnPlayerEnter(L2Player player)
	{
		if(_active)
			Announcements.getInstance().announceToPlayerByCustomMessage(player, "scripts.events.coins.coins.AnnounceEventStarted", null);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	/**
	 * Обработчик смерти мобов, управляющий эвентовым дропом
	 */
	public static void onDie(L2Character cha, L2Character attacker)
	{
		// Можно убивать мобов, которые выше 70-ого лвл, разница лвл составляет 10 лвл, не РБ
		if(_active && cha.isMonster() && !cha.isRaid() && attacker != null && attacker.getPlayer() != null && cha.getLevel() >= 70 && Math.abs(cha.getLevel() - attacker.getLevel()) < 10)
		{
			if(Rnd.chance(MOUSE_COIN_CHANCE))
			{
				addItem(attacker.getPlayer(), MOUSE_COIN, 1);
			}
		}
		// При убийстве РБ выше, чем 70 лвл, даётся много вкусностей
		if(_active && cha.isRaid() && attacker != null && attacker.getPlayer() != null && Math.abs(cha.getLevel() - attacker.getLevel()) < 10)
		{
			// Даём много итемов, всем пати мемберам
			try
			{
				if(attacker instanceof L2Playable)
				{
					final L2Player player = attacker.getPlayer();
					if(player == null)
						return;

					if(player.getParty() != null)
					{
						// Даём каждому, что не всасывл никто
						for(@SuppressWarnings("unused")
						L2Player pl : player.getParty().getPartyMembers())
						{
							// Формула расчёт своя, если что редактируем
							int count = (BASE_COIN_AFTER_RB * (cha.getLevel() - 69)) / player.getParty().getPartyMembers().size();
							if(count > 0)
							{
								addItem(attacker.getPlayer(), MOUSE_COIN, count);
							}
						}
					}
					else
					{
						// Убил один РБ (???), то получаешь все итемы только ты
						// Если подозрение, что с пати будут выходить.
						int count = (BASE_COIN_AFTER_RB * (cha.getLevel() - 69));
						if(count > 0)
						{
							addItem(attacker.getPlayer(), MOUSE_COIN, count);
						}
					}
				}
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}