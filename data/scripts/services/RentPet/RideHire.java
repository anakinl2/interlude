package services.RentPet;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SetupGauge;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.PetDataTable;

public class RideHire extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public static String DialogAppend_30827(Integer val)
	{
		if(val == 0)
		{
			L2Player player = (L2Player) self;
			String lang = player.getVar("lang@");
			if(lang == null)
				lang = "en";
			if(lang.equalsIgnoreCase("en"))
				return "<br>[scripts_services.RideHire:ride_prices|Ride hire mountable pet.]";
			return "<br>[scripts_services.RideHire:ride_prices|Взять на прокат ездовое животное.]";
		}
		return "";
	}

	public static void ride_prices()
	{
		show("data/scripts/services/RentPet/ride-prices.htm", (L2Player) self);
	}

	public static void ride(String[] args)
	{
		L2Player player = (L2Player) self;
		String lang = player.getVar("lang@");
		if(lang == null)
			lang = "en";
		if(args.length != 3)
		{
			if(lang.equalsIgnoreCase("en"))
				show("Incorrect input", player);
			else
				show("Некорректные данные", player);
			return;
		}

		if(player.isActionsDisabled() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 250)
			return;

		if(player.getPet() != null || player.isMounted())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ALREADY_HAVE_A_PET));
			return;
		}

		int npc_id;

		switch(Integer.parseInt(args[0]))
		{
			case 1:
				npc_id = PetDataTable.WYVERN_ID;
				break;
			case 2:
				npc_id = PetDataTable.STRIDER_WIND_ID;
				break;
			default:
				if(lang.equalsIgnoreCase("en"))
					show("Unknown pet.", player);
				else
					show("У меня нет таких питомцев!", player);
				return;
		}

		if((npc_id == PetDataTable.WYVERN_ID || npc_id == PetDataTable.STRIDER_WIND_ID) && !SiegeManager.getCanRide())
		{
			if(lang.equalsIgnoreCase("en"))
				show("Can't ride wyvern/strider while Siege in progress.", player);
			else
				show("Прокат виверн/страйдеров не работает во время осады.", player);
			return;
		}

		Integer time = Integer.parseInt(args[1]);
		Integer price = Integer.parseInt(args[2]);

		if(time > 1800)
		{
			if(lang.equalsIgnoreCase("en"))
				show("Too long time to ride.", player);
			else
				show("Слишком большое время.", player);
			return;
		}

		if(player.getAdena() < price)
		{
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}

		player.reduceAdena(price);

		doLimitedRide(player, npc_id, time);
	}

	public static void doLimitedRide(L2Player player, Integer npc_id, Integer time)
	{
		if(!ride(player, npc_id))
			return;
		player.sendPacket(new SetupGauge(3, time * 1000));
		executeTask(player, "services.RideHire", "rideOver", new Object[0], time * 1000);
	}

	public static void rideOver()
	{
		if(self == null)
			return;
		L2Player player = (L2Player) self;
		unRide(player);
		String lang = player.getVar("lang@");
		if(lang.equalsIgnoreCase("en"))
			show("Ride time is over.<br><br>Welcome back again!", player);
		else
			show("Время проката закончилось. Приходите еще!", player);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Ride Hire");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}