package services;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.tables.DoorTable;

/**
 * Используется в локации Eastern Border Outpost
 */
public class BorderOutpostDoormans extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;
	private static int DoorId = 24170001;

	public void onLoad()
	{
		System.out.println("Loaded Service: Border Outpost Doormans");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public static void openDoor()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		L2DoorInstance door = DoorTable.getInstance().getDoor(DoorId);
		door.openMe();
	}

	public static void closeDoor()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		L2DoorInstance door = DoorTable.getInstance().getDoor(DoorId);
		door.closeMe();
	}
}