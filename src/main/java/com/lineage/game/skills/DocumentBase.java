package com.lineage.game.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import com.lineage.game.skills.conditions.ConditionPlayerMaxPK;
import com.lineage.game.skills.conditions.ConditionTargetPlayable;
import com.lineage.game.skills.conditions.ConditionTargetRace;
import com.lineage.game.skills.effects.EffectTemplate;
import com.lineage.game.skills.funcs.FuncTemplate;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Character.TargetDirection;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.model.L2Skill;
import com.lineage.game.skills.conditions.Condition;
import com.lineage.game.skills.conditions.ConditionElementSeed;
import com.lineage.game.skills.conditions.ConditionForceBuff;
import com.lineage.game.skills.conditions.ConditionGameTime;
import com.lineage.game.skills.conditions.ConditionGameTime.CheckGameTime;
import com.lineage.game.skills.conditions.ConditionHasSkill;
import com.lineage.game.skills.conditions.ConditionInstanceOf;
import com.lineage.game.skills.conditions.ConditionLogicAnd;
import com.lineage.game.skills.conditions.ConditionLogicNot;
import com.lineage.game.skills.conditions.ConditionLogicOr;
import com.lineage.game.skills.conditions.ConditionPlayerInvSize;
import com.lineage.game.skills.conditions.ConditionPlayerMaxLevel;
import com.lineage.game.skills.conditions.ConditionPlayerMaxPercentCp;
import com.lineage.game.skills.conditions.ConditionPlayerMinHp;
import com.lineage.game.skills.conditions.ConditionPlayerMinLevel;
import com.lineage.game.skills.conditions.ConditionPlayerMinPercentCp;
import com.lineage.game.skills.conditions.ConditionPlayerPercentCp;
import com.lineage.game.skills.conditions.ConditionPlayerPercentHp;
import com.lineage.game.skills.conditions.ConditionPlayerPercentMp;
import com.lineage.game.skills.conditions.ConditionPlayerRace;
import com.lineage.game.skills.conditions.ConditionPlayerRiding;
import com.lineage.game.skills.conditions.ConditionPlayerRiding.CheckPlayerRiding;
import com.lineage.game.skills.conditions.ConditionPlayerState;
import com.lineage.game.skills.conditions.ConditionPlayerState.CheckPlayerState;
import com.lineage.game.skills.conditions.ConditionPlayerWeight;
import com.lineage.game.skills.conditions.ConditionSlotItemId;
import com.lineage.game.skills.conditions.ConditionTargetAggro;
import com.lineage.game.skills.conditions.ConditionTargetCastleDoor;
import com.lineage.game.skills.conditions.ConditionTargetDirection;
import com.lineage.game.skills.conditions.ConditionTargetHasBuff;
import com.lineage.game.skills.conditions.ConditionTargetHasBuffId;
import com.lineage.game.skills.conditions.ConditionTargetMob;
import com.lineage.game.skills.conditions.ConditionTargetNpcId;
import com.lineage.game.skills.conditions.ConditionTargetPlayerRace;
import com.lineage.game.skills.conditions.ConditionUsingArmor;
import com.lineage.game.skills.conditions.ConditionUsingItemType;
import com.lineage.game.skills.conditions.ConditionUsingSkill;
import com.lineage.game.skills.conditions.ConditionZone;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2Armor.ArmorType;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Weapon.WeaponType;
import com.lineage.game.templates.StatsSet;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

abstract class DocumentBase
{
	static Logger _log = Logger.getLogger(DocumentBase.class.getName());

	private File file;
	protected HashMap<String, Number[]> tables;

	DocumentBase(final File file)
	{
		this.file = file;
		tables = new HashMap<String, Number[]>();
	}

	Document parse()
	{
		Document doc;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Error loading file " + file, e);
			return null;
		}
		try
		{
			parseDocument(doc);
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Error in file " + file, e);
			return null;
		}
		return doc;
	}

	protected abstract void parseDocument(Document doc);

	protected abstract Number getTableValue(String name);

	protected abstract Number getTableValue(String name, int idx);

	protected void resetTable()
	{
		tables = new HashMap<String, Number[]>();
	}

	protected void setTable(final String name, final Number[] table)
	{
		tables.put(name, table);
	}

	protected void parseTemplate(Node n, final Object template)
	{
		n = n.getFirstChild();
		if(n == null)
			return;
		for(; n != null; n = n.getNextSibling())
		{
			final String nodeName = n.getNodeName();
			if("add".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Add");
			else if("sub".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Sub");
			else if("mul".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Mul");
			else if("div".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Div");
			else if("set".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Set");
			else if("enchant".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Enchant");
			else if("effect".equalsIgnoreCase(nodeName))
			{
				if(template instanceof EffectTemplate)
					throw new RuntimeException("Nested effects");
				attachEffect(n, template);
			}
			else if("skill".equalsIgnoreCase(nodeName))
			{
				if(!(template instanceof L2Item))
					throw new RuntimeException("Nested skills");
				attachSkill(n, (L2Item) template);
			}
			else if(template instanceof EffectTemplate)
			{
				final Condition cond = parseCondition(n);
				if(cond != null)
					((EffectTemplate) template).attachCond(cond);
			}
		}
	}

	protected void attachSkill(final Node n, final L2Item template)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final int skillId = Short.valueOf(attrs.getNamedItem("id").getNodeValue());
		final int skillLevel = Byte.valueOf(attrs.getNamedItem("level").getNodeValue());
		final String action = attrs.getNamedItem("action").getNodeValue();
		final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if("add".equalsIgnoreCase(action))
			template.attachSkill(skill);
		else
			throw new NoSuchElementException("Unsupported action type for weapon-attached skill");
	}

	protected void attachFunc(final Node n, final Object template, final String name)
	{
		final Stats stat = Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		final String order = n.getAttributes().getNamedItem("order").getNodeValue();
		final int ord = getNumber(order, template).intValue();
		final Condition applyCond = parseCondition(n.getFirstChild());
		final FuncTemplate ft = new FuncTemplate(applyCond, name, stat, ord, getVal(n, template));
		if(template instanceof L2Item)
			((L2Item) template).attachFunction(ft);
		else if(template instanceof L2Skill)
			((L2Skill) template).attach(ft);
		else if(template instanceof EffectTemplate)
			((EffectTemplate) template).attachFunc(ft);
	}

	protected void attachEffect(final Node n, final Object template)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final StatsSet set = new StatsSet();

		set.set("name", attrs.getNamedItem("name").getNodeValue());
		set.set("object", template);

		// TODO set.set("attachCond", attachCond);

		if(attrs.getNamedItem("count") != null)
			set.set("count", getNumber(attrs.getNamedItem("count").getNodeValue(), template).intValue());
		if(attrs.getNamedItem("time") != null)
			set.set("time", getNumber(attrs.getNamedItem("time").getNodeValue(), template).intValue());

		set.set("value", getVal(n, template));

		if(attrs.getNamedItem("abnormal") != null)
		{
			final String abn = attrs.getNamedItem("abnormal").getNodeValue();
			if(abn.equals("poison"))
				set.set("abnormal", L2Character.ABNORMAL_EFFECT_POISON);
			if(abn.equals("bleeding"))
				set.set("abnormal", L2Character.ABNORMAL_EFFECT_BLEEDING);
			if(abn.equals("flame"))
				set.set("abnormal", L2Character.ABNORMAL_EFFECT_FLAME);
			if(abn.equals("bighead"))
				set.set("abnormal", L2Character.ABNORMAL_EFFECT_BIG_HEAD);
			if(abn.equals("shadow"))
				set.set("abnormal", L2Character.ABNORMAL_EFFECT_SILENT_MOVE);
		}

		if(attrs.getNamedItem("stackType") != null)
			set.set("stackType", attrs.getNamedItem("stackType").getNodeValue());
		if(attrs.getNamedItem("stackType2") != null)
			set.set("stackType2", attrs.getNamedItem("stackType2").getNodeValue());
		if(attrs.getNamedItem("stackOrder") != null)
			set.set("stackOrder", getNumber(attrs.getNamedItem("stackOrder").getNodeValue(), template).intValue());

		if(attrs.getNamedItem("applyOnCaster") != null)
			set.set("applyOnCaster", Boolean.valueOf(attrs.getNamedItem("applyOnCaster").getNodeValue()));

		if(attrs.getNamedItem("hidden") != null)
			set.set("hidden", Boolean.valueOf(attrs.getNamedItem("hidden").getNodeValue()));

		if(attrs.getNamedItem("displayId") != null)
			set.set("displayId", getNumber(attrs.getNamedItem("displayId").getNodeValue(), template).intValue());
		if(attrs.getNamedItem("displayLevel") != null)
			set.set("displayLevel", getNumber(attrs.getNamedItem("displayLevel").getNodeValue(), template).intValue());

		final EffectTemplate lt = new EffectTemplate(set);

		parseTemplate(n, lt);

		if(template instanceof L2Skill)
			((L2Skill) template).attach(lt);
	}

	protected Condition parseCondition(Node n)
	{
		while(n != null && n.getNodeType() != Node.ELEMENT_NODE)
			n = n.getNextSibling();
		if(n == null)
			return null;
		if("and".equalsIgnoreCase(n.getNodeName()))
			return parseLogicAnd(n);
		if("or".equalsIgnoreCase(n.getNodeName()))
			return parseLogicOr(n);
		if("not".equalsIgnoreCase(n.getNodeName()))
			return parseLogicNot(n);
		if("player".equalsIgnoreCase(n.getNodeName()))
			return parsePlayerCondition(n);
		if("target".equalsIgnoreCase(n.getNodeName()))
			return parseTargetCondition(n);
		if("has".equalsIgnoreCase(n.getNodeName()))
			return parseHasCondition(n);
		if("using".equalsIgnoreCase(n.getNodeName()))
			return parseUsingCondition(n);
		if("game".equalsIgnoreCase(n.getNodeName()))
			return parseGameCondition(n);
		if("zone".equalsIgnoreCase(n.getNodeName()))
			return parseZoneCondition(n);
		return null;
	}

	protected Condition parseLogicAnd(Node n)
	{
		final ConditionLogicAnd cond = new ConditionLogicAnd();
		for(n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if(n.getNodeType() == Node.ELEMENT_NODE)
				cond.add(parseCondition(n));
		if(cond._conditions == null || cond._conditions.length == 0)
			_log.severe("Empty <and> condition in " + file);
		return cond;
	}

	protected Condition parseLogicOr(Node n)
	{
		final ConditionLogicOr cond = new ConditionLogicOr();
		for(n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if(n.getNodeType() == Node.ELEMENT_NODE)
				cond.add(parseCondition(n));
		if(cond._conditions == null || cond._conditions.length == 0)
			_log.severe("Empty <or> condition in " + file);
		return cond;
	}

	protected Condition parseLogicNot(Node n)
	{
		for(n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if(n.getNodeType() == Node.ELEMENT_NODE)
				return new ConditionLogicNot(parseCondition(n));
		_log.severe("Empty <not> condition in " + file);
		return null;
	}

	protected Condition parsePlayerCondition(final Node n)
	{
		int[] ElementSeeds = new int[5];
		final int[] forces = new int[2];
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			final String nodeName = a.getNodeName();
			if("race".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionPlayerRace(a.getNodeValue()));
			else if("minLevel".equalsIgnoreCase(nodeName))
			{
				final int lvl = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerMinLevel(lvl));
			}
			else if("maxLevel".equalsIgnoreCase(nodeName))
			{
				final int lvl = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerMaxLevel(lvl));
			}
			else if("maxPK".equalsIgnoreCase(nodeName))
			{
				final int pk = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerMaxPK(pk));
			}
			else if("resting".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RESTING, val));
			}
			else if("moving".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.MOVING, val));
			}
			else if("running".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RUNNING, val));
			}
			else if("standing".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.STANDING, val));
			}
			else if("flying".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.FLYING, val));
			}
			else if("combat".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.COMBAT, val));
			}
			else if("combat_pvp".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.COMBAT_PVP, val));
			}
			else if("percentHP".equalsIgnoreCase(nodeName))
			{
				final int percentHP = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerPercentHp(percentHP));
			}
			else if("minHP".equalsIgnoreCase(nodeName))
			{
				final int minHP = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerMinHp(minHP));
			}
			else if("percentMP".equalsIgnoreCase(nodeName))
			{
				final int percentMP = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerPercentMp(percentMP));
			}
			else if("percentCP".equalsIgnoreCase(nodeName))
			{
				final int percentCP = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerPercentCp(percentCP));
			}
			else if("minpercentCP".equalsIgnoreCase(nodeName))
			{
				final int minpercentCP = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerMinPercentCp(minpercentCP));
			}
			else if("maxpercentCP".equalsIgnoreCase(nodeName))
			{
				final int maxpercentCP = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerMaxPercentCp(maxpercentCP));
			}
			else if("riding".equalsIgnoreCase(nodeName))
			{
				final String riding = a.getNodeValue();
				if("strider".equalsIgnoreCase(riding))
					cond = joinAnd(cond, new ConditionPlayerRiding(CheckPlayerRiding.STRIDER));
				else if("wyvern".equalsIgnoreCase(riding))
					cond = joinAnd(cond, new ConditionPlayerRiding(CheckPlayerRiding.WYVERN));
				else if("none".equalsIgnoreCase(riding))
					cond = joinAnd(cond, new ConditionPlayerRiding(CheckPlayerRiding.NONE));
			}
			else if("weight".equalsIgnoreCase(nodeName))
			{
				int weight = getNumber(a.getNodeValue(), null).intValue();
				return new ConditionPlayerWeight(weight);
			}
			else if("invSize".equalsIgnoreCase(nodeName))
			{
				int size = getNumber(a.getNodeValue(), null).intValue();
				return new ConditionPlayerInvSize(size);
			}
			else if("seed_fire".equalsIgnoreCase(a.getNodeName()))
				ElementSeeds[0] = getNumber(a.getNodeValue(), null).intValue();
			else if("seed_water".equalsIgnoreCase(a.getNodeName()))
				ElementSeeds[1] = getNumber(a.getNodeValue(), null).intValue();
			else if("seed_wind".equalsIgnoreCase(a.getNodeName()))
				ElementSeeds[2] = getNumber(a.getNodeValue(), null).intValue();
			else if("seed_various".equalsIgnoreCase(a.getNodeName()))
				ElementSeeds[3] = getNumber(a.getNodeValue(), null).intValue();
			else if("seed_any".equalsIgnoreCase(a.getNodeName()))
				ElementSeeds[4] = getNumber(a.getNodeValue(), null).intValue();
			else if("battle_force".equalsIgnoreCase(a.getNodeName()))
				forces[0] = getNumber(a.getNodeValue(), null).intValue();
			else if("spell_force".equalsIgnoreCase(a.getNodeName()))
				forces[1] = getNumber(a.getNodeValue(), null).intValue();
		}

		// Elemental seed condition processing
		for(int i = 0; i < ElementSeeds.length; i++)
		{
			if(ElementSeeds[i] > 0)
			{
				cond = joinAnd(cond, new ConditionElementSeed(ElementSeeds));
				break;
			}
		}

		if(forces[0] + forces[1] > 0)
			cond = joinAnd(cond, new ConditionForceBuff(forces));

		if(cond == null)
			_log.severe("Unrecognized <player> condition in " + file);
		return cond;
	}

	protected Condition parseTargetCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			final String nodeName = a.getNodeName();
			final String nodeValue = a.getNodeValue();
			if("aggro".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(nodeValue);
				cond = joinAnd(cond, new ConditionTargetAggro(val));
			}
			else if("pvp".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(nodeValue);
				cond = joinAnd(cond, new ConditionTargetPlayable(val));
			}
			else if("mob".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(nodeValue);
				cond = joinAnd(cond, new ConditionTargetMob(val));
			}
			else if("npcId".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetNpcId(nodeValue.split(",")));
			else if("instanceof".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionInstanceOf(nodeValue, true));
			else if("thisinstanceof".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionInstanceOf(nodeValue, false));
			else if("race".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetRace(nodeValue));
			else if("playerRace".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetPlayerRace(nodeValue));
			else if("castledoor".equalsIgnoreCase(nodeName))
			{
				final boolean val = Boolean.valueOf(nodeValue);
				cond = joinAnd(cond, new ConditionTargetCastleDoor(val));
			}
			else if("direction".equalsIgnoreCase(nodeName))
			{
				final TargetDirection Direction = TargetDirection.valueOf(nodeValue.toUpperCase());
				cond = joinAnd(cond, new ConditionTargetDirection(Direction));
			}
			else if("hasBuffId".equalsIgnoreCase(nodeName))
			{
				final StringTokenizer st = new StringTokenizer(nodeValue, ";");
				final int id = Integer.parseInt(st.nextToken().trim());
				int level = -1;
				if(st.hasMoreTokens())
					level = Integer.parseInt(st.nextToken().trim());
				cond = joinAnd(cond, new ConditionTargetHasBuffId(id, level));
			}
			else if("hasBuff".equalsIgnoreCase(nodeName))
			{
				final StringTokenizer st = new StringTokenizer(nodeValue, ";");
				final EffectType et = Enum.valueOf(EffectType.class, st.nextToken().trim());
				int level = -1;
				if(st.hasMoreTokens())
					level = Integer.parseInt(st.nextToken().trim());
				cond = joinAnd(cond, new ConditionTargetHasBuff(et, level));
			}
		}
		if(cond == null)
			_log.severe("Unrecognized <target> condition in " + file);
		return cond;
	}

	protected Condition parseUsingCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			final String nodeName = a.getNodeName();
			final String nodeValue = a.getNodeValue();
			if("kind".equalsIgnoreCase(nodeName))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(nodeValue, ",");
				while(st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					for(final WeaponType wt : WeaponType.values())
						if(wt.toString().equalsIgnoreCase(item))
						{
							mask |= wt.mask();
							break;
						}
				}
				cond = joinAnd(cond, new ConditionUsingItemType(mask));
			}
			else if("armor".equalsIgnoreCase(nodeName))
			{
				final ArmorType armor = ArmorType.valueOf(nodeValue.toUpperCase());
				cond = joinAnd(cond, new ConditionUsingArmor(armor));
			}
			else if("skill".equalsIgnoreCase(nodeName))
			{
				final int id = Integer.parseInt(nodeValue);
				cond = joinAnd(cond, new ConditionUsingSkill(id));
			}
			else if("slotitem".equalsIgnoreCase(nodeName))
			{
				final StringTokenizer st = new StringTokenizer(nodeValue, ";");
				final int id = Integer.parseInt(st.nextToken().trim());
				final short slot = Short.parseShort(st.nextToken().trim());
				int enchant = 0;
				if(st.hasMoreTokens())
					enchant = Integer.parseInt(st.nextToken().trim());
				cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
			}
			else if("direction".equalsIgnoreCase(nodeName))
			{
				final TargetDirection Direction = TargetDirection.valueOf(nodeValue.toUpperCase());
				cond = joinAnd(cond, new ConditionTargetDirection(Direction));
			}
			else if("instanceof".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionInstanceOf(nodeValue, true));
			else if("thisinstanceof".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionInstanceOf(nodeValue, false));
		}
		if(cond == null)
			_log.severe("Unrecognized <using> condition in " + file);
		return cond;
	}

	protected Condition parseHasCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			final String nodeName = a.getNodeName();
			final String nodeValue = a.getNodeValue();
			if("skill".equalsIgnoreCase(nodeName))
			{
				final StringTokenizer st = new StringTokenizer(nodeValue, ";");
				final Integer id = getNumber(st.nextToken().trim(), null).intValue();
				final short level = getNumber(st.nextToken().trim(), null).shortValue();
				cond = joinAnd(cond, new ConditionHasSkill(id, level));
			}
		}
		if(cond == null)
			_log.severe("Unrecognized <has> condition in " + file);
		return cond;
	}

	protected Condition parseGameCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if("night".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(CheckGameTime.NIGHT, val));
			}
		}
		if(cond == null)
			_log.severe("Unrecognized <game> condition in " + file);
		return cond;
	}

	protected Condition parseZoneCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if("type".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionZone(a.getNodeValue()));
		}
		if(cond == null)
			_log.severe("Unrecognized <zone> condition in " + file);
		return cond;
	}

	protected Number[] parseTable(final Node n, final boolean store)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		if(name.charAt(0) != '#')
			throw new IllegalArgumentException("Table name must start with #");
		final StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
		final ArrayList<String> array = new ArrayList<String>();
		while(data.hasMoreTokens())
			array.add(data.nextToken());
		final Number[] res = new Number[array.size()];
		for(int i = 0; i < array.size(); i++)
			res[i] = getNumber(array.get(i), null);
		setTable(name, res);
		return res;
	}

	protected void parseBeanSet(final Node n, final StatsSet set, final Integer level)
	{
		final String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		final String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		final char ch = value.length() == 0 ? ' ' : value.charAt(0);
		if((ch == '#' || ch == '-' || Character.isDigit(ch)) && !value.contains(" ") && !value.contains(";"))
			set.set(name, String.valueOf(getNumber(value, level)));
		else
			set.set(name, value);
	}

	protected double getVal(final Node n, final Object template)
	{
		final Node nval = n.getAttributes().getNamedItem("val");
		if(nval != null)
		{
			final String val = nval.getNodeValue();
			if(val.charAt(0) == '#') // table by level
				return getTableValue(val.trim()).doubleValue();
			return Double.parseDouble(val);
		}
		return 0;
	}

	protected Number getNumber(String value, final Object template)
	{
		if(value.charAt(0) == '#')
			if(template == null || template instanceof L2Skill)
				return getTableValue(value);
			else if(template instanceof Integer)
				return getTableValue(value, (Integer) template);
			else
				throw new IllegalStateException();
		if(value.indexOf('.') == -1)
		{
			int radix = 10;
			if(value.length() > 2 && value.substring(0, 2).equalsIgnoreCase("0x"))
			{
				value = value.substring(2);
				radix = 16;
			}
			return Integer.valueOf(value, radix);
		}
		return Double.valueOf(value);
	}

	protected Condition joinAnd(final Condition cond, final Condition c)
	{
		if(cond == null)
			return c;
		if(cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		final ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}
