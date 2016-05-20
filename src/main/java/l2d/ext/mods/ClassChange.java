package l2d.ext.mods;

import l2d.game.model.L2Player;
import l2d.game.model.base.ClassId;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.TutorialCloseHtml;
import l2d.game.serverpackets.TutorialShowHtml;
import l2d.game.tables.CharTemplateTable;
import l2d.util.Files;

/**
 * 
 * @author Midnex
 *
 */
public class ClassChange
{
	public static void showHtml(L2Player player)
	{
		String html = Files.read("data/html/mods/class_change/" + player.getActiveClassId() + ".htm", player);
		if(html == null || html.length() < 20)
		{			
			for(ClassId cid : ClassId.values())
			{
				if(cid.childOf(player.getClassId()) && cid.getLevel() == 4)
				{
					player.setNoble(true);
					player.broadcastPacket(new SocialAction(player.getObjectId(), 16));					
					changeClass(player, cid.getId());
					html = Files.read("data/html/mods/class_change/3rd.htm", player).replaceAll("%class_name%", CharTemplateTable.getClassNameById(cid.getId()));
					break;
				}
			}
		}
		
		if(html != null && html.length()>0)
		player.sendPacket(new TutorialShowHtml(html));
	}

	public static void useBypass(L2Player player, String bypass)
	{
		if(bypass.startsWith("_guide:chooseClass"))
		{			
			String[] cm = bypass.split(" ");
			short clas = Short.parseShort(cm[1]);
			if(clas == 999)
				player.sendPacket(new TutorialCloseHtml());
			else
			changeClass(player, clas);
		}
	}

	private static void changeClass(L2Player player, int i)
	{
		if(player.getClassId().getLevel() == 3)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_COMPLETED_THE_QUEST_FOR_3RD_OCCUPATION_CHANGE_AND_MOVED_TO_ANOTHER_CLASS_CONGRATULATIONS)); // для 3 профы
		else
			player.sendPacket(new SystemMessage(SystemMessage.CONGRATULATIONS_YOU_HAVE_TRANSFERRED_TO_A_NEW_CLASS)); // для 1 и 2 профы

		player.setClassId(i, false);
		player.broadcastUserInfo(true);
		player.rewardSkills();

		if(player.getClassId().getLevel() == 2)
			showHtml(player);
		else if (player.getClassId().getLevel() == 3)
		{
			player.sendPacket(new TutorialCloseHtml());
		}
	}
}