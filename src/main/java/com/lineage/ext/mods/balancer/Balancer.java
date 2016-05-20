package com.lineage.ext.mods.balancer;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import com.lineage.ext.scripts.Functions;
import javolution.util.FastMap;
import com.lineage.Config;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.L2World;
import l2d.game.model.base.ClassId;
import l2d.game.model.base.PlayerClass;
import l2d.game.serverpackets.ShowBoard;
import l2d.game.serverpackets.TutorialShowHtml;
import l2d.game.skills.effects.EffectTemplate;
import l2d.game.tables.CharTemplateTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import com.lineage.util.Files;
import com.lineage.util.GArray;
import com.lineage.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 * @author Midnex
 *
 */
public class Balancer
{
	public enum bflag
	{
		hp,
		mp,
		cp,
		patak,
		matak,
		pdef,
		mdef,
		accuracy,
		evasion,
		criticalHit,
		runSpeed,
		attackSpeed,
		castSpeed,
		damge_to_h,
		damge_to_r,
		damge_to_l,
		damge_phys_to_h,
		damge_phys_to_r,
		damge_phys_to_l,
		damge_blow_to_h,
		damge_blow_to_r,
		damge_blow_to_l,
		damge_magic_to_h,
		damge_magic_to_r,
		damge_magic_to_l;
	}

	static FastMap<Integer, clazBalance> injectStats = new FastMap<Integer, clazBalance>();
	static GArray<Integer> injectSkills = new GArray<Integer>();

	public static void usebypass(L2Player player, String bypass)
	{
		if(player == null)
			return;

		if(bypass.startsWith("b_bk_page"))
		{			
			String[] byp = bypass.split(" ");
			String[] byp1 = byp[1].split(":");
			openDetailedHTML(player, Integer.parseInt(byp1[0]), byp1[1].equals("-") ? "" : byp1[1]);
		}
		else if(bypass.startsWith("b_bs_bedit"))
		{
			String[] byp = bypass.split(" ");
			String[] byp1 = byp[1].split(":");
			openSkillEditHTML(player, Integer.parseInt(byp1[0]), Integer.parseInt(byp1[1]), Integer.parseInt(byp1[2]), Integer.parseInt(byp1[3]));
		}
		else if(bypass.startsWith("b_bs"))
		{
			String[] byp = bypass.split(" ");
			String[] byp1 = byp[1].split(":");
			setSetting(player, Integer.parseInt(byp1[0]), Integer.parseInt(byp1[1]), Integer.parseInt(byp1[2]));
		}
		else if(bypass.startsWith("b_bd"))
		{
			String[] byp = bypass.split(" ");
			String[] byp1 = byp[1].split(":");
			openDetailedSkillHTML(player, Integer.parseInt(byp1[0]), Integer.parseInt(byp1[1]));
		}
		else if(bypass.startsWith("_bbs_b_bd"))
		{
			String[] byp = bypass.split(" ");
			openDetailedSkillHTML(player, Integer.parseInt(byp[1]), Integer.parseInt(byp[2]));
		}
		else if(bypass.startsWith("_bbs_bec"))
		{
			String[] byp = bypass.split(" ");
			openDetailedHTML(player, Integer.parseInt(byp[1]),"");
		}
		else if(bypass.startsWith("_bbs_bset"))
		{
			String[] byp = bypass.split(" ");
			setSettingSkill(player, Integer.parseInt(byp[1]), Integer.parseInt(byp[2]), Integer.parseInt(byp[3]), Integer.parseInt(byp[4]), Integer.parseInt(byp[5]), Integer.parseInt(byp[6]));
		}
	}

	public static void openDetailedSkillHTML(L2Player activeChar, int classid, int type)
	{
		if(classid == -1)
			return;
		//18
		ArrayList<L2SkillLearn> skills = SkillTreeTable.getInstance().getTOPSkills(ClassId.values()[classid]);
		String html = Files.read("data/html/admin/balance/detailedSkill.htm");
		String one = "<a action=\"link b_bs_bedit%%id%:%level%:%classid%:%type%\">%isedited%%name% [%id%][%level%]%isedited2%</a><br1>";
		String torep = "";

		int skillsc = 0;

		for(L2SkillLearn sk : skills)
		{
			L2Skill rs = SkillTable.getInstance().getInfo(sk.getId(), sk.getLevel());
			if(type == 0 && rs.isOffensive() || type == 1 && rs.isPassive() || type == 2 && !rs.isPassive() && !rs.isOffensive())
			{
				skillsc++;
				torep += one.replaceAll("%id%", "" + rs.getId()).replaceFirst("%isedited%", injectSkills.contains(rs.getId()) ? "<font color=7abc57>" : "").replaceFirst("%isedited2%", injectSkills.contains(rs.getId()) ? "</font>" : "").replaceFirst("%name%", rs.getName()).replaceAll("%level%", "" + rs.getLevel()).replaceAll("%color%", skillsc % 2 == 0 ? "090908" : "0f100f");
			}
		}

		html = html.replaceFirst("%skills%", torep);

		if(type == 0)
			html = html.replaceAll("%skillstype%", "Offensive").replaceAll("%type%", 0 + "");
		else if(type == 1)
			html = html.replaceAll("%skillstype%", "Passive").replaceAll("%type%", 1 + "");
		else
			html = html.replaceAll("%skillstype%", "Other").replaceAll("%type%", 2 + "");

		html = html.replaceFirst("%classname%", classid == -1 ? "Global Settings" : CharTemplateTable.getClassNameById(classid));
		html = html.replaceAll("%classid%", "" + classid);

		if(html.length() > 6000)
			activeChar.sendMessage("Html lenght:" + html.length());
		else
			activeChar.sendPacket(new TutorialShowHtml(html));
			//Functions.show(html, activeChar);
	}

	public static void openSkillEditHTML(L2Player activeChar, int skid, int sklevel, int back, int backtype)
	{
		L2Skill sk = SkillTable.getInstance().getInfo(skid, sklevel);
		if(sk == null)
		{
			activeChar.sendMessage("Error skill doesent exist.");
			return;
		}

		String html = Files.read("data/html/admin/balance/editskill.htm");

		if(sk.getPowerOriginal() > 0)
		{
			html = html.replaceAll("%ispower%", Files.read("data/html/admin/balance/power.htm"));
			html = html.replaceAll("%powers%", Math.round(sk.getPowerOriginal()) + "|" + Math.round(sk.getPower()));
			html = html.replaceAll("%powersmod%", getColoredPercent(Math.round((sk.getPower() - sk.getPowerOriginal()) * 100 / sk.getPowerOriginal())));
		}
		else
			html = html.replaceAll("%ispower%", "");

		if(sk.getActivateRateOriginal() > 0)
		{
			html = html.replaceAll("%ischance%", Files.read("data/html/admin/balance/chance.htm"));
			html = html.replaceAll("%chance%", sk.getActivateRateOriginal() + "");
			html = html.replaceAll("%chancemod%", getColoredPercentforChance(sk.getActivateRateOriginal() - sk.getActivateRate(), sk.getActivateRate()));
		}
		else
			html = html.replaceAll("%ischance%", "");

		if(sk.getLethal1() > 0)
		{
			html = html.replaceAll("%ischancelethal1%", Files.read("data/html/admin/balance/lethal1.htm"));
			html = html.replaceAll("%chanceLethal1%", sk.getLethal1Original() + "");
			html = html.replaceAll("%chanceLethal1mod%", getColoredPercentforChance(sk.getLethal1Original() - sk.getLethal1(), Math.round(sk.getLethal1())));
		}
		else
			html = html.replaceAll("%ischancelethal1%", "");

		if(sk.getLethal2() > 0)
		{
			html = html.replaceAll("%ischancelethal2%", Files.read("data/html/admin/balance/lethal2.htm"));
			html = html.replaceAll("%chanceLethal2%", sk.getLethal2Original() + "");
			html = html.replaceAll("%chanceLethal2mod%", getColoredPercentforChance(sk.getLethal2Original() - sk.getLethal2(), Math.round(sk.getLethal2())));
		}
		else
			html = html.replaceAll("%ischancelethal2%", "");

		/*if(sk.getCriticalRate() > 0)
		{
			html = html.replaceAll("%ischancecriticalrate%", Files.read("data/html/admin/balance/criticalrate.htm"));
			html = html.replaceAll("%chancecriticalrate%", sk.getCriticalRateOriginal() + "");
			html = html.replaceAll("%chancecriticalratemod%", getColoredPercentforChance(sk.getCriticalRateOriginal() - sk.getCriticalRate(), sk.getCriticalRate()));
		}
		else*/
		html = html.replaceAll("%ischancecriticalrate%", "");
		String one = "<table width=274 height=20><tr><td align=\"center\" width=79>%effectname%(%effectTime%)</td><td width=30>(%effectTimeMod%)</td><td><a action=\"bypass _bbs_bset %classid% %skillid% %skilllvl% 5 1000 %type%\">[+1s]</a><br1><a action=\"bypass _bbs_bset %classid% %skillid% %skilllvl% 5 1 %type%\">[+1c]</a></td><td><a action=\"bypass _bbs_bset %classid% %skillid% %skilllvl% 6 -1000 %type%\">[-1s]</a><br1><a action=\"bypass _bbs_bset %classid% %skillid% %skilllvl% 6 -1 %type%\">[-1c]</a></td></tr></table>";
		String torep = "";

		html = html.replaceAll("%reuses%", getS(sk.getReuseDelayOriginal()) + "|" + getS(sk.getReuseDelay()));
		html = html.replaceAll("%hittimes%", getS(sk.getHitTimeOriginal()) + "|" + getS(sk.getHitTime()));
		html = html.replaceAll("%reusestatic%", sk.isReuseDelayPermanentOriginal() ? "YES" : "NO");

		if(sk.getReuseDelay() != 0)
			html = html.replaceAll("%reusesmod%", getColoredPercent((sk.getReuseDelay() - sk.getReuseDelayOriginal()) * 100 / sk.getReuseDelayOriginal()));
		else
			html = html.replaceAll("%reusesmod%", "0");

		if(sk.getHitTime() != 0)
			html = html.replaceAll("%hittimesmod%", getColoredPercent((sk.getHitTime() - sk.getHitTimeOriginal()) * 100 / sk.getHitTimeOriginal()));
		else
			html = html.replaceAll("%hittimesmod%", "0");

		html = html.replaceAll("%reusestaticmod%", sk.isReuseDelayPermanent() ? "<font color=7abc57>YES</font>" : "<font color=bd4539>NO</font>");

		if(sk.getEffectTemplates() != null)
		{
			html = html.replaceAll("%effectheader%", Files.read("data/html/admin/balance/effects.htm"));

			for(EffectTemplate eff : sk.getEffectTemplates())
				if(eff._counterOriginal < 99999)
					torep += one.replaceFirst("%effectname%", eff.getEffectType().toString()).replaceAll("%effectTime%", getS(eff.getPeriodOriginal()) + "*" + eff._counterOriginal + "=" + getS(eff.getPeriodOriginal() * eff._counterOriginal)).replaceAll("%effectTimeMod%", getS(eff.getPeriod()) + "*" + eff._counter + "=" + getS(eff.getPeriod() * eff._counter));
		}
		else
			html = html.replaceAll("%effectheader%", "");

		html = html.replaceFirst("%effects%", torep);

		html = html.replaceAll("%type%", backtype + "");

		if(backtype == 0)
			html = html.replaceAll("%typename%", "Offensive");
		else if(backtype == 1)
			html = html.replaceAll("%typename%", "Passive");
		else
			html = html.replaceAll("%typename%", "Other");

		html = html.replaceAll("%skillid%", skid + "");
		html = html.replaceAll("%skilllvl%", sklevel + "");
		html = html.replaceAll("%skillname%", sk.getName()).replaceAll("%classid%", "" + back).replaceAll("%classname%", CharTemplateTable.getClassNameById(back));
		Functions.show(html, activeChar);
	}

	public static String getS(long l)
	{
		return String.valueOf(l / 1000);
	}

	public static void openDetailedHTML(L2Player activeChar, int classid, String prefix)
	{
		clazBalance c = injectStats.get(classid);

		if(c == null)
			return;
		
		try{openHTML(activeChar);}catch(Exception e){e.printStackTrace();}

		String html = Files.read("data/html/admin/balance/detailed" + prefix + ".htm");

		html = html.replaceFirst("%classname%", classid == -1 ? "Global Settings" : CharTemplateTable.getClassNameById(classid));
		html = html.replaceAll("%classid%", "" + classid);

		
		if(prefix.equals("_damage_to_armors"))
		{
			html = html.replaceFirst("%percentDAMAGE_TO_H%", getColoredPercent(c.damge_blow_to_h));
			html = html.replaceFirst("%percentDAMAGE_TO_R%", getColoredPercent(c.damge_blow_to_r));
			html = html.replaceFirst("%percentDAMAGE_TO_L%", getColoredPercent(c.damge_blow_to_l));
			
			html = html.replaceFirst("%percentDAMAGE_n_TO_H%", getColoredPercent(c.damge_to_h));
			html = html.replaceFirst("%percentDAMAGE_n_TO_R%", getColoredPercent(c.damge_to_r));
			html = html.replaceFirst("%percentDAMAGE_n_TO_L%", getColoredPercent(c.damge_to_l));

			html = html.replaceFirst("%percentDAMAGE_m_TO_H%", getColoredPercent(c.damge_magic_to_h));
			html = html.replaceFirst("%percentDAMAGE_m_TO_R%", getColoredPercent(c.damge_magic_to_r));
			html = html.replaceFirst("%percentDAMAGE_m_TO_L%", getColoredPercent(c.damge_magic_to_l));

			html = html.replaceFirst("%percentDAMAGE_p_TO_H%", getColoredPercent(c.damge_phys_to_h));
			html = html.replaceFirst("%percentDAMAGE_p_TO_R%", getColoredPercent(c.damge_phys_to_r));
			html = html.replaceFirst("%percentDAMAGE_p_TO_L%", getColoredPercent(c.damge_phys_to_l));
		}
		else
		{
			html = html.replaceFirst("%skillcount%", classid == -1 ? "0" : "" + SkillTreeTable.getInstance().getTOPSkills(ClassId.values()[classid]).size());
			html = html.replaceFirst("%percentHP%", getColoredPercent(c.hp));
			html = html.replaceFirst("%percentMP%", getColoredPercent(c.mp));
			html = html.replaceFirst("%percentCP%", getColoredPercent(c.cp));
			html = html.replaceFirst("%percentPATAK%", getColoredPercent(c.patak));
			html = html.replaceFirst("%percentMATAK%", getColoredPercent(c.matak));
			html = html.replaceFirst("%percentPDEF%", getColoredPercent(c.pdef));
			html = html.replaceFirst("%percentMDEF%", getColoredPercent(c.mdef));
			html = html.replaceFirst("%percentACCURACY%", getColoredPercent(c.accuracy));
			html = html.replaceFirst("%percentEVASION%", getColoredPercent(c.evasion));
			html = html.replaceFirst("%percentCRITICALHIT%", getColoredPercent(c.criticalHit));
			html = html.replaceFirst("%percentRUNSPEED%", getColoredPercent(c.runSpeed));
			html = html.replaceFirst("%percentATACKSPEED%", getColoredPercent(c.attackSpeed));
			html = html.replaceFirst("%percentCASTSPEED%", getColoredPercent(c.castSpeed));
		}
		activeChar.sendPacket(new TutorialShowHtml(html));
	}

	public static void setSetting(L2Player player, int classid, int stat, int multip)
	{
		clazBalance c = injectStats.get(classid);

		if(c == null)
			return;

		switch(stat)
		{
			case 0:
				c.hp += multip;
				break;
			case 1:
				c.mp += multip;
				break;
			case 2:
				c.cp += multip;
				break;
			case 3:
				c.patak += multip;
				break;
			case 4:
				c.matak += multip;
				break;
			case 5:
				c.pdef += multip;
				break;
			case 6:
				c.mdef += multip;
				break;
			case 7:
				c.accuracy += multip;
				break;
			case 8:
				c.evasion += multip;
				break;
			case 9:
				c.criticalHit += multip;
				break;
			case 10:
				c.runSpeed += multip;
				break;
			case 11:
				c.attackSpeed += multip;
				break;
			case 12:
				c.castSpeed += multip;
				break;
			case 13:
				c.damge_to_h += multip;
				break;
			case 14:
				c.damge_to_r += multip;
				break;
			case 15:
				c.damge_to_l += multip;
				break;
			case 16:
				c.damge_blow_to_h += multip;
				break;
			case 17:
				c.damge_blow_to_r += multip;
				break;
			case 18:
				c.damge_blow_to_l += multip;
				break;
			case 19:
				c.damge_magic_to_h += multip;
				break;
			case 20:
				c.damge_magic_to_r += multip;
				break;
			case 21:
				c.damge_magic_to_l += multip;
				break;
			case 22:
				c.damge_phys_to_h += multip;
				break;
			case 23:
				c.damge_phys_to_r += multip;
				break;
			case 24:
				c.damge_phys_to_l += multip;
				break;		
		}
		openDetailedHTML(player, classid, stat > 12 ? "_damage_to_armors" : "");
		for(L2Player pl : L2World.getAllPlayers())
			pl.broadcastUserInfo(true);
		saveToFile(player.getName());
	}

	public static String getColoredPercent(long l)
	{
		String sMore = "<font color=7abc57>" + l + "%</font>";
		String sLess = "<font color=bd4539>" + l + "%</font>";
		String sNeutral = l + "%";

		if(l > 0)
			return sMore;
		if(l < 0)
			return sLess;
		return sNeutral;
	}

	public static String getColoredPercentforChance(double d, long l)
	{
		String sMore = "<font color=7abc57>" + l + "%</font>";
		String sLess = "<font color=bd4539>" + l + "%</font>";
		String sNeutral = l + "%";

		if(d < 0)
			return sMore;
		if(d > 0)
			return sLess;
		return sNeutral;
	}

	public static String getColoredPercentForIndex(String name, int percent)
	{
		String sMore = "[" + name + ":" + percent + "%]&nbsp;";
		String sLess = "[" + name + ":" + percent + "%]&nbsp;";
		if(percent > 0)
			return sMore;
		if(percent < 0)
			return sLess;
		return sMore;
	}

	public static void openHTML(L2Player activeChar) throws IllegalArgumentException, IllegalAccessException
	{
		String html = Files.read("data/html/admin/balance/index.htm");

		clazBalance global = injectStats.get(-1);
		String repGlobal = "";

		if(global != null)
			for(Field lol : global.getClass().getDeclaredFields())
				if(lol.getInt(global) != 0)
					repGlobal += getColoredPercentForIndex(lol.getName().toUpperCase(), lol.getInt(global));

		html = sortOnlineCounts(html);

		if(repGlobal.length() > 100)
		{
			repGlobal = repGlobal.substring(0, repGlobal.length() - (repGlobal.length() - 100));
			repGlobal += "...";
		}

		html = html.replaceAll("%globalmod%", repGlobal);
		activeChar.sendPacket(new ShowBoard(html));
	}


	public static String sortOnlineCounts(String html)
	{
		FastMap<Integer, Integer> _onlinemap = new FastMap<Integer, Integer>();
		FastMap<Integer, Integer> _onlineSortedmap = new FastMap<Integer, Integer>();

		for(L2Player pl : L2World.getAllPlayers())
		{
			if(pl.getLevel() < 76)
				continue;

			if(!_onlinemap.containsKey(pl.getActiveClassId()))
				_onlinemap.put(pl.getActiveClassId(), 1);
			else
				_onlinemap.put(pl.getActiveClassId(), _onlinemap.get(pl.getActiveClassId()) + 1);

			int type = PlayerClass.values()[pl.getActiveClassId()].getTypeExtended();
			if(!_onlineSortedmap.containsKey(type))
				_onlineSortedmap.put(type, 1);
			else
				_onlineSortedmap.put(type, _onlineSortedmap.get(type) + 1);
		}
		for(int i = 88; i != 137; i++)
			if(_onlinemap.containsKey(i))
				html = html.replaceAll("%c+" + i + "%", "(" + _onlinemap.get(i) + ")");
			else
				html = html.replaceAll("%c+" + i + "%", "(0)");
		for(int i = 3; i != 13; i++)
			if(_onlineSortedmap.containsKey(i))
				html = html.replaceAll("%s+" + i + "%", "(" + _onlineSortedmap.get(i) + ")");
			else
				html = html.replaceAll("%s+" + i + "%", "(0)");

		html = html.replaceFirst("%total%", "(" + L2World.getAllPlayersCount() + ")");
		return html;
	}
	
	public static int getModify(bflag f, int originalValue, int claz)
	{
		clazBalance c = injectStats.get(claz);
		clazBalance global = injectStats.get(-1);

		switch(f)
		{
			case hp:
				return getFinalModify((c == null ? 0 : c.hp) + (global == null ? 0 : global.hp), originalValue);
			case mp:
				return getFinalModify((c == null ? 0 : c.mp) + (global == null ? 0 : global.mp), originalValue);
			case cp:
				return getFinalModify((c == null ? 0 : c.cp) + (global == null ? 0 : global.cp), originalValue);
			case patak:
				return getFinalModify((c == null ? 0 : c.patak) + (global == null ? 0 : global.patak), originalValue);
			case matak:
				return getFinalModify((c == null ? 0 : c.matak) + (global == null ? 0 : global.matak), originalValue);
			case pdef:
				return getFinalModify((c == null ? 0 : c.pdef) + (global == null ? 0 : global.pdef), originalValue);
			case mdef:
				return getFinalModify((c == null ? 0 : c.mdef) + (global == null ? 0 : global.mdef), originalValue);
			case accuracy:
				return getFinalModify((c == null ? 0 : c.accuracy) + (global == null ? 0 : global.accuracy), originalValue);
			case evasion:
				return getFinalModify((c == null ? 0 : c.evasion) + (global == null ? 0 : global.evasion), originalValue);
			case criticalHit:
				return getFinalModify((c == null ? 0 : c.criticalHit) + (global == null ? 0 : global.criticalHit), originalValue);
			case runSpeed:
				return getFinalModify((c == null ? 0 : c.runSpeed) + (global == null ? 0 : global.runSpeed), originalValue);
			case attackSpeed:
				return getFinalModify((c == null ? 0 : c.attackSpeed) + (global == null ? 0 : global.attackSpeed), originalValue);
		}
		return originalValue;
	}
	
	
	public static double getModifyD(bflag f, int originalValue, int claz)
	{
		clazBalance c = injectStats.get(claz);
		clazBalance global = injectStats.get(-1);

		switch(f)
		{
			case damge_blow_to_h:
				return getFinalModify((c == null ? 0 : c.damge_blow_to_h) + (global == null ? 0 : global.damge_blow_to_h), originalValue)/100.0;
			case damge_blow_to_r:
				return getFinalModify((c == null ? 0 : c.damge_blow_to_r) + (global == null ? 0 : global.damge_blow_to_r), originalValue)/100.0;
			case damge_blow_to_l:
				return getFinalModify((c == null ? 0 : c.damge_blow_to_l) + (global == null ? 0 : global.damge_blow_to_l), originalValue)/100.0;
			case damge_to_h:
				return getFinalModify((c == null ? 0 : c.damge_to_h) + (global == null ? 0 : global.damge_to_h), originalValue);
			case damge_to_r:
				return getFinalModify((c == null ? 0 : c.damge_to_r) + (global == null ? 0 : global.damge_to_r), originalValue);
			case damge_to_l:
				return getFinalModify((c == null ? 0 : c.damge_to_l) + (global == null ? 0 : global.damge_to_l), originalValue);
			case damge_magic_to_h:
				return getFinalModify((c == null ? 0 : c.damge_magic_to_h) + (global == null ? 0 : global.damge_magic_to_h), originalValue);
			case damge_magic_to_r:
				return getFinalModify((c == null ? 0 : c.damge_magic_to_r) + (global == null ? 0 : global.damge_magic_to_r), originalValue);
			case damge_magic_to_l:
				return getFinalModify((c == null ? 0 : c.damge_magic_to_l) + (global == null ? 0 : global.damge_magic_to_l), originalValue);
			case damge_phys_to_h:
				return getFinalModify((c == null ? 0 : c.damge_phys_to_h) + (global == null ? 0 : global.damge_phys_to_h), originalValue);
			case damge_phys_to_r:
				return getFinalModify((c == null ? 0 : c.damge_phys_to_r) + (global == null ? 0 : global.damge_phys_to_r), originalValue);
			case damge_phys_to_l:
				return getFinalModify((c == null ? 0 : c.damge_phys_to_l) + (global == null ? 0 : global.damge_phys_to_l), originalValue);
		}
		return originalValue;
	}


	private static int getFinalModify(int percent, int originalValue)
	{
		return (100 + percent) * originalValue / 100;
	}

	public static void saveToFile(String lastauthor)
	{
		try
		{
			String data = "";

			data += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
			data += "<!-- Auto saved at: " + Util.convertDateToString(System.currentTimeMillis() / 1000) + " by " + lastauthor + " -->\n";
			data += "<list>\n";

			clazBalance global = injectStats.get(-1);

			data += "	<!-- Global stats -->\n";

			data += "	<global id=\"-1\" ";

			if(global.hp != 0)
				data += "hp=\"" + global.hp + "\"";
			if(global.mp != 0)
				data += " mp=\"" + global.mp + "\"";
			if(global.cp != 0)
				data += " cp=\"" + global.cp + "\"";
			if(global.patak != 0)
				data += " patak=\"" + global.patak + "\"";
			if(global.matak != 0)
				data += " matak=\"" + global.matak + "\"";
			if(global.pdef != 0)
				data += " pdef=\"" + global.pdef + "\"";
			if(global.mdef != 0)
				data += " mdef=\"" + global.mdef + "\"";
			if(global.accuracy != 0)
				data += " accuracy=\"" + global.accuracy + "\"";
			if(global.evasion != 0)
				data += " evasion=\"" + global.evasion + "\"";
			if(global.criticalHit != 0)
				data += " criticalHit=\"" + global.criticalHit + "\"";
			if(global.runSpeed != 0)
				data += " runSpeed=\"" + global.runSpeed + "\"";
			if(global.attackSpeed != 0)
				data += " attackSpeed=\"" + global.attackSpeed + "\"";
			if(global.castSpeed != 0)
				data += " castSpeed=\"" + global.castSpeed + "\"";
			if(global.damge_to_h != 0)
				data += " damge_to_h=\"" + global.damge_to_h + "\"";
			if(global.damge_to_r != 0)
				data += " damge_to_r=\"" + global.damge_to_r + "\"";
			if(global.damge_to_l != 0)
				data += " damge_to_l=\"" + global.damge_to_l + "\"";
			if(global.damge_blow_to_h != 0)
				data += " damge_blow_to_h=\"" + global.damge_blow_to_h + "\"";
			if(global.damge_blow_to_r != 0)
				data += " damge_blow_to_r=\"" + global.damge_blow_to_r + "\"";
			if(global.damge_blow_to_l != 0)
				data += " damge_blow_to_l=\"" + global.damge_blow_to_l + "\"";
			if(global.damge_magic_to_h != 0)
				data += " damge_magic_to_h=\"" + global.damge_magic_to_h + "\"";
			if(global.damge_magic_to_r != 0)
				data += " damge_magic_to_r=\"" + global.damge_magic_to_r + "\"";
			if(global.damge_magic_to_l != 0)
				data += " damge_magic_to_l=\"" + global.damge_magic_to_l + "\"";
			if(global.damge_phys_to_h != 0)
				data += " damge_phys_to_h=\"" + global.damge_phys_to_h + "\"";
			if(global.damge_phys_to_r != 0)
				data += " damge_phys_to_r=\"" + global.damge_phys_to_r + "\"";
			if(global.damge_phys_to_l != 0)
				data += " damge_phys_to_l=\"" + global.damge_phys_to_l + "\"";
			data += " />\n";

			data += "\n	<!-- Base stats -->\n";

			for(Entry<Integer, clazBalance> stats : injectStats.entrySet())
			{
				if(stats.getKey() == -1)
					continue;

				int total = 0;
				for(Field lol : stats.getValue().getClass().getDeclaredFields())
					if(lol.getInt(stats.getValue()) != 0)
						total += 1;

				if(total == 0)
					continue;

				data += "	<class id=\"" + stats.getKey() + "\" ";
				data += "name=\"" + CharTemplateTable.getClassNameById(stats.getKey()) + "\"";

				if(stats.getValue().hp != 0)
					data += " hp=\"" + stats.getValue().hp + "\"";
				if(stats.getValue().mp != 0)
					data += " mp=\"" + stats.getValue().mp + "\"";
				if(stats.getValue().cp != 0)
					data += " cp=\"" + stats.getValue().cp + "\"";
				if(stats.getValue().patak != 0)
					data += " patak=\"" + stats.getValue().patak + "\"";
				if(stats.getValue().matak != 0)
					data += " matak=\"" + stats.getValue().matak + "\"";
				if(stats.getValue().pdef != 0)
					data += " pdef=\"" + stats.getValue().pdef + "\"";
				if(stats.getValue().mdef != 0)
					data += " mdef=\"" + stats.getValue().mdef + "\"";
				if(stats.getValue().accuracy != 0)
					data += " accuracy=\"" + stats.getValue().accuracy + "\"";
				if(stats.getValue().evasion != 0)
					data += " evasion=\"" + stats.getValue().evasion + "\"";
				if(stats.getValue().criticalHit != 0)
					data += " criticalHit=\"" + stats.getValue().criticalHit + "\"";
				if(stats.getValue().runSpeed != 0)
					data += " runSpeed=\"" + stats.getValue().runSpeed + "\"";
				if(stats.getValue().attackSpeed != 0)
					data += " attackSpeed=\"" + stats.getValue().attackSpeed + "\"";
				if(stats.getValue().castSpeed != 0)
					data += " castSpeed=\"" + stats.getValue().castSpeed + "\"";
				if(stats.getValue().damge_to_h != 0)
					data += " damge_to_h=\"" + stats.getValue().damge_to_h + "\"";
				if(stats.getValue().damge_to_r != 0)
					data += " damge_to_r=\"" + stats.getValue().damge_to_r + "\"";
				if(stats.getValue().damge_to_l != 0)
					data += " damge_to_l=\"" + stats.getValue().damge_to_l + "\"";
				if(stats.getValue().damge_blow_to_h != 0)
					data += " damge_blow_to_h=\"" + stats.getValue().damge_blow_to_h + "\"";
				if(stats.getValue().damge_blow_to_r != 0)
					data += " damge_blow_to_r=\"" + stats.getValue().damge_blow_to_r + "\"";
				if(stats.getValue().damge_blow_to_l != 0)
					data += " damge_blow_to_l=\"" + stats.getValue().damge_blow_to_l + "\"";
				if(stats.getValue().damge_magic_to_h != 0)
					data += " damge_magic_to_h=\"" + stats.getValue().damge_magic_to_h + "\"";
				if(stats.getValue().damge_magic_to_r != 0)
					data += " damge_magic_to_r=\"" + stats.getValue().damge_magic_to_r + "\"";
				if(stats.getValue().damge_magic_to_l != 0)
					data += " damge_magic_to_l=\"" + stats.getValue().damge_magic_to_l + "\"";
				if(stats.getValue().damge_phys_to_h != 0)
					data += " damge_phys_to_h=\"" + stats.getValue().damge_phys_to_h + "\"";
				if(stats.getValue().damge_phys_to_r != 0)
					data += " damge_phys_to_r=\"" + stats.getValue().damge_phys_to_r + "\"";
				if(stats.getValue().damge_phys_to_l != 0)
					data += " damge_phys_to_l=\"" + stats.getValue().damge_phys_to_l + "\"";
				data += " />\n";
			}

			data += "\n	<!-- Skills -->\n";

			for(Integer skill : injectSkills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(skill, 1);

				if(sk.getPowerOriginal() == sk.getPower() && sk.getActivateRateOriginal() == sk.getActivateRate() && sk.getLethal1Original() == sk.getLethal1() && sk.getLethal1Original() == sk.getLethal1())
					if(/*sk.getCriticalRateOriginal() == sk.getCriticalRate() && */sk.getReuseDelayOriginal() == sk.getReuseDelay() && sk.getHitTime() == sk.getHitTime())
					{
						boolean val = false;

						if(sk.getEffectTemplates() != null)
							for(EffectTemplate ef : sk.getEffectTemplates())
								if(ef.getPeriodOriginal() != ef.getPeriod() || ef._counterOriginal != ef._counter)
									val = true;
						if(!val)
							continue;
					}

				data += "	<skill id=\"" + sk.getId() + "\" ";
				data += "name=\"" + sk.getName() + "\"";
				if(sk.getPowerOriginal() != sk.getPower())
					data += " power=\"" + (int) (sk.getPower() - sk.getPowerOriginal()) + "\"";
				if(sk.getActivateRateOriginal() != sk.getActivateRate())
					data += " chance=\"" + (sk.getActivateRate() - sk.getActivateRateOriginal()) + "\"";
				if(sk.getLethal1Original() != sk.getLethal1())
					data += " lethal1=\"" + (sk.getLethal1() - sk.getLethal1Original()) + "\"";
				if(sk.getLethal2Original() != sk.getLethal2())
					data += " lethal2=\"" + (sk.getLethal2() - sk.getLethal2Original()) + "\"";
				//if(sk.getCriticalRateOriginal() != sk.getCriticalRate())
				//	data += " criticalRate=\"" + (sk.getCriticalRate() - sk.getCriticalRateOriginal()) + "\"";
				if(sk.getReuseDelayOriginal() != sk.getReuseDelay())
					data += " reuse=\"" + (sk.getReuseDelay() - sk.getReuseDelayOriginal()) + "\"";
				if(sk.getHitTimeOriginal() != sk.getHitTime())
					data += " hittime=\"" + (sk.getHitTime() - sk.getHitTimeOriginal()) + "\"";
				if(sk.getEffectTemplates() != null)
				{

					String efftime = "";
					String effcount = "";

					for(EffectTemplate ef : sk.getEffectTemplates())
					{
						if(ef.getPeriodOriginal() != ef.getPeriod())
							efftime += ef.getEffectType() + "," + (ef.getPeriod() - ef.getPeriodOriginal()) + ";";
						if(ef._counterOriginal != ef._counter)
							effcount += ef.getEffectType() + "," + (ef._counter - ef._counterOriginal) + ";";
					}

					if(efftime.length() > 3)
					{
						efftime = efftime.substring(0, efftime.length() - 1);
						data += " time=\"" + efftime + "\"";
					}
					if(effcount.length() > 3)
					{
						effcount = efftime.substring(0, effcount.length() - 1);
						data += " count=\"" + effcount + "\"";
					}
				}

				if(sk.isReuseDelayPermanentOriginal() != sk.isReuseDelayPermanent())
					data += " reusePermanent=\"" + sk.isReuseDelayPermanent() + "\"";

				data += " />\n";
			}

			data += "</list>";
			Writer writer = new FileWriter("data/stats/class_inject.xml");
			writer.write(data);
			writer.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void setSettingSkill(L2Player player, int back, int skillid, int skilllvl, int stat, int mod, int backtype)
	{
		//chance
		//power
		//reuse
		//hitime

		L2Skill sk = SkillTable.getInstance().getInfo(skillid, skilllvl);
		if(sk == null)
		{
			player.sendMessage("Error skill doesent exist.");
			return;
		}

		switch(stat)
		{
			case 0:
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setPower(skilas.getPower() + mod);
				break;
			case 1:
				if(SkillTable.getInstance().getMaxChance(skillid) + mod > 100)
				{
					player.sendMessage("You cant go more bcouse you will make some skill level's unselles.");
					return;
				}
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setActivateRate(skilas.getActivateRate() + mod);
				break;
			case 2:
				if(SkillTable.getInstance().getMaxReuse(skillid) + mod < 1)
				{
					player.sendMessage("You cant go more bcouse you will make some skill level's unselles!");
					return;
				}
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setReuseDelay(skilas.getReuseDelay() + mod);
				break;
			case 3:
				if(SkillTable.getInstance().getMaxHitTime(skillid) + mod < 1)
				{
					player.sendMessage("You cant go more bcouse you will make some skill level's unselles!");
					return;
				}
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setHitTime(skilas.getHitTime() + mod);
				break;
			case 4:
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setReuseDelayPermanent(mod == 1 ? true : false, false);
				break;
			case 5://time
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					for(EffectTemplate ef : skilas.getEffectTemplates())
						ef._period += mod;
				break;
			case 6://count
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					for(EffectTemplate ef : skilas.getEffectTemplates())
						ef._counter += mod;
				break;
			case 7://lethal1
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setLethal1(skilas.getLethal1() + mod);
				break;
			case 8://lethal2
				for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
					skilas.setLethal2(skilas.getLethal2() + mod);
				break;
			/*case 9://crt.rate
			for(L2Skill skilas : SkillTable.getInstance().getAllLevels(skillid))
				skilas.setCriticalRate(skilas.getCriticalRate() + mod);
			break;*/
		}

		if(!injectSkills.contains(skillid))
			injectSkills.add(skillid);
		openSkillEditHTML(player, skillid, skilllvl, back, backtype);
		saveToFile(player.getName());
	}

	public static void load()
	{
		//loaddummys();
		injectStats = new FastMap<Integer, clazBalance>();
		injectSkills = new GArray<Integer>();
		injectStats.put(-1, new clazBalance());

		for(int i = 88; i != 137; i++)
			injectStats.put(i, new clazBalance());
		try
		{			
			File file = new File(Config.DATAPACK_ROOT + "/data/stats/class_inject.xml");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(file);
			for(Node g = doc.getFirstChild(); g != null; g = g.getNextSibling())
				for(Node z = g.getFirstChild(); z != null; z = z.getNextSibling())
				{
					if(z.getNodeName().equals("class") || z.getNodeName().equals("global"))
					{
						int classid = Integer.valueOf(z.getAttributes().getNamedItem("id").getNodeValue());
						clazBalance cbs = new clazBalance();

						if(z.getAttributes().getNamedItem("hp") != null)
							cbs.hp = Integer.valueOf(z.getAttributes().getNamedItem("hp").getNodeValue());
						if(z.getAttributes().getNamedItem("mp") != null)
							cbs.mp = Integer.valueOf(z.getAttributes().getNamedItem("mp").getNodeValue());
						if(z.getAttributes().getNamedItem("cp") != null)
							cbs.cp = Integer.valueOf(z.getAttributes().getNamedItem("cp").getNodeValue());
						if(z.getAttributes().getNamedItem("patak") != null)
							cbs.patak = Integer.valueOf(z.getAttributes().getNamedItem("patak").getNodeValue());
						if(z.getAttributes().getNamedItem("matak") != null)
							cbs.matak = Integer.valueOf(z.getAttributes().getNamedItem("matak").getNodeValue());
						if(z.getAttributes().getNamedItem("pdef") != null)
							cbs.pdef = Integer.valueOf(z.getAttributes().getNamedItem("pdef").getNodeValue());
						if(z.getAttributes().getNamedItem("mdef") != null)
							cbs.mdef = Integer.valueOf(z.getAttributes().getNamedItem("mdef").getNodeValue());
						if(z.getAttributes().getNamedItem("accuracy") != null)
							cbs.accuracy = Integer.valueOf(z.getAttributes().getNamedItem("accuracy").getNodeValue());
						if(z.getAttributes().getNamedItem("evasion") != null)
							cbs.evasion = Integer.valueOf(z.getAttributes().getNamedItem("evasion").getNodeValue());
						if(z.getAttributes().getNamedItem("criticalHit") != null)
							cbs.criticalHit = Integer.valueOf(z.getAttributes().getNamedItem("criticalHit").getNodeValue());
						if(z.getAttributes().getNamedItem("runSpeed") != null)
							cbs.runSpeed = Integer.valueOf(z.getAttributes().getNamedItem("runSpeed").getNodeValue());
						if(z.getAttributes().getNamedItem("attackSpeed") != null)
							cbs.attackSpeed = Integer.valueOf(z.getAttributes().getNamedItem("attackSpeed").getNodeValue());
						if(z.getAttributes().getNamedItem("castSpeed") != null)
							cbs.castSpeed = Integer.valueOf(z.getAttributes().getNamedItem("castSpeed").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_to_h") != null)
							cbs.damge_to_h = Integer.valueOf(z.getAttributes().getNamedItem("damge_to_h").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_to_r") != null)
							cbs.damge_to_r = Integer.valueOf(z.getAttributes().getNamedItem("damge_to_r").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_to_l") != null)
							cbs.damge_to_l = Integer.valueOf(z.getAttributes().getNamedItem("damge_to_l").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_blow_to_h") != null)
							cbs.damge_blow_to_h = Integer.valueOf(z.getAttributes().getNamedItem("damge_blow_to_h").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_blow_to_r") != null)
							cbs.damge_blow_to_r = Integer.valueOf(z.getAttributes().getNamedItem("damge_blow_to_l").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_blow_to_l") != null)
							cbs.damge_blow_to_l = Integer.valueOf(z.getAttributes().getNamedItem("damge_blow_to_r").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_magic_to_h") != null)
							cbs.damge_magic_to_h = Integer.valueOf(z.getAttributes().getNamedItem("damge_magic_to_h").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_magic_to_r") != null)
							cbs.damge_magic_to_r = Integer.valueOf(z.getAttributes().getNamedItem("damge_magic_to_r").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_magic_to_l") != null)
							cbs.damge_magic_to_l = Integer.valueOf(z.getAttributes().getNamedItem("damge_magic_to_l").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_phys_to_h") != null)
							cbs.damge_phys_to_h = Integer.valueOf(z.getAttributes().getNamedItem("damge_phys_to_h").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_phys_to_r") != null)
							cbs.damge_phys_to_r = Integer.valueOf(z.getAttributes().getNamedItem("damge_phys_to_r").getNodeValue());
						if(z.getAttributes().getNamedItem("damge_phys_to_l") != null)
							cbs.damge_phys_to_l = Integer.valueOf(z.getAttributes().getNamedItem("damge_phys_to_l").getNodeValue());
						injectStats.put(classid, cbs);
					}
					if(z.getNodeName().equals("skill"))
					{
						int intskillid = Integer.valueOf(z.getAttributes().getNamedItem("id").getNodeValue());

						for(L2Skill skilas : SkillTable.getInstance().getAllLevels(intskillid))
						{
							if(z.getAttributes().getNamedItem("power") != null)
								skilas.setPower(skilas.getPower() + Integer.valueOf(z.getAttributes().getNamedItem("power").getNodeValue()));
							if(z.getAttributes().getNamedItem("chance") != null)
								skilas.setActivateRate(skilas.getActivateRate() + Integer.valueOf(z.getAttributes().getNamedItem("chance").getNodeValue()));
							if(z.getAttributes().getNamedItem("lethal1") != null)
								skilas.setLethal1(skilas.getLethal1() + Integer.valueOf(z.getAttributes().getNamedItem("lethal1").getNodeValue()));
							if(z.getAttributes().getNamedItem("lethal2") != null)
								skilas.setLethal2(skilas.getLethal2() + Integer.valueOf(z.getAttributes().getNamedItem("lethal2").getNodeValue()));
							//if(z.getAttributes().getNamedItem("criticalRate") != null)
							//	skilas.setCriticalRate(skilas.getCriticalRate() + Integer.valueOf(z.getAttributes().getNamedItem("criticalRate").getNodeValue()));
							if(z.getAttributes().getNamedItem("reuse") != null)
								skilas.setReuseDelay(skilas.getReuseDelay() + Integer.valueOf(z.getAttributes().getNamedItem("reuse").getNodeValue()));
							if(z.getAttributes().getNamedItem("hittime") != null)
								skilas.setHitTime(skilas.getHitTime() + Integer.valueOf(z.getAttributes().getNamedItem("hittime").getNodeValue()));
							if(z.getAttributes().getNamedItem("reusePermanent") != null)
								skilas.setReuseDelayPermanent(Boolean.valueOf(z.getAttributes().getNamedItem("reusePermanent").getNodeValue()), true);
							if(skilas.getEffectTemplates() != null)
								for(EffectTemplate ef : skilas.getEffectTemplates())
								{
									if(z.getAttributes().getNamedItem("time") != null)
									{
										String[] cc = String.valueOf(z.getAttributes().getNamedItem("time").getNodeValue()).split(";");

										for(String effects : cc)
										{
											String[] effect = effects.split(",");
											//if(effect[0].equals(ef.getEffectType().toString()))
											ef._period += Integer.parseInt(effect[1]);
										}
									}
									if(z.getAttributes().getNamedItem("count") != null)
									{
										String[] cc = String.valueOf(z.getAttributes().getNamedItem("count").getNodeValue()).split(";");

										for(String counters : cc)
										{
											String[] counter = counters.split(",");
											//if(counter[0].equals(ef.getEffectType().toString()))
											ef._counter += Integer.parseInt(counter[1]);
										}
									}
								}
						}
						injectSkills.add(intskillid);
					}
				}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		for(L2Player pl : L2World.getAllPlayers())
			pl.broadcastUserInfo(true);
	}

	public static class clazBalance
	{
		int hp;
		int mp;
		int cp;
		int patak;
		int matak;
		int pdef;
		int mdef;
		int accuracy;
		int evasion;
		int criticalHit;
		int runSpeed;
		int attackSpeed;
		int castSpeed;
		int damge_to_h;
		int damge_to_r;
		int damge_to_l;
		int damge_blow_to_h;
		int damge_blow_to_r;
		int damge_blow_to_l;
		int damge_magic_to_h;
		int damge_magic_to_r;
		int damge_magic_to_l;
		int damge_phys_to_h;
		int damge_phys_to_r;
		int damge_phys_to_l;
	}

	//private static GArray<Integer> _dummys = new GArray<Integer>();

	/*public static void loaddummys()
	{
		if(_dummys.size() > 0)
		{
			for(Integer d : _dummys)
			{
				L2Player pl = L2ObjectsStorage.getPlayer(d);
				if(pl != null)
				{
					pl.setOfflineMode(false);
					pl.logout(false, false, true, false);
				}
			}
			_dummys.clear();
		}

		String[] qq = { "Wizard", "Daggermaster", "BowMaster", "Healer", "Enchanter", "Summoner", "ShieldMaster",
				"WeaponMaster", "ForceMaster", "Bard" };

		for(String acc : qq)
		{
			ThreadConnection con = null;
			FiltredStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.createStatement();
				rset = statement.executeQuery("SELECT `obj_Id`,`account_name` FROM `characters` WHERE `account_name` = '" + acc + "'");
				while(rset.next())
				{
					L2GameClient client = new L2GameClient(new MMOConnection<L2GameClient>(null), true);
					client.setCharSelection(rset.getInt("obj_Id"));
					L2Player p = client.loadCharFromDisk(0);
					if(p == null || p.isDead())
						continue;
					client.setLoginName(rset.getString("account_name") == null ? "OfflineTrader_" + p.getName() : rset.getString("account_name"));
					client.OnOfflineTrade();
					p.spawnMe();
					p.updateTerritories();
					p.setOnlineStatus(true);
					p.setOfflineMode(true);
					p.setDummy();

					p.setConnected(false);
					p.restoreEffects();
					p.restoreDisableSkills();
					p.setTitle(acc);
					p.broadcastUserInfo(true);
					_dummys.add(p.getObjectId());
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				DatabaseUtils.closeDatabaseCSR(con, statement, rset);
			}
		}
	}*/
}
