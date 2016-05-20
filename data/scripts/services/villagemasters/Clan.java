package services.villagemasters;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2VillageMasterInstance;

public class Clan extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void onLoad()
	{
		System.out.println("Loaded Service: Villagemasters [Clan Operations]");
	}

	public void CheckCreateClan()
	{
		if(npc == null || self == null)
			return;
		L2Player pl = (L2Player) self;
		String htmltext = "clan-02.htm";
		// Player less 10 levels, and can not create clan
		if(pl.getLevel() <= 9)
			htmltext = "clan-06.htm";
		// Player already is a clan by leader and can not newly create clan
		else if(pl.isClanLeader())
			htmltext = "clan-07.htm";
		// Player already consists in clan and can not create clan
		else if(pl.getClan() != null)
			htmltext = "clan-09.htm";
		((L2VillageMasterInstance) npc).showChatWindow(pl, "data/html/villagemaster/" + htmltext);
	}

	public void CheckDissolveClan()
	{
		if(npc == null || self == null)
			return;
		L2Player pl = (L2Player) self;
		String htmltext = "clan-01.htm";
		if(pl.isClanLeader())
			htmltext = "clan-04.htm";
		else
		// Player already consists in clan and can not create clan
		if(pl.getClan() != null)
			htmltext = "clan-08.htm";
		// Player not in clan and can not dismiss clan
		else
			htmltext = "clan-11.htm";
		((L2VillageMasterInstance) npc).showChatWindow(pl, "data/html/villagemaster/" + htmltext);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}