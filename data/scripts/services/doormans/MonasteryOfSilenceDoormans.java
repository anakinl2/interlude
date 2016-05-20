package services.doormans;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.tables.DoorTable;

/**
 * @author PaInKiLlEr
 *         Дверь на последнем этаже в Monastery of Silence.
 *         Открыть нельзя по одной половинке.
 *         Закрывается через 40 секунд после открытия.
 *         Сделать метод что бы после закрытия игроки не могут открыть дверь в течении 30 минут.
 *         Выполнено специально для L2Dream.su
 */

public class MonasteryOfSilenceDoormans extends Functions implements ScriptFile
{
	public final static int Key_of_Splendor_Room = 8056;
	private static final int Door1 = 23150003;
	private static final int Door2 = 23150004;

	public void onLoad()
	{
		System.out.println("Loaded Service: Monastery Of Silence Doormans");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public static void openSecondDoor()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		if(getItemCount(player, Key_of_Splendor_Room) == 0)
			return;

		openDoor(Door1);
		openDoor(Door2);
	}

	public static void press2ndSkull()
	{
		L2Player player = (L2Player) self;

		if(player == null || player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
			return;

		openDoor(Door1);
		openDoor(Door2);
	}

	private static void openDoor(int doorId)
	{
		final int CLOSE_TIME = 40000; // 40 секунд
		L2DoorInstance door = DoorTable.getInstance().getDoor(doorId);
		if(!door.isOpen() && door.allowOpen())
		{
			door.openMe();
			door.scheduleCloseMe(CLOSE_TIME);
			door.setAllowOpenTime(System.currentTimeMillis() + (30 * 60 * 1000) + CLOSE_TIME); // Разрешаем открывать только через 30 мин, после закрытия
		}
	}
}