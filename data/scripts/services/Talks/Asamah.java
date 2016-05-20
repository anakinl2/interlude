package services.Talks;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Files;

public class Asamah extends Functions implements ScriptFile
{
	private static final int Elrokian_Trap = 8763;

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void AsamahTrap()
	{
		L2NpcInstance n = (L2NpcInstance) npc;
		L2Player player = (L2Player) self;

		if(getItemCount(player, Elrokian_Trap) > 0)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				show(Files.read("data/html-ru/default/32115-3.htm", player), player);
			}
			else
			{
				show(Files.read("data/html/default/32115-3.htm", player), player);
			}
		}
		else
			n.onBypassFeedback(player, "Buy 233");
	}
}