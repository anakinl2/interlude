package services.EnterNornilsGarden;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Files;

public class EnterNornilsGarden extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void enter()
	{
		L2Player player = (L2Player) self;

		if(!player.isInParty())
		{
			show(Files.read("data/scripts/services/EnterNornilsGarden/32330-01.htm", player), player);
			return;
		}

		for(L2Player member : player.getParty().getPartyMembers())
		{
			//if(member.getQuestState(_179_IntoTheLargeCavern.class) != null)
			//{
			//	show(Files.read("data/scripts/services/EnterNornilsGarden/32330-02.htm", player), player);
			//	return;
			//}
			if(member.getLevel() < 15 || member.getLevel() > 22 || member.getClassId().level() != 0)
			{
				player.getParty().broadcastToPartyMembers(new SystemMessage(2097).addString(member.getName()));
				show(Files.read("data/scripts/services/EnterNornilsGarden/32330-03.htm", player), player);
				return;
			}
		}

		executeTask(player, "Util", "InstanceGatekeeper", new Object[] { new String[] { "11" } }, 5000);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Enter Nornils Garden");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}