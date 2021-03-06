//***************
//* Autor       *
//* HellSystem  *
//* For L2Dream *
//***************
package npc.model;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Location;

public class PaganTeleporterInstance extends L2NpcInstance implements ScriptFile
{
	public final static int Pagan_Mark = 8067;

	public PaganTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("PaganTeleport "))
		{
			String xyz = command.replaceFirst("PaganTeleport ", "");
			Location loc;
			try
			{
				loc = new Location(xyz);
			}
			catch(Exception e)
			{
				System.out.println("PaganTeleport [" + xyz + "] Error! NPC: " + this.getNpcId() + " | " + player);
				e.printStackTrace();
				return;
			}
			if(player.getInventory().getCountOf(Pagan_Mark) > 0)
			{
				Functions.removeItem(player, Pagan_Mark, 0);
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