package l2d.game.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.cache.Msg;
import l2d.game.geodata.GeoEngine;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.base.ClassId;
import l2d.game.model.entity.siege.Siege;
import l2d.game.model.instances.L2ArtefactInstance;
import l2d.game.model.instances.L2ChestInstance;
import l2d.game.model.instances.L2ControlTowerInstance;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.instances.L2PetInstance;
import l2d.game.model.instances.L2RaidBossInstance;
import l2d.game.model.instances.L2StaticObjectInstance;
import l2d.game.model.quest.Quest;
import l2d.game.serverpackets.FlyToLocation.FlyType;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import l2d.game.skills.Formulas;
import l2d.game.skills.SkillTimeStamp;
import l2d.game.skills.Stats;
import l2d.game.skills.conditions.Condition;
import l2d.game.skills.effects.EffectTemplate;
import l2d.game.skills.funcs.Func;
import l2d.game.skills.funcs.FuncTemplate;
import l2d.game.skills.skillclasses.AIeffects;
import l2d.game.skills.skillclasses.Aggression;
import l2d.game.skills.skillclasses.Balance;
import l2d.game.skills.skillclasses.BeastFeed;
import l2d.game.skills.skillclasses.BuffCharger;
import l2d.game.skills.skillclasses.CPDam;
import l2d.game.skills.skillclasses.Call;
import l2d.game.skills.skillclasses.Cancel;
import l2d.game.skills.skillclasses.Charge;
import l2d.game.skills.skillclasses.ChargeSoul;
import l2d.game.skills.skillclasses.ClanGate;
import l2d.game.skills.skillclasses.CombatPointHeal;
import l2d.game.skills.skillclasses.Continuous;
import l2d.game.skills.skillclasses.Craft;
import l2d.game.skills.skillclasses.DeathPenalty;
import l2d.game.skills.skillclasses.Default;
import l2d.game.skills.skillclasses.DeleteHate;
import l2d.game.skills.skillclasses.DeleteHateOfMe;
import l2d.game.skills.skillclasses.DestroySummon;
import l2d.game.skills.skillclasses.Disablers;
import l2d.game.skills.skillclasses.Drain;
import l2d.game.skills.skillclasses.DrainSoul;
import l2d.game.skills.skillclasses.Effect;
import l2d.game.skills.skillclasses.EffectsFromSkills;
import l2d.game.skills.skillclasses.FatalBlow;
import l2d.game.skills.skillclasses.Fishing;
import l2d.game.skills.skillclasses.Harvesting;
import l2d.game.skills.skillclasses.Heal;
import l2d.game.skills.skillclasses.HealPercent;
import l2d.game.skills.skillclasses.ItemHeal;
import l2d.game.skills.skillclasses.L2SkillSeed;
import l2d.game.skills.skillclasses.LethalShot;
import l2d.game.skills.skillclasses.MDam;
import l2d.game.skills.skillclasses.ManaDam;
import l2d.game.skills.skillclasses.ManaHeal;
import l2d.game.skills.skillclasses.ManaHealPercent;
import l2d.game.skills.skillclasses.ManaPotion;
import l2d.game.skills.skillclasses.NegateEffects;
import l2d.game.skills.skillclasses.NegateStats;
import l2d.game.skills.skillclasses.PDam;
import l2d.game.skills.skillclasses.Recall;
import l2d.game.skills.skillclasses.ReelingPumping;
import l2d.game.skills.skillclasses.RemoveAgroPoints;
import l2d.game.skills.skillclasses.Resurrect;
import l2d.game.skills.skillclasses.Ride;
import l2d.game.skills.skillclasses.SPHeal;
import l2d.game.skills.skillclasses.ShiftAggression;
import l2d.game.skills.skillclasses.SiegeFlag;
import l2d.game.skills.skillclasses.Signet;
import l2d.game.skills.skillclasses.Sowing;
import l2d.game.skills.skillclasses.Spoil;
import l2d.game.skills.skillclasses.StealBuff;
import l2d.game.skills.skillclasses.Summon;
import l2d.game.skills.skillclasses.SummonItem;
import l2d.game.skills.skillclasses.Sweep;
import l2d.game.skills.skillclasses.Switch;
import l2d.game.skills.skillclasses.TakeCastle;
import l2d.game.skills.skillclasses.TeleportNpc;
import l2d.game.skills.skillclasses.Toggle;
import l2d.game.skills.skillclasses.Unlock;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2Weapon.WeaponType;
import l2d.game.templates.StatsSet;
import com.lineage.util.GArray;
import com.lineage.util.Rnd;

public abstract class L2Skill implements Cloneable
{
	public static class AddedSkill
	{
		public int id;
		public int level;

		public AddedSkill(final int id, final int level)
		{
			this.id = id;
			this.level = level;
		}

		public L2Skill getSkill()
		{
			return SkillTable.getInstance().getInfo(id, level);
		}
	}

	public static enum Element
	{
		FIRE,
		WATER,
		WIND,
		EARTH,
		SACRED,
		UNHOLY,
		NONE;

		public static Element getById(final int id)
		{
			switch(id)
			{
				case 0:
					return FIRE;
				case 1:
					return WATER;
				case 2:
					return WIND;
				case 3:
					return EARTH;
				case 4:
					return SACRED;
				case 5:
					return UNHOLY;
				case 6:
				default:
					return NONE;
			}
		}
	}

	public static enum NextAction
	{
		ATTACK, //
		CAST, //
		DEFAULT, //
		MOVE, //
		NONE
		//
	}

	public static enum SkillOpType
	{
		OP_ACTIVE, //
		OP_PASSIVE, //
		OP_TOGGLE, //
		OP_ON_ACTION;//
	}

	public static enum TriggerActionType
	{
		ADD, // скилл срабатывает при добавлении в лист
		ATTACK, // OP_ON_ATTACK
		CRIT, // OP_ON_CRIT
		// SKILL_USE,
		// MAGIC_SKILL_USE,
		// PHYSICAL_SKILL_USE,
		OFFENSIVE_MAGICAL_SKILL_USE, // OP_ON_MAGIC_ATTACK
		SUPPORT_MAGICAL_SKILL_USE, // OP_ON_MAGIC_SUPPORT
		UNDER_ATTACK, // OP_ON_UNDER_ATTACK
		UNDER_MISSED_ATTACK, // OP_ON_EVASION
		UNDER_SKILL_ATTACK, // OP_ON_UNDER_SKILL_ATTACK
		// UNDER_MAGIC_SKILL_ATTACK,
		// UNDER_OFFENSIVE_SKILL_ATTACK,
		// UNDER_MAGIC_SUPPORT,
	}

	public final boolean isDebuff()
	{
		if(!_isDebuff)
			switch(_skillType)
			{
				case MUTE:
				case PARALYZE:
				case ROOT:
				case SLEEP:
				case STUN:
				case CANCEL:
				case DEBUFF:
				case TELEPORT_NPC:
				case POISON:
				case BLEED:
					return true;
				default:
					return false;
			}
		return true;
	}

	public static enum SkillTargetType
	{
		TARGET_ALLY, //
		TARGET_AREA, //
		TARGET_AREA_AIM_CORPSE, //
		TARGET_AURA, //
		TARGET_PET_AURA, //
		TARGET_CHEST, //
		TARGET_CLAN, //
		TARGET_CLAN_PARTY,
		TARGET_CORPSE, //
		TARGET_CORPSE_PLAYER, //
		TARGET_ENEMY_PET, //
		TARGET_ENEMY_SUMMON, //
		TARGET_ENEMY_SERVITOR, //
		TARGET_FLAGPOLE, //
		TARGET_HOLY, //
		TARGET_ITEM, //
		TARGET_MULTIFACE, //
		TARGET_MULTIFACE_AURA, //
		TARGET_NONE, //
		TARGET_ONE, //
		TARGET_OWNER, //
		TARGET_PARTY, //
		TARGET_PARTY_ONE, //
		TARGET_PET, //
		TARGET_SELF, //
		TARGET_SIEGE, //
		TARGET_TYRANNOSAURUS, //
		TARGET_UNLOCKABLE, //
		TARGET_GROUND
	}

	public static enum SkillType
	{
		AGGRESSION(Aggression.class), //
		AIEFFECTS(AIeffects.class), //
		BALANCE(Balance.class), //
		BEAST_FEED(BeastFeed.class), //
		BLEED(Continuous.class), //
		BUFF(Continuous.class), //
		BUFF_CHARGER(BuffCharger.class), //
		CALL(Call.class), //
		CANCEL(Cancel.class), //
		CHARGE(Charge.class), //
		CLAN_GATE(ClanGate.class), //
		CHARGE_SOUL(ChargeSoul.class), //
		COMBATPOINTHEAL(CombatPointHeal.class), //
		CONT(Toggle.class), //
		CPDAM(CPDam.class), //
		CPHOT(Continuous.class), //
		CRAFT(Craft.class), //
		MUSIC(Continuous.class), //
		DEATH_PENALTY(DeathPenalty.class), //
		DEBUFF(Continuous.class), //
		DELETE_HATE(DeleteHate.class), //
		DELETE_HATE_OF_ME(DeleteHateOfMe.class), //
		DESTROY_SUMMON(DestroySummon.class), //
		DISCORD(Continuous.class), //
		DOT(Continuous.class), //
		DRAIN(Drain.class), //
		DRAIN_SOUL(DrainSoul.class), //
		EFFECT(Effect.class), //
		EFFECTS_FROM_SKILLS(EffectsFromSkills.class), //
		ENCHANT_ARMOR, //
		ENCHANT_WEAPON, //
		FATALBLOW(FatalBlow.class), //
		FEED_PET, //
		FISHING(Fishing.class), //
		FORCE_BUFF(Continuous.class), //
		HARDCODED(Effect.class), //
		HARVESTING(Harvesting.class), //
		HEAL(Heal.class), //
		HEAL_PERCENT(HealPercent.class), //
		ITEM_HEAL(ItemHeal.class), //
		HOT(Continuous.class), //
		LETHAL_SHOT(LethalShot.class), //
		LUCK, //
		MANADAM(ManaDam.class), //
		MANAHEAL(ManaHeal.class), //
		MANAHEAL_PERCENT(ManaHealPercent.class), //
		MANAPOTION(ManaPotion.class), //
		MDAM(MDam.class), //
		MDOT(Continuous.class), //
		MPHOT(Continuous.class), //
		MUTE(Disablers.class), //
		NEGATE_EFFECTS(NegateEffects.class), //
		NEGATE_STATS(NegateStats.class), //
		NOTDONE, //
		PARALYZE(Disablers.class), //
		PASSIVE, //
		PDAM(PDam.class), //
		POISON(Continuous.class), //
		PUMPING(ReelingPumping.class), //
		RECALL(Recall.class), //
		REELING(ReelingPumping.class), //
		RESURRECT(Resurrect.class), //
		RIDE(Ride.class), //
		ROOT(Disablers.class), //
		REMOVE_AGRO_POINTS(RemoveAgroPoints.class), //
		SHIFT_AGGRESSION(ShiftAggression.class), //
		SIEGEFLAG(SiegeFlag.class), //
		SLEEP(Disablers.class), //
		SWITCH(Switch.class), //
		SOULSHOT, //
		SOWING(Sowing.class), //
		SPHEAL(SPHeal.class), //
		SPIRITSHOT, //
		SPOIL(Spoil.class), //
		STEAL_BUFF(StealBuff.class), //
		STUN(Disablers.class), //
		SUMMON(Summon.class), //
		SUMMON_ITEM(SummonItem.class), //
		SWEEP(Sweep.class), //
		TAKECASTLE(TakeCastle.class), //
		TELEPORT_NPC(TeleportNpc.class), //
		UNLOCK(Unlock.class), //
		WATCHER_GAZE(Continuous.class), //
		SIGNET(Signet.class), //
		SEED(L2SkillSeed.class); //

		private final Class<? extends L2Skill> clazz;

		private SkillType()
		{
			clazz = Default.class;
		}

		private SkillType(final Class<? extends L2Skill> clazz)
		{
			this.clazz = clazz;
		}

		public L2Skill makeSkill(final StatsSet set)
		{
			try
			{
				final Constructor<? extends L2Skill> c = clazz.getConstructor(StatsSet.class);
				return c.newInstance(set);
			}
			catch(final Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	protected static Logger _log = Logger.getLogger(L2Skill.class.getName());

	private static AddedSkill[] _emptyAddedSkills = new AddedSkill[0];
	private static final Func[] _emptyFunctionSet = new Func[0];

	protected static final HashMap<Integer, ArrayList<Integer>> _reuseGroups = new HashMap<Integer, ArrayList<Integer>>();

	protected EffectTemplate[] _effectTemplates;
	protected FuncTemplate[] _funcTemplates;

	protected ArrayList<Integer> _teachers; // which NPC teaches
	protected final ArrayList<ClassId> _canLearn; // which classes can learn

	protected AddedSkill[] _addedSkills;
	private final boolean _allowinOlimpiad;

	protected final int[] _itemConsume;
	protected final int[] _itemConsumeId;

	public static final int SKILL_CUBIC_MASTERY = 143;
	public static final int SKILL_CRAFTING = 172;
	public static final int SKILL_POLEARM_MASTERY = 216;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_WEAPON_MAGIC_MASTERY1 = 249;
	public static final int SKILL_WEAPON_MAGIC_MASTERY2 = 250;
	public static final int SKILL_BLINDING_BLOW = 321;
	public static final int SKILL_STRIDER_ASSAULT = 325;
	public static final int SKILL_BLUFF = 358;
	public static final int SKILL_TRANSFER_PAIN = 1262;
	public static final int SKILL_FISHING_MASTERY = 1315;
	public static final int SKILL_MYSTIC_IMMUNITY = 1411;
	public static final int SKILL_HINDER_STRIDER = 4258;
	public static final int SKILL_WYVERN_BREATH = 4289;
	public static final int SKILL_RAID_CURSE = 4515;

	public static final int SKILL_HEAVY_ARMOR_MASTERY1 = 231;
	public static final int SKILL_HEAVY_ARMOR_MASTERY2 = 232;
	public static final int SKILL_HEAVY_ARMOR_MASTERY3 = 253;
	public static final int SKILL_HEAVY_ARMOR_MASTERY4 = 259;
	public static final int SKILL_LIGHT_ARMOR_MASTERY1 = 227;
	public static final int SKILL_LIGHT_ARMOR_MASTERY2 = 233;
	public static final int SKILL_LIGHT_ARMOR_MASTERY3 = 236;
	public static final int SKILL_LIGHT_ARMOR_MASTERY4 = 252;
	public static final int SKILL_LIGHT_ARMOR_MASTERY5 = 258;

	public final static int SAVEVS_CON = 4;
	public final static int SAVEVS_DEX = 5;
	public final static int SAVEVS_INT = 1;
	public final static int SAVEVS_MEN = 3;
	public final static int SAVEVS_STR = 6;
	public final static int SAVEVS_WIT = 2;

	protected boolean _isAltUse;
	protected boolean _isBehind;
	protected boolean _isCancelable;
	protected boolean _isCorpse;
	protected boolean _isCritical;
	protected boolean _isCommon;
	protected boolean _isItemHandler;
	protected boolean _isOffensive;
	protected boolean _isPvm;
	protected boolean _isMagic;
	protected boolean _isSaveable;
	protected boolean _isSkillTimePermanent;
	protected boolean _isReuseDelayPermanent;
	protected boolean _isSuicideAttack;
	protected boolean _isValidateable;
	protected boolean _isShieldignore;
	protected boolean _isUndeadOnly;
	protected boolean _isUseSS;
	protected boolean _isOverhit;
	protected boolean _isChargeBoost;
	protected boolean _isUsingWhileCasting;
	protected boolean _cancelTarget;
	protected boolean _skillInterrupt;
	private final int _effectNpcId;

	protected final int _triggerEffectId;
	protected final int _triggerEffectLevel;

	protected SkillType _skillType;
	protected SkillOpType _operateType;
	protected TreeMap<TriggerActionType, Double> _triggerActions;
	private static final TreeMap<TriggerActionType, Double> EMPTY_ACTIONS = new TreeMap<TriggerActionType, Double>();
	protected SkillTargetType _targetType;
	protected NextAction _nextAction;
	protected Element _element;
	protected FlyType _flyType;
	protected Condition _preCondition;

	protected Integer _id;
	protected Short _level;
	protected Short _baseLevel;
	protected Integer _displayId;
	protected Short _displayLevel;

	protected int _activateRate;
	protected int _castRange;
	protected int _condCharges;
	protected int _coolTime;
	protected int _effectPoint;
	protected int _elementPower;
	protected int _flyRadius;
	protected int _forceId;
	protected int _hitTime;
	protected int _hpConsume;
	protected int _levelModifier;
	protected int _magicLevel;
	protected int _matak;
	protected int _minPledgeClass;
	protected int _minRank;
	protected int _negatePower;
	protected int _negateSkill;
	protected int _npcId;
	protected int _numCharges;
	protected int _reuseGroupId;
	protected int _savevs;
	protected int _skillInterruptTime;
	protected int _skillRadius;
	protected int _soulsConsume;
	protected int _weaponsAllowed;

	protected long _reuseDelay;

	protected double _power;
	protected double _mpConsume1;
	protected double _mpConsume2;

	protected double _lethal1;
	protected double _lethal2;

	protected boolean _deathlink;

	private final boolean _isDebuff;

	protected String _name;

	//for balance panel
	protected int _activateRateOriginal;
	protected double _powerOriginal;
	protected long _reuseDelayOriginal;
	protected int _hitTimeOriginal;
	protected boolean _isReuseDelayPermanentOriginal;
	protected double _lethal1Original;
	protected double _lethal2Original;
	protected int _criticalRateOriginal;

	// Жрет много памяти, включить только если будет необходимость
	// protected StatsSet _set;

	/**
	 * Внимание!!! У наследников вручную надо поменять тип на public
	 * 
	 * @param set
	 *            парамерты скилла
	 */
	protected L2Skill(final StatsSet set)
	{
		// _set = set;
		_id = set.getInteger("skill_id");
		_level = set.getShort("level");
		_displayId = set.getInteger("displayId", _id);
		_displayLevel = set.getShort("displayLevel", _level);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_isMagic = set.getBool("isMagic", false);
		_isAltUse = set.getBool("altUse", false);
		_allowinOlimpiad = set.getBool("allowinOlimpiad", false);
		_mpConsume1 = set.getInteger("mpConsume1", 0);
		_mpConsume2 = set.getInteger("mpConsume2", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_soulsConsume = set.getInteger("soulsConsume", 0);
		_isChargeBoost = set.getBool("chargeBoost", false);
		_isUsingWhileCasting = set.getBool("isUsingWhileCasting", false);
		_matak = set.getInteger("mAtk", 0);
		_isUseSS = set.getBool("useSS", true);
		_forceId = set.getInteger("forceId", 0);
		_effectNpcId = set.getInteger("effectNpcId", 0);
		_magicLevel = set.getInteger("magicLevel", 0);
		_triggerEffectId = set.getInteger("triggerEffectId", 0);
		_triggerEffectLevel = set.getInteger("triggerEffectLevel", set.getInteger("level"));
		_lethal1 = set.getDouble("lethal1", 0);
		_lethal2 = set.getDouble("lethal2", 0);
		_deathlink = set.getBool("deathlink", false);

		if(_operateType == SkillOpType.OP_ON_ACTION)
		{
			final StringTokenizer st = new StringTokenizer(set.getString("triggerActions", ""), ";");
			if(st.hasMoreTokens())
			{
				_triggerActions = new TreeMap<TriggerActionType, Double>();
				while(st.hasMoreTokens())
				{
					final TriggerActionType tat = Enum.valueOf(TriggerActionType.class, st.nextToken());
					final Double chance = Double.valueOf(st.nextToken());
					_triggerActions.put(tat, chance);
				}
			}
		}

		if(_triggerActions == null)
			_triggerActions = EMPTY_ACTIONS;

		final String s1 = set.getString("itemConsumeCount", "");
		final String s2 = set.getString("itemConsumeId", "");

		if(s1.length() == 0)
			_itemConsume = new int[] { 0 };
		else
		{
			final String[] s = s1.split(" ");
			_itemConsume = new int[s.length];
			for(int i = 0; i < s.length; i++)
				_itemConsume[i] = Integer.parseInt(s[i]);
		}

		if(s2.length() == 0)
			_itemConsumeId = new int[] { 0 };
		else
		{
			final String[] s = s2.split(" ");
			_itemConsumeId = new int[s.length];
			for(int i = 0; i < s.length; i++)
				_itemConsumeId[i] = Integer.parseInt(s[i]);
		}

		_isItemHandler = set.getBool("isHandler", false);
		_reuseGroupId = set.getInteger("reuseGroup", 0);
		if(_reuseGroupId > 0)
		{
			if(_reuseGroups.get(_reuseGroupId) == null)
				_reuseGroups.put(_reuseGroupId, new ArrayList<Integer>());
			if(!_reuseGroups.get(_reuseGroupId).contains(_id))
				_reuseGroups.get(_reuseGroupId).add(_id);
		}

		_isCommon = set.getBool("isCommon", false);
		_isSaveable = set.getBool("isSaveable", true);
		_isValidateable = set.getBool("isValidateable", !_name.contains("Item Skill"));
		_coolTime = set.getInteger("coolTime", 0);
		_skillInterruptTime = set.getInteger("skillInterruptTime", 0);
		_reuseDelay = set.getLong("reuseDelay", 0);
		_skillRadius = set.getInteger("skillRadius", 80);
		_targetType = set.getEnum("target", SkillTargetType.class);
		_isUndeadOnly = set.getBool("undeadOnly", false);
		_isCorpse = set.getBool("corpse", false);
		_power = set.getDouble("power", 0.);
		_effectPoint = set.getInteger("effectPoint", 0);
		_nextAction = NextAction.valueOf(set.getString("nextAction", "DEFAULT").toUpperCase());
		_skillType = set.getEnum("skillType", SkillType.class);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		_isSkillTimePermanent = set.getBool("isSkillTimePermanent", false);
		_isReuseDelayPermanent = set.getBool("isReuseDelayPermanent", false);

		_isDebuff = set.getBool("isDebuff", false);

		if(Quest.isdigit(set.getString("element", "NONE")))
			_element = Element.getById(set.getInteger("element", 6));
		else
			_element = Element.valueOf(set.getString("element", "NONE").toUpperCase());

		_elementPower = set.getInteger("elementPower", 0);

		if(Quest.isdigit(set.getString("save", "0")))
			_savevs = set.getInteger("save", 0);
		else
			try
			{
				_savevs = L2Skill.class.getField("SAVEVS_" + set.getString("save").toUpperCase()).getInt(null);
			}
			catch(final Exception e)
			{
				_log.warning("Invalid savevs value: " + set.getString("save"));
				e.printStackTrace();
			}

		_activateRate = set.getInteger("activateRate", -1);
		_levelModifier = set.getInteger("levelModifier", 1);
		_isCancelable = set.getBool("cancelable", true);
		_isShieldignore = set.getBool("shieldignore", false);
		_isCritical = set.getBool("critical", false);
		_isOverhit = set.getBool("overHit", false);
		_weaponsAllowed = set.getInteger("weaponsAllowed", 0);
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_minRank = set.getInteger("minRank", 0);
		_isOffensive = set.getBool("isOffensive", false);
		_isPvm = set.getBool("isPvm", false);
		_isBehind = set.getBool("behind", false);
		_npcId = set.getInteger("npcId", 0);
		_flyType = FlyType.valueOf(set.getString("flyType", "NONE").toUpperCase());
		_flyRadius = set.getInteger("flyRadius", 200);
		_negateSkill = set.getInteger("negateSkill", 0);
		_negatePower = set.getInteger("negatePower", Integer.MAX_VALUE);
		_numCharges = set.getInteger("num_charges", 0);
		_condCharges = set.getInteger("cond_charges", 0);
		_cancelTarget = set.getBool("cancelTarget", false);
		_skillInterrupt = set.getBool("skillInterrupt", false);

		StringTokenizer st = new StringTokenizer(set.getString("addSkills", ""), ";");
		if(st.hasMoreTokens())
		{
			_addedSkills = new AddedSkill[st.countTokens() / 2];
			int i = 0;
			while(st.hasMoreTokens())
			{
				final int id = Integer.valueOf(st.nextToken());
				int level = Integer.valueOf(st.nextToken());
				if(level == -1)
					level = _level;
				_addedSkills[i] = new AddedSkill(id, level);
				i++;
			}
		}

		if(_nextAction == NextAction.DEFAULT || _nextAction == NextAction.NONE)
			switch(_skillType)
			{
				case PDAM:
				case CPDAM:
				case FATALBLOW:
				case LETHAL_SHOT:
				case SPOIL:
				case SOWING:
				case STUN:
				case DRAIN_SOUL:
					_nextAction = NextAction.ATTACK;
					break;
				default:
					_nextAction = NextAction.NONE;
			}
		if(_savevs == 0)
			switch(_skillType)
			{
				case BLEED:
				case DOT:
				case MDOT:
				case FATALBLOW:
				case LETHAL_SHOT:
				case PDAM:
				case CPDAM:
				case POISON:
				case STUN:
					_savevs = SAVEVS_CON;
					break;
				case CANCEL:
				case MANADAM:
				case DEBUFF:
				case MDAM:
				case MUTE:
				case PARALYZE:
				case ROOT:
				case SLEEP:
					_savevs = SAVEVS_MEN;
					break;
			}

		final String canLearn = set.getString("canLearn", null);
		if(canLearn == null)
			_canLearn = null;
		else
		{
			_canLearn = new ArrayList<ClassId>();
			st = new StringTokenizer(canLearn, " \r\n\t,;");
			while(st.hasMoreTokens())
			{
				final String cls = st.nextToken();
				try
				{
					_canLearn.add(ClassId.valueOf(cls));
				}
				catch(final Throwable t)
				{
					_log.log(Level.SEVERE, "Bad class " + cls + " to learn skill", t);
				}
			}
		}

		final String teachers = set.getString("teachers", null);
		if(teachers == null)
			_teachers = null;
		else
		{
			_teachers = new ArrayList<Integer>();
			st = new StringTokenizer(teachers, " \r\n\t,;");
			while(st.hasMoreTokens())
			{
				final String npcid = st.nextToken();
				try
				{
					_teachers.add(Integer.parseInt(npcid));
				}
				catch(final Throwable t)
				{
					_log.log(Level.SEVERE, "Bad teacher id " + npcid + " to teach skill", t);
				}
			}
		}
	}

	public final boolean getWeaponDependancy(final L2Character activeChar, final boolean chance)
	{
		final int weaponsAllowed = getWeaponsAllowed();
		WeaponType playerWeapon;
		int mask;

		// check to see if skill has a weapon dependency.
		if(weaponsAllowed == 0)
			return true;

		if(activeChar.getActiveWeaponItem() != null)
		{
			playerWeapon = activeChar.getActiveWeaponItem().getItemType();
			mask = playerWeapon.mask();
			if((mask & weaponsAllowed) != 0)
				return true;
		}
		// can be on the secondary weapon
		if(activeChar.getSecondaryWeaponItem() != null)
		{
			playerWeapon = activeChar.getSecondaryWeaponItem().getItemType();
			mask = playerWeapon.mask();
			if((mask & weaponsAllowed) != 0)
				return true;
		}

		final StringBuffer skillmsg = new StringBuffer();
		skillmsg.append(_name);
		skillmsg.append(" can only be used with weapons of type ");
		for(final WeaponType wt : WeaponType.values())
			if((wt.mask() & weaponsAllowed) != 0)
				skillmsg.append(wt).append('/');
		skillmsg.setCharAt(skillmsg.length() - 1, '.');
		activeChar.sendMessage(skillmsg.toString());

		return false;
	}

	public boolean checkCondition(final L2Character activeChar, final L2Character target, final boolean forceUse, final boolean dontMove, final boolean first)
	{
		final L2Player player = activeChar.getPlayer();

		if(activeChar.isDead())
			return false;

		if(target != null && activeChar.getReflection() != target.getReflection())
		{
			activeChar.sendPacket(Msg.CANNOT_SEE_TARGET);
			return false;
		}

		if(_skillType == SkillType.FORCE_BUFF && target == activeChar)
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}

		if(!getWeaponDependancy(activeChar, false))
			return false;

		if(first && activeChar.isSkillDisabled(_id))
		{
			if(activeChar.isPlayer())
			{
				final SkillTimeStamp sts = ((L2Player) activeChar).getSkillReuseTimeStamps().get(_id);
				if(sts == null)
					return false;
				final long timeleft = sts.getReuseCurrent();
				if(!Config.ALT_SHOW_REUSE_MSG && timeleft < 10000)
					return false;
				final long hours = timeleft / 3600000;
				final long minutes = (timeleft - hours * 3600000) / 60000;
				final long seconds = (long) Math.ceil((timeleft - hours * 3600000 - minutes * 60000) / 1000.);	
				if(((L2Player) activeChar).getLastReuseMsgSkill() == _id && ((L2Player) activeChar).getLastReuseMsg() + 1000 >= System.currentTimeMillis())
					return false;
				if(hours > 0)
					activeChar.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S2_HOURS_S3_MINUTES_AND_S4_SECONDS_REMAINING_IN_S1S_REUSE_TIME).addSkillName(_id, getDisplayLevel()).addNumber(hours).addNumber(minutes).addNumber(seconds));
				else if(minutes > 0)
					activeChar.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S2_MINUTES_S3_SECONDS_REMAINING_IN_S1S_REUSE_TIME).addSkillName(_id, getDisplayLevel()).addNumber(minutes).addNumber(seconds));
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S2_SECONDS_REMAINING_IN_S1S_REUSE_TIME).addSkillName(_id, getDisplayLevel()).addNumber(seconds));
				((L2Player) activeChar).updateLastReuseMsg(_id);
			}
			return false;
		}

		if(first && activeChar.getCurrentMp() < (isMagic() ? activeChar.calcStat(Stats.MP_MAGIC_SKILL_CONSUME, _mpConsume1 + _mpConsume2, null, this) : activeChar.calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, _mpConsume1 + _mpConsume2, null, this)))
		{
			activeChar.sendPacket(Msg.NOT_ENOUGH_MP);
			return false;
		}

		if(activeChar.getCurrentHp() < _hpConsume + 1)
		{
			activeChar.sendPacket(Msg.NOT_ENOUGH_HP);
			return false;
		}

		if(!(_isItemHandler || _isAltUse) && (isMagic() && activeChar.isMuted() || !isMagic() && activeChar.isPMuted()))
			return false;

		if(_soulsConsume > activeChar.getConsumedSouls())
		{
			activeChar.sendPacket(Msg.THERE_IS_NOT_ENOUGHT_SOUL);
			return false;
		}

		// TODO перенести потребление из формул сюда
		if(activeChar.getIncreasedForce() < _condCharges || activeChar.getIncreasedForce() < _numCharges)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY_));
			return false;
		}

		if(player != null)
		{
			if((_isItemHandler || _isAltUse) && player.isInOlympiadMode())
			{
				player.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
				return false;
			}

			if(player.isInBoat())
				return false;

			if(player.inObserverMode())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.OBSERVERS_CANNOT_PARTICIPATE));
				return false;
			}

			// If summon siege golem, hog cannon, Swoop Cannon, check its ok to place the flag
			if(_id == 13 || _id == 299 || _id == 448)
			{
				SystemMessage sm = null;
				final Siege siege = SiegeManager.getSiege(player, true);
				if(siege == null || !siege.isInProgress())
					sm = new SystemMessage(SystemMessage.YOU_ARE_NOT_IN_SIEGE);
				else if(player.getClanId() != 0 && siege.getAttackerClan(player.getClanId()) == null)
					sm = new SystemMessage(SystemMessage.OBSERVATION_IS_ONLY_POSSIBLE_DURING_A_SIEGE);
				if(sm != null)
				{
					player.sendPacket(sm);
					return false;
				}
			}

			if(first && _itemConsume[0] > 0)
				for(int i = 0; i < _itemConsume.length; i++)
				{
					Inventory inv = ((L2Playable) activeChar).getInventory();
					if(inv == null)
						inv = player.getInventory();
					final L2ItemInstance requiredItems = inv.getItemByItemId(_itemConsumeId[i]);
					if(requiredItems == null || requiredItems.getCount() < _itemConsume[i])
					{
						if(activeChar == player)
							player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
						return false;
					}
				}

			if(player.isFishing() && _id != 1312 && _id != 1313 && _id != 1314 && !altUse())
			{
				if(activeChar == player)
					player.sendPacket(Msg.ONLY_FISHING_SKILLS_ARE_AVAILABLE);
				return false;
			}
		}

		if(getFlyType() != FlyType.NONE && activeChar.isImobilised())
		{
			activeChar.getPlayer().sendPacket(Msg.YOUR_TARGET_IS_OUT_OF_RANGE);
			return false;
		}

		// Fly скиллы нельзя использовать слишком близко
		if(first && target != null && target != activeChar && getFlyType() != FlyType.NONE && (oneTarget() || _targetType == SkillTargetType.TARGET_AREA || _targetType == SkillTargetType.TARGET_MULTIFACE) && activeChar.isInRange(target.getLoc(), Math.min(150, getFlyRadius())))
		{
			activeChar.getPlayer().sendPacket(Msg.THERE_IS_NOT_ENOUGH_SPACE_TO_MOVE_THE_SKILL_CANNOT_BE_USED);
			return false;
		}

		final SystemMessage msg = checkTarget(activeChar, target, target, forceUse, first);
		if(msg != null && activeChar.getPlayer() != null)
		{
			activeChar.getPlayer().sendPacket(msg);
			return false;
		}

		if(_preCondition == null)
			return true;

		final Env env = new Env();
		env.character = activeChar;
		env.skill = this;
		env.target = target;

		if(!_preCondition.test(env))
		{
			String cond_msg = _preCondition.getMessage();
			int msgId = _preCondition.getMessageId();
			if(cond_msg != null)
				activeChar.sendMessage(cond_msg);
			if(msgId != 0)
			{
				SystemMessage sm = new SystemMessage(msgId);
				if(_preCondition.isAddName())
					sm.addSkillName(_id);
				activeChar.sendPacket(sm);
			}
			return false;
		}

		return true;
	}

	public SystemMessage checkTarget(final L2Character activeChar, final L2Character target, final L2Character aimingTarget, final boolean forceUse, final boolean first)
	{
		if(target == activeChar && isNotTargetAoE() || target == activeChar.getPet() && _targetType == SkillTargetType.TARGET_PET_AURA)
			return null;
		if(target == null || isOffensive() && target == activeChar && (target.isSummon() || target.isPet()) && _targetType != SkillTargetType.TARGET_GROUND)
			return Msg.THAT_IS_THE_INCORRECT_TARGET;
		if(activeChar.getReflection() != target.getReflection())
			return Msg.CANNOT_SEE_TARGET;
		// Попадает ли цель в радиус действия в конце каста
		if(!first && target != activeChar && target == aimingTarget && getCastRange() > 0 && getCastRange() != 32767 && !activeChar.isInRange(target.getLoc(), getCastRange() + (getCastRange() < 200 ? 400 : 500)))
			return Msg.YOUR_TARGET_IS_OUT_OF_RANGE;
		// Конусообразные скиллы
		if(!first && target != activeChar && (_targetType == SkillTargetType.TARGET_MULTIFACE || _targetType == SkillTargetType.TARGET_MULTIFACE_AURA) && (_isBehind ? activeChar.isInFront(target, 120) : !activeChar.isInFront(target, 60)))
			return Msg.YOUR_TARGET_IS_OUT_OF_RANGE;
		// Проверка на каст по трупу
		if(target.isDead() != _isCorpse && _targetType != SkillTargetType.TARGET_AREA_AIM_CORPSE || _isUndeadOnly && !target.isUndead())
			return Msg.INVALID_TARGET;
		if(target.isMonster() && ((L2MonsterInstance) target).isDying())
			return Msg.INVALID_TARGET;
		if(_targetType != SkillTargetType.TARGET_UNLOCKABLE && target instanceof L2DoorInstance && !((L2DoorInstance) target).isAttackable(activeChar))
			return Msg.INVALID_TARGET;
		// Для различных бутылок, и для скилла кормления, дальнейшие проверки не нужны
		if(_isAltUse || _skillType == SkillType.BEAST_FEED || _targetType == SkillTargetType.TARGET_UNLOCKABLE || _targetType == SkillTargetType.TARGET_CHEST)
			return null;
		if(activeChar instanceof L2Playable)
		{
			final L2Player player = activeChar.getPlayer();
			if(player == null)
				return Msg.THAT_IS_THE_INCORRECT_TARGET;
			if(target instanceof L2Playable)
			{
				if(isPvM())
					return Msg.THAT_IS_THE_INCORRECT_TARGET;
				final L2Player pcTarget = target.getPlayer();
				if(pcTarget == null)
					return Msg.THAT_IS_THE_INCORRECT_TARGET;
				if(pcTarget.isInOlympiadMode() && player.getOlympiadGameId() != pcTarget.getOlympiadGameId())
					return Msg.THAT_IS_THE_INCORRECT_TARGET;
				if(player.getTeam() > 0 && player.isChecksForTeam() && pcTarget.getTeam() == 0)
					return Msg.THAT_IS_THE_INCORRECT_TARGET;
				if(pcTarget.getTeam() > 0 && pcTarget.isChecksForTeam() && player.getTeam() == 0)
					return Msg.THAT_IS_THE_INCORRECT_TARGET;
				if(isOffensive())
				{
					if(player.isInOlympiadMode() && !player.isOlympiadCompStart())
						return Msg.THAT_IS_THE_INCORRECT_TARGET;
					if(player.getTeam() > 0 && player.isChecksForTeam() && pcTarget.getTeam() > 0 && pcTarget.isChecksForTeam() && player.getTeam() == pcTarget.getTeam())
						return Msg.THAT_IS_THE_INCORRECT_TARGET;
					if(isAoE() && getCastRange() < Integer.MAX_VALUE && !GeoEngine.canSeeTarget(activeChar, target, false))
						return Msg.CANNOT_SEE_TARGET;
					if(activeChar.isInZoneBattle() != target.isInZoneBattle() || activeChar.isInZonePeace() || target.isInZonePeace())
						return Msg.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE;
					if(_targetType == SkillTargetType.TARGET_GROUND)
						return null;
					if(activeChar.isInZoneBattle())
					{
						if(!forceUse && player.getParty() != null && player.getParty() == pcTarget.getParty())
							return Msg.INVALID_TARGET;
					}
					else
					{
						if(player != pcTarget && player.getDuel() != null && pcTarget.getDuel() != null && pcTarget.getDuel() == player.getDuel())
							return null;

						if(isPvpSkill() || !forceUse || isAoE())
						{
							if(player == pcTarget)
								return Msg.INVALID_TARGET;
							if(player.getParty() != null && player.getParty() == pcTarget.getParty())
								return Msg.INVALID_TARGET;
							if(player.getClanId() != 0 && player.getClanId() == pcTarget.getClanId())
								return Msg.INVALID_TARGET;
							// if(player.getAllyId() != 0 && player.getAllyId() == pcTarget.getAllyId())
							// return Msg.INVALID_TARGET;
							if(player.getDuel() != null && pcTarget.getDuel() != player.getDuel())
								return Msg.INVALID_TARGET;
						}

						if(player.atMutualWarWith(pcTarget))
							return null;

						if(activeChar.isInZone(ZoneType.Siege) && target.isInZone(ZoneType.Siege))
						{
							final L2Clan clan1 = player.getClan();
							final L2Clan clan2 = pcTarget.getClan();
							if(clan1 == null || clan2 == null)
								return null;
							if(!clan1.isDefender() && !clan1.isAttacker())
								return null;
							if(!clan2.isDefender() && !clan2.isAttacker())
								return null;
							if(clan1.getSiege() != clan2.getSiege())
								return null;
							if(clan1 != clan2 && !(clan1.isDefender() && clan2.isDefender()))
								return null;
							return Msg.INVALID_TARGET;
						}

						if(pcTarget.getPvpFlag() != 0)
							return null;
						if(pcTarget.getKarma() > 0)
							return null;
						if(forceUse && !isPvpSkill() && (!isAoE() || aimingTarget == target))
							return null;

						return Msg.INVALID_TARGET;
					}
				}
				else
				{
					if(pcTarget == player)
						return null;

					if(player.isInOlympiadMode() && !player.isOlympiadGameStart() && !player.isOlympiadCompStart())
						return Msg.INVALID_TARGET;
					if(player.getTeam() > 0 && player.isChecksForTeam() && pcTarget.getTeam() > 0 && pcTarget.isChecksForTeam() && player.getTeam() != pcTarget.getTeam())
						return Msg.THAT_IS_THE_INCORRECT_TARGET;
					if(!activeChar.isInZoneBattle() && target.isInZoneBattle())
						return Msg.INVALID_TARGET;
					if(activeChar.isInZonePeace() && !target.isInZonePeace())
						return Msg.INVALID_TARGET;

					if(forceUse)
						return null;

					if(player.getDuel() != null && pcTarget.getDuel() != player.getDuel())
						return Msg.INVALID_TARGET;
					if(player != pcTarget && player.getDuel() != null && pcTarget.getDuel() != null && pcTarget.getDuel() == pcTarget.getDuel())
						return Msg.INVALID_TARGET;

					if(player.getParty() != null && player.getParty() == pcTarget.getParty())
						return null;
					if(player.getClanId() != 0 && player.getClanId() == pcTarget.getClanId())
						return null;
					// if(player.getAllyId() != 0 && player.getAllyId() == pcTarget.getAllyId())
					// return null;

					if(player.atMutualWarWith(pcTarget))
						return Msg.INVALID_TARGET;
					if(pcTarget.getPvpFlag() != 0)
						return Msg.INVALID_TARGET;
					if(pcTarget.getKarma() > 0)
						return Msg.INVALID_TARGET;
				}
				return null;
			}
			if(!forceUse && isOffensive() && !target.isAutoAttackable(activeChar))
				return Msg.INVALID_TARGET;
			if(!target.isAttackable() && _skillType != SkillType.TAKECASTLE)
				return Msg.INVALID_TARGET;
			return null;
		}
		if(!GeoEngine.canSeeTarget(activeChar, target, false))
			return Msg.CANNOT_SEE_TARGET;
		if(!forceUse && !isOffensive() && target.isAutoAttackable(activeChar))
			return Msg.INVALID_TARGET;
		if(!forceUse && isOffensive() && !target.isAutoAttackable(activeChar))
			return Msg.INVALID_TARGET;
		if(!target.isAttackable())
			return Msg.INVALID_TARGET;
		return null;
	}

	public final L2Character getAimingTarget(final L2Character activeChar, final L2Object obj)
	{
		L2Character target = obj == null || !obj.isCharacter() ? null : (L2Character) obj;
		switch(_targetType)
		{
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_CLAN_PARTY:
			case TARGET_PARTY:
			case TARGET_SELF:
				return activeChar;
			case TARGET_AURA:
			case TARGET_MULTIFACE_AURA:
				return activeChar;
			case TARGET_HOLY:
				return activeChar.isPlayer() && target instanceof L2ArtefactInstance ? target : null;
			case TARGET_FLAGPOLE:
				return activeChar.isPlayer() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 3 ? target : null;
			case TARGET_UNLOCKABLE:
				return target instanceof L2DoorInstance || target instanceof L2ChestInstance ? target : null;
			case TARGET_CHEST:
				return target instanceof L2ChestInstance ? target : null;
			case TARGET_TYRANNOSAURUS:
				return target != null && target.isMonster() && (target.getNpcId() == 22217 || target.getNpcId() == 22216 || target.getNpcId() == 22215) ? target : null;
			case TARGET_PET:
			case TARGET_PET_AURA:
				target = activeChar.getPet();
				return target != null && target.isDead() == _isCorpse ? target : null;
			case TARGET_OWNER:
				if(activeChar.isPet())
					target = ((L2PetInstance) activeChar).getPlayer();
				else
					return null;
				return target != null && target.isDead() == _isCorpse ? target : null;
			case TARGET_ENEMY_PET:
				if(target == null || target == activeChar.getPet() || !target.isPet())
					return null;
				return target;
			case TARGET_ENEMY_SUMMON:
				if(target == null || target == activeChar.getPet() || !target.isSummon())
					return null;
				return target;
			case TARGET_ENEMY_SERVITOR:
				if(target == null || target == activeChar.getPet() || !(target instanceof L2Summon))
					return null;
				return target;
			case TARGET_ONE:
				return target != null && target.isDead() == _isCorpse && !(target == activeChar && isOffensive()) && (!_isUndeadOnly || target.isUndead()) ? target : null;
			case TARGET_PARTY_ONE:
				if(target == null)
					return null;
				// self or self pet.
				if(target.getPlayer() != null && target.getPlayer().equals(activeChar))
					return target;
				// party member or party member pet.
				if(target.getPlayer() != null && activeChar.getPlayer() != null && activeChar.getPlayer().getParty() != null && activeChar.getPlayer().getParty().containsMember(target.getPlayer()) && target.isDead() == _isCorpse && !(target == activeChar && isOffensive()) && (!_isUndeadOnly || target.isUndead()))
					return target;
				return null;
			case TARGET_AREA:
			case TARGET_MULTIFACE:
				return target != null && target.isDead() == _isCorpse && !(target == activeChar && isOffensive()) && (!_isUndeadOnly || target.isUndead()) ? target : null;
			case TARGET_AREA_AIM_CORPSE:
				return target != null && target.isDead() ? target : null;
			case TARGET_CORPSE:
				return activeChar.getTeam() > 0 ? activeChar : (target != null  && target.isNpc() && target.isDead() ? target : null);
			case TARGET_CORPSE_PLAYER:
				return target instanceof L2Playable && target.isDead() ? target : null;
			case TARGET_SIEGE:
				return target != null && !target.isDead() && (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance) ? target : null;
			case TARGET_GROUND:
				return activeChar;
			default:
				activeChar.sendMessage("Target type of skill is not currently handled");
				return null;
		}
	}

	public FastList<L2Character> getTargets(final L2Character activeChar, final L2Character aimingTarget, final boolean forceUse)
	{
		final FastList<L2Character> targets = new FastList<L2Character>();
		if(oneTarget())
		{
			targets.add(aimingTarget);
			return targets;
		}

		switch(_targetType)
		{
			case TARGET_AREA:
			case TARGET_MULTIFACE:
			{
				if(aimingTarget.isDead() == _isCorpse && (!_isUndeadOnly || aimingTarget.isUndead()))
					targets.add(aimingTarget);
				addTargetsToList(targets, aimingTarget, activeChar, forceUse);
				break;
			}
			case TARGET_AREA_AIM_CORPSE:
			case TARGET_AURA:
			case TARGET_MULTIFACE_AURA:
			{
				addTargetsToList(targets, activeChar, activeChar, forceUse);
				break;
			}
			case TARGET_PET_AURA:
			{
				if(activeChar.getPet() == null)
					break;
				addTargetsToList(targets, activeChar.getPet(), activeChar, forceUse);
				break;
			}
			case TARGET_PARTY:
			{
				final L2Player player = activeChar.getPlayer();
				if(player == null)
				{
					if(activeChar.isPet() || activeChar.isSummon())
						break;
					_log.log(Level.SEVERE, "L2Skill.getTargets :: TARGET_PARTY | player = null | activeChar = " + activeChar + "[" + activeChar.getNpcId() + "] | SkillID: " + getId());
					Thread.dumpStack();
					break;
				}
				final L2Party party = player.getParty();
				if(party == null)
				{
					if(!_isCorpse)
						targets.add(player);
					final L2Summon pet = player.getPet();
					if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
						targets.add(pet);
					break;
				}
				for(final L2Player pm : party.getPartyMembers())
				{
					if(player.isInRange(pm, _skillRadius) && pm.isDead() == _isCorpse)
						targets.add(pm);
					final L2Summon pet = pm.getPet();
					if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
						targets.add(pet);
				}
				break;
			}
			case TARGET_CLAN:
			{
				final L2Player player = activeChar.getPlayer();
				if(player == null)
				{
					if(activeChar.isPet() || activeChar.isSummon())
						break;
					_log.log(Level.SEVERE, "L2Skill.getTargets :: TARGET_CLAN | player = null | activeChar = " + activeChar + "[" + activeChar.getNpcId() + "] | SkillID: " + getId());
					Thread.dumpStack();
					break;
				}
				if(player.getClanId() != 0)
					for(final L2Player target : L2World.getAroundPlayers(player, _skillRadius, 200))
						if(target.getClanId() == player.getClanId() || player.getParty() != null && target.getParty() == player.getParty())
						{
							if(target.isDead() == _isCorpse)
								targets.add(target);
							final L2Summon pet = target.getPet();
							if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
								targets.add(pet);
						}

				if(!_isCorpse)
					targets.add(player);
				final L2Summon pet = player.getPet();
				if(pet != null && pet.isDead() == _isCorpse)
					targets.add(pet);
				break;
			}
			case TARGET_CLAN_PARTY:
			{
				final L2Player player = activeChar.getPlayer();
				if(player == null)
				{
					if(activeChar.isPet() || activeChar.isSummon())
						break;
					_log.log(Level.SEVERE, "L2Skill.getTargets :: TARGET_CLAN_PARTY | player = null | activeChar = " + activeChar + "[" + activeChar.getNpcId() + "] | SkillID: " + getId());
					Thread.dumpStack();
					break;
				}
				if(player.getClanId() != 0)
					for(final L2Player target : L2World.getAroundPlayers(player, _skillRadius, 200))
						if(target.getClanId() == player.getClanId() || player.getParty() != null && target.getParty() == player.getParty())
						{
							if(target.isDead() == _isCorpse)
								targets.add(target);
							final L2Summon pet = target.getPet();
							if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
								targets.add(pet);
						}

				final L2Party party = player.getParty();
				if(party == null)
				{
					if(!_isCorpse)
						targets.add(player);
					final L2Summon pet = player.getPet();
					if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
						targets.add(pet);
					break;
				}
				for(final L2Player pm : party.getPartyMembers())
				{
					if(player.isInRange(pm, _skillRadius) && pm.isDead() == _isCorpse)
						targets.add(pm);
					final L2Summon pet = pm.getPet();
					if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
						targets.add(pet);
				}
				break;
			}
			case TARGET_ALLY:
			{
				L2Player[] target_list;
				final L2Player player = activeChar.getPlayer();
				if(player == null)
				{
					if(activeChar.isPet() || activeChar.isSummon())
						break;
					_log.log(Level.SEVERE, "L2Skill.getTargets :: TARGET_ALLY | player = null | activeChar = " + activeChar + "[" + activeChar.getNpcId() + "] | SkillID: " + getId());
					Thread.dumpStack();
					break;
				}
				if(player.getClanId() != 0)
				{
					if(player.getAllyId() == 0)
						target_list = player.getClan().getOnlineMembers(0);
					else
						target_list = player.getAlliance().getOnlineMembers(null);
					for(final L2Player pl : target_list)
					{
						if(player.isInRange(pl, _skillRadius) && pl.isDead() == _isCorpse)
							targets.add(pl);
						final L2Summon pet = pl.getPet();
						if(pet != null && player.isInRange(pet, _skillRadius) && pet.isDead() == _isCorpse)
							targets.add(pet);
					}
				}
				else
				{
					if(!_isCorpse)
						targets.add(player);
					final L2Summon pet = player.getPet();
					if(pet != null && pet.isDead() == _isCorpse)
						targets.add(pet);
				}
				break;
			}
			case TARGET_GROUND:
			{
				for(final L2Character trg : activeChar.getAroundCharacters(_skillRadius, 200))
				{
					if(trg == null)
						continue;

					if(isOffensive() && (trg == aimingTarget || trg.isInZonePeace() || trg.isSummon() && trg.getPlayer() == aimingTarget))
						continue;

					if(isOffensive() && activeChar.getPlayer() != null && trg.getPlayer() != null && !forceUse && activeChar.getPlayer().getParty() != null && trg.getPlayer().getParty() != null && activeChar.getPlayer().getParty().containsMember(trg))
						continue;

					if(checkTarget(activeChar, trg, aimingTarget, forceUse, false) != null)
						continue;

					if((trg.isMonster() || trg.isPlayable()) && !trg.isDead())
						targets.add(trg);
				}
				break;
			}
		}
		return targets;
	}

	private void addTargetsToList(final FastList<L2Character> targets, final L2Character aimingTarget, final L2Character activeChar, final boolean forceUse)
	{
		int count = 0;
		final GArray<L2Character> temp = aimingTarget.getAroundCharacters(_skillRadius, 200);

		for(final L2Character target : temp)
		{
			if(target == null || activeChar == target || activeChar.getPlayer() != null && activeChar.getPlayer() == target.getPlayer())
				continue;
			if(checkTarget(activeChar, target, aimingTarget, forceUse, false) != null)
				continue;
			if(activeChar instanceof L2NpcInstance && target instanceof L2NpcInstance)
				continue;
			if(target instanceof L2NpcInstance)
				if(count < 20)
					count++;
				else
					continue;
			targets.add(target);
		}
	}

	public final void getEffects(final L2Character effector, final L2Character effected, final boolean calcChance, final boolean applyOnCaster)
	{
		if(isPassive() || _effectTemplates == null || _effectTemplates.length == 0 || effector == null || effected == null)
			return;

		// Mystic Immunity Makes a target temporarily immune to buffs/debuffs
		if(effected.isEffectImmune())
		{
			effector.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(effected.getName()).addSkillName(_displayId, _displayLevel));
			return;
		}

		// No effect on invulnerable characters unless they cast it themselves.
		if(effector != effected && effected.isInvul())
		{
			if(effector.isPlayer())
				effector.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(effected.getName()).addSkillName(_displayId, _displayLevel));
			return;
		}

		// No effect on doors/walls
		if(effected instanceof L2DoorInstance)
			return;

		ThreadPoolManager.getInstance().executeEffect(new Runnable(){
			public void run()
			{
				int mastery = effector.getSkillMastery(getId());
				if(mastery == 2 && !applyOnCaster)
					effector.removeSkillMastery(getId());

				boolean success = false;

				for(final EffectTemplate et : _effectTemplates)
				{
					if(applyOnCaster != et._applyOnCaster)
						continue;

					L2Character target = et._applyOnCaster ? effector : effected;

					if(et._counter == 0)
						continue;

					if(et._stackOrder == -1)
					{
						boolean already = false;
						if(et._stackType != EffectTemplate.NO_STACK)
						{
							for(final L2Effect e : target.getEffectList().getAllEffects())
								if(e.getStackType().equalsIgnoreCase(et._stackType))
								{
									already = true;
									break;
								}
							if(already)
								continue;
						}
						else if(target.getEffectList().getEffectsBySkillId(getId()) != null)
							continue;
					}

					if(target instanceof L2RaidBossInstance && et.getEffectType().isRaidImmune())
					{
						effector.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(effected.getName()).addSkillName(_displayId, _displayLevel));
						continue;
					}

					final Env env = new Env(effector, target, L2Skill.this);

					if(calcChance && !et._applyOnCaster)
					{
						env.value = _activateRate;
						if(!Formulas.calcSkillSuccess(env, et.getEffectType().getResistType(), et.getEffectType().getAttibuteType()))
						{
							effector.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(effected.getName()).addSkillName(_displayId, _displayLevel));
							continue;
						}
					}

					if(target != effector && isOffensive())
						if(Rnd.chance(target.calcStat(isMagic() ? Stats.REFLECT_MAGIC_DEBUFF : Stats.REFLECT_PHYSIC_DEBUFF, 0, null, L2Skill.this)))
						{
							target.sendPacket(new SystemMessage(SystemMessage.YOU_COUNTERED_C1S_ATTACK).addName(effector));
							effector.sendPacket(new SystemMessage(SystemMessage.C1_DODGES_THE_ATTACK).addName(target));
							target = effector;
							env.target = target;
						}

					if(isBlockedByChar(target, et))
						continue;

					success = true;
					final L2Effect e = et.getEffect(env);
					if(e != null)
						if(e._count == 1 && e.getPeriod() == 0)
						{
							// Эффекты однократного действия не шедулятся а применяются немедленно
							// Как правило это побочные эффекты для скиллов моментального действия
							e.onStart();
							e.onActionTime();
							e.onExit();
						}
						else
							target.getEffectList().addEffect(e);
				}

				if(calcChance)
					if(success)
						effector.sendPacket(new SystemMessage(SystemMessage.S1_HAS_SUCCEEDED).addSkillName(_displayId, _displayLevel));
					else
						effector.sendPacket(new SystemMessage(SystemMessage.S1_HAS_FAILED).addSkillName(_displayId, _displayLevel));
			}
		});
	}
	
	public final void attach(final EffectTemplate effect)
	{
		if(_effectTemplates == null)
			_effectTemplates = new EffectTemplate[] { effect };
		else
		{
			final int len = _effectTemplates.length;
			final EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}

	public final void attach(final FuncTemplate f)
	{
		if(_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			final int len = _funcTemplates.length;
			final FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public final Func[] getStatFuncs()
	{
		if(_funcTemplates == null)
			return _emptyFunctionSet;
		final ArrayList<Func> funcs = new ArrayList<Func>();
		for(final FuncTemplate t : _funcTemplates)
		{
			final Func f = t.getFunc(this); // skill is owner
			if(f != null)
				funcs.add(f);
		}
		if(funcs.size() == 0)
			return _emptyFunctionSet;
		return funcs.toArray(new Func[funcs.size()]);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		final L2Skill other = (L2Skill) obj;
		if(_displayId == null)
		{
			if(other._displayId != null)
				return false;
		}
		else if(!_displayId.equals(other._displayId))
			return false;
		if(_displayLevel == null)
		{
			if(other._displayLevel != null)
				return false;
		}
		else if(!_displayLevel.equals(other._displayLevel))
			return false;
		if(_id == null)
		{
			if(other._id != null)
				return false;
		}
		else if(!_id.equals(other._id))
			return false;
		if(_level == null)
		{
			if(other._level != null)
				return false;
		}
		else if(!_level.equals(other._level))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (_displayId == null ? 0 : _displayId.hashCode());
		result = prime * result + (_displayLevel == null ? 0 : _displayLevel.hashCode());
		result = prime * result + (_id == null ? 0 : _id.hashCode());
		result = prime * result + (_level == null ? 0 : _level.hashCode());
		return result;
	}

	public final void attach(final Condition c)
	{
		_preCondition = c;
	}

	public final boolean altUse()
	{
		return _isAltUse;
	}

	public final boolean canTeachBy(final int npcId)
	{
		return _teachers == null || _teachers.contains(npcId);
	}

	public final int getActivateRate()
	{
		return _activateRate;
	}

	public AddedSkill[] getAddedSkills()
	{
		if(_addedSkills == null)
			return _emptyAddedSkills;
		return _addedSkills;
	}

	public final boolean getCanLearn(final ClassId cls)
	{
		return _canLearn == null || _canLearn.contains(cls);
	}

	/**
	 * @return Returns the castRange.
	 */
	public final int getCastRange()
	{
		return _castRange;
	}

	public int getCondCharges()
	{
		return _condCharges;
	}

	public final int getCoolTime()
	{
		return _coolTime;
	}

	public boolean getCorpse()
	{
		return _isCorpse;
	}

	public final int getDisplayId()
	{
		return _displayId;
	}

	public short getDisplayLevel()
	{
		return _displayLevel;
	}

	public int getEffectPoint()
	{
		return _effectPoint;
	}

	public EffectTemplate[] getEffectTemplates()
	{
		return _effectTemplates;
	}

	public final Element getElement()
	{
		return _element;
	}

	public final int getElementPower()
	{
		return _elementPower;
	}

	public L2Skill getFirstAddedSkill()
	{
		if(_addedSkills == null)
			return null;
		return _addedSkills[0].getSkill();
	}

	public int getFlyRadius()
	{
		return _flyRadius;
	}

	public FlyType getFlyType()
	{
		return _flyType;
	}

	public int getForceId()
	{
		return _forceId;
	}

	public final int getHitTime()
	{
		return _hitTime;
	}

	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return _hpConsume;
	}

	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}

	public void setId(final int id)
	{
		_id = id;
	}

	/**
	 * @return Returns the itemConsume.
	 */
	public final int[] getItemConsume()
	{
		return _itemConsume;
	}

	/**
	 * @return Returns the itemConsumeId.
	 */
	public final int[] getItemConsumeId()
	{
		return _itemConsumeId;
	}

	/**
	 * @return Returns the level.
	 */
	public final short getLevel()
	{
		return _level;
	}

	public final short getBaseLevel()
	{
		return _baseLevel;
	}

	public final void setBaseLevel(final short baseLevel)
	{
		_baseLevel = baseLevel;
	}

	public final int getLevelModifier()
	{
		return _levelModifier;
	}

	public final int getMagicLevel()
	{
		return _magicLevel;
	}

	public int getMatak()
	{
		return _matak;
	}

	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}

	public int getMinRank()
	{
		return _minRank;
	}

	/**
	 * @return Returns the mpConsume as _mpConsume1 + _mpConsume2.
	 */
	public final double getMpConsume()
	{
		return _mpConsume1 + _mpConsume2;
	}

	/**
	 * @return Returns the mpConsume1.
	 */
	public final double getMpConsume1()
	{
		return _mpConsume1;
	}

	/**
	 * @return Returns the mpConsume2.
	 */
	public final double getMpConsume2()
	{
		return _mpConsume2;
	}

	/**
	 * @return Returns the name.
	 */
	public final String getName()
	{
		return _name;
	}

	public int getNegatePower()
	{
		return _negatePower;
	}

	public int getNegateSkill()
	{
		return _negateSkill;
	}

	public NextAction getNextAction()
	{
		return _nextAction;
	}

	public int getNpcId()
	{
		return _npcId;
	}

	public int getNumCharges()
	{
		return _numCharges;
	}

	/**
	 * Return the power of the skill.<BR><BR>
	 */
	public final double getPower()
	{
		return _power;
	}

	/**
	 * @return Returns the reuseDelay.
	 */
	public final long getReuseDelay()
	{
		return _reuseDelay;
	}

	public final ArrayList<Integer> getReuseGroup()
	{
		return _reuseGroups.get(_reuseGroupId);
	}

	public final int getReuseGroupId()
	{
		return _reuseGroupId;
	}

	public final int getSavevs()
	{
		return _savevs;
	}

	public final boolean getShieldIgnore()
	{
		return _isShieldignore;
	}

	public final int getSkillInterruptTime()
	{
		return _skillInterruptTime;
	}

	public final int getSkillRadius()
	{
		return _skillRadius;
	}

	public final SkillType getSkillType()
	{
		return _skillType;
	}

	public final SkillTargetType getTargetType()
	{
		return _targetType;
	}

	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}

	public boolean isBehind()
	{
		return _isBehind;
	}

	public boolean isBlockedByChar(final L2Character effected, final EffectTemplate et)
	{
		if(et._funcTemplates == null)
			return false;
		for(final FuncTemplate func : et._funcTemplates)
			if(func != null && effected.checkBlockedStat(func._stat))
				return true;
		return false;
	}

	public final boolean isCancelable()
	{
		return _isCancelable;
	}

	/**
	 * Является ли скилл общим
	 */
	public final boolean isCommon()
	{
		return _isCommon;
	}

	public final boolean isCritical()
	{
		return _isCritical;
	}

	public final boolean isHandler()
	{
		return _isItemHandler;
	}

	/**
	 * @return Returns true if skill is magic.
	 */
	public final boolean isMagic()
	{
		return _isMagic;
	}

	public void setOperateType(final SkillOpType type)
	{
		_operateType = type;
	}

	public double getChanceForAction(final TriggerActionType action)
	{
		return _triggerActions.get(action);
	}

	public TreeMap<TriggerActionType, Double> getTriggerActions()
	{
		return _triggerActions;
	}

	public final boolean isOnAction()
	{
		return _operateType == SkillOpType.OP_ON_ACTION;
	}

	public final boolean isOverhit()
	{
		return _isOverhit;
	}

	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}

	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}

	public final boolean isLikePassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE || _operateType == SkillOpType.OP_ON_ACTION;
	}

	public boolean isSaveable()
	{
		if(!Config.ALT_SAVE_UNSAVEABLE && (isSongDance() || _name.startsWith("Herb of")))
			return false;
		return _isSaveable;
	}

	/**
	 * На некоторые скиллы и хендлеры предметов скорости каста/атаки не влияет
	 */
	public final boolean isSkillTimePermanent()
	{
		return _isSkillTimePermanent || _isItemHandler;
	}

	public final boolean isReuseDelayPermanent()
	{
		return _isReuseDelayPermanent || _isItemHandler;
	}

	public boolean isChargeBoost()
	{
		return _isChargeBoost;
	}

	public boolean isUsingWhileCasting()
	{
		return _isUsingWhileCasting;
	}

	/**
	 * Может ли скилл тратить шоты, для хендлеров всегда false
	 */
	public boolean isSSPossible()
	{
		return _isUseSS && !_isItemHandler && !isSongDance();
	}

	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}

	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}

	/**
	 * Скилл подлежит удалению из ярлыков при валидейте, в случае отсутсвия данного скила у чара
	 * 
	 * @return true, если скилл подлежит удалению
	 */
	public boolean isValidateable()
	{
		return _isValidateable;
	}

	public void setCastRange(final int castRange)
	{
		_castRange = castRange;
	}

	public void setDisplayLevel(final Short lvl)
	{
		_displayLevel = lvl;
	}

	public void setHitTime(final int hitTime)
	{
		_hitTime = hitTime;
	}

	public void setHpConsume(final int hpConsume)
	{
		_hpConsume = hpConsume;
	}

	public void setIsMagic(final boolean isMagic)
	{
		_isMagic = isMagic;
	}

	public final void setMagicLevel(final int newlevel)
	{
		_magicLevel = newlevel;
	}

	public void setMpConsume1(final double mpConsume1)
	{
		_mpConsume1 = mpConsume1;
	}

	public void setMpConsume2(final double mpConsume2)
	{
		_mpConsume2 = mpConsume2;
	}

	public void setName(final String name)
	{
		_name = name;
	}

	public void setOverhit(final boolean isOverhit)
	{
		_isOverhit = isOverhit;
	}

	public final void setPower(final double power)
	{
		_power = power;
	}

	public void setSkillInterruptTime(final int skillInterruptTime)
	{
		_skillInterruptTime = skillInterruptTime;
	}

	public boolean isItemSkill()
	{
		return _name.contains("Item Skill");
	}

	@Override
	public String toString()
	{
		return _name + "[id=" + _id + ",lvl=" + _level + "]";
	}

	public abstract void useSkill(L2Character activeChar, FastList<L2Character> targets);

	/**
	 * Такие скиллы не аггрят цель, и не флагают чара, но являются "плохими"
	 */
	public boolean isAI()
	{
		switch(_skillType)
		{
			case AGGRESSION:
			case AIEFFECTS:
			case SOWING:
			case DELETE_HATE:
			case DELETE_HATE_OF_ME:
				return true;
			default:
				return false;
		}
	}

	public boolean isAoE()
	{
		switch(_targetType)
		{
			case TARGET_AREA:
			case TARGET_AREA_AIM_CORPSE:
			case TARGET_AURA:
			case TARGET_PET_AURA:
			case TARGET_MULTIFACE:
			case TARGET_MULTIFACE_AURA:
				return true;
			default:
				return false;
		}
	}

	public boolean isNotTargetAoE()
	{
		switch(_targetType)
		{
			case TARGET_AURA:
			case TARGET_MULTIFACE_AURA:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_CLAN_PARTY:
			case TARGET_PARTY:
				return true;
			default:
				return false;
		}
	}

	public boolean isOffensive()
	{
		if(_isOffensive)
			return _isOffensive;

		switch(_skillType)
		{
			case AGGRESSION:
			case AIEFFECTS:
			case BLEED:
			case CANCEL:
			case DEBUFF:
			case DOT:
			case DRAIN:
			case DRAIN_SOUL:
			case FATALBLOW:
			case LETHAL_SHOT:
			case MANADAM:
			case MDAM:
			case MDOT:
			case MUTE:
			case PARALYZE:
			case PDAM:
			case CPDAM:
			case POISON:
			case ROOT:
			case SLEEP:
			case SOULSHOT:
			case SPIRITSHOT:
			case SPOIL:
			case STUN:
			case SWEEP:
			case HARVESTING:
			case TELEPORT_NPC:
			case SOWING:
			case DELETE_HATE:
			case DELETE_HATE_OF_ME:
			case DESTROY_SUMMON:
			case STEAL_BUFF:
			case DISCORD:
			case SWITCH:
			case REMOVE_AGRO_POINTS:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Работают только против npc
	 */
	public boolean isPvM()
	{
		if(_isPvm)
			return _isPvm;

		switch(_skillType)
		{
			case DISCORD:
				return true;
			default:
				return false;
		}
	}

	public final boolean isPvpSkill()
	{
		switch(_skillType)
		{
			case BLEED:
			case CANCEL:
			case AGGRESSION:
			case DEBUFF:
			case DOT:
			case MDOT:
			case MUTE:
			case PARALYZE:
			case POISON:
			case ROOT:
			case SLEEP:
			case MANADAM:
			case DESTROY_SUMMON:
			case NEGATE_STATS:
			case STEAL_BUFF:
			case DELETE_HATE:
			case DELETE_HATE_OF_ME:
				return true;
			default:
				return false;
		}
	}

	public boolean isSongDance()
	{
		return _skillType == SkillType.MUSIC;
	}

	public boolean oneTarget()
	{
		switch(_targetType)
		{
			case TARGET_CORPSE:
			case TARGET_CORPSE_PLAYER:
			case TARGET_HOLY:
			case TARGET_FLAGPOLE:
			case TARGET_ITEM:
			case TARGET_NONE:
			case TARGET_ONE:
			case TARGET_PARTY_ONE:
			case TARGET_PET:
			case TARGET_OWNER:
			case TARGET_ENEMY_PET:
			case TARGET_ENEMY_SUMMON:
			case TARGET_ENEMY_SERVITOR:
			case TARGET_SELF:
			case TARGET_UNLOCKABLE:
			case TARGET_CHEST:
			case TARGET_SIEGE:
				return true;
			default:
				return false;
		}
	}

	public boolean isCancelTarget()
	{
		return _cancelTarget;
	}

	public boolean isSkillInterrupt()
	{
		return _skillInterrupt;
	}

	public int getTriggerEffectId()
	{
		return _triggerEffectId;
	}

	public int getTriggerEffectLevel()
	{
		return _triggerEffectLevel;
	}

	public int getEffectNpcId()
	{
		return _effectNpcId;
	}

	public final boolean allowinOlimpiad()
	{
		return _allowinOlimpiad;
	}

	public double getLethal1()
	{
		return _lethal1;
	}

	public double getLethal2()
	{
		return _lethal2;
	}

	public boolean isDeathlink()
	{
		return _deathlink;
	}

	public final int getActivateRateOriginal()
	{
		if(_activateRateOriginal == 0)
			_activateRateOriginal = _activateRate;
		return _activateRateOriginal;
	}

	public final void setActivateRate(int rate)
	{
		if(_activateRateOriginal == 0)
			_activateRateOriginal = _activateRate;
		_activateRate = rate;
	}

	public void setLethal1(double val)
	{
		if(_lethal1Original == 0)
			_lethal1Original = _lethal1;
		_lethal1 = val;
	}

	public void setLethal2(double val)
	{
		if(_lethal2Original == 0)
			_lethal2Original = _lethal2;
		_lethal2 = val;
	}

	public void setReuseDelay(long newReuseDelay)
	{
		if(_reuseDelayOriginal == 0)
			_reuseDelayOriginal = _reuseDelay;
		_reuseDelay = newReuseDelay;		
	}

	public void setReuseDelayPermanent(boolean val, boolean init)
	{
		if(init)
			_isReuseDelayPermanentOriginal = _isReuseDelayPermanent;
		_isReuseDelayPermanent = val;		
	}

	public final boolean isReuseDelayPermanentOriginal()
	{
		return _isReuseDelayPermanentOriginal;
	}

	public final int getHitTimeOriginal()
	{
		if(_hitTimeOriginal == 0)
			_hitTimeOriginal = _hitTime;
		return _hitTimeOriginal;
	}
	
	public final double getPowerOriginal()
	{
		if(_powerOriginal == 0)
			_powerOriginal = _power;
		return _powerOriginal;
	}
	
	public final long getReuseDelayOriginal()
	{
		if(_reuseDelayOriginal == 0)
			_reuseDelayOriginal = _reuseDelay;
		return _reuseDelayOriginal;
	}

	public double getLethal1Original()
	{
		if(_lethal1Original == 0)
			_lethal1Original = _lethal1;
		return _lethal1Original;
	}
	
	public double getLethal2Original()
	{
		if(_lethal2Original == 0)
			_lethal2Original = _lethal2;
		return _lethal2Original;
	}

}