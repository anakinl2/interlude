package commands.voiced;

import l2d.Config;
import l2d.db.mysql;
import l2d.ext.multilang.CustomMessage;
import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.model.L2Clan;
import l2d.game.model.L2ClanMember;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.ItemTable;
import l2d.util.Files;
import l2d.util.PrintfFormat;
import l2d.util.Strings;

public class Lang extends Functions implements IVoicedCommandHandler, ScriptFile
{

	public static L2Object self;
	public static L2NpcInstance npc;
	private String[] _commandList = new String[] { "lang", "cfg", "clan" };
	public static final PrintfFormat cfg_row = new PrintfFormat("<table><tr><td width=20></td><td width=120>%s:</td><td width=100>%s</td></tr></table>");
	public static final PrintfFormat cfg_button = new PrintfFormat("<button width=%d height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\" action=\"bypass -h user_cfg %s\" value=\"%s\">");

	public boolean useVoicedCommand(String command, L2Player activeChar, String args)
	{
		if(command.equals("cfg"))
		{
			if(args != null)
			{
				String[] param = args.split(" ");
				if(param.length == 2)
				{
					if(param[0].equalsIgnoreCase("dli"))
					{
						if(param[1].equalsIgnoreCase("on"))
						{
							activeChar.setVar("DroplistIcons", "1");
						}
						else if(param[1].equalsIgnoreCase("of"))
						{
							activeChar.unsetVar("DroplistIcons");
						}
					}

					if(param[0].equalsIgnoreCase("ssc"))
					{
						if(param[1].equalsIgnoreCase("of") && Config.SKILLS_SHOW_CHANCE)
						{
							activeChar.setVar("SkillsHideChance", "1");
						}
						else if(param[1].equalsIgnoreCase("on"))
						{
							activeChar.unsetVar("SkillsHideChance");
						}
					}

					if(param[0].equalsIgnoreCase("nomats"))
					{
						if(param[1].equalsIgnoreCase("of"))
						{
							activeChar.unsetVar("NoMats");
						}
						else if(param[1].equalsIgnoreCase("on"))
						{
							activeChar.setVar("NoMats", "1");
						}
					}
					
					if(param[0].equalsIgnoreCase("noe"))
					{
						if(param[1].equalsIgnoreCase("on"))
						{
							activeChar.setVar("NoExp", "1");
						}
						else if(param[1].equalsIgnoreCase("of"))
						{
							activeChar.unsetVar("NoExp");
						}
					}

					if(param[0].equalsIgnoreCase("notraders"))
					{
						if(param[1].equalsIgnoreCase("on"))
						{
							activeChar.setVar("notraders", "1");
						}
						else if(param[1].equalsIgnoreCase("of"))
						{
							activeChar.unsetVar("notraders");
						}
					}

					if(param[0].equalsIgnoreCase("notShowBuffAnim"))
					{
						if(param[1].equalsIgnoreCase("on"))
						{
							activeChar.setNotShowBuffAnim(true);
							activeChar.setVar("notShowBuffAnim", "1");
						}
						else if(param[1].equalsIgnoreCase("of"))
						{
							activeChar.setNotShowBuffAnim(false);
							activeChar.unsetVar("notShowBuffAnim");
						}
					}

					if(param[0].equalsIgnoreCase("autoloot"))
					{
						activeChar.setAutoLoot(Boolean.parseBoolean(param[1]));
					}

					if(param[0].equalsIgnoreCase("autolooth"))
					{
						activeChar.setAutoLootHerbs(Boolean.parseBoolean(param[1]));
					}
				}
			}
		}
		else if(command.equals("clan"))
		{
			if(!((activeChar.getClanPrivileges() & L2Clan.CP_CL_MANAGE_RANKS) == L2Clan.CP_CL_MANAGE_RANKS)){ return false; }
			if(args != null)
			{
				String[] param = args.split(" ");
				if(param.length > 0)
				{
					if(param[0].equalsIgnoreCase("allowwh"))
					{
						L2ClanMember cm = activeChar.getClan().getClanMember(param[1]);
						if(cm != null && cm.getPlayer() != null)
						{
							if(cm.getPlayer().getVarB("canWhWithdraw"))
							{
								cm.getPlayer().unsetVar("canWhWithdraw");
								activeChar.sendMessage("Privilege removed successfully");
							}
							else
							{
								cm.getPlayer().setVar("canWhWithdraw", "1");
								activeChar.sendMessage("Privilege given successfully");
							}
						}
						else
						{
							int pl_id = mysql.simple_get_int("obj_Id", "characters", "name LIKE '" + Strings.addSlashes(param[1]) + "' AND clanid=" + activeChar.getClanId());
							if(pl_id > 0)
							{
								int state = mysql.simple_get_int("value", "character_variables", "obj_id=" + pl_id + " AND name LIKE 'canWhWithdraw'");
								if(state > 0)
								{
									mysql.set("DELETE FROM `character_variables` WHERE obj_id=" + pl_id + " AND name LIKE 'canWhWithdraw'");
									activeChar.sendMessage("Privilege removed successfully");
								}
								else
								{
									mysql.set("INSERT INTO character_variables  (obj_id, type, name, value, expire_time) VALUES (" + pl_id + ",'user-var','canWhWithdraw','1',-1)");
									activeChar.sendMessage("Privilege given successfully");
								}
							}
							else
							{
								activeChar.sendMessage("Player not found.");
							}
						}
					}
				}
			}
			String dialog = Files.read("data/scripts/commands/voiced/clan.htm", activeChar);
			if(!Config.SERVICES_EXPAND_CWH_ENABLED)
			{
				dialog = dialog.replaceFirst("%whextprice%", "service disabled");
			}
			else
			{
				dialog = dialog.replaceFirst("%whextprice%", Config.SERVICES_EXPAND_CWH_PRICE + " " + ItemTable.getInstance().getTemplate(Config.SERVICES_EXPAND_CWH_ITEM).getName());
			}
			show(dialog, activeChar);
			return true;
		}

		String dialog = Files.read("data/scripts/commands/voiced/lang.htm", activeChar);

/*	if(activeChar.getVar("lang@").equalsIgnoreCase("en"))
		{
			dialog = dialog.replaceFirst("%lang%", "EN");
		}
		else
		{
			dialog = dialog.replaceFirst("%lang%", "RU");
		}*/

		/*if(activeChar.getVarB("DroplistIcons"))
		{
			dialog = dialog.replaceFirst("%dli%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%dli%", "Off");
		}
		*/

		
		if(activeChar.getVarB("NoMats"))
		{
			dialog = dialog.replaceFirst("%nomats%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%nomats%", "Off");
		}
		
		if(activeChar.getVarB("NoExp"))
		{
			dialog = dialog.replaceFirst("%noe%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%noe%", "Off");
		}

		/*if(activeChar.getVarB("trace"))
		{
			dialog = dialog.replaceFirst("%trace%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%trace%", "Off");
		}*/

		if(activeChar.getVarB("notraders"))
		{
			dialog = dialog.replaceFirst("%notraders%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%notraders%", "Off");
		}

		if(activeChar.getVarB("notShowBuffAnim"))
		{
			dialog = dialog.replaceFirst("%notShowBuffAnim%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%notShowBuffAnim%", "Off");
		}

	/*	if(!Config.SKILLS_SHOW_CHANCE)
		{
			dialog = dialog.replaceFirst("%ssc%", "N/A");
		}
		else if(!activeChar.getVarB("SkillsHideChance"))
		{
			dialog = dialog.replaceFirst("%ssc%", "On");
		}
		else
		{
			dialog = dialog.replaceFirst("%ssc%", "Off");
		}*/

		String additional = "";

		if(Config.AUTO_LOOT_INDIVIDUAL)
		{
			String bt;
			if(activeChar.isAutoLootEnabled())
			{
				bt = cfg_button.sprintf(new Object[] {
						100,
						"autoloot false",
						new CustomMessage("scripts.commands.voiced.Lang.Disable", activeChar).toString() });
			}
			else
			{
				bt = cfg_button.sprintf(new Object[] { 100, "autoloot true", new CustomMessage("scripts.commands.voiced.Lang.Enable", activeChar).toString() });
			}
			additional += cfg_row.sprintf(new Object[] { "Auto-loot", bt });

			if(activeChar.isAutoLootHerbsEnabled())
			{
				bt = cfg_button.sprintf(new Object[] {
						100,
						"autolooth false",
						new CustomMessage("scripts.commands.voiced.Lang.Disable", activeChar).toString() });
			}
			else
			{
				bt = cfg_button.sprintf(new Object[] { 100, "autolooth true", new CustomMessage("scripts.commands.voiced.Lang.Enable", activeChar).toString() });
			}
			additional += cfg_row.sprintf(new Object[] { "Auto-loot herbs", bt });
		}

		dialog = dialog.replaceFirst("%additional%", additional);

		show(dialog, activeChar);

		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}

	public void onLoad()
	{
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}
