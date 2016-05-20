package services.Tele;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;

public class TeleToCT extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void onLoad()
	{
		System.out.println("Loaded Service: Teleport to Cruma Tower");
	}

	public void toCT()
	{
		L2Player player = (L2Player) self;

		int Level = player.getLevel();

		if(Level <= 55)
			player.teleToLocation(17724, 114004, -11672);
		else
		{
			((L2NpcInstance) npc).showChatWindow(player, "data/html/teleporter/30483-1.htm");
		}
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}