package services.Tele;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.util.Files;

public class TeleToPI extends Functions implements ScriptFile
{
	public static L2Object self;

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void OrahochinTele()
	{
		L2Player player = (L2Player) self;

		if(player.isInCombat() || player.isInDuel() || player.getKarma() > 0)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				show(Files.read("data/html-ru/teleporter/32111-1.htm", player), player);
			}
			else
			{
				show(Files.read("data/html/teleporter/32111-1.htm", player), player);
			}
			return;
		}
		else
		{
			player.teleToLocation(6200, -2930, -2965);
		}
		return;
	}

	public void GariachinTele()
	{
		L2Player player = (L2Player) self;

		if(player.isInCombat() || player.isInDuel() || player.getKarma() > 0)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				show(Files.read("data/html-ru/teleporter/32112-1.htm", player), player);
			}
			else
			{
				show(Files.read("data/html/teleporter/32112-1.htm", player), player);
			}
			return;
		}
		else
		{
			player.teleToLocation(7557, -5513, -3221);
		}
		return;
	}
}