package npc.model;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2MerchantInstance;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Files;

/**
 * @author Shef
 *         AI for Asamah
 */

public class AsamahInstance extends L2MerchantInstance implements ScriptFile
{
	public AsamahInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage(player, this);
		final QuestState quest = player.getQuestState("_111_ElrokianHuntersProof");
		String html;
		if(player.getLang().equalsIgnoreCase("ru"))
			html = Files.read("data/html-ru/default/32115.htm", player);
		else
			html = Files.read("data/html/default/32115.htm", player);

		if(player.isGM() || quest != null && quest.isCompleted())
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.Talks.Asamah:AsamahTrap\">Взять ловушку.</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.Talks.Asamah:AsamahTrap\">Equip the trap.</a>";
			}
		}
		msg.setHtml(html);
		player.sendPacket(msg);
	}
}