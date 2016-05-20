package services.villagemasters;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2VillageMasterInstance;

public class Ally extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void onLoad()
	{
		System.out.println("Loaded Service: Villagemasters [Alliance Operations]");
	}

	public void CheckCreateAlly()
	{
		if(npc == null || self == null)
			return;
		L2Player pl = (L2Player) self;
		String htmltext = "ally-01.htm";
		if(pl.isClanLeader())
			htmltext = "ally-02.htm";
		((L2VillageMasterInstance) npc).showChatWindow(pl, "data/html/villagemaster/" + htmltext);
	}

	public void CheckDissolveAlly()
	{
		if(npc == null || self == null)
			return;
		L2Player pl = (L2Player) self;
		String htmltext = "ally-01.htm";
		if(pl.isAllyLeader())
			htmltext = "ally-03.htm";
		((L2VillageMasterInstance) npc).showChatWindow(pl, "data/html/villagemaster/" + htmltext);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}