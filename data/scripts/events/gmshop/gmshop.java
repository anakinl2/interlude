package events.gmshop;

import java.util.ArrayList;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.Announcements;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import com.lineage.util.Files;
import events.Helper;

// Эвент gmshop
public class gmshop extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;
	private static int EVENT_MANAGER_ID = 91100;
	private static ArrayList<L2Spawn> _spawns = new ArrayList<L2Spawn>();

	public static boolean _active = false;

	/**
	 * Спавнит эвент менеджеров
	 */
	private void spawnEventManagers()
	{
		final int EVENT_MANAGERS[][] =
		{
			{ -84122, 242356, -3755, 17330 },	// Talking Island Village
			{ 46841, 49971, -3086, 31228 },		// Elven Village
			{ 11118, 16004, -4611, 8192 },		// Dark Elven Village
			{ 114914, -178976, -867, 57892 },	// Dwarven Village
			{ -44817, -113232, -246, 9785 },	// Orc Village
			{ 117631, 45945, 324, 62552 },		// Kamael Village
			{ -14877, 123687, -3143, 6712 },	// Gludio
			{ 82756, 53565, -1522, 48161 },		// Oren
			{ 17912, 145373, -3077, 8993 },		// Dion
			{ 82116, 148136, -3493, 49511 },	// Giran
			{ 82119, 149076, -3494, 8192 },		// Giran2
			{ 111535, 220010, -3697, 57703 },	// Heine
			{ 117114, 77151, -2720, 34826 },	// Hunter
			{ 147451, 27114, -2230, 46596 },	// Aden
			{ 148114, -55929, -2807, 57112 },	// Goddard
			{ 86927, -142760, -1366, 17953 },	// Schuttgart
			{ 42741, -48531, -823, 4837 }		// Rune
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

	/**
	 * Читает статус эвента из базы.
	 * @return
	 */
	private static boolean isActive()
	{
		return Helper.IsActive("gmshop");
	}

	/**
	* Запускает эвент
	*/
	public void startEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(Helper.SetActive("gmshop", true))
		{
			spawnEventManagers();
			System.out.println("Event 'gmshop' started.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.gmshop.gmshop.AnnounceEventStarted", null);
		}
		else
			player.sendMessage("Event 'gmshop' already started.");

		_active = true;

		show(Files.read("data/html/admin/events2.htm", player), player);
	}

	/**
	* Останавливает эвент
	*/
	public void stopEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(Helper.SetActive("gmshop", false))
		{
			unSpawnEventManagers();
			System.out.println("Event 'gmshop' stopped.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.gmshop.gmshop.AnnounceEventStoped", null);
		}
		else
			player.sendMessage("Event 'gmshop' not started.");

		_active = false;
		show(Files.read("data/html/admin/events2.htm", player), player);
	}

	public void onLoad()
	{
		if(isActive())
		{
			_active = true;
			spawnEventManagers();
			System.out.println("Loaded Event: gmshop [state: activated]");
		}
		else
			System.out.println("Loaded Event: gmshop [state: deactivated]");
	}

	public void onReload()
	{
		unSpawnEventManagers();
	}

	public void onShutdown()
	{
		unSpawnEventManagers();
	}
}