package services.RemoveDeathPenalty;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.SkillTable;
import l2d.util.Files;

/**
 * Используется NPC Black Judge (id: 30981) для сниятия с игрока Death Penalty
 */

public class RemoveDeathPenalty extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public static void showdialog()
	{
		L2Player player = (L2Player) self;
		String lang = player.getVar("lang@");
		String htmltext;
		if(player.getDeathPenalty().getLevel() > 0)
		{
			htmltext = Files.read("data/scripts/services/RemoveDeathPenalty/RemoveDeathPenalty.htm", player);
			htmltext += "<a action=\"bypass -h scripts_services.RemoveDeathPenalty.RemoveDeathPenalty:remove\">Снять штраф за смерть 1 уровня (" + getPrice() + " adena).</a>";
		}
		else
			htmltext = Files.read("data/scripts/services/RemoveDeathPenalty/RemoveDeathPenalty_no_deathpenalty.htm", player);

		show(htmltext, (L2Player) self);
	}

	public static void remove()
	{
		if(npc == null)
			return;
		L2Player player = (L2Player) self;
		if(player.getDeathPenalty().getLevel() > 0)
			if(player.getAdena() >= getPrice())
			{
				player.reduceAdena(getPrice());
				((L2NpcInstance) npc).doCast(SkillTable.getInstance().getInfo(5077, 1), player, false);
			}
			else
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else
			show(Files.read("data/scripts/services/RemoveDeathPenalty/RemoveDeathPenalty_no_aden.htm", player), player);
	}

	public static int getPrice()
	{
		byte playerLvl = ((L2Player) self).getLevel();
		if(playerLvl <= 19)
			return 3600; // Non-grade (confirmed)
		else if(playerLvl >= 20 && playerLvl <= 39)
			return 16400; // D-grade
		else if(playerLvl >= 40 && playerLvl <= 51)
			return 36200; // C-grade
		else if(playerLvl >= 52 && playerLvl <= 60)
			return 50400; // B-grade (confirmed)
		else if(playerLvl >= 61 && playerLvl <= 75)
			return 78200; // A-grade
		else
			return 102800; // S-grade
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: NPC RemoveDeathPenalty");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}