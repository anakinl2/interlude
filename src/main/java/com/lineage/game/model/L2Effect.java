package com.lineage.game.model;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.serverpackets.AbnormalStatusUpdate;
import com.lineage.game.serverpackets.ExOlympiadSpelledInfo;
import com.lineage.game.serverpackets.PartySpelled;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Stats;
import com.lineage.game.skills.effects.EffectAddSkills;
import com.lineage.game.skills.effects.EffectAntiSummon;
import com.lineage.game.skills.effects.EffectBetray;
import com.lineage.game.skills.effects.EffectBigHead;
import com.lineage.game.skills.effects.EffectBlessNoblesse;
import com.lineage.game.skills.effects.EffectBlockStat;
import com.lineage.game.skills.effects.EffectBluff;
import com.lineage.game.skills.effects.EffectBuff;
import com.lineage.game.skills.effects.EffectBuffImmunity;
import com.lineage.game.skills.effects.EffectCPDamPercent;
import com.lineage.game.skills.effects.EffectCancelEffect;
import com.lineage.game.skills.effects.EffectClanGate;
import com.lineage.game.skills.effects.EffectCombatPointHealOverTime;
import com.lineage.game.skills.effects.EffectConsumeSoulsOverTime;
import com.lineage.game.skills.effects.EffectCurseOfLifeFlow;
import com.lineage.game.skills.effects.EffectDamOverTime;
import com.lineage.game.skills.effects.EffectDamOverTimeLethal;
import com.lineage.game.skills.effects.EffectDestroySummon;
import com.lineage.game.skills.effects.EffectDiscord;
import com.lineage.game.skills.effects.EffectEnervation;
import com.lineage.game.skills.effects.EffectFakeDeath;
import com.lineage.game.skills.effects.EffectFear;
import com.lineage.game.skills.effects.EffectForce;
import com.lineage.game.skills.effects.EffectGrow;
import com.lineage.game.skills.effects.EffectHeal;
import com.lineage.game.skills.effects.EffectHealBlock;
import com.lineage.game.skills.effects.EffectHealCPPercent;
import com.lineage.game.skills.effects.EffectHealOverTime;
import com.lineage.game.skills.effects.EffectHealPercent;
import com.lineage.game.skills.effects.EffectImobileBuff;
import com.lineage.game.skills.effects.EffectInterrupt;
import com.lineage.game.skills.effects.EffectInvisible;
import com.lineage.game.skills.effects.EffectInvulnerable;
import com.lineage.game.skills.effects.EffectLDManaDamOverTime;
import com.lineage.game.skills.effects.EffectManaDamOverTime;
import com.lineage.game.skills.effects.EffectManaHeal;
import com.lineage.game.skills.effects.EffectManaHealOverTime;
import com.lineage.game.skills.effects.EffectManaHealPercent;
import com.lineage.game.skills.effects.EffectMeditation;
import com.lineage.game.skills.effects.EffectMute;
import com.lineage.game.skills.effects.EffectMuteAll;
import com.lineage.game.skills.effects.EffectMuteAttack;
import com.lineage.game.skills.effects.EffectMutePhisycal;
import com.lineage.game.skills.effects.EffectParalyze;
import com.lineage.game.skills.effects.EffectPetrification;
import com.lineage.game.skills.effects.EffectRecoverForce;
import com.lineage.game.skills.effects.EffectRelax;
import com.lineage.game.skills.effects.EffectRoot;
import com.lineage.game.skills.effects.EffectSalvation;
import com.lineage.game.skills.effects.EffectSeed;
import com.lineage.game.skills.effects.EffectSignet;
import com.lineage.game.skills.effects.EffectSilentMove;
import com.lineage.game.skills.effects.EffectSleep;
import com.lineage.game.skills.effects.EffectStun;
import com.lineage.game.skills.effects.EffectTemplate;
import com.lineage.game.skills.effects.EffectTrigger;
import com.lineage.game.skills.effects.EffectTurner;
import com.lineage.game.skills.effects.EffectUltimateDefense;
import com.lineage.game.skills.effects.EffectUnAggro;
import com.lineage.game.skills.funcs.Func;
import com.lineage.game.skills.funcs.FuncTemplate;
import com.lineage.game.taskmanager.EffectTaskManager;

@SuppressWarnings("unchecked")
public abstract class L2Effect implements Comparable
{
	protected static final Logger _log = Logger.getLogger(L2Effect.class.getName());

	private final int[] SetModBuffs = { 1476, // Appetite for Destruction
			1477, // Vampiric Impulce
			1478, // Protection Instinct
			1479, // Magic Impulce
			4699, // Blessing of Queen
			4700, // Gift of Queen
			4702, // Blessing of Seraphim
			4703 // Gift of Seraphim
	};

	private final int[] ModBuffs = { 
			1268,
			86, // Reflect Damage
			123, // Spirit Barrier
			285, // Higher Mana Gain
			1002, // Flame Chant
			1003, // Pa'agrian Gift
			1004, // The Wisdom of Pa'agrio
			1005, // Blessings of Pa'agrio
			1006, // Chant of Fire
			1007, // Chant of Battle
			1008, // The Glory of Pa'agrio
			1009, // Chant of Shielding
			1033, // Resist Poison
			1035, // Mental Shield
			1036, // Magic Barrier
			1040, // Shield
			1043, // Holy Weapon
			1044, // Regeneration
			1045, // Blessed Body
			1048, // Blessed Soul
			1059, // Empower
			1062, // Berserker Spirit
			1068, // Might
			1073, // Kiss of Eva
			1077, // Focus
			1078, // Concentration
			1085, // Acumen
			1086, // Haste
			1087, // Agility
			1139, // Servitor Magic Shield
			1140, // Servitor Physical Shield
			1141, // Servitor Haste
			1144, // Servitor Wind Walk
			1145, // Bright Servitor
			1146, // Mighty Servitor
			1182, // Resist Aqua
			1189, // Resist Wind
			1191, // Resist Fire
			1204, // Wind Walk
			1240, // Guidance
			1242, // Death Whisper
			1243, // Bless Shield
			1249, // The Vision of Paagrio
			1250, // Protection of Paagrio
			1251, // Chant of Fury
			1252, // Chant of Evasion
			1253, // Chant of Rage
			1257, // Decrease Weight
			1259, // Resist Shock
			1282, // Pa'agrian Haste
			1284, // Chant of Revenge
			1299, // Servitor Empowerment
			1303, // Wild Magic
			1304, // Advanced Block
			1307, // Prayer
			1308, // Chant of Predator
			1309, // Chant of Eagle
			1310, // Chant of Vampire
			1311, // Body of Avatar
			1346, // Warrior Servitor
			1347, // Wizard Servitor
			1348, // Assassin Servitor
			1349, // Final Servitor
			1352, // Elemental Protection
			1353, // Divine Protection
			1354, // Arcane Protection
			1355, // Prophecy of Water
			1356, // Prophecy of Fire
			1357, // Prophecy of Wind
			1362, // Chant of Spirit
			1363, // Chant of Victory
			1388, // Greater Might
			1389, // Greater Shield
			1390, // War Chant
			1391, // Earth Chant
			1392, // Holy Resistance
			1393, // Unholy Resistance
			1397, // Clarity
			1413, // Magnus Chant
			1414, // Victory of Paagrio
			1459, // Divine Power
			1460, // Mana Gain
			2379, // Wind
			2380, // Shield
			2381, // Magic Barrier
			2382, // Bless Shield
			2383, // Haste
			2384, // Acumen
			2385, // Empower
			2386, // Divine Power
			2387, // Might
			2388, // Focus
			2389, // Guidance
			2390, // Berserker Spirit
			2391, // Clarity
			2404, // Might
			2405, // Shield
			2406, // Wind Walk
			2407, // Focus
			2408, // Death Whisper
			2409, // Guidance
			2410, // Bless Shield
			2411, // Blessed Body
			2412, // Haste
			2413, // Vampiric Rage
			2414, // Berserker Spirit
			2415, // Magic Barrier
			2416, // Blessed Soul
			2417, // Empower
			2418, // Acumen
			2419, // Clarity
			4342, // Wind Walk
			4343, // Decrease Weight
			4344, // Shield
			4345, // Might
			4346, // Mental Shield
			4347, // Blessed Body
			4348, // Blessed Soul
			4349, // Magic Barrier
			4350, // Resist Stun
			4351, // Concentration
			4352, // Berserker Spirit
			4353, // Bless Shield
			4354, // Vampiric Rage
			4355, // Acumen
			4356, // Empower
			4357, // Haste
			4358, // Guidance
			4359, // Focus
			4360, // Death Whisper
			4391, // Wind Walk
			4392, // Shield
			4393, // Might
			4394, // Blessed Body
			4395, // Blessed Soul
			4396, // Magic Barrier
			4397, // Berserker Spirit
			4398, // Bless Shield
			4399, // Vampiric Rage
			4400, // Acumen
			4401, // Empower
			4402, // Haste
			4403, // Guidance
			4404, // Focus
			4405, // Death Whisper
			4429, // Greater Resist Fire Attacks
			4430, // Greater Resist Water Attacks
			4431, // Greater Resist Wind Attacks
			4432, // Greater Resist Earth Attacks
			4433, // Greater Resist Sacred Attacks
			4434, // Greater Resist Dark Attacks
			4435, // Greater Resist Stun
			4436, // Greater Resist Poison
			4437, // Greater Resist Bleeding
			4438, // Greater Resist Sleep
			4439, // Greater Resist Hold
			4440, // Greater Resist Paralysis
			4441, // Greater Resist Mental Derangement
			4491, // Holy Weapon
			4699, // Blessing of Queen
			4700, // Gift of Queen
			4702, // Blessing of Seraphim
			4703, // Gift of Seraphim
			5147, // Blessed Body
			5148, // Prayer
			5150, // Blessed Soul
			5151, // Mana Gain
			5154, // Might
			5156, // Empower
			5158, // Shield
			5159, // Magic Barrier
			5162, // Guidance
			5163, // Focus
			5164, // Wild Magic
			7057, // Master''s Blessing - Greater Might
			7058, // Master''s Blessing - Greater Shield
			7059, // Master''s Blessing - Wild Magic
			7060 // Master''s Blessing - Clarity
	};

	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING,
		FINISHED
	}

	public static enum EffectType
	{
		// Основные эффекты
		AddSkills(EffectAddSkills.class, false), //
		Betray(EffectBetray.class, Stats.MENTAL_RECEPTIVE, Stats.MENTAL_POWER, true), //
		BigHead(EffectBigHead.class, true), //
		BlessNoblesse(EffectBlessNoblesse.class, true), //
		BlockStat(EffectBlockStat.class, Stats.DEBUFF_RECEPTIVE, Stats.DEBUFF_POWER, true), //
		Buff(EffectBuff.class, Stats.DEBUFF_RECEPTIVE, Stats.DEBUFF_POWER, false), //
		BuffImmunity(EffectBuffImmunity.class, true), //
		ClanGate(EffectClanGate.class, true), //
		CombatPointHealOverTime(EffectCombatPointHealOverTime.class, true), //
		ConsumeSoulsOverTime(EffectConsumeSoulsOverTime.class, true), //
		CPDamPercent(EffectCPDamPercent.class, true), //
		DamOverTime(EffectDamOverTime.class, false), //
		DamOverTimeLethal(EffectDamOverTimeLethal.class, false), //
		DestroySummon(EffectDestroySummon.class, Stats.MENTAL_RECEPTIVE, Stats.MENTAL_POWER, true), //
		Discord(EffectDiscord.class, Stats.MENTAL_RECEPTIVE, Stats.MENTAL_POWER, true), //
		Enervation(EffectEnervation.class, false), //
		FakeDeath(EffectFakeDeath.class, true), //
		Fear(EffectFear.class, Stats.MENTAL_RECEPTIVE, Stats.MENTAL_POWER, true), //
		Force(EffectForce.class, false), //
		Grow(EffectGrow.class, false), //
		Heal(EffectHeal.class, false), //
		HealBlock(EffectHealBlock.class, true), //
		HealCPPercent(EffectHealCPPercent.class, true), //
		HealOverTime(EffectHealOverTime.class, false), //
		HealPercent(EffectHealPercent.class, false), //
		ImobileBuff(EffectImobileBuff.class, true), //
		Interrupt(EffectInterrupt.class, true), //
		Invulnerable(EffectInvulnerable.class, false), //
		CurseOfLifeFlow(EffectCurseOfLifeFlow.class, true), //
		LDManaDamOverTime(EffectLDManaDamOverTime.class, true), //
		ManaDamOverTime(EffectManaDamOverTime.class, true), //
		ManaHeal(EffectManaHeal.class, false), //
		ManaHealOverTime(EffectManaHealOverTime.class, false), //
		ManaHealPercent(EffectManaHealPercent.class, false), //
		Meditation(EffectMeditation.class, false), //
		Mute(EffectMute.class, Stats.SILENCE_RECEPTIVE, Stats.SILENCE_POWER, true), //
		MuteAll(EffectMuteAll.class, Stats.SILENCE_RECEPTIVE, Stats.SILENCE_POWER, true), //
		MuteAttack(EffectMuteAttack.class, Stats.SILENCE_RECEPTIVE, Stats.SILENCE_POWER, true), //
		MutePhisycal(EffectMutePhisycal.class, Stats.SILENCE_RECEPTIVE, Stats.SILENCE_POWER, true), //
		Paralyze(EffectParalyze.class, Stats.PARALYZE_RECEPTIVE, Stats.PARALYZE_POWER, true), //
		Petrification(EffectPetrification.class, Stats.PARALYZE_RECEPTIVE, Stats.PARALYZE_POWER, true), //
		Relax(EffectRelax.class, true), //
		Root(EffectRoot.class, Stats.ROOT_RECEPTIVE, Stats.ROOT_POWER, true), //
		Salvation(EffectSalvation.class, true), //
		SilentMove(EffectSilentMove.class, true),
		Sleep(EffectSleep.class, Stats.SLEEP_RECEPTIVE, Stats.SLEEP_POWER, true), //
		Stun(EffectStun.class, Stats.STUN_RECEPTIVE, Stats.STUN_POWER, true), //
		Turner(EffectTurner.class, Stats.STUN_RECEPTIVE, Stats.STUN_POWER, true), //
		UltimateDefense(EffectUltimateDefense.class, false),
		UnAggro(EffectUnAggro.class, true), //
		Vitality(EffectBuff.class, true), //
		Bluff(EffectBluff.class, true), //
		Signet(EffectSignet.class, true), //
		AntiSummon(EffectAntiSummon.class, true), //
		CancelEffect(EffectCancelEffect.class, true), //
		RecoverForce(EffectRecoverForce.class, true), //
		Trigger(EffectTrigger.class, true), //
		Invisible(EffectInvisible.class, true), //

		// Производные от основных эффектов
		Poison(EffectDamOverTime.class, Stats.POISON_RECEPTIVE, Stats.POISON_POWER, false), //
		PoisonLethal(EffectDamOverTimeLethal.class, Stats.POISON_RECEPTIVE, Stats.POISON_POWER, false), //
		Bleed(EffectDamOverTime.class, Stats.BLEED_RECEPTIVE, Stats.BLEED_POWER, false), //
		Debuff(EffectBuff.class, Stats.DEBUFF_RECEPTIVE, Stats.DEBUFF_POWER, false), //
		BattleForce(EffectForce.class, false), //
		SpellForce(EffectForce.class, false), //
		Seed(EffectSeed.class, false),
		WatcherGaze(EffectBuff.class, false); //

		private final Class<? extends L2Effect> clazz;
		private final Stats resistType;
		private final Stats attibuteType;
		private final boolean isRaidImmune;

		private EffectType(final Class<? extends L2Effect> clazz, final boolean isRaidImmune)
		{
			this(clazz, null, null, isRaidImmune);
		}

		private EffectType(final Class<? extends L2Effect> clazz, final Stats resistType, final Stats attibuteType, final boolean isRaidImmune)
		{
			this.clazz = clazz;
			this.resistType = resistType;
			this.attibuteType = attibuteType;
			this.isRaidImmune = isRaidImmune;
		}

		public Stats getResistType()
		{
			return resistType;
		}

		public Stats getAttibuteType()
		{
			return attibuteType;
		}

		public boolean isRaidImmune()
		{
			return isRaidImmune;
		}

		public L2Effect makeEffect(final Env env, final EffectTemplate template)
		{
			try
			{
				final Constructor<? extends L2Effect> c = clazz.getConstructor(Env.class, EffectTemplate.class);
				return c.newInstance(env, template);
			}
			catch(final Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	private static final Func[] _emptyFunctionSet = new Func[0];

	/** Накладывающий эффект */
	protected final L2Character _effector;
	/** Тот, на кого накладывают эффект */
	protected final L2Character _effected;

	protected final L2Skill _skill;
	protected final int _displayId;
	protected final int _displayLevel;

	// the value of an update
	private final double _value;

	// the current state
	protected EffectState _state;

	// period, milliseconds
	private long _period;
	private long _periodStartTime;

	// function templates
	private final FuncTemplate[] _funcTemplates;

	private final EffectType _effectType;

	// counter
	protected int _count;

	// abnormal effect mask
	private int _abnormalEffect;

	/** The Identifier of the stack group */
	private final String _stackType;
	private final String _stackType2;

	/** The position of the effect in the stack group */
	private final int _stackOrder;

	private boolean _inUse = false;
	private L2Effect _next = null;

	private String _options;
	private boolean _skillMastery = false;

	public final EffectTemplate _template;

	public L2Effect(final Env env, final EffectTemplate template)
	{
		_template = template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		_effector = env.character;
		_effected = env.target;
		_value = template._value;
		_funcTemplates = template._funcTemplates;
		_count = template._counter;
		_period = template.getPeriod();
		_options = template._options;
		_displayId = template._displayId != 0 ? template._displayId : _skill.getDisplayId();
		_displayLevel = template._displayLevel != 0 ? template._displayLevel : _skill.getDisplayLevel();

		if(_skill.getSkillType() == L2Skill.SkillType.BUFF && _period > 30000 && _skill.getId() != 396 && _skill.getId() != 1374)
		{
			for(int i = 0; i < ModBuffs.length; i++)
			{
				if(_skill.getId() == ModBuffs[i])
				{
					_period *= Config.BUFFTIME_MODIFIER;
					break;
				}
			}
		}
		if(Config.SUMMON_SET_BUFF_TYPE == 1)
		{
			for(int i = 0; i < SetModBuffs.length; i++)
			{
				if(_skill.getId() == SetModBuffs[i])
				{
					_period *= Config.SUMMON_BUFF_MODIFIER;
					break;
				}
			}
		}

		if(Config.SUMMON_SET_BUFF_TYPE == 2)
		{
			for(int i = 0; i < SetModBuffs.length; i++)
			{
				if(_skill.getId() == SetModBuffs[i])
				{
					_period = Config.SUMMON_BUFF_TIME;
					break;
				}
			}
		}
		if(_skill.getSkillType() == L2Skill.SkillType.MUSIC)
			_period *= Config.SONGDANCETIME_MODIFIER;
		if(_skill.getId() >= 4342 && _skill.getId() <= 4360)
			_period *= Config.BUFFTIME_MODIFIER_CLANHALL;
		if(_skill.getId() == 1363 || _skill.getId() == 1355 || _skill.getId() == 1356 || _skill.getId() == 1357 || _skill.getId() == 4699 || _skill.getId() == 4702 || _skill.getId() == 4700 || _skill.getId() == 4703 || _skill.getId() == 1413)
			_period *= Config.BUFF15MINUTES_MODIFIER;
		_abnormalEffect = template._abnormalEffect;
		_stackType = template._stackType;
		_stackType2 = template._stackType2;
		_stackOrder = template._stackOrder;
		_effectType = template._effectType;
		_periodStartTime = System.currentTimeMillis();
	}

	public long getPeriod()
	{
		return _period;
	}

	public void setPeriod(final long time)
	{
		_period = time;
	}

	public int getCount()
	{
		return _count;
	}

	public void setCount(final int newcount)
	{
		_count = newcount;
	}

	public long getTime()
	{
		return System.currentTimeMillis() - _periodStartTime;
	}

	public long getPeriodStartTime()
	{
		return _periodStartTime;
	}

	/** Возвращает оставшееся время в миллисекундах. */
	public long getTimeLeft()
	{
		return getPeriod() * getCount() - getTime();
	}

	public boolean isInUse()
	{
		return _inUse;
	}

	public void setInUse(final boolean inUse)
	{
		_inUse = inUse;
		if(_inUse)
			scheduleEffect();
		else if(_state != EffectState.FINISHED)
			_state = EffectState.FINISHING;
	}

	public String getStackType()
	{
		return _stackType;
	}

	public String getStackType2()
	{
		return _stackType2;
	}

	public int getStackOrder()
	{
		return _stackOrder;
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	public L2Character getEffector()
	{
		return _effector;
	}

	public L2Character getEffected()
	{
		return _effected;
	}

	public double calc()
	{
		return _value;
	}

	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character </li>
	 * <li>Stop the task of the L2Effect, remove it and update client magic icon </li><BR><BR>
	 */
	public void exit()
	{
		if(_next != null)
			_next.exit();
		_next = null;

		if(_state == EffectState.FINISHED)
			return;
		if(_state != EffectState.CREATED)
		{
			_state = EffectState.FINISHING;
			scheduleEffect();
		}
		else
			_state = EffectState.FINISHING;
	}

	public boolean isEnded()
	{
		return _state == EffectState.FINISHED || _state == EffectState.FINISHING;
	}

	public boolean isFinishing()
	{
		return _state == EffectState.FINISHING;
	}

	public boolean isFinished()
	{
		return _state == EffectState.FINISHED;
	}

	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Cancel the task </li>
	 * <li>Stop and remove L2Effect from L2Character and update client magic icon </li><BR><BR>
	 */
	private synchronized void stopEffectTask()
	{
		_effected.getEffectList().removeEffect(this);
		updateEffects();
	}

	/** Notify started */
	public void onStart()
	{
		if(_abnormalEffect != 0)
			getEffected().startAbnormalEffect(_abnormalEffect);
	}

	/**
	 * Cancel the effect in the the abnormal effect map of the effected L2Character.<BR><BR>
	 */
	public void onExit()
	{
		if(_abnormalEffect != 0)
			getEffected().stopAbnormalEffect(_abnormalEffect);
	}

	/** Return true for continuation of this effect */
	public abstract boolean onActionTime();

	public final void scheduleEffect()
	{
		if(_state == EffectState.CREATED)
		{
			_state = EffectState.ACTING;
			onStart();

			// Fake Death и Silent Move не отображаются
			// Отображать сообщение только для первого эффекта скилла
			if(_skill.getId() != 60 && _skill.getId() != 221 && getEffected().getEffectList().getEffectsCountForSkill(getSkill().getId()) == 1 && !isHidden())
				getEffected().sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(_displayId, _displayLevel));

			updateEffects(); // Обрабатываем отображение статов

			EffectTaskManager.getInstance().addDispelTask(this, (int) (_period / 1000));

			_periodStartTime = System.currentTimeMillis();

			return;
		}

		if(_state == EffectState.ACTING)
		{
			if(_count > 0)
			{
				_count--;
				if(onActionTime() && _count > 0)
					return;
			}
			_state = EffectState.FINISHING;
		}

		if(_state == EffectState.FINISHING)
		{
			_state = EffectState.FINISHED;

			// Для ускоренной "остановки" эффекта
			_inUse = false;

			// Cancel the effect in the the abnormal effect map of the L2Character
			onExit();

			// If the time left is equal to zero, send the message
			// Отображать сообщение только для последнего оставшегося эффекта скилла
			if(_count == 0 && getEffected().getEffectList().getEffectsCountForSkill(getSkill().getId()) == 1 && !isHidden())
				getEffected().sendPacket(new SystemMessage(SystemMessage.S1_HAS_WORN_OFF).addSkillName(_displayId, _displayLevel));

			// Stop the task of the L2Effect, remove it and update client magic icon
			stopEffectTask();
		}
	}

	public void updateEffects()
	{
		_effected.updateStats();
	}

	public Func[] getStatFuncs()
	{
		if(_funcTemplates == null)
			return _emptyFunctionSet;
		final Func[] funcs = new Func[_funcTemplates.length];
		for(int i = 0; i < funcs.length; i++)
		{
			final Func f = _funcTemplates[i].getFunc(this); // effect is owner
			funcs[i] = f;
		}
		return funcs;
	}

	public void addIcon(final AbnormalStatusUpdate mi)
	{
		if(isHidden())
			return;

		if(_state != EffectState.ACTING)
			return;
		final int duration = _skill.isToggle() ? AbnormalStatusUpdate.INFINITIVE_EFFECT : (int) (getTimeLeft() / 1000);
		mi.addEffect(_displayId, _displayLevel, duration);
	}

	public void addPartySpelledIcon(final PartySpelled ps)
	{
		if(isHidden())
			return;

		if(_state != EffectState.ACTING)
			return;
		final int duration = _skill.isToggle() ? AbnormalStatusUpdate.INFINITIVE_EFFECT : (int) (getTimeLeft() / 1000);
		ps.addPartySpelledEffect(_displayId, _displayLevel, duration);
	}

	public void addOlympiadSpelledIcon(final L2Player player, final ExOlympiadSpelledInfo os)
	{
		if(isHidden())
			return;

		if(_state != EffectState.ACTING)
			return;
		final int duration = _skill.isToggle() ? AbnormalStatusUpdate.INFINITIVE_EFFECT : (int) (getTimeLeft() / 1000);
		os.addSpellRecivedPlayer(player);
		os.addEffect(_displayId, _displayLevel, duration);
	}

	protected int getLevel()
	{
		return _skill.getLevel();
	}

	public boolean containsStat(final Stats stat)
	{
		if(_funcTemplates != null)
			for(int i = 0; i < _funcTemplates.length; i++)
				if(_funcTemplates[i]._stat == stat)
					return true;
		return false;
	}

	public EffectType getEffectType()
	{
		return _effectType;
	}

	public boolean isSkillMasteryEffect()
	{
		return _skillMastery;
	}

	public int compareTo(final Object obj)
	{
		if(obj.equals(this))
			return 0;
		return 1;
	}

	public void removeNext()
	{
		_next = null;
	}

	public void scheduleNext(final L2Effect e)
	{
		if(_next != null)
			_next.exit();
		_next = e;
	}

	public L2Effect getNext()
	{
		return _next;
	}

	public int getAbnormalEffect()
	{
		return _abnormalEffect;
	}

	public boolean isSaveable()
	{
		return getTimeLeft() >= 15000 && getSkill().isSaveable();
	}

	public String getOptions()
	{
		return _options;
	}

	public boolean isHidden()
	{
		return _template._hidden;
	}

	public boolean isNotForCaster()
	{
		return _template._noforcaster;
	}

	@Override
	public String toString()
	{
		return "Skill: " + _skill + ", state: " + _state.name() + ", inUse: " + _inUse;
	}
}