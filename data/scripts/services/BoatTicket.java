package services;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.util.Files;

public class BoatTicket extends Functions implements ScriptFile
{
	// Boat Ticket: Primeval Isle to Rune.
	private static final int Boat_Ticket = 8924;

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void SingsingBoatTicket()
	{
		L2Player player = (L2Player) self;

		if(player.getAdena() >= 25000)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
				show(Files.read("data/html-ru/default/32106-1.htm", player), player);
			else
				show(Files.read("data/html/default/32106-1.htm", player), player);
			removeItem(player, 57, 25000);
			addItem(player, Boat_Ticket, 1);
		}
		else
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
	}
}