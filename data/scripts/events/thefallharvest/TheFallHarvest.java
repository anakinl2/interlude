package events.thefallharvest;

import java.io.File;
import java.util.ArrayList;

import com.lineage.Config;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.Announcements;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Multisell;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Files;
import com.lineage.util.Rnd;
import events.Helper;

public class TheFallHarvest extends Functions implements ScriptFile
{
	public L2Object self;
	public L2NpcInstance npc;
	private static int EVENT_MANAGER_ID = 31255;
	private static ArrayList<L2Spawn> _spawns = new ArrayList<L2Spawn>();

	private static boolean _active = false;
	private static boolean MultiSellLoaded = false;

	private static File multiSellFile = new File(Config.DATAPACK_ROOT, "data/scripts/events/thefallharvest/31255.xml");

	public void onLoad()
	{
		if(isActive())
		{
			_active = true;
			loadMultiSell();
			spawnEventManagers();
			System.out.println("Loaded Event: The Fall Harvest [state: activated]");
		}
		else
			System.out.println("Loaded Event: The Fall Harvest [state: deactivated]");
	}

	/**
	 * Читает статус эвента из базы.
	 * @return
	 */
	private static boolean isActive()
	{
		return Helper.IsActive("TheFallHarvest");
	}

	/**
	* Запускает эвент
	*/
	public void startEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(Helper.SetActive("TheFallHarvest", true))
		{
			loadMultiSell();
			spawnEventManagers();
			System.out.println("Event 'The Fall Harvest' started.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.thefallharvest.TheFallHarvest.AnnounceEventStarted", null);
		}
		else
			player.sendMessage("Event 'The Fall Harvest' already started.");

		_active = true;

		show(Files.read("data/html/admin/events3.htm", player), player);
	}

	/**
	* Останавливает эвент
	*/
	public void stopEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;
		if(Helper.SetActive("TheFallHarvest", false))
		{
			unSpawnEventManagers();
			System.out.println("Event 'The Fall Harvest' stopped.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.thefallharvest.TheFallHarvest.AnnounceEventStoped", null);
		}
		else
			player.sendMessage("Event 'The Fall Harvest' not started.");

		_active = false;

		show(Files.read("data/html/admin/events3.htm", player), player);
	}

	/**
	 * Спавнит эвент менеджеров
	 */
	private void spawnEventManagers()
	{
		final int EVENT_MANAGERS[][] =
		{
			{ 81921, 148921, -3467, 16384 },
			{ 146405, 28360, -2269, 49648 },
			{ 19319, 144919, -3103, 31135 },
			{ -82805, 149890, -3129, 33202 },
			{ -12347, 122549, -3104, 32603 },
			{ 110642, 220165, -3655, 61898 },
			{ 116619, 75463, -2721, 20881 },
			{ 85513, 16014, -3668, 23681 },
			{ 81999, 53793, -1496, 61621 },
			{ 148159, -55484, -2734, 44315 },
			{ 44185, -48502, -797, 27479 },
			{ 86899, -143229, -1293, 22021 }
		};

		Helper.SpawnNPCs(EVENT_MANAGER_ID, EVENT_MANAGERS, _spawns);
	}

	/**
	 * Удаляет спавн эвент менеджеров
	 */
	private void unSpawnEventManagers()
	{
		Helper.deSpawnNPCs(_spawns);
	}

	private static void loadMultiSell()
	{
		if(MultiSellLoaded)
			return;
		L2Multisell.getInstance().parseFile(multiSellFile);
		MultiSellLoaded = true;
	}

	public void onReload()
	{
		unSpawnEventManagers();
		if(MultiSellLoaded)
		{
			L2Multisell.getInstance().remove(multiSellFile);
			MultiSellLoaded = false;
		}
	}

	public void onShutdown()
	{
		unSpawnEventManagers();
		if(MultiSellLoaded)
		{
			L2Multisell.getInstance().remove(multiSellFile);
			MultiSellLoaded = false;
		}
	}

	/**
	 * Обработчик смерти мобов, управляющий эвентовым дропом
	 */
	public static void OnDie(L2Character cha, L2Character killer)
	{
		if(_active && Helper.SimpleCheckDrop(cha, killer) && Rnd.get(1000) <= Config.EVENT_TFH_POLLEN_CHANCE * killer.getPlayer().getRateItems() * Config.RATE_DROP_ITEMS * ((L2NpcInstance) cha).getTemplate().rateHp)
			((L2NpcInstance) cha).dropItem(killer.getPlayer(), 6391, 1);
	}

	public static void OnPlayerEnter(L2Player player)
	{
		if(_active)
			Announcements.getInstance().announceToPlayerByCustomMessage(player, "scripts.events.thefallharvest.TheFallHarvest.AnnounceEventStarted", null);
	}

	public static void OnReloadMultiSell()
	{
		MultiSellLoaded = false;
		if(_active)
			loadMultiSell();
	}
}