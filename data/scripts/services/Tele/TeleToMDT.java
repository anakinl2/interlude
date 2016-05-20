package services.Tele;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;

public class TeleToMDT extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void onLoad()
	{
		System.out.println("Loaded Service: Teleport to Race Track");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void toMDT()
	{
		L2Player player = (L2Player) self;

		if(!checkCondition(player))
			return;

		player.setVar("backCoords", player.getX() + " " + player.getY() + " " + player.getZ());
		player.teleToLocation(12661, 181687, -3560);
	}

	public void fromMDT()
	{
		L2Player player = (L2Player) self;

		if(!checkCondition(player))
			return;

		String var = player.getVar("backCoords");
		if(var == null || var.equals(""))
		{
			teleOut(player);
			return;
		}
		String[] coords = var.split(" ");
		if(coords.length != 3)
		{
			teleOut(player);
			return;
		}
		player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
	}

	public void teleOut(L2Player player)
	{
		player.teleToLocation(12902, 181011, -3563);
		if(player.getVar("lang@").equalsIgnoreCase("en"))
			show("I don't know from where you came here, but I can teleport you the another border side.", player);
		else
			show("Я не знаю, как Вы попали сюда, но я могу Вас отправить за ограждение.", player);
	}

	public boolean checkCondition(L2Player player)
	{
		return !(player.isActionsDisabled() || player.isSitting());
	}
}