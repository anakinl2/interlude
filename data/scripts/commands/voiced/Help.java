package commands.voiced;

import java.text.NumberFormat;
import java.util.Locale;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IVoicedCommandHandler;
import com.lineage.game.handler.VoicedCommandHandler;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.serverpackets.RadarControl;
import com.lineage.game.skills.Calculator;
import com.lineage.game.skills.Formulas;
import com.lineage.game.skills.Stats;
import com.lineage.game.skills.funcs.Func;
import com.lineage.game.tables.FakePlayersTable;
import com.lineage.game.templates.L2Weapon.WeaponType;
import com.lineage.util.Files;

public class Help extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private String[] _commandList = new String[] { "help", "whoami", "whoiam", "heading", "whofake", "sweep", "pflag", "cflag", "exp", "stats" };

	public void onLoad()
	{
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public boolean useVoicedCommand(String command, L2Player activeChar, String args)
	{
		command = command.intern();
		if(command.equalsIgnoreCase("help"))
			return help(command, activeChar, args);
		if(command.equalsIgnoreCase("whoami") || command.equalsIgnoreCase("whoiam"))
			return whoami(command, activeChar, args);
		if(command.equalsIgnoreCase("stats"))
			return stats(activeChar);
		if(command.equalsIgnoreCase("heading"))
		{
			activeChar.sendMessage(String.valueOf(activeChar.getHeading()));
			return true;
		}
		if(command.equalsIgnoreCase("whofake"))
			return whofake(command, activeChar, args);
		if(command.equalsIgnoreCase("sweep"))
			return sweep(command, activeChar, args);
		if(command.equalsIgnoreCase("pflag"))
			return pflag(command, activeChar, args);
		if(command.equalsIgnoreCase("cflag"))
			return cflag(command, activeChar, args);
		if(command.equalsIgnoreCase("exp"))
			return exp(command, activeChar, args);

		return false;
	}

	private static NumberFormat df = NumberFormat.getNumberInstance();
	static
	{
		df.setMaximumFractionDigits(2);
	}

	private boolean exp(String command, L2Player activeChar, String args)
	{
		if(activeChar.getLevel() >= (activeChar.isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()))
			show("Maximum level!", activeChar);
		else
		{
			long exp = Experience.LEVEL[activeChar.getLevel() + 1] - activeChar.getExp();
			double count = 0;
			if(args != null && !args.isEmpty())
			{
				String[] param = args.split(" ");
				if(Quest.isdigit(param[0]))
					count = exp / Double.parseDouble(param[0]);
			}
			String ret = "Exp left: " + exp;
			if(count > 0)
				ret += "<br>Monsters left: " + df.format(count);
			show(ret, activeChar);
		}
		return true;
	}

	private boolean pflag(String command, L2Player activeChar, String args)
	{
		if(!activeChar.isInParty())
			return false;
		RadarControl rc = new RadarControl(0, 1, activeChar.getLoc());
		for(L2Player p : activeChar.getParty().getPartyMembers())
			if(p != activeChar)
				p.sendPacket(rc);
		return true;
	}

	private boolean cflag(String command, L2Player activeChar, String args)
	{
		if(activeChar.getClan() == null)
			return false;
		RadarControl rc = new RadarControl(0, 1, activeChar.getLoc());
		for(L2Player p : activeChar.getClan().getOnlineMembers(activeChar.getObjectId()))
			p.sendPacket(rc);
		return true;
	}

	private boolean help(String command, L2Player activeChar, String args)
	{
		String dialog = Files.read("data/scripts/commands/voiced/help.htm", activeChar);
		show(dialog, activeChar);
		return true;
	}

	private boolean whoami(String command, L2Player activeChar, String args)
	{
		showInfo(activeChar);
		return true;
	}

	private boolean whofake(String command, L2Player activeChar, String args)
	{
		if(!activeChar.isGM())
			return false;
		StringBuilder sb = new StringBuilder("");
		for(String p : FakePlayersTable.getActiveFakePlayers())
			sb.append(p).append(" ");
		show(sb.toString(), activeChar);
		return true;
	}

	private boolean sweep(String command, L2Player activeChar, String args)
	{
		if(activeChar.getSkillLevel(42) > 0)
			for(L2Character target : activeChar.getAroundCharacters(300, 200))
				if(target.isMonster() && target.isDead() && ((L2MonsterInstance) target).isSweepActive())
				{
					activeChar.getAI().Cast(activeChar.getKnownSkill(42), target);
					return true;
				}
		return false;
	}

	public static void showInfo(L2Player player)
	{
		if(player == null)
			return;
		StringBuilder dialog = new StringBuilder("<html><body>");

		NumberFormat df = NumberFormat.getNumberInstance(Locale.ENGLISH);
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(0);

		dialog.append("<center><font color=\"LEVEL\">Basic info</font></center><br><table width=\"100%\"><tr>");

		dialog.append("<td>Name</td><td>").append(player.getName()).append("</td>");
		dialog.append("<td>Login</td><td>").append(player.getAccountName()).append("</td>");
		dialog.append("</tr><tr>");
		dialog.append("<td>Class</td><td>").append(player.getClassId().name()).append("</td>");
		dialog.append("<td>IP</td><td>").append(player.getNetConnection().getIpAddr()).append("</td>");
		dialog.append("</tr><tr>");
		dialog.append("<td>Level</td><td>").append(player.getLevel()).append("</td>");
		dialog.append("<td>ObjId</td><td>").append(player.getObjectId()).append("</td>");

		dialog.append("</tr></table><br><center><font color=\"LEVEL\">Stats</font></center><br><table width=\"100%\"><tr>");

		dialog.append("<td>HP regen</td><td>").append(df.format(Formulas.calcHpRegen(player))).append("</td>");
		dialog.append("<td>CP regen</td><td>").append(df.format(Formulas.calcCpRegen(player))).append("</td>");
		dialog.append("</tr><tr>");
		dialog.append("<td>MP regen</td><td>").append(df.format(Formulas.calcMpRegen(player))).append("</td>");
		dialog.append("<td>HP drain</td><td>").append(df.format(player.calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null))).append("%</td>");
		dialog.append("</tr><tr>");
		dialog.append("<td>HP gain</td><td>").append(df.format(player.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null))).append("%</td>");
		dialog.append("<td>MP gain</td><td>").append(df.format(player.calcStat(Stats.MANAHEAL_EFFECTIVNESS, 100, null, null))).append("%</td>");
		dialog.append("</tr><tr>");
		dialog.append("<td>Crit damage</td><td>").append(df.format(player.calcStat(Stats.CRITICAL_DAMAGE, 200, null, null))).append("% + ").append((int) player.calcStat(Stats.CRITICAL_DAMAGE_STATIC, null, null)).append("</td>");
		dialog.append("<td>Magic crit</td><td>").append(df.format(player.getMagicCriticalRate(null, null))).append("%</td>");
		dialog.append("</tr><tr>");
		dialog.append("<td>Blow rate</td><td>").append(df.format(player.calcStat(Stats.FATALBLOW_RATE, player.getDEX(), null, null))).append("%</td>");
		dialog.append("<td>Luck</td><td>").append((int) (player.getRateExp() * 100)).append("%</td>");

		L2ItemInstance shld = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(shld != null && shld.getItemType() == WeaponType.NONE)
		{
			dialog.append("</tr><tr>");
			dialog.append("<td>Shield Def</td><td>").append(player.getShldDef()).append("</td>");
			dialog.append("<td></td><td></td>");
		}

		dialog.append("</tr></table><br><center><font color=\"LEVEL\">Resists</font></center><br><table width=\"70%\">");

		int FIRE_RECEPTIVE = (int) player.calcStat(Stats.FIRE_RECEPTIVE, 0, null, null);
		if(FIRE_RECEPTIVE != 0)
			dialog.append("<tr><td>Fire</td><td>").append(-FIRE_RECEPTIVE).append("</td></tr>");

		int WIND_RECEPTIVE = (int) player.calcStat(Stats.WIND_RECEPTIVE, 0, null, null);
		if(WIND_RECEPTIVE != 0)
			dialog.append("<tr><td>Wind</td><td>").append(-WIND_RECEPTIVE).append("</td></tr>");

		int WATER_RECEPTIVE = (int) player.calcStat(Stats.WATER_RECEPTIVE, 0, null, null);
		if(WATER_RECEPTIVE != 0)
			dialog.append("<tr><td>Water</td><td>").append(-WATER_RECEPTIVE).append("</td></tr>");

		int EARTH_RECEPTIVE = (int) player.calcStat(Stats.EARTH_RECEPTIVE, 0, null, null);
		if(EARTH_RECEPTIVE != 0)
			dialog.append("<tr><td>Earth</td><td>").append(-EARTH_RECEPTIVE).append("</td></tr>");

		int SACRED_RECEPTIVE = (int) player.calcStat(Stats.SACRED_RECEPTIVE, 0, null, null);
		if(SACRED_RECEPTIVE != 0)
			dialog.append("<tr><td>Light</td><td>").append(-SACRED_RECEPTIVE).append("</td></tr>");

		int UNHOLY_RECEPTIVE = (int) player.calcStat(Stats.UNHOLY_RECEPTIVE, 0, null, null);
		if(UNHOLY_RECEPTIVE != 0)
			dialog.append("<tr><td>Darkness</td><td>").append(-UNHOLY_RECEPTIVE).append("</td></tr>");

		int BLEED_RECEPTIVE = 100 - (int) player.calcStat(Stats.BLEED_RECEPTIVE, 100, null, null);
		if(BLEED_RECEPTIVE != 0)
			dialog.append("<tr><td>Bleed</td><td>").append(BLEED_RECEPTIVE).append("%</td></tr>");

		int POISON_RECEPTIVE = 100 - (int) player.calcStat(Stats.POISON_RECEPTIVE, 100, null, null);
		if(POISON_RECEPTIVE != 0)
			dialog.append("<tr><td>Poison</td><td>").append(POISON_RECEPTIVE).append("%</td></tr>");

		int DEATH_RECEPTIVE = 100 - (int) player.calcStat(Stats.DEATH_RECEPTIVE, 100, null, null);
		if(DEATH_RECEPTIVE != 0)
			dialog.append("<tr><td>Death</td><td>").append(DEATH_RECEPTIVE).append("%</td></tr>");

		int STUN_RECEPTIVE = 100 - (int) player.calcStat(Stats.STUN_RECEPTIVE, 100, null, null);
		if(STUN_RECEPTIVE != 0)
			dialog.append("<tr><td>Stun</td><td>").append(STUN_RECEPTIVE).append("%</td></tr>");

		int ROOT_RECEPTIVE = 100 - (int) player.calcStat(Stats.ROOT_RECEPTIVE, 100, null, null);
		if(ROOT_RECEPTIVE != 0)
			dialog.append("<tr><td>Root</td><td>").append(ROOT_RECEPTIVE).append("%</td></tr>");

		int SLEEP_RECEPTIVE = 100 - (int) player.calcStat(Stats.SLEEP_RECEPTIVE, 100, null, null);
		if(SLEEP_RECEPTIVE != 0)
			dialog.append("<tr><td>Sleep</td><td>").append(SLEEP_RECEPTIVE).append("%</td></tr>");

		int PARALYZE_RECEPTIVE = 100 - (int) player.calcStat(Stats.PARALYZE_RECEPTIVE, 100, null, null);
		if(PARALYZE_RECEPTIVE != 0)
			dialog.append("<tr><td>Paralyze</td><td>").append(PARALYZE_RECEPTIVE).append("%</td></tr>");

		int MENTAL_RECEPTIVE = 100 - (int) player.calcStat(Stats.MENTAL_RECEPTIVE, 100, null, null);
		if(MENTAL_RECEPTIVE != 0)
			dialog.append("<tr><td>Mental</td><td>").append(MENTAL_RECEPTIVE).append("%</td></tr>");

		int DEBUFF_RECEPTIVE = 100 - (int) player.calcStat(Stats.DEBUFF_RECEPTIVE, 100, null, null);
		if(DEBUFF_RECEPTIVE != 0)
			dialog.append("<tr><td>Debuff</td><td>").append(DEBUFF_RECEPTIVE).append("%</td></tr>");

		int CANCEL_RECEPTIVE = 100 - (int) player.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null);
		if(CANCEL_RECEPTIVE != 0)
			dialog.append("<tr><td>Cancel</td><td>").append(CANCEL_RECEPTIVE).append("%</td></tr>");

		int SWORD_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.SWORD_WPN_RECEPTIVE, 100, null, null);
		if(SWORD_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Sword</td><td>").append(SWORD_WPN_RECEPTIVE).append("%</td></tr>");

		int DUAL_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.DUAL_WPN_RECEPTIVE, 100, null, null);
		if(DUAL_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Dual Sword</td><td>").append(DUAL_WPN_RECEPTIVE).append("%</td></tr>");

		int BLUNT_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.BLUNT_WPN_RECEPTIVE, 100, null, null);
		if(BLUNT_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Blunt</td><td>").append(BLUNT_WPN_RECEPTIVE).append("%</td></tr>");

		int DAGGER_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.DAGGER_WPN_RECEPTIVE, 100, null, null);
		if(DAGGER_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Dagger/Rapier</td><td>").append(DAGGER_WPN_RECEPTIVE).append("%</td></tr>");

		int BOW_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.BOW_WPN_RECEPTIVE, 100, null, null);
		if(BOW_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Bow/Crossbow</td><td>").append(BOW_WPN_RECEPTIVE).append("%</td></tr>");

		int POLE_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.POLE_WPN_RECEPTIVE, 100, null, null);
		if(POLE_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Polearm</td><td>").append(POLE_WPN_RECEPTIVE).append("%</td></tr>");

		int FIST_WPN_RECEPTIVE = 100 - (int) player.calcStat(Stats.FIST_WPN_RECEPTIVE, 100, null, null);
		if(FIST_WPN_RECEPTIVE != 0)
			dialog.append("<tr><td>Fist weapons</td><td>").append(FIST_WPN_RECEPTIVE).append("%</td></tr>");

		int CRIT_CHANCE_RECEPTIVE = 100 - (int) player.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null);
		if(CRIT_CHANCE_RECEPTIVE != 0)
			dialog.append("<tr><td>Crit get chance</td><td>").append(CRIT_CHANCE_RECEPTIVE).append("%</td></tr>");

		int CRIT_DAMAGE_RECEPTIVE = 100 - (int) player.calcStat(Stats.CRIT_DAMAGE_RECEPTIVE, 100, null, null);
		if(CRIT_DAMAGE_RECEPTIVE != 0)
			dialog.append("<tr><td>Crit get damage</td><td>").append(CRIT_DAMAGE_RECEPTIVE).append("%</td></tr>");

		if(FIRE_RECEPTIVE == 0 && WIND_RECEPTIVE == 0 && WATER_RECEPTIVE == 0 && EARTH_RECEPTIVE == 0 && UNHOLY_RECEPTIVE == 0 && SACRED_RECEPTIVE // primary elements
		== 0 && BLEED_RECEPTIVE == 0 && DEATH_RECEPTIVE == 0 && STUN_RECEPTIVE // phys debuff
		== 0 && POISON_RECEPTIVE == 0 && ROOT_RECEPTIVE == 0 && SLEEP_RECEPTIVE == 0 && PARALYZE_RECEPTIVE == 0 && MENTAL_RECEPTIVE == 0 && DEBUFF_RECEPTIVE == 0 && CANCEL_RECEPTIVE // mag debuff
		== 0 && SWORD_WPN_RECEPTIVE == 0 && DUAL_WPN_RECEPTIVE == 0 && BLUNT_WPN_RECEPTIVE == 0 && DAGGER_WPN_RECEPTIVE == 0 && BOW_WPN_RECEPTIVE == 0 && POLE_WPN_RECEPTIVE == 0 && FIST_WPN_RECEPTIVE // weapons
		== 0 && CRIT_CHANCE_RECEPTIVE == 0 && CRIT_DAMAGE_RECEPTIVE == 0 // other
		)
			dialog.append("</table>No resists</body></html>");
		else
			dialog.append("</table></body></html>");
		show(dialog.toString(), player);
	}

	private static boolean stats(L2Player player)
	{
		if(player == null)
			return false;

		StringBuilder dialog = new StringBuilder("<html><body>");

		dialog.append("<center><font color=\"LEVEL\">All Stats</font></center><br><br>");

		Calculator[] calculators = player.getCalculators();
		if(calculators == null || calculators.length == 0)
			dialog.append("None");
		else
			for(Calculator c : calculators)
				dialog.append("[scripts_commands.voiced.Help:showfuncs ").append(c._stat.getValue()).append("|").append(c._stat.getValue()).append("]<br1>");

		dialog.append("</body></html>");

		show(dialog.toString(), player);
		return true;
	}

	public static void showstats()
	{
		stats((L2Player) self);
	}

	public static void showfuncs(String[] var)
	{
		L2Player player = (L2Player) self;
		if(var.length != 1)
		{
			show("Некорректные данные", player);
			return;
		}

		String value = var[0];
		Stats stat = Stats.valueOfXml(value);

		StringBuilder dialog = new StringBuilder("<html><body>");

		dialog.append("<table><tr>");
		dialog.append("<td><button value=\"Back\" action=\"bypass -h scripts_commands.voiced.Help:showstats\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		dialog.append("<td><button value=\"Refresh\" action=\"bypass -h scripts_commands.voiced.Help:showfuncs ").append(value).append("\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		dialog.append("</tr></table>");

		dialog.append("<br><center><font color=\"LEVEL\">Stat: ").append(stat).append("</font></center><br><br>");

		Calculator[] calculators = player.getCalculators();
		if(calculators == null || calculators.length == 0 || stat == null)
			dialog.append("None");
		else
		{
			Calculator c = calculators[stat.ordinal()];
			Func[] funcs = c.getFunctions();
			for(int i = 0; i < funcs.length; i++)
				if(funcs[i]._funcOwner != null)
					dialog.append(funcs[i]._funcOwner.toString()).append(" [").append(Integer.toHexString(funcs[i]._order)).append("]<br1>");
				else
					dialog.append(funcs[i].getClass().getSimpleName()).append(" [").append(Integer.toHexString(funcs[i]._order)).append("]<br1>");
		}

		dialog.append("</body></html>");

		show(dialog.toString(), player);
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}