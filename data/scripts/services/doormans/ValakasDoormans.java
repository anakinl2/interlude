package services.doormans;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.tables.DoorTable;

/**
 * Используется в локации Forge Of The Gods
 * @autor: HellSystem
 * 27.09.09 | 15.21
 */
public class ValakasDoormans extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;
	private static final int Door1Id = 24210004;
	private static final int Door2Id = 24210005;
	private static final int Door3Id = 24210006;

	public void onLoad()
	{
		System.out.println("Loaded Service: Valakas Doormans");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public static void openDoor1()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		openDoor(Door1Id);
	}

	public static void openDoor2()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		openDoor(Door2Id);
	}

	public static void openDoor3()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		openDoor(Door3Id);
	}

	private static void openDoor(int doorId)
	{
		final int CLOSE_TIME = 20000; // 20 секунд
		L2DoorInstance door = DoorTable.getInstance().getDoor(doorId);
		if(!door.isOpen())
		{
			door.openMe();
			door.scheduleCloseMe(CLOSE_TIME);
		}
	}
}