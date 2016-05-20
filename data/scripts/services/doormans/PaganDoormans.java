package services.doormans;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.DoorTable;
import com.lineage.util.Files;

/**
 * Используется в локации Pagan Temple
 * @author: HellSystem
 * @moding: Felixx
 */
public class PaganDoormans extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;
	public final static int Anteroom_Key = 8273;
	public final static int Chapel_Key = 8274;
	private static final int MainDoorId = 19160001;
	private static final int SecondDoor1Id = 19160011;
	private static final int SecondDoor2Id = 19160010;
	private static final int Door1Id = 19160012;
	private static final int Door2Id = 19160013;
	private static final int Door3Id = 19160014;
	private static final int Door4Id = 19160015;
	private static final int Door5Id = 19160016;
	private static final int Door6Id = 19160017;

	public void onLoad()
	{
		System.out.println("Loaded Service: Pagan Doormans");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public static void openMainDoor()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		if(getItemCount(player, 8064) == 0 && getItemCount(player, 8067) == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
			return;
		}

		openDoor(MainDoorId);
	}

	public static void openSecondDoor()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		if(getItemCount(player, 8067) == 0)
		{
			return;
		}

		openDoor(SecondDoor1Id);
		openDoor(SecondDoor2Id);
	}

	public static void pressSkull()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		openDoor(MainDoorId);
	}

	public static void press2ndSkull()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		openDoor(SecondDoor1Id);
		openDoor(SecondDoor2Id);
	}

	public static void openDoor1()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		if(player.getInventory().getCountOf(Anteroom_Key) > 0)
		{
			Functions.removeItem(player, Anteroom_Key, 1);
			openDoor(Door1Id);
			return;
		}
		else
		{
			String lang = player.getVar("lang@");
				show(Files.read("data/scripts/services/doormans/NoAnteroom-" + lang.toLowerCase() + ".htm", player), player);
			return;
		}
	}
	
	public static void openDoor2()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		if(player.getInventory().getCountOf(Anteroom_Key) > 0)
		{
			Functions.removeItem(player, Anteroom_Key, 1);
			openDoor(Door2Id);
			return;
		}
		else
		{
			String lang = player.getVar("lang@");
				show(Files.read("data/scripts/services/doormans/NoAnteroom-" + lang.toLowerCase() + ".htm", player), player);
			return;
		}
	}
	
	public static void openDoor3()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		if(player.getInventory().getCountOf(Anteroom_Key) > 0)
		{
			Functions.removeItem(player, Anteroom_Key, 1);
			openDoor(Door3Id);
			return;
		}
		else
		{
			String lang = player.getVar("lang@");
				show(Files.read("data/scripts/services/doormans/NoAnteroom-" + lang.toLowerCase() + ".htm", player), player);
			return;
		}
	}
	
	public static void openDoor4()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;
		
		if(player.getInventory().getCountOf(Anteroom_Key) > 0)
		{
			Functions.removeItem(player, Anteroom_Key, 1);
			openDoor(Door4Id);
			return;
		}
		else
		{
			String lang = player.getVar("lang@");
				show(Files.read("data/scripts/services/doormans/NoAnteroom-" + lang.toLowerCase() + ".htm", player), player);
			return;
		}
	}
	
	public static void openDoor5()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;
		
		if(player.getInventory().getCountOf(Chapel_Key) > 0)
		{
			Functions.removeItem(player, Chapel_Key, 1);
			openDoor(Door5Id);
			return;
		}
		else
		{
			String lang = player.getVar("lang@");
				show(Files.read("data/scripts/services/doormans/NoChapel-" + lang.toLowerCase() + ".htm", player), player);
			return;
		}
	}
	
	public static void openDoor6()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;
		
		if(player.getInventory().getCountOf(Chapel_Key) > 0)
		{
			Functions.removeItem(player, Chapel_Key, 1);
			openDoor(Door6Id);
			return;
		}
		else
		{
			String lang = player.getVar("lang@");
				show(Files.read("data/scripts/services/doormans/NoChapel-" + lang.toLowerCase() + ".htm", player), player);
			return;
		}
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