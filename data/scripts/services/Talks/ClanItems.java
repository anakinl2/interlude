package services.Talks;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Player;
import l2d.util.Files;

/**
 * @author PaInKiLlEr
 *         Сервис проверки клан лидера у менеджеров продающих Apella сет, если не клан лидер
 *         то линкует на диалог о том что вы не клан лидер
 *         Выполнено специально для L2Dream.su
 */

public class ClanItems extends Functions implements ScriptFile
{
	private static String EnFilePatch = "data/html/default/";
	private static String RuFilePatch = "data/html-ru/default/";

	@Override
	public void onLoad()
	{}

	@Override
	public void onReload()
	{}

	@Override
	public void onShutdown()
	{}

	public void ClanItems32024()
	{
		L2Player player = (L2Player) self;
		if(player.isClanLeader())
		{
			if(player.getVar("lang@").equalsIgnoreCase("ru"))
				show(Files.read(RuFilePatch + "32024.htm", player), player);
			else
				show(Files.read(EnFilePatch + "32024.htm", player), player);
			return;
		}
		else if(player.getVar("lang@").equalsIgnoreCase("ru")) 
			show(Files.read(RuFilePatch + "32024-no.htm", player), player);
		else
			show(Files.read(EnFilePatch + "32024-no.htm", player), player);
	}

	public void ClanItems32025()
	{
		L2Player player = (L2Player) self;
		if(player.isClanLeader())
		{
			if(player.getVar("lang@").equalsIgnoreCase("ru"))
				show(Files.read(RuFilePatch + "32025.htm", player), player);
			else
				show(Files.read(EnFilePatch + "32025.htm", player), player);
			return;
		}
		else if(player.getVar("lang@").equalsIgnoreCase("ru")) 
			show(Files.read(RuFilePatch + "32025-no.htm", player), player);
		else
			show(Files.read(EnFilePatch + "32025-no.htm", player), player);
	}
}