package l2d.game.skills;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import l2d.Config;
import l2d.ext.mods.balancer.Balancer;
import l2d.ext.mods.balancer.Balancer.bflag;
import l2d.ext.multilang.CustomMessage;
import l2d.game.cache.Msg;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.ClanHallManager;
import l2d.game.model.Inventory;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Summon;
import l2d.game.model.base.Race;
import l2d.game.model.entity.SevenSigns;
import l2d.game.model.entity.residence.Castle;
import l2d.game.model.entity.residence.ClanHall;
import l2d.game.model.entity.residence.ResidenceFunction;
import l2d.game.model.entity.residence.ResidenceType;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2RaidBossInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.conditions.ConditionPlayerState;
import l2d.game.skills.conditions.ConditionPlayerState.CheckPlayerState;
import l2d.game.skills.funcs.Func;
import l2d.game.templates.L2Armor.ArmorType;
import l2d.game.templates.L2PlayerTemplate;
import l2d.game.templates.L2Weapon;
import l2d.game.templates.L2Weapon.WeaponType;
import l2d.util.Rnd;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Formulas
{
	/** Regen Task period */
	protected static final Logger _log = Logger.getLogger(L2Character.class.getName());

	public static int MAX_STAT_VALUE = 100;

	public static final double[] WITbonus = new double[MAX_STAT_VALUE];
	public static final double[] MENbonus = new double[MAX_STAT_VALUE];
	public static final double[] INTbonus = new double[MAX_STAT_VALUE];
	public static final double[] STRbonus = new double[MAX_STAT_VALUE];
	public static final double[] DEXbonus = new double[MAX_STAT_VALUE];
	public static final double[] CONbonus = new double[MAX_STAT_VALUE];

	static
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.AttributeBonusFile);
		Document doc = null;

		try
		{
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch(final SAXException e)
		{
			e.printStackTrace();
		}
		catch(final IOException e)
		{
			e.printStackTrace();
		}
		catch(final ParserConfigurationException e)
		{
			e.printStackTrace();
		}

		int i;
		double val;

		if(doc != null)
			for(Node z = doc.getFirstChild(); z != null; z = z.getNextSibling())
				for(Node n = z.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if(n.getNodeName().equalsIgnoreCase("str_bonus"))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String node = d.getNodeName();
							if(node.equalsIgnoreCase("set"))
							{
								i = Integer.valueOf(d.getAttributes().getNamedItem("attribute").getNodeValue());
								val = Integer.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
								STRbonus[i] = (100 + val) / 100;
							}
						}
					if(n.getNodeName().equalsIgnoreCase("int_bonus"))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String node = d.getNodeName();
							if(node.equalsIgnoreCase("set"))
							{
								i = Integer.valueOf(d.getAttributes().getNamedItem("attribute").getNodeValue());
								val = Integer.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
								INTbonus[i] = (100 + val) / 100;
							}
						}
					if(n.getNodeName().equalsIgnoreCase("con_bonus"))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String node = d.getNodeName();
							if(node.equalsIgnoreCase("set"))
							{
								i = Integer.valueOf(d.getAttributes().getNamedItem("attribute").getNodeValue());
								val = Integer.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
								CONbonus[i] = (100 + val) / 100;
							}
						}
					if(n.getNodeName().equalsIgnoreCase("men_bonus"))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String node = d.getNodeName();
							if(node.equalsIgnoreCase("set"))
							{
								i = Integer.valueOf(d.getAttributes().getNamedItem("attribute").getNodeValue());
								val = Integer.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
								MENbonus[i] = (100 + val) / 100;
							}
						}
					if(n.getNodeName().equalsIgnoreCase("dex_bonus"))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String node = d.getNodeName();
							if(node.equalsIgnoreCase("set"))
							{
								i = Integer.valueOf(d.getAttributes().getNamedItem("attribute").getNodeValue());
								val = Integer.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
								DEXbonus[i] = (100 + val) / 100;
							}
						}
					if(n.getNodeName().equalsIgnoreCase("wit_bonus"))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String node = d.getNodeName();
							if(node.equalsIgnoreCase("set"))
							{
								i = Integer.valueOf(d.getAttributes().getNamedItem("attribute").getNodeValue());
								val = Integer.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
								WITbonus[i] = (100 + val) / 100;
							}
						}

				}
	}

	private static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[] func = new FuncMultRegenResting[Stats.NUM_STATS];

		static Func getFunc(final Stats stat)
		{
			final int pos = stat.ordinal();
			if(func[pos] == null)
				func[pos] = new FuncMultRegenResting(stat);
			return func[pos];
		}

		private FuncMultRegenResting(final Stats stat)
		{
			super(stat, 0x30, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.RESTING, true));
		}

		@Override
		public void calc(final Env env)
		{
			if(!_cond.test(env))
				return;

			if(env.character.isPlayer() && env.character.getLevel() <= 40 && ((L2Player) env.character).getClassId().getLevel() < 3)
				env.value *= 6;
			else
				env.value *= 1.5;
		}
	}

	private static class FuncMultRegenStanding extends Func
	{
		static final FuncMultRegenStanding[] func = new FuncMultRegenStanding[Stats.NUM_STATS];

		static Func getFunc(final Stats stat)
		{
			final int pos = stat.ordinal();
			if(func[pos] == null)
				func[pos] = new FuncMultRegenStanding(stat);
			return func[pos];
		}

		private FuncMultRegenStanding(final Stats stat)
		{
			super(stat, 0x30, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.STANDING, true));
		}

		@Override
		public void calc(final Env env)
		{
			if(!_cond.test(env))
				return;

			env.value *= 1.1;
		}
	}

	private static class FuncMultRegenRunning extends Func
	{
		static final FuncMultRegenRunning[] func = new FuncMultRegenRunning[Stats.NUM_STATS];

		static Func getFunc(final Stats stat)
		{
			final int pos = stat.ordinal();
			if(func[pos] == null)
				func[pos] = new FuncMultRegenRunning(stat);
			return func[pos];
		}

		private FuncMultRegenRunning(final Stats stat)
		{
			super(stat, 0x30, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.RUNNING, true));
		}

		@Override
		public void calc(final Env env)
		{
			if(!_cond.test(env))
				return;
			env.value *= 0.7;
		}
	}

	private static class FuncPAtkMul extends Func
	{
		static final FuncPAtkMul func = new FuncPAtkMul();

		private FuncPAtkMul()
		{
			super(Stats.POWER_ATTACK, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= STRbonus[env.character.getSTR()] * env.character.getLevelMod();
		}
	}

	private static class FuncMAtkMul extends Func
	{
		static final FuncMAtkMul func = new FuncMAtkMul();

		private FuncMAtkMul()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			// {Wpn*(lvlbn^2)*[(1+INTbn)^2]+Msty}
			final double ib = INTbonus[env.character.getINT()];
			final double lvlb = env.character.getLevelMod();
			env.value *= lvlb * lvlb * ib * ib;
		}
	}

	private static class FuncPDefMul extends Func
	{
		static final FuncPDefMul func = new FuncPDefMul();

		private FuncPDefMul()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= env.character.getLevelMod();
		}
	}

	private static class FuncMDefMul extends Func
	{
		static final FuncMDefMul func = new FuncMDefMul();

		private FuncMDefMul()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= MENbonus[env.character.getMEN()] * env.character.getLevelMod();
		}
	}

	private static class FuncAttackRange extends Func
	{
		static final FuncAttackRange func = new FuncAttackRange();

		private FuncAttackRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				env.value += weapon.getAttackRange();
		}
	}

	private static class FuncAccuracyAdd extends Func
	{
		static final FuncAccuracyAdd func = new FuncAccuracyAdd();

		private FuncAccuracyAdd()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			// [Square(DEX)]*6 + lvl + weapon hitbonus;
			env.value += Math.sqrt(env.character.getDEX()) * 6 + env.character.getLevel();
			if(env.character.isSummon())
				env.value += 5;
		}
	}

	private static class FuncEvasionAdd extends Func
	{
		static final FuncEvasionAdd func = new FuncEvasionAdd();

		private FuncEvasionAdd()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value += Math.sqrt(env.character.getDEX()) * 6 + env.character.getLevel();
		}
	}

	private static class FuncMCriticalRateMul extends Func
	{
		static final FuncMCriticalRateMul func = new FuncMCriticalRateMul();

		private FuncMCriticalRateMul()
		{
			super(Stats.MCRITICAL_RATE, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= WITbonus[env.character.getWIT()];
		}
	}

	private static class FuncPCriticalRateMul extends Func
	{
		static final FuncPCriticalRateMul func = new FuncPCriticalRateMul();

		private FuncPCriticalRateMul()
		{
			super(Stats.CRITICAL_BASE, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			if(!(env.character instanceof L2Summon))
				env.value *= DEXbonus[env.character.getDEX()];
			env.value *= 0.01 * env.character.calcStat(Stats.CRITICAL_RATE, env.target, env.skill);
		}
	}

	/**
	 * надо уточнить какие типы брони как влияют
	 */
	private static class FuncPCriticalDmgRcpt extends Func
	{
		static final FuncPCriticalDmgRcpt func = new FuncPCriticalDmgRcpt();

		private FuncPCriticalDmgRcpt()
		{
			super(Stats.CRIT_DAMAGE_RECEPTIVE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2ItemInstance i = ((L2Player) env.character).getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if(i != null && i.getItemType() == ArmorType.HEAVY && (env.character.getSkillLevel(L2Skill.SKILL_HEAVY_ARMOR_MASTERY1) > 0 || env.character.getSkillLevel(L2Skill.SKILL_HEAVY_ARMOR_MASTERY2) > 0 || env.character.getSkillLevel(L2Skill.SKILL_HEAVY_ARMOR_MASTERY3) > 0 || env.character.getSkillLevel(L2Skill.SKILL_HEAVY_ARMOR_MASTERY4) > 0))
				env.value -= 20;
		}
	}

	/**
	 * надо уточнить какие типы брони как влияют
	 * вроде бы можно удалять
	 */
	@SuppressWarnings("unused")
	private static class FuncPCriticalChanceRcpt extends Func
	{
		static final FuncPCriticalChanceRcpt func = new FuncPCriticalChanceRcpt();

		private FuncPCriticalChanceRcpt()
		{
			super(Stats.CRIT_CHANCE_RECEPTIVE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2ItemInstance i = ((L2Player) env.character).getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if(i != null && i.getItemType() == ArmorType.LIGHT && (env.character.getSkillLevel(L2Skill.SKILL_LIGHT_ARMOR_MASTERY1) > 0 || env.character.getSkillLevel(L2Skill.SKILL_LIGHT_ARMOR_MASTERY2) > 0 || env.character.getSkillLevel(L2Skill.SKILL_LIGHT_ARMOR_MASTERY3) > 0 || env.character.getSkillLevel(L2Skill.SKILL_LIGHT_ARMOR_MASTERY4) > 0 || env.character.getSkillLevel(L2Skill.SKILL_LIGHT_ARMOR_MASTERY5) > 0))
				env.value -= 10;
		}
	}

	private static class FuncMoveSpeedMul extends Func
	{
		static final FuncMoveSpeedMul func = new FuncMoveSpeedMul();

		private FuncMoveSpeedMul()
		{
			super(Stats.RUN_SPEED, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= DEXbonus[env.character.getDEX()];
		}
	}

	private static class FuncPAtkSpeedMul extends Func
	{
		static final FuncPAtkSpeedMul func = new FuncPAtkSpeedMul();

		private FuncPAtkSpeedMul()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= DEXbonus[env.character.getDEX()];
		}
	}

	private static class FuncMAtkSpeedMul extends Func
	{
		static final FuncMAtkSpeedMul func = new FuncMAtkSpeedMul();

		private FuncMAtkSpeedMul()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= WITbonus[env.character.getWIT()];
		}
	}

	private static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR func = new FuncHennaSTR();

		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatSTR());
		}
	}

	private static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX func = new FuncHennaDEX();

		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatDEX());
		}
	}

	private static class FuncHennaINT extends Func
	{
		static final FuncHennaINT func = new FuncHennaINT();

		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatINT());
		}
	}

	private static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN func = new FuncHennaMEN();

		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatMEN());
		}
	}

	private static class FuncHennaCON extends Func
	{
		static final FuncHennaCON func = new FuncHennaCON();

		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatCON());
		}
	}

	private static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT func = new FuncHennaWIT();

		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatWIT());
		}
	}

	private static class FuncMaxHpAdd extends Func
	{
		static final FuncMaxHpAdd func = new FuncMaxHpAdd();

		private FuncMaxHpAdd()
		{
			super(Stats.MAX_HP, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2PlayerTemplate t = (L2PlayerTemplate) env.character.getTemplate();
			final int lvl = Math.max(0, env.character.getLevel() - t.classBaseLevel);
			final double hpmod = t.lvlHpMod * lvl;
			final double hpmax = (t.lvlHpAdd + hpmod) * lvl;
			final double hpmin = t.lvlHpAdd * lvl + hpmod;
			env.value += (hpmax + hpmin) / 2;
		}
	}

	private static class FuncMaxHpMul extends Func
	{
		static final FuncMaxHpMul func = new FuncMaxHpMul();

		private FuncMaxHpMul()
		{
			super(Stats.MAX_HP, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= CONbonus[env.character.getCON()];
		}
	}

	private static class FuncMaxCpAdd extends Func
	{
		static final FuncMaxCpAdd func = new FuncMaxCpAdd();

		private FuncMaxCpAdd()
		{
			super(Stats.MAX_CP, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2PlayerTemplate t = (L2PlayerTemplate) env.character.getTemplate();
			final int lvl = Math.max(0, env.character.getLevel() - t.classBaseLevel);
			final double cpmod = t.lvlCpMod * lvl;
			final double cpmax = (t.lvlCpAdd + cpmod) * lvl;
			final double cpmin = t.lvlCpAdd * lvl + cpmod;
			env.value += (cpmax + cpmin) / 2;
		}
	}

	private static class FuncMaxCpMul extends Func
	{
		static final FuncMaxCpMul func = new FuncMaxCpMul();

		private FuncMaxCpMul()
		{
			super(Stats.MAX_CP, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			double cpSSmod = 1;
			final int sealOwnedBy = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE);
			final int playerCabal = SevenSigns.getInstance().getPlayerCabal((L2Player) env.character);

			if(sealOwnedBy != SevenSigns.CABAL_NULL)
				if(playerCabal == sealOwnedBy)
					cpSSmod = 1.1;
				else
					cpSSmod = 0.9;

			env.value *= CONbonus[env.character.getCON()] * cpSSmod;
		}
	}

	private static class FuncMaxMpAdd extends Func
	{
		static final FuncMaxMpAdd func = new FuncMaxMpAdd();

		private FuncMaxMpAdd()
		{
			super(Stats.MAX_MP, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2PlayerTemplate t = (L2PlayerTemplate) env.character.getTemplate();
			final int lvl = Math.max(0, env.character.getLevel() - t.classBaseLevel);
			final double mpmod = t.lvlMpMod * lvl;
			final double mpmax = (t.lvlMpAdd + mpmod) * lvl;
			final double mpmin = t.lvlMpAdd * lvl + mpmod;
			env.value += (mpmax + mpmin) / 2;
		}
	}

	private static class FuncMaxMpMul extends Func
	{
		static final FuncMaxMpMul func = new FuncMaxMpMul();

		private FuncMaxMpMul()
		{
			super(Stats.MAX_MP, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= MENbonus[env.character.getMEN()];
		}
	}

	private static class FuncFatalBlowMul extends Func
	{
		static final FuncFatalBlowMul func = new FuncFatalBlowMul();

		private FuncFatalBlowMul()
		{
			super(Stats.FATALBLOW_RATE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			if(env.target == null)
				return;

			if(!env.target.isInCombat())
				env.value *= 1.1;

			if(env.skill != null && env.skill.isBehind())
			{
				final int head = env.character.getHeadingTo(env.target, true);
				if(head <= 10430 || head >= 55105)
					env.value = 90;
				else if(head <= 21000 || head >= 44500)
					env.value = 60;
				else
					env.value = 10;
			}
			if(env.target.isPlayer())
			{
				final L2Player pcTarget = (L2Player) env.target;
				if(pcTarget.isWearingArmor(ArmorType.HEAVY))
					env.value *= env.character.isPlayer() ? Balancer.getModifyD(bflag.damge_blow_to_h, 60, env.character.getPlayer().getActiveClassId()) : 0.6;
				else if(pcTarget.isWearingArmor(ArmorType.MAGIC))
					env.value *= env.character.isPlayer() ? Balancer.getModifyD(bflag.damge_blow_to_r, 119, env.character.getPlayer().getActiveClassId()) : 1.19;
				else if(pcTarget.isWearingArmor(ArmorType.LIGHT))
					env.value *= env.character.isPlayer() ? Balancer.getModifyD(bflag.damge_blow_to_l, 100, env.character.getPlayer().getActiveClassId()) : 1.00;
			}
		}
	}

	private static class FuncPDamageResists extends Func
	{
		static final FuncPDamageResists func = new FuncPDamageResists();

		private FuncPDamageResists()
		{
			super(Stats.PHYSICAL_DAMAGE, 0x30, null);
		}

		@Override
		public void calc(final Env env)
		{
			if(env.target.isRaid() && env.character.getLevel() - env.target.getLevel() > Config.RAID_MAX_LEVEL_DIFF)
			{
				env.value = 1;
				return;
			}

			final L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				switch(weapon.getItemType())
				{
					case BOW:
						env.value *= 0.01 * env.target.calcStat(Stats.BOW_WPN_RECEPTIVE, 100, null, null);
						break;
					case BLUNT:
						env.value *= 0.01 * env.target.calcStat(Stats.BLUNT_WPN_RECEPTIVE, 100, null, null);
						break;
					case DAGGER:
						env.value *= 0.01 * env.target.calcStat(Stats.DAGGER_WPN_RECEPTIVE, 100, null, null);
						break;
					case DUAL:
						env.value *= 0.01 * env.target.calcStat(Stats.DUAL_WPN_RECEPTIVE, 100, null, null);
						break;
					case BIGSWORD:
					case SWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.SWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case POLE:
						env.value *= 0.01 * env.target.calcStat(Stats.POLE_WPN_RECEPTIVE, 100, null, null);
						break;
					case DUALFIST:
						env.value *= 0.01 * env.target.calcStat(Stats.FIST_WPN_RECEPTIVE, 100, null, null);
						break;
				}

			env.value = calcDamageResists(env.skill, env.character, env.target, env.value);
		}
	}

	private static class FuncMDamageResists extends Func
	{
		static final FuncMDamageResists func = new FuncMDamageResists();

		private FuncMDamageResists()
		{
			super(Stats.MAGIC_DAMAGE, 0x30, null);
		}

		@Override
		public void calc(final Env env)
		{
			if(env.target.isRaid() && Math.abs(env.character.getLevel() - env.target.getLevel()) > Config.RAID_MAX_LEVEL_DIFF)
			{
				env.value = 1;
				return;
			}
			env.value = calcDamageResists(env.skill, env.character, env.target, env.value);
		}
	}

	private static class FuncInventory extends Func
	{
		static final FuncInventory func = new FuncInventory();

		private FuncInventory()
		{
			super(Stats.INVENTORY_LIMIT, 0x01, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player player = (L2Player) env.character;
			if(player.isGM())
				env.value = Config.INVENTORY_MAXIMUM_GM;
			else if(player.getTemplate().race == Race.dwarf)
				env.value = Config.INVENTORY_MAXIMUM_DWARF;
			else
				env.value = Config.INVENTORY_MAXIMUM_NO_DWARF;
			env.value += player.getExpandInventory();
		}
	}

	private static class FuncWarehouse extends Func
	{
		static final FuncWarehouse func = new FuncWarehouse();

		private FuncWarehouse()
		{
			super(Stats.STORAGE_LIMIT, 0x01, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player player = (L2Player) env.character;
			if(player.getTemplate().race == Race.dwarf)
				env.value = Config.WAREHOUSE_SLOTS_DWARF;
			else
				env.value = Config.WAREHOUSE_SLOTS_NO_DWARF;
			env.value += player.getExpandWarehouse();
		}
	}

	private static class FuncTradeLimit extends Func
	{
		static final FuncTradeLimit func = new FuncTradeLimit();

		private FuncTradeLimit()
		{
			super(Stats.TRADE_LIMIT, 0x01, null);
		}

		@Override
		public void calc(final Env env)
		{
			final L2Player _cha = (L2Player) env.character;
			if(_cha.getRace() == Race.dwarf)
				env.value = Config.MAX_PVTSTORE_SLOTS_DWARF;
			else
				env.value = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
	}

	private static class FuncSDefAll extends Func
	{
		static final FuncSDefAll func = new FuncSDefAll();

		private FuncSDefAll()
		{
			super(Stats.SHIELD_RATE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			if(env.value == 0)
				return;

			final L2Character target = env.target;
			if(target != null)
			{
				final L2Weapon weapon = target.getActiveWeaponItem();
				if(weapon != null)
					switch(weapon.getItemType())
					{
						case BOW:
							env.value += 30.;
							break;
						case DAGGER:
							env.value += 12.;
							break;
					}
			}
		}
	}

	private static class FuncSDefPlayers extends Func
	{
		static final FuncSDefPlayers func = new FuncSDefPlayers();

		private FuncSDefPlayers()
		{
			super(Stats.SHIELD_RATE, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			if(env.value == 0)
				return;

			final L2Character cha = env.character;
			final L2ItemInstance shld = ((L2Player) cha).getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if(shld == null || shld.getItemType() != WeaponType.NONE)
				return;
			env.value *= DEXbonus[cha.getDEX()];
		}
	}

	/**
	 * Add basics Func objects to L2Player and L2Summon.<BR><BR>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).
	 * In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <BR><BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
	 * 
	 * @param cha
	 *            L2Player or L2Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(final L2Character cha)
	{
		if(cha.isPlayer())
		{
			cha.addStatFunc(FuncMultRegenResting.getFunc(Stats.REGENERATE_CP_RATE));
			cha.addStatFunc(FuncMultRegenStanding.getFunc(Stats.REGENERATE_CP_RATE));
			cha.addStatFunc(FuncMultRegenRunning.getFunc(Stats.REGENERATE_CP_RATE));
			cha.addStatFunc(FuncMultRegenResting.getFunc(Stats.REGENERATE_HP_RATE));
			cha.addStatFunc(FuncMultRegenStanding.getFunc(Stats.REGENERATE_HP_RATE));
			cha.addStatFunc(FuncMultRegenRunning.getFunc(Stats.REGENERATE_HP_RATE));
			cha.addStatFunc(FuncMultRegenResting.getFunc(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncMultRegenStanding.getFunc(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncMultRegenRunning.getFunc(Stats.REGENERATE_MP_RATE));

			cha.addStatFunc(FuncMaxCpAdd.func);
			cha.addStatFunc(FuncMaxHpAdd.func);
			cha.addStatFunc(FuncMaxMpAdd.func);

			cha.addStatFunc(FuncMaxCpMul.func);
			cha.addStatFunc(FuncMaxHpMul.func);
			cha.addStatFunc(FuncMaxMpMul.func);

			cha.addStatFunc(FuncAttackRange.func);

			cha.addStatFunc(FuncMoveSpeedMul.func);

			cha.addStatFunc(FuncHennaSTR.func);
			cha.addStatFunc(FuncHennaDEX.func);
			cha.addStatFunc(FuncHennaINT.func);
			cha.addStatFunc(FuncHennaMEN.func);
			cha.addStatFunc(FuncHennaCON.func);
			cha.addStatFunc(FuncHennaWIT.func);

			cha.addStatFunc(FuncInventory.func);
			cha.addStatFunc(FuncWarehouse.func);
			cha.addStatFunc(FuncTradeLimit.func);

			cha.addStatFunc(FuncFatalBlowMul.func);

			cha.addStatFunc(FuncPAtkMul.func);
			cha.addStatFunc(FuncMAtkMul.func);
			cha.addStatFunc(FuncPDefMul.func);
			cha.addStatFunc(FuncMDefMul.func);

			cha.addStatFunc(FuncPCriticalDmgRcpt.func);
			// cha.addStatFunc(FuncPCriticalChanceRcpt.func); -- отключаем

			cha.addStatFunc(FuncPAtkSpeedMul.func);
			cha.addStatFunc(FuncMAtkSpeedMul.func);

			cha.addStatFunc(FuncSDefPlayers.func);
		}

		cha.addStatFunc(FuncSDefAll.func);

		cha.addStatFunc(FuncPCriticalRateMul.func);
		cha.addStatFunc(FuncMCriticalRateMul.func);

		cha.addStatFunc(FuncAccuracyAdd.func);
		cha.addStatFunc(FuncEvasionAdd.func);

		cha.addStatFunc(FuncPDamageResists.func);
		cha.addStatFunc(FuncMDamageResists.func);
	}

	/**
	 * Calculate the HP regen rate (base + modifiers).<BR><BR>
	 */
	public static double calcHpRegen(final L2Character cha)
	{
		double init;
		if(cha.isPlayer())
			init = (cha.getLevel() <= 10 ? 1.95 + cha.getLevel() / 20. : 1.4 + cha.getLevel() / 10.) * cha.getLevelMod() * CONbonus[cha.getCON()];
		else
			init = cha.getTemplate().baseHpReg * CONbonus[cha.getCON()];

		if(cha instanceof L2Playable)
		{
			final L2Player player = cha.getPlayer();
			if(player != null && player.getClan() != null && player.getInResidence() != ResidenceType.None)
				switch(player.getInResidence())
				{
					case Clanhall:
						final int clanHallIndex = player.getClan().getHasHideout();
						if(clanHallIndex > 0)
						{
							final ClanHall clansHall = ClanHallManager.getInstance().getClanHall(clanHallIndex);
							if(clansHall != null)
								if(clansHall.isFunctionActive(ResidenceFunction.RESTORE_HP))
									init *= 1. + clansHall.getFunction(ResidenceFunction.RESTORE_HP).getLevel() / 100.;
						}
						break;
					case Castle:
						final int caslteIndex = player.getClan().getHasCastle();
						if(caslteIndex > 0)
						{
							final Castle castle = CastleManager.getInstance().getCastleByIndex(caslteIndex);
							if(castle != null)
								if(castle.isFunctionActive(ResidenceFunction.RESTORE_HP))
									init *= 1. + castle.getFunction(ResidenceFunction.RESTORE_HP).getLevel() / 100.;
						}
						break;
				}
		}

		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null);
	}

	/**
	 * Calculate the MP regen rate (base + modifiers).<BR><BR>
	 */
	public static double calcMpRegen(final L2Character cha)
	{
		double init;
		if(cha.isPlayer())
			init = (.87 + cha.getLevel() * .03) * cha.getLevelMod();
		else
			init = cha.getTemplate().baseMpReg;

		if(cha.isPlayable())
			init *= MENbonus[cha.getMEN()];
		if(cha.isSummon())
			init *= 2;
		if(cha.isRaid())
			init *= 3;

		if(cha.isPlayable())
		{
			final L2Player player = cha.getPlayer();
			if(player != null)
			{
				final L2Clan clan = player.getClan();
				if(clan != null)
					switch(player.getInResidence())
					{
						case Clanhall:
							final int clanHallIndex = clan.getHasHideout();
							if(clanHallIndex > 0)
							{
								final ClanHall clansHall = ClanHallManager.getInstance().getClanHall(clanHallIndex);
								if(clansHall != null)
									if(clansHall.isFunctionActive(ResidenceFunction.RESTORE_MP))
										init *= 1. + clansHall.getFunction(ResidenceFunction.RESTORE_MP).getLevel() / 100.;
							}
							break;
						case Castle:
							final int caslteIndex = clan.getHasCastle();
							if(caslteIndex > 0)
							{
								final Castle castle = CastleManager.getInstance().getCastleByIndex(caslteIndex);
								if(castle != null)
									if(castle.isFunctionActive(ResidenceFunction.RESTORE_MP))
										init *= 1. + castle.getFunction(ResidenceFunction.RESTORE_MP).getLevel() / 100.;
							}
							break;
					}
			}
		}

		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null);
	}

	/**
	 * Calculate the CP regen rate (base + modifiers).<BR><BR>
	 */
	public static double calcCpRegen(final L2Character cha)
	{
		// double init = ((0.08 * cha.getLevel() + cha.getTemplate().baseCpReg) * (0.6 + CONbonus[cha.getCON()])) * (Config.CP_REGEN_MULTIPLIER / 100);
		final double init = (1.5 + cha.getLevel() / 10) * cha.getLevelMod() * CONbonus[cha.getCON()];
		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null);
	}

	public static class AttackInfo
	{
		public double damage;
		public double defence;
		public double crit_rcpt;
		public double crit_static;
		public double death_rcpt;
		public double lethal1;
		public double lethal2;
		public boolean crit;
		public boolean shld;
		public boolean lethal;
	}

	/**
	 * Для простых ударов
	 * patk = patk
	 * При крите простым ударом:
	 * patk = patk * (1 + crit_damage_rcpt) * crit_damage_mod + crit_damage_static
	 * Для blow скиллов
	 * patk = (patk + skill_power + crit_damage_static) * crit_damage_mod
	 * Для скилловых критов, повреждения просто удваиваются, бафы не влияют (кроме blow, для них выше)
	 * patk = (1 + crit_damage_rcpt) * (patk + skill_power)
	 * Для любых атак по игрокам
	 * damage = patk * ss_bonus * 70 / pdef
	 * По монстрам
	 * damage = patk * ss_bonus * 100 / pdef
	 */
	public static AttackInfo calcPhysDam(final L2Character attacker, final L2Character target, final L2Skill skill, final boolean dual, final boolean blow, final boolean ss)
	{
		target.doCounterAttack(skill, attacker);

		final AttackInfo info = new AttackInfo();

		info.damage = attacker.getPAtk(target);
		info.defence = target.getPDef(attacker);
		info.crit_rcpt = 0.01 * target.calcStat(Stats.CRIT_DAMAGE_RECEPTIVE, null, null);
		info.crit_static = attacker.calcStat(Stats.CRITICAL_DAMAGE_STATIC, target, null);
		info.death_rcpt = 0.01 * target.calcStat(Stats.DEATH_RECEPTIVE, 100, null, null);
		info.lethal1 = skill != null ? skill.getLethal1() * info.death_rcpt : 0;
		info.lethal2 = skill != null ? skill.getLethal2() * info.death_rcpt : 0;
		// info.crit = skill == null && calcCrit(attacker, target, attacker.getCriticalHit(target, null));
		info.crit = calcCrit(attacker, target, skill);
		info.shld = (skill == null || !skill.getShieldIgnore()) && calcShldUse(attacker, target);
		info.lethal = false;

		if(info.shld)
			info.defence += target.getShldDef();

		if(info.defence == 0)
			info.defence = 1;

		if(skill != null)
		{
			double power = Math.max(0., skill.getPower());
			if(blow)
				power = info.crit_rcpt * (power + info.crit_static) * 0.01 * attacker.calcStat(Stats.CRITICAL_DAMAGE, target, skill);
			// Если все ок - каменты удалить
			// power = (ss ? 1.5 : 1.0) * info.crit_rcpt * attacker.calcStat(Stats.CRITICAL_DAMAGE, power + info.crit_static, target, skill);
			// else if(ss)
			// power *= 2.;
			info.damage += power;
			if(skill.isChargeBoost())
				info.damage *= 0.8 + 0.2 * Math.min(attacker.getIncreasedForce(), 5);
			if(info.crit)
				info.damage = (1.0 + info.crit_rcpt) * info.damage;
		}
		else
		{
			if(dual)
				info.damage /= 2.;
			// Если все ок - каменты удалить
			// if(ss)
			// info.damage *= 2.;
			if(info.crit)
			{
				info.damage = (1.0 + info.crit_rcpt) * info.damage * 0.01 * attacker.calcStat(Stats.CRITICAL_DAMAGE, target, skill);
				info.damage += info.crit_static;
			}
		}

		if(info.crit)
		{
			int chance = attacker.getSkillLevel(467);
			if(chance > 0)
			{
				if(chance >= 21)
					chance = 30;
				else if(chance >= 15)
					chance = 25;
				else if(chance >= 9)
					chance = 20;
				else if(chance >= 4)
					chance = 15;
				if(Rnd.chance(chance))
					attacker.setConsumedSouls(attacker.getConsumedSouls() + 1, null);
			}
		}

		switch(attacker.getDirectionTo(target, true))
		{
			case BEHIND:
				info.damage *= info.crit ? 1.1 : 1.2;
				break;
			case SIDE:
				info.damage *= info.crit ? 1.025 : 1.05;
				break;
		}

		if(ss)
			info.damage *= blow ? 1.5 : 2;

		info.damage *= 70. / info.defence;
		info.damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;
		info.damage = attacker.calcStat(Stats.PHYSICAL_DAMAGE, info.damage, target, skill);

		// In C5 summons make 10 % less dmg in PvP.
		if(attacker.isSummon() && target.isPlayer())
			info.damage *= 0.9;
		if(info.shld && Rnd.chance(5))
			info.damage = 1;

		// Тут проверяем только если skill != null, т.к. L2Character.onHitTimer не обсчитывает дамаг.
		if(skill != null)
		{
			if(info.shld)
				if(info.damage == 1)
					target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
				else
					target.sendPacket(Msg.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);

			if(Rnd.chance(target.calcStat(Stats.PSKILL_EVASION, 0, null, skill)))
			{
				attacker.sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_WENT_ASTRAY).addName(attacker));
				target.sendPacket(new SystemMessage(SystemMessage.C1_HAS_EVADED_C2S_ATTACK).addName(target).addName(attacker));
				info.damage = 1;
			}

			if(info.damage > 1.0 && skill.isDeathlink())
			{
				//info.damage *= 1.8 * (1.0 - attacker.getCurrentHpRatio());
				double part = attacker.getCurrentHp() / attacker.getMaxHp();
				info.damage *= Math.pow(1.7165 - part, 2) * 0.577;
			}
			
			if(attacker.isPlayer() && target.isPlayer())
			{
				L2Player targetp = ((L2Player) target);
				L2Player attackp = ((L2Player) attacker);

				if(targetp != null && attackp != null)
				{
					if(targetp.isWearingArmor(ArmorType.HEAVY))
						info.damage = Balancer.getModifyD(bflag.damge_phys_to_h, (int) info.damage, attackp.getActiveClassId());
					else if(targetp.isWearingArmor(ArmorType.MAGIC))
						info.damage = Balancer.getModifyD(bflag.damge_phys_to_r, (int) info.damage, attackp.getActiveClassId());
					else if(targetp.isWearingArmor(ArmorType.LIGHT))
						info.damage = Balancer.getModifyD(bflag.damge_phys_to_l, (int) info.damage, attackp.getActiveClassId());
				}
			}

			if(Rnd.chance(info.lethal1))
			{
				if(target.isPlayer())
				{
					info.damage = Math.max(info.damage, target.getCurrentCp());
					target.sendPacket(Msg.CP_DISAPPEARS_WHEN_HIT_WITH_A_HALF_KILL_SKILL);
					info.lethal = true;
				}
				else if(target.isLethalImmune())
					info.damage *= 2;
				else
					info.damage = Math.max(info.damage, target.getCurrentHp() / 2);
				attacker.sendPacket(Msg.HALF_KILL);
			}
			else if(Rnd.chance(info.lethal2))
			{
				if(target.isPlayer())
				{
					info.damage = Math.max(info.damage, target.getCurrentHp() + target.getCurrentCp() - 1.0);
					target.sendPacket(Msg.LETHAL_STRIKE);
					info.lethal = true;
				}
				else if(target.isLethalImmune())
					info.damage *= 3;
				else
					info.damage = Math.max(info.damage, target.getCurrentHp());
				attacker.sendPacket(Msg.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
			}
			
			if(attacker instanceof L2Summon)
				((L2Summon) attacker).displayHitMessage(target, (int) info.damage, info.crit, false);
			else if(attacker instanceof L2Player)
			{
				if(info.crit)
					attacker.sendPacket(new SystemMessage(SystemMessage.C1_HAD_A_CRITICAL_HIT).addName(attacker));
				attacker.sendPacket(new SystemMessage(SystemMessage.C1_HAS_GIVEN_C2_DAMAGE_OF_S3).addName(attacker).addName(target).addNumber((int) info.damage));
			}

			if(target.isStunned() && calcStunBreak(info.crit))
			{
				target.getEffectList().stopEffects(EffectType.Stun);
				target.getEffectList().stopEffects(EffectType.Turner); // stun from bluff
			}

			if(calcCastBreak(target, info.crit))
				target.breakCast(false);
		}
		else
		{
			if(attacker.isPlayer() && target.isPlayer())
			{
				L2Player targetp = ((L2Player) target);
				L2Player attackp = ((L2Player) attacker);

				if(targetp != null && attackp != null)
				{
					if(targetp.isWearingArmor(ArmorType.HEAVY))
						info.damage = Balancer.getModifyD(bflag.damge_to_h, (int) info.damage, attackp.getActiveClassId());
					else if(targetp.isWearingArmor(ArmorType.MAGIC))
						info.damage = Balancer.getModifyD(bflag.damge_to_r, (int) info.damage, attackp.getActiveClassId());
					else if(targetp.isWearingArmor(ArmorType.LIGHT))
						info.damage = Balancer.getModifyD(bflag.damge_to_l, (int) info.damage, attackp.getActiveClassId());
				}
			}
		}
		
		
		
		info.damage = Math.max(1., info.damage);
		return info;
	}

	public static double calcMagicDam(final L2Character attacker, final L2Character target, final L2Skill skill, final int sps)
	{
		final boolean shield = skill.getShieldIgnore() && calcShldUse(attacker, target);
		double mAtk = attacker.getMAtk(target, skill);

		if(sps == 2)
			mAtk *= 4;
		else if(sps == 1)
			mAtk *= 2;

		double mdef = target.getMDef(null, skill);

		if(shield)
			mdef += target.getShldDef();
		if(mdef == 0)
			mdef = 1;

		double power = skill.getPower();

		double damage = 91 * power * Math.sqrt(mAtk) / mdef;

		damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;

		final boolean crit = calcMCrit(attacker.getMagicCriticalRate(target, skill));

		if(crit)
			damage *= 3;

		if(damage > 1 && skill.isDeathlink())
			damage *= 1.8 * (1.0 - attacker.getCurrentHpRatio());

		damage = attacker.calcStat(Stats.MAGIC_DAMAGE, damage, target, skill);

		if(shield)
		{
			if(Rnd.chance(5))
			{
				damage = 1;
				target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			}
			else
				target.sendPacket(Msg.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
			attacker.sendMessage("Spell deflected");
		}

		final int mLevel = skill.getMagicLevel() == 0 ? attacker.getLevel() : skill.getMagicLevel();
		final int levelDiff = target.getLevel() - mLevel;

		if(levelDiff > -4) // Фейлы возможны даже на зеленых мобах
		{
			final double failChance = 3 * Math.max(1, levelDiff);
			if(Rnd.chance(.1 * failChance))
			{
				damage = 1;
				final SystemMessage msg = new SystemMessage(SystemMessage.C1_RESISTED_C2S_MAGIC).addName(target).addName(attacker);
				attacker.sendPacket(msg);
				target.sendPacket(msg);
			}
			else if(Rnd.chance(failChance))
			{
				damage /= 2;
				final SystemMessage msg = new SystemMessage(SystemMessage.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_AGAINST_C2S_MAGIC).addName(target).addName(attacker);
				attacker.sendPacket(msg);
				target.sendPacket(msg);
			}
		}
		
		if(attacker.isPlayer() && target.isPlayer())
		{
			L2Player targetp = ((L2Player) target);
			L2Player attackp = ((L2Player) attacker);

			if(targetp != null && attackp != null)
			{
				if(targetp.isWearingArmor(ArmorType.HEAVY))
					damage = Balancer.getModifyD(bflag.damge_magic_to_h, (int) damage, attackp.getActiveClassId());
				else if(targetp.isWearingArmor(ArmorType.MAGIC))
					damage = Balancer.getModifyD(bflag.damge_magic_to_r, (int) damage, attackp.getActiveClassId());
				else if(targetp.isWearingArmor(ArmorType.LIGHT))
					damage = Balancer.getModifyD(bflag.damge_magic_to_l, (int) damage, attackp.getActiveClassId());
			}
		}
		
		if(Rnd.chance(target.calcStat(Stats.MSKILL_EVASION, 0, null, skill)))
		{
			attacker.sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_WENT_ASTRAY).addName(attacker));
			target.sendPacket(new SystemMessage(SystemMessage.C1_HAS_EVADED_C2S_ATTACK).addName(target).addName(attacker));
			damage = 1;
		}

		if(attacker instanceof L2Summon)
			((L2Summon) attacker).displayHitMessage(target, (int) damage, crit, false);
		else if(attacker instanceof L2Player)
		{
			if(crit)
				attacker.sendPacket(new SystemMessage(SystemMessage.MAGIC_CRITICAL_HIT).addName(attacker));
			attacker.sendPacket(new SystemMessage(SystemMessage.C1_HAS_GIVEN_C2_DAMAGE_OF_S3).addName(attacker).addName(target).addNumber((int) damage));
		}

		if(calcCastBreak(target, crit))
			target.breakCast(false);
		return damage;
	}

	public static boolean calcStunBreak(final boolean crit)
	{
		return Rnd.chance(crit ? 75 : 10);
	}

	/** Returns true in case of fatal blow success */
	public static boolean calcBlow(final L2Character activeChar, final L2Character target, final L2Skill skill)
	{
		return Rnd.chance((int) activeChar.calcStat(Stats.FATALBLOW_RATE, activeChar.getDEX(), target, skill));
	}

	/** Returns true in case of critical hit */
	public static boolean calcCrit(final L2Character attacker, final L2Character target, final L2Skill skill)
	{
		if(attacker.isPlayer() && attacker.getActiveWeaponItem() == null)
			return false;
		if(skill != null && !skill.isCritical())
			return false;
		int rate = skill != null ? (int) (attacker.getTemplate().baseCritRate * STRbonus[attacker.getSTR()]) : attacker.getCriticalHit(target, null);
		if(skill == null)
		{
			rate *= 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, null, null);
			switch(attacker.getDirectionTo(target, true))
			{
				case BEHIND:
					rate *= 1.4;
					break;
				case SIDE:
					rate *= 1.2;
					break;
			}
		}
		return Rnd.get() * 1000 <= rate;
	}

	/** Returns true in case of magic critical hit */
	public static boolean calcMCrit(final double mRate)
	{
		return Rnd.get() * 100 <= Math.min(Config.MAX_MCRIT_RATE, mRate);
	}

	/** Returns true in case when ATTACK is canceled due to hit */
	public static boolean calcCastBreak(final L2Character target, final boolean crit)
	{
		return !(!target.isCastingNow() || target instanceof L2RaidBossInstance) && Rnd.chance((int) target.calcStat(Stats.CAST_INTERRUPT, crit ? 75 : 10, null, null));
	}

	/** Calculate delay (in milliseconds) before next ATTACK */
	public static int calcPAtkSpd(final double rate)
	{
		return (int) (500000 / rate); // в миллисекундах поэтому 500*1000
	}

	/** Calculate delay (in milliseconds) for skills cast */
	public static int calcMAtkSpd(final L2Character attacker, final L2Skill skill, final double skillTime)
	{
		if(skill.isMagic())
			return (int) (skillTime * 333 / Math.max(attacker.getMAtkSpd(), 1));
		return (int) (skillTime * 333 / Math.max(attacker.getPAtkSpd(), 1));
	}

	/** Calculate reuse delay (in milliseconds) for skills */
	public static long calcSkillReuseDelay(final L2Character actor, final L2Skill skill)
	{
		long reuseDelay = skill.getReuseDelay();
		if(!actor.isPlayer() && !actor.isSummon() && !actor.isPet())
			if(skill.isMagic())
				reuseDelay = 0;
			else
				reuseDelay = Math.min(skill.isMagic() ? skill.getHitTime() < 1000 ? 5000 : skill.getHitTime() + 2000 : skill.getHitTime() + 5000, reuseDelay);
		if(skill.isReuseDelayPermanent() || skill.isHandler() || skill.isItemSkill())
			return reuseDelay;
		if(actor.getSkillMastery(skill.getId()) == 1)
		{
			actor.removeSkillMastery(skill.getId());
			return 0;
		}
		if(skill.isMagic())
			return (long) actor.calcStat(Stats.MAGIC_REUSE_RATE, reuseDelay, null, skill) * 333 / Math.max(actor.getMAtkSpd(), 1);
		return (long) actor.calcStat(Stats.PHYSIC_REUSE_RATE, reuseDelay, null, skill) * 333 / Math.max(actor.getPAtkSpd(), 1);
	}

	/** Returns true if hit missed (target evaded) */
	public static boolean calcHitMiss(final L2Character attacker, final L2Character target)
	{
		int chanceToHit = 88 + 2 * (attacker.getAccuracy() - target.getEvasionRate(attacker));

		chanceToHit = Math.max(chanceToHit, 28);
		chanceToHit = Math.min(chanceToHit, 98);

		if(attacker.isBehindTarget(target))
			chanceToHit *= 1.2;
		if(attacker.isToSideOfTarget(target))
			chanceToHit *= 1.1;

		return !Rnd.chance(chanceToHit);
	}

	/** Returns true if shield defence successfull */
	public static boolean calcShldUse(final L2Character attacker, final L2Character target)
	{
		final int angle = (int) target.calcStat(Stats.SHIELD_ANGLE, null, null);
		if(!target.isInFront(attacker, angle))
			return false;
		return Rnd.chance(target.calcStat(Stats.SHIELD_RATE, attacker, null));
	}

	public static double calcSavevsDependence(final int save, final L2Character cha)
	{
		try
		{
			switch(save)
			{
				case L2Skill.SAVEVS_INT:
					return INTbonus[cha.getINT()];
				case L2Skill.SAVEVS_WIT:
					return WITbonus[cha.getWIT()];
				case L2Skill.SAVEVS_MEN:
					return MENbonus[cha.getMEN()];
				case L2Skill.SAVEVS_CON:
					return CONbonus[cha.getCON()];
				case L2Skill.SAVEVS_DEX:
					return DEXbonus[cha.getDEX()];
				case L2Skill.SAVEVS_STR:
					return STRbonus[cha.getSTR()];
			}
		}
		catch(final ArrayIndexOutOfBoundsException e)
		{
			_log.warning("Failed calc savevs on char " + cha + " with save-stat " + save);
			e.printStackTrace();
		}
		return 1.;
	}
	
	
	public static boolean calcSkillSuccess(final Env env, final Stats resistType, final Stats attibuteType)
	{
		if(env.value == -1)
			return true;

		env.value = Math.max(Math.min(env.value, 100), 1); // На всякий случай

		final double min = Math.min(env.value, Config.SKILLS_CHANCE_MIN); // Запоминаем базовый шанс (нужен позже)

		if(env.skill.isMagic()) // Этот блок только для магических скиллов
		{
			final int mdef = Math.max(1, env.target.getMDef(env.target, env.skill)); // Вычисляем mDef цели
			// env.value *= 128 * Math.pow(env.character.getMAtk(env.target, env.skill), .2) / mdef; // Старая формула
			env.value *= Config.SKILLS_CHANCE_MOD * Math.sqrt(env.character.getMAtk(env.target, env.skill)) / mdef;

			if(env.skill.isSSPossible()) // Считаем бонус от шотов
				switch(env.character.getChargedSpiritShot())
				{
					case 1:
						env.value *= 1.41;
						break;
					case 2:
						env.value *= 2;
						break;
				}
		}

		env.value = Math.max(Math.min(env.value, 100), 1); // Убираем лишнее

		env.value /= calcSavevsDependence(env.skill.getSavevs(), env.target); // Бонус от MEN/CON/etc

		if(resistType != null)
			env.value *= 0.01 * env.target.calcStat(resistType, 100, null, null); // Различные сопротивляемости/восприимчивости

		if(attibuteType != null)
			env.value *= 0.01 * env.character.calcStat(attibuteType, 100, null, null); // Различные аттрибуты (не стихийные)

		env.value = env.character.calcStat(Stats.ACTIVATE_RATE, env.value, env.target, env.skill); // Учитываем общий бонус к шансам, если есть

		env.value = Math.max(env.value, min); // Если базовый шанс более Config.SKILLS_CHANCE_MIN, то при небольшой разнице в уровнях, делаем
		// кап снизу.

		final double mLevel = env.skill.getMagicLevel() == 0 || !env.character.isPlayer() ? env.character.getLevel() : env.skill.getMagicLevel(); // Разница в уровнях
		env.value += (mLevel - env.target.getLevel()) * env.skill.getLevelModifier(); // Бонус к шансу от разницы в уровнях

		env.value = Math.max(Math.min(env.value, Config.SKILLS_CHANCE_CAP), 1); // Применяем кап

		if(Config.SKILLS_SHOW_CHANCE && env.character.isPlayer() && !((L2Player) env.character).getVarB("SkillsHideChance")) // Выводим сообщение с шансом
			env.character.sendMessage(new CustomMessage("l2d.game.skills.Formulas.Chance", env.character).addString(env.skill.getName()).addNumber((int) env.value));
		return Rnd.chance(env.value);
	}
	
	public static boolean calcSkillSuccess(Env env, Stats resistType, Stats attibuteType, int spiritshot)
	{
		if(env.value == -1)
			return true;
		L2Skill skill = env.skill;
		if(!skill.isOffensive())
			return Rnd.chance(env.value);
		L2Character character = env.character;
		L2Character target = env.target;
		env.value = Math.max(Math.min(env.value, 100), 1); // На всякий случай
		double base = env.value; // Запоминаем базовый шанс (нужен позже)
		double mLevel = skill.getMagicLevel() == 0 || !character.isPlayer() ? character.getLevel() : skill.getMagicLevel(); // Разница в уровнях
		mLevel = (mLevel - target.getLevel() + 3) * skill.getLevelModifier();
		env.value += mLevel >= 0 ? 0 : mLevel;

		if(skill.getSavevs() > 0)
			env.value += 30 - calcSavevsDependence(skill.getSavevs(), target);

		env.value = Math.max(env.value, 1);

		if(skill.isMagic()) // Этот блок только для магических скиллов
		{
			int mdef = Math.max(1, target.getMDef(target, skill)); // Вычисляем mDef цели
			double matk = character.getMAtk(target, skill);
			if(skill.isSSPossible() && spiritshot > 0)
				matk *= spiritshot * 2;
			env.value *= Config.SKILLS_CHANCE_MOD * Math.pow(matk, 0.5) / mdef;
		}
		if(resistType != null)
		{
			double res = 0;
			if(resistType != null)
				res += target.calcStat(resistType, character, skill);
			if(attibuteType != null)
				res -= character.calcStat(attibuteType, target, skill);
			res += target.calcStat(Stats.DEBUFF_RECEPTIVE, character, skill);
			if(res != 0)
			{
				double mod = Math.abs(0.02 * res) + 1;
				env.value = res > 0 ? env.value / mod : env.value * mod;
			}
		}
		env.value = character.calcStat(Stats.ACTIVATE_RATE, env.value, target, skill); // Учитываем общий бонус к шансам, если есть
		//if(skill.isSoulBoost()) // Бонус от душ камаелей
		//	env.value *= 0.85 + 0.06 * Math.min(character.getConsumedSouls(), 5);
		env.value = Math.max(env.value, Math.min(base, Config.SKILLS_CHANCE_MIN)); // Если базовый шанс более Config.SKILLS_CHANCE_MIN, то при небольшой разнице в уровнях, делаем кап снизу.
		env.value = Math.max(Math.min(env.value, Config.SKILLS_CHANCE_CAP), 1); // Применяем кап
		return Rnd.chance(env.value);
	}


	public static boolean calcSkillSuccess(final L2Character player, final L2Character target, final L2Skill skill)
	{
		final Env env = new Env();
		env.character = player;
		env.target = target;
		env.skill = skill;
		env.value = skill.getActivateRate();
		switch(skill.getSkillType())
		{
			case CANCEL:
			case NEGATE_EFFECTS:
			case NEGATE_STATS:
				return calcSkillSuccess(env, Stats.CANCEL_RECEPTIVE, Stats.CANCEL_POWER, player.getChargedSpiritShot());
			case DESTROY_SUMMON:
				return calcSkillSuccess(env, Stats.MENTAL_RECEPTIVE, Stats.MENTAL_POWER, player.getChargedSpiritShot());
			default:
				return calcSkillSuccess(env, null,null,player.getChargedSpiritShot());
		}
	}

	public static void calcSkillMastery(final L2Skill skill, final L2Character activeChar)
	{
		if(skill.isHandler())
			return;

		// Skill id 330 for fighters, 331 for mages
		// Actually only GM can have 2 skill masteries, so let's make them more lucky ^^
		if(activeChar.getSkillLevel(331) > 0 && activeChar.calcStat(Stats.SKILL_MASTERY, activeChar.getINT(), null, skill) >= Rnd.get(1000) || activeChar.getSkillLevel(330) > 0 && activeChar.calcStat(Stats.SKILL_MASTERY, activeChar.getSTR(), null, skill) >= Rnd.get(1000))
		{
			// byte mastery level, 0 = no skill mastery, 1 = no reuseTime, 2 = buff duration*2, 3 = power*3
			byte masteryLevel;
			final L2Skill.SkillType type = skill.getSkillType();
			if(type == L2Skill.SkillType.BUFF || type == L2Skill.SkillType.MUSIC || type == L2Skill.SkillType.HOT || type == L2Skill.SkillType.HEAL_PERCENT) // Hope i didn't forget skills to multiply
				// their time
				masteryLevel = 2;
			else if(type == L2Skill.SkillType.HEAL)
				masteryLevel = 3;
			else
				masteryLevel = 1;
			if(masteryLevel > 0)
				activeChar.setSkillMastery(skill.getId(), masteryLevel);
		}
	}

	public static double calcDamageResists(final L2Skill skill, final L2Character attacker, final L2Character defender, final double value)
	{
		if(skill != null)
			switch(skill.getElement())
			{
				case FIRE:
					return applyDefense(attacker, defender, Stats.FIRE_RECEPTIVE, (int) attacker.calcStat(Stats.ATTACK_ELEMENT_FIRE, skill.getElementPower(), null, null), value, skill);
				case WATER:
					return applyDefense(attacker, defender, Stats.WATER_RECEPTIVE, (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WATER, skill.getElementPower(), null, null), value, skill);
				case WIND:
					return applyDefense(attacker, defender, Stats.WIND_RECEPTIVE, (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WIND, skill.getElementPower(), null, null), value, skill);
				case EARTH:
					return applyDefense(attacker, defender, Stats.EARTH_RECEPTIVE, (int) attacker.calcStat(Stats.ATTACK_ELEMENT_EARTH, skill.getElementPower(), null, null), value, skill);
				case SACRED:
					return applyDefense(attacker, defender, Stats.SACRED_RECEPTIVE, (int) attacker.calcStat(Stats.ATTACK_ELEMENT_SACRED, skill.getElementPower(), null, null), value, skill);
				case UNHOLY:
					return applyDefense(attacker, defender, Stats.UNHOLY_RECEPTIVE, (int) attacker.calcStat(Stats.ATTACK_ELEMENT_UNHOLY, skill.getElementPower(), null, null), value, skill);
				default:
					return value;
			}

		final int fire_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_FIRE, 0, null, null);
		final int water_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WATER, 0, null, null);
		final int wind_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WIND, 0, null, null);
		final int earth_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_EARTH, 0, null, null);
		final int sacred_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_SACRED, 0, null, null);
		final int unholy_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_UNHOLY, 0, null, null);

		if(fire_attack == 0 && water_attack == 0 && earth_attack == 0 && wind_attack == 0 && unholy_attack == 0 && sacred_attack == 0)
			return value;

		final TreeMap<Integer, Stats> sort_attibutes = new TreeMap<Integer, Stats>();
		sort_attibutes.put(fire_attack, Stats.FIRE_RECEPTIVE);
		sort_attibutes.put(water_attack, Stats.WATER_RECEPTIVE);
		sort_attibutes.put(wind_attack, Stats.WIND_RECEPTIVE);
		sort_attibutes.put(earth_attack, Stats.EARTH_RECEPTIVE);
		sort_attibutes.put(sacred_attack, Stats.SACRED_RECEPTIVE);
		sort_attibutes.put(unholy_attack, Stats.UNHOLY_RECEPTIVE);

		final int attack = sort_attibutes.lastEntry().getKey();
		final Stats defence_type = sort_attibutes.lastEntry().getValue();

		return applyDefense(attacker, defender, defence_type, attack, value, null);
	}

	public static double applyDefense(final L2Character attacker, final L2Character defender, final Stats defence_type, final int attack, double value, final L2Skill skill)
	{
		double defenseFull = -defender.calcStat(defence_type, 0, null, null);

		if(skill == null || !skill.isMagic())
		{
			if(defenseFull < attack)
				return value + value * (attack - defenseFull) / 400.;
			return value;
		}

		final double defenseFirst60 = Math.min(60, defenseFull);

		value *= 1 + (attack - defenseFirst60) / 400.;

		if(defenseFull <= defenseFirst60)
			return value;

		defenseFull -= defenseFirst60;

		if(defenseFull > 0 && Rnd.chance(defenseFull / 3.))
		{
			value /= 2.;
			attacker.sendPacket(new SystemMessage(SystemMessage.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_AGAINST_C2S_MAGIC).addName(defender).addName(attacker));
		}

		return value;
	}

	/**
	 * пспользуется только для отображения в окне информации
	 */
	public static int[] calcAttackElement(final L2Character attacker)
	{
		final int fire_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_FIRE, 0, null, null);
		final int water_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WATER, 0, null, null);
		final int wind_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WIND, 0, null, null);
		final int earth_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_EARTH, 0, null, null);
		final int sacred_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_SACRED, 0, null, null);
		final int unholy_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_UNHOLY, 0, null, null);

		if(fire_attack == 0 && water_attack == 0 && earth_attack == 0 && wind_attack == 0 && unholy_attack == 0 && sacred_attack == 0)
			return null;

		final TreeMap<Integer, Stats> sort_attibutes = new TreeMap<Integer, Stats>();
		sort_attibutes.put(fire_attack, Stats.ATTACK_ELEMENT_FIRE);
		sort_attibutes.put(water_attack, Stats.ATTACK_ELEMENT_WATER);
		sort_attibutes.put(wind_attack, Stats.ATTACK_ELEMENT_WIND);
		sort_attibutes.put(earth_attack, Stats.ATTACK_ELEMENT_EARTH);
		sort_attibutes.put(sacred_attack, Stats.ATTACK_ELEMENT_SACRED);
		sort_attibutes.put(unholy_attack, Stats.ATTACK_ELEMENT_UNHOLY);

		int element = 0;
		switch(sort_attibutes.lastEntry().getValue())
		{
			case ATTACK_ELEMENT_FIRE:
				element = 0;
				break;
			case ATTACK_ELEMENT_WATER:
				element = 1;
				break;
			case ATTACK_ELEMENT_WIND:
				element = 2;
				break;
			case ATTACK_ELEMENT_EARTH:
				element = 3;
				break;
			case ATTACK_ELEMENT_SACRED:
				element = 4;
				break;
			case ATTACK_ELEMENT_UNHOLY:
				element = 5;
				break;
		}

		return new int[] { element, sort_attibutes.lastEntry().getKey() };
	}
}