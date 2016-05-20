package npc.model;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Location;

public class RoyalRushTeleporterInstance extends L2NpcInstance implements ScriptFile
{
	public final static int Used_Grave_Pass = 7261;
	public final static int Antique_Brooch = 7262;

	public RoyalRushTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("RoyalRushTeleport "))
		{
			String xyz = command.replaceFirst("RoyalRushTeleport ", "");
			Location loc;
			try
			{
				loc = new Location(xyz);
			}
			catch(Exception e)
			{
				System.out.println("RoyalRushTeleport [" + xyz + "] Error! NPC: " + this.getNpcId() + " | " + player);
				e.printStackTrace();
				return;
			}
			if(player.getInventory().getCountOf(Used_Grave_Pass) > 0)
			{
				Functions.removeItem(player, Used_Grave_Pass, 1);
				player.teleToLocation(loc);
				return;
			}
			if(player.getInventory().getCountOf(Antique_Brooch) > 0)
			{
				player.teleToLocation(loc);
				return;
			}
			showChatWindow(player, 1);
			return;
		}
		super.onBypassFeedback(player, command);
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}
}