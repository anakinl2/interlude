package com.lineage.game.model;

import static com.lineage.game.ai.CtrlEvent.EVT_FORGET_OBJECT;
import static com.lineage.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastTable;
import com.lineage.Config;
import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.PropertyCollection;
import com.lineage.ext.listeners.StatsChangeListener;
import com.lineage.ext.mods.balancer.Balancer;
import com.lineage.ext.mods.balancer.Balancer.bflag;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Scripts;
import com.lineage.ext.scripts.Scripts.ScriptClassAndMethod;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.ai.DefaultAI;
import com.lineage.game.ai.L2CharacterAI;
import com.lineage.game.ai.L2PlayableAI.nextAction;
import com.lineage.game.cache.Msg;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.geodata.GeoMove;
import com.lineage.game.instancemanager.DimensionalRiftManager;
import com.lineage.game.model.entity.Duel;
import com.lineage.game.model.entity.Duel.DuelState;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2MinionInstance;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2TamedBeastInstance;
import com.lineage.game.model.quest.QuestEventType;
import com.lineage.game.model.quest.QuestState;
import com.lineage.game.serverpackets.Attack;
import com.lineage.game.serverpackets.AutoAttackStart;
import com.lineage.game.serverpackets.AutoAttackStop;
import com.lineage.game.serverpackets.ChangeMoveType;
import com.lineage.game.serverpackets.ChangeWaitType;
import com.lineage.game.serverpackets.CharMoveToLocation;
import com.lineage.game.serverpackets.FlyToLocation;
import com.lineage.game.serverpackets.FlyToLocation.FlyType;
import com.lineage.game.serverpackets.L2GameServerPacket;
import com.lineage.game.serverpackets.MagicSkillCanceled;
import com.lineage.game.serverpackets.MagicSkillLaunched;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.MyTargetSelected;
import com.lineage.game.serverpackets.Revive;
import com.lineage.game.serverpackets.SetupGauge;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.serverpackets.StopMove;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.TeleportToLocation;
import com.lineage.game.serverpackets.ValidateLocation;
import com.lineage.game.serverpackets.VehicleDeparture;
import com.lineage.game.skills.Calculator;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Formulas;
import com.lineage.game.skills.Formulas.AttackInfo;
import com.lineage.game.skills.SkillTimeStamp;
import com.lineage.game.skills.Stats;
import com.lineage.game.skills.effects.EffectForce;
import com.lineage.game.skills.funcs.Func;
import com.lineage.game.tables.MapRegion;
import com.lineage.game.taskmanager.RegenTaskManager;
import com.lineage.game.templates.L2CharTemplate;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.game.templates.L2Weapon;
import com.lineage.game.templates.L2Weapon.WeaponType;
import com.lineage.util.GArray;
import com.lineage.util.Location;
import com.lineage.util.Log;
import com.lineage.util.Rnd;
import com.lineage.util.Util;

public abstract class L2Character extends L2Object
{
	protected boolean _isInSocialAction = false;

	public enum TargetDirection
	{
		NONE, //
		FRONT, //
		SIDE, //
		BEHIND
	}

	public boolean isInSocialAction()
	{
		return _isInSocialAction;
	}

	public void setIsInSocialAction(final boolean value)
	{
		_isInSocialAction = value;
	}

	public L2Character getFollowTarget()
	{
		if(followTarget == null)
			return null;

		final L2Character target = followTarget.get();
		if(target == null)
			followTarget = null;
		return target;
	}

	public void setFollowTarget(final L2Character target)
	{
		followTarget = target == null ? null : new WeakReference<L2Character>(target);
	}

	static class AltMagicUseTask implements Runnable
	{
		public final L2Skill _skill;
		final WeakReference<L2Character> character_ref, target_ref;

		public AltMagicUseTask(final L2Character character, final L2Character target, final L2Skill skill)
		{
			character_ref = new WeakReference<L2Character>(character);
			target_ref = new WeakReference<L2Character>(target);
			_skill = skill;
		}

		public void run()
		{
			final L2Character character = character_ref.get(), target = target_ref.get();
			if(character != null && target != null)
				character.altOnMagicUseTimer(target, _skill);
		}
	}

	static class CancelAttackStance implements Runnable
	{
		final WeakReference<L2Character> character_ref;

		public CancelAttackStance(final L2Character character)
		{
			character_ref = new WeakReference<L2Character>(character);
		}

		@Override
		public void run()
		{
			final L2Character character = character_ref.get();
			if(character != null)
				character.stopAttackStanceTask();
		}
	}

	/** Task lauching the function enableSkill() */
	static class EnableSkill implements Runnable
	{
		final int _skillId;
		final WeakReference<L2Character> character_ref;

		public EnableSkill(final L2Character character, final int skillId)
		{
			character_ref = new WeakReference<L2Character>(character);
			_skillId = skillId;
		}

		@Override
		public void run()
		{
			try
			{
				final L2Character character = character_ref.get();
				if(character != null)
					character.enableSkill(_skillId);
			}
			catch(final Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	static class CastEndTimeTask implements Runnable
	{
		private final WeakReference<L2Character> character_ref;

		public CastEndTimeTask(final L2Character character)
		{
			character_ref = new WeakReference<L2Character>(character);
		}

		public void run()
		{
			final L2Character character = character_ref.get();
			if(character != null)
				character.onCastEndTime();
		}
	}

	private class HitTask implements Runnable
	{
		boolean _crit;
		int _damage;
		L2Character _hitTarget;
		boolean _miss;
		boolean _shld;
		boolean _soulshot;
		boolean _unchargeSS;
		boolean _notify;

		public HitTask(final L2Character target, final int damage, final boolean crit, final boolean miss, final boolean soulshot, final boolean shld, final boolean unchargeSS, final boolean notify)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_unchargeSS = unchargeSS;
			_notify = notify;
		}

		@Override
		public void run()
		{
			try
			{
				onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld, _unchargeSS);

				if(_notify)
				{
					_attackEndTime = 0;
					getAI().notifyEvent(CtrlEvent.EVT_READY_TO_ACT);
				}
			}
			catch(final Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	/** Task launching the function onMagicUseTimer() */
	class MagicUseTask implements Runnable
	{
		public boolean _forceUse;

		public MagicUseTask(final boolean forceUse)
		{
			_forceUse = forceUse;
		}

		@Override
		public void run()
		{
			if((isPet() || isSummon()) && getPlayer() == null)
			{
				_castingSkill = null;
				_skillTask = null;
				_skillLaunchedTask = null;
				return;
			}
			onMagicUseTimer(getCastingTarget(), _castingSkill, _forceUse);
		}
	}

	class MagicLaunchedTask implements Runnable
	{
		public boolean _forceUse;

		public MagicLaunchedTask(final boolean forceUse)
		{
			_forceUse = forceUse;
		}

		@Override
		public void run()
		{
			final L2Skill castingSkill = _castingSkill;
			if(castingSkill == null || (isPet() || isSummon()) && getPlayer() == null)
			{
				_castingSkill = null;
				_skillTask = null;
				_skillLaunchedTask = null;
				return;
			}
			FastList<L2Character> targets = castingSkill.getTargets(L2Character.this, getCastingTarget(), _forceUse);
			broadcastPacket(new MagicSkillLaunched(_objectId, castingSkill.getDisplayId(), castingSkill.getLevel(), targets, castingSkill.isOffensive()));
		}
	}

	/** Task of AI notification */
	public class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;

		public NotifyAITask(final CtrlEvent evt)
		{
			_evt = evt;
		}

		@Override
		public void run()
		{
			if((isPet() || isSummon()) && getPlayer() == null)
				return;
			try
			{
				getAI().notifyEvent(_evt, null, null);
			}
			catch(final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}

	protected static final Logger _log = Logger.getLogger(L2Character.class.getName());

	public static final int ABNORMAL_EFFECT_BLEEDING = 0x00000001;
	public static final int ABNORMAL_EFFECT_POISON = 0x00000002;
	public static final int ABNORMAL_EFFECT_REDCIRCLE = 0x00000004; // блид
	public static final int ABNORMAL_EFFECT_ICE = 0x00000008; // блид

	public static final int ABNORMAL_EFFECT_AFFRAID = 0x00000010;
	public static final int ABNORMAL_EFFECT_CONFUSED = 0x00000020;
	public static final int ABNORMAL_EFFECT_STUN = 0x00000040;
	public static final int ABNORMAL_EFFECT_SLEEP = 0x00000080;

	public static final int ABNORMAL_EFFECT_MUTED = 0x00000100;
	public static final int ABNORMAL_EFFECT_ROOT = 0x00000200;
	public static final int ABNORMAL_EFFECT_HOLD_1 = 0x000400;
	public static final int ABNORMAL_EFFECT_HOLD_2 = 0x000800;

	public static final int ABNORMAL_EFFECT_UNKNOWN_13 = 0x00001000; // пусто
	public static final int ABNORMAL_EFFECT_BIG_HEAD = 0x00002000;
	public static final int ABNORMAL_EFFECT_FLAME = 0x00004000;
	public static final int ABNORMAL_EFFECT_UNKNOWN_16 = 0x00008000; // пусто

	public static final int ABNORMAL_EFFECT_GROW = 0x00010000; // эффект роста
	public static final int ABNORMAL_EFFECT_FLOATING_ROOT = 0x00020000; // висит в воздухе, на спине
	public static final int ABNORMAL_EFFECT_DANCE_STUNNED = 0x00040000; // танцует со звездочками над головой
	public static final int ABNORMAL_EFFECT_FIREROOT_STUN = 0x00080000; // красная аура со звездочками над головой

	public static final int ABNORMAL_EFFECT_SILENT_MOVE = 0x00100000;
	public static final int ABNORMAL_EFFECT_IMPRISIONING_1 = 0x00200000; // синяя аура на уровне пояса
	public static final int ABNORMAL_EFFECT_IMPRISIONING_2 = 0x00400000; // синяя аура на уровне пояса
	public static final int ABNORMAL_EFFECT_MAGIC_CIRCLE = 0x00800000; // большой синий круг вокруг чара

	public static final int ABNORMAL_EFFECT_ICE2 = 0x01000000; // небольшая ледяная аура, скорее всего DOT
	public static final int ABNORMAL_EFFECT_EARTHQUAKE = 0x02000000; // землетрясение
	public static final int ABNORMAL_EFFECT_TEST_1_4 = 0x04000000; // пусто
	public static final int ABNORMAL_EFFECT_INVULNERABLE = 0x08000000; // Неуязвимость (аура похожая на ультиму)

	public static final int ABNORMAL_EFFECT_VITALITY_HERB = 0x10000000; // Vitality херб, красное пламя
	public static final int ABNORMAL_EFFECT_TEST_2_2 = 0x20000000; // пусто
	public static final int ABNORMAL_EFFECT_TEST_2_4 = 0x40000000; // пусто
	public static final int ABNORMAL_EFFECT_TEST_2_8 = 0x80000000; // пусто

	private int _customEffect;

	public static final double HEADINGS_IN_PI = 10430.378350470452724949566316381;
	public static final int INTERACTION_DISTANCE = 200;

	/** List of all QuestState instance that needs to be notified of this character's death */
	private ArrayList<QuestState> _NotifyQuestOfDeathList;

	/** Array containing all clients that need to be notified about hp/mp updates of the L2Character */
	private CopyOnWriteArraySet<L2Character> _statusListener;
	private final Object _statusListenerLock = new Object();

	public Future<?> _skillTask;
	public Future<?> _skillLaunchedTask;
	public Future<?> _stanceTask;

	private long _stanceInited;

	private double _lastHpUpdate = -99999999;

	protected double _currentCp = 1;
	protected double _currentHp = 1;
	protected double _currentMp = 1;

	protected boolean _isAttackAborted;
	protected long _attackEndTime;
	protected long _attackReuseEndTime;

	/** HashMap(Integer, L2Skill) containing all skills of the L2Character */
	protected HashMap<Integer, L2Skill> _skills = new HashMap<Integer, L2Skill>();
	protected ConcurrentHashMap<L2Skill.TriggerActionType, ConcurrentLinkedQueue<L2Skill>> _skillsOnAction;

	public L2Skill _castingSkill;
	private WeakReference<L2Character> castingTarget;

	private long _castInterruptTime;
	private long _animationEndTime;

	/** Table containing all skillId that are disabled */
	protected List<Integer> _disabledSkills;

	protected ForceBuff _forceBuff;

	protected EffectList _effectList;

	private boolean _massUpdating;

	private FastList<Stats> _blockedStats;

	/** Map 32 bits (0x00000000) containing all abnormal effect in progress */
	private int _abnormalEffects;

	private boolean _flying;
	private boolean _riding;

	private boolean _fakeDeath;
	private boolean _fishing;

	protected boolean _isInvul;
	protected boolean _isPendingRevive;
	protected boolean _isTeleporting;
	protected boolean _overloaded;
	protected boolean _killedAlready;

	private long _dropDisabled;

	private boolean _isBlessedByNoblesse; // Восстанавливает все бафы после смерти
	private byte _isSalvation; // Восстанавливает все бафы после смерти и полностью CP, MP, HP
	private byte _buffImmunity; // Иммунитет к бафам/дебафам
	private HashMap<Integer, Byte> _skillMastery;

	private boolean _afraid;
	private boolean _meditated;
	private boolean _muted;
	private boolean _pmuted;
	private boolean _amuted;
	private boolean _paralyzed;
	private boolean _rooted;
	private boolean _sleeping;
	private boolean _stunned;
	private boolean _imobilised;
	private boolean _confused;
	private boolean _blocked;
	private boolean _bigHead;
	private boolean _healBlocked;
	private double _vampPen = 1; // Allseron: variable used to nerf vampiric rage while using a polearm weapon.

	private boolean _running;

	public Future<?> _moveTask;
	private final MoveNextRunnable _moveTaskRunnable;
	public boolean isMoving;
	public boolean isFollow;
	protected final Location movingFrom = new Location(0, 0, 0);
	protected Location movingTo = new Location(0, 0, 0);

	/**
	 * при moveToLocation используется для хранения геокоординат в которые мы двигаемся для того что бы избежать повторного построения одного и того же пути при followToCharacter используется для
	 * хранения мировых координат в которых находилась последний раз приследуемая цель для отслеживания необходимости перестраивания пути
	 */
	protected final Location movingDestTempPos = new Location(0, 0, 0);
	public int _offset;

	protected boolean _forestalling;

	protected WeakReference<L2Character> followTarget;

	protected Vector<Location> _targetRecorder = new Vector<Location>();

	protected long _followTimestamp, _startMoveTime, _arriveTime;
	protected double _previousSpeed = -1;

	private int _heading;

	private WeakReference<L2Object> _target;

	private Calculator[] _calculators;

	protected L2CharTemplate _template;
	protected L2CharTemplate _baseTemplate;
	protected L2CharacterAI _ai;

	protected String _name;
	protected String _visname;
	protected String _title;
	protected String _vistitle;

	public L2Character(final int objectId, final L2CharTemplate template)
	{
		super(objectId);

		// Set its template to the new L2Character
		_template = template;
		_baseTemplate = template;

		_calculators = new Calculator[Stats.NUM_STATS];
		if(this.isPlayer())
			for(final Stats stat : Stats.values())
				_calculators[stat.ordinal()] = new Calculator(stat, this);

		if(template != null && (this instanceof L2NpcInstance || this instanceof L2Summon))
			if(((L2NpcTemplate) template).getSkills().size() > 0)
				for(final L2Skill skill : ((L2NpcTemplate) template).getSkills().values())
					addSkill(skill);

		_moveTaskRunnable = new MoveNextRunnable(this); // FIXME check hasAI???
		Formulas.addFuncsToNewCharacter(this);
	}

	public final void abortAttack()
	{
		if(isAttackingNow())
		{
			_attackEndTime = 0;
			_isAttackAborted = true;
			sendActionFailed();
			getAI().setIntention(AI_INTENTION_ACTIVE);
		}
	}

	public final void abortCast()
	{
		if(isCastingNow())
		{
			_castInterruptTime = 0;

			if(_castingSkill != null)
			{
				if(_castingSkill.isUsingWhileCasting())
				{
					final L2Character target = getAI().getAttackTarget();
					if(target != null)
						target.getEffectList().stopEffect(_castingSkill.getId());
				}

				if(_skillMastery != null)
					_skillMastery.remove(_castingSkill.getId());
				_castingSkill = null;
				_flyLoc = null;
			}

			if(_skillTask != null)
			{
				_skillTask.cancel(false); // cancels the skill hit scheduled task
				_skillTask = null;
			}

			if(_skillLaunchedTask != null)
			{
				_skillLaunchedTask.cancel(false); // cancels the skill hit scheduled task
				_skillLaunchedTask = null;
			}

			if(_forceBuff != null)
				_forceBuff.delete();

			if(getEffectList().getEffectByType(L2Effect.EffectType.Signet) != null)
				getEffectList().getEffectByType(L2Effect.EffectType.Signet).exit();

			broadcastPacket(new MagicSkillCanceled(_objectId)); // broadcast packet to stop animations client-side
			sendActionFailed(); // send an "action failed" packet to the caster
			getAI().setIntention(AI_INTENTION_ACTIVE);
		}
	}

	public void breakAttack()
	{
		if(isAttackingNow())
		{
			abortAttack();

			if(isPlayer())
				sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_FAILED).addName(this));
		}
	}

	public void breakCast(final boolean force)
	{
		if(isCastingNow() && (force || canAbortCast()))
		{
			abortCast();

			if(isPlayer())
				sendPacket(Msg.CASTING_HAS_BEEN_INTERRUPTED);
		}
	}

	public final boolean canAbortCast()
	{
		return _castInterruptTime > System.currentTimeMillis();
	}

	public void setVampPen(double i) // Allseron: functions used to pass the penality to Vampiric Rage on to the onHitTimer function.
	{
		_vampPen = i;
	}

	public double getVampPen()
	{
		return _vampPen;
	}// Allseron: end of modification.

	public void absorbAndReflect(final L2Character target, final L2Skill skill, double damage)
	{
		if(isRaid() || target.isRaid())
			return;

		double absorb = target.calcStat(Stats.ABSORB_DAMAGE_ENEMY_PERCENT, 0, this, null);
		if(absorb > 0 && !target.isHealBlocked())
			if(getActiveWeaponItem().getItemType() == WeaponType.POLE)// Allseron: modification reducing vampiric rage effect while using polearm weapons on two or more targets.
			{
				target.setCurrentHp((target.getCurrentHp() + damage * absorb / 100) * target.getVampPen(), false);
			}
			else
			{
				target.setCurrentHp(target.getCurrentHp() + damage * absorb / 100, false);
			}

		if(skill != null)
			return;

		if(getActiveWeaponItem() != null && (getActiveWeaponItem().getItemType() == WeaponType.BOW))
			return;

		if(!target.isDead())
		{
			final double reflect = target.calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
			if(reflect > 0)
			{
				final double rdmg = damage * reflect / 100.;
				if(this instanceof L2Playable && !target.isNpc())
					reduceCurrentHp(rdmg, this, null, true, true, false, false);
				else
					reduceCurrentHp(rdmg, target, null, true, true, false, false);
				if(target.isPlayer() && rdmg >= 1.)
					target.sendPacket(new SystemMessage(SystemMessage.C1_HAS_GIVEN_C2_DAMAGE_OF_S3).addName(target).addName(this).addNumber((int) rdmg));
			}
		}

		damage = (int) (damage - target.getCurrentCp());

		if(damage <= 0)
			return;

		absorb = calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, target, null);
		if(absorb > 0 && !(target instanceof L2DoorInstance))
			setCurrentHp(_currentHp + damage * absorb / 200, false);

		absorb = calcStat(Stats.ABSORB_DAMAGEMP_PERCENT, 0, target, null);
		if(absorb > 0 && !target.isHealBlocked())
			setCurrentMp(_currentMp + damage * absorb / 200);
	}

	public void addBlockStats(final FastList<Stats> stats)
	{
		if(_blockedStats == null)
			_blockedStats = new FastList<Stats>(stats);
		else
			_blockedStats.addAll(stats);
	}

	public L2Skill addSkill(final L2Skill newSkill)
	{
		if(newSkill == null)
			return null;

		final L2Skill oldSkill = _skills.get(newSkill.getId());

		if(oldSkill != null && oldSkill.getLevel() == newSkill.getLevel())
			return newSkill;

		// Replace oldSkill by newSkill or Add the newSkill
		_skills.put(newSkill.getId(), newSkill);
		if(newSkill.isOnAction())
			addTriggerableSkill(newSkill);

		// If an old skill has been replaced, remove all its Func objects
		if(oldSkill != null)
			removeStatsOwner(oldSkill);

		// Add Func objects of newSkill to the calculator set of the L2Character
		addStatFuncs(newSkill.getStatFuncs());

		return oldSkill;
	}

	public final synchronized void addStatFunc(final Func f)
	{
		if(f == null)
			return;
		final int stat = f._stat.ordinal();
		if(_calculators[stat] == null)
			_calculators[stat] = new Calculator(f._stat, this);
		_calculators[stat].addFunc(f);
	}

	public final synchronized void addStatListener(final StatsChangeListener l)
	{
		if(l == null || l._stat == null)
			return;
		final int stat = l._stat.ordinal();
		if(_calculators[stat] == null)
			_calculators[stat] = new Calculator(l._stat, this);
		_calculators[l._stat.ordinal()].addListener(l);
	}

	public final synchronized void addStatFuncs(final Func[] funcs)
	{
		for(final Func f : funcs)
			addStatFunc(f);
	}

	public void altOnMagicUseTimer(final L2Character aimingTarget, final L2Skill skill)
	{
		if(isAlikeDead())
			return;
		final int magicId = skill.getDisplayId();
		final int level = Math.max(1, getSkillDisplayLevel(skill.getId()));
		final FastList<L2Character> targets = skill.getTargets(this, aimingTarget, true);
		broadcastPacket(new MagicSkillLaunched(_objectId, magicId, level, targets, skill.isOffensive()));
		final double mpConsume2 = skill.getMpConsume2();
		if(mpConsume2 > 0)
		{
			if(_currentMp < mpConsume2)
			{
				sendPacket(Msg.NOT_ENOUGH_MP);
				return;
			}
			if(skill.isMagic())
				reduceCurrentMp(calcStat(Stats.MP_MAGIC_SKILL_CONSUME, mpConsume2, null, skill), null);
			else
				reduceCurrentMp(calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, mpConsume2, null, skill), null);
		}
		callSkill(skill, targets, false);
	}

	public void altUseSkill(final L2Skill skill, L2Character target)
	{
		if(skill == null)
			return;
		final int magicId = skill.getId();
		if(isSkillDisabled(magicId))
		{
			if(isPlayer())
			{
				final SkillTimeStamp sts = ((L2Player) this).getSkillReuseTimeStamps().get(magicId);
				if(sts == null)
					return;
				final long timeleft = sts.getReuseCurrent();
				if(Config.ALT_SHOW_REUSE_MSG && timeleft < 10000)
					return;
				final long hours = timeleft / 3600000;
				final long minutes = (timeleft - hours * 3600000) / 60000;
				final long seconds = (long) Math.ceil((timeleft - hours * 3600000 - minutes * 60000) / 1000.);
				if(((L2Player) this).getLastReuseMsgSkill() == magicId && ((L2Player) this).getLastReuseMsg() + 1000 >= System.currentTimeMillis())
					return;
				
				if(hours > 0)
					sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S2_HOURS_S3_MINUTES_AND_S4_SECONDS_REMAINING_IN_S1S_REUSE_TIME).addSkillName(magicId, skill.getDisplayLevel()).addNumber(hours).addNumber(minutes).addNumber(seconds));
				else if(minutes > 0)
					sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S2_MINUTES_S3_SECONDS_REMAINING_IN_S1S_REUSE_TIME).addSkillName(magicId, skill.getDisplayLevel()).addNumber(minutes).addNumber(seconds));
				else
					sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S2_SECONDS_REMAINING_IN_S1S_REUSE_TIME).addSkillName(magicId, skill.getDisplayLevel()).addNumber(seconds));
				((L2Player) this).updateLastReuseMsg(magicId);
			}
			return;
		}
		if(target == null)
		{
			target = skill.getAimingTarget(this, getTarget());
			if(target == null)
				return;
		}

		final int itemConsume[] = skill.getItemConsume();

		if(itemConsume[0] > 0)
			for(int i = 0; i < itemConsume.length; i++)
				if(!consumeItem(skill.getItemConsumeId()[i], itemConsume[i]))
				{
					sendPacket(Msg.INCORRECT_ITEM_COUNT);
					sendChanges();
					return;
				}

		int level = Math.max(1, getSkillDisplayLevel(magicId));
		Formulas.calcSkillMastery(skill, this);
		long reuseDelay = Formulas.calcSkillReuseDelay(this, skill);
		if(!skill.isToggle())
			broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), level, skill.getHitTime(), reuseDelay));
		// Не показывать сообщение для хербов и кубиков
		if(!(skill.getId() >= 4049 && skill.getId() <= 4055 || skill.getId() >= 4164 && skill.getId() <= 4166 || skill.getId() >= 2278 && skill.getId() <= 2285 || skill.getId() >= 2512 && skill.getId() <= 2514 || skill.getId() == 5115 || skill.getId() == 5116 || skill.getId() == 2580))
			if(!skill.isHandler())
				sendPacket(new SystemMessage(SystemMessage.YOU_USE_S1).addSkillName(magicId, (short) level));
			else
				sendPacket(new SystemMessage(SystemMessage.YOU_USE_S1).addItemName(skill.getItemConsumeId()[0]));
		// Skill reuse check
		if(reuseDelay > 10)
		{
			addSkillTimeStamp(skill.getId(), reuseDelay);
			disableItem(skill, reuseDelay, reuseDelay);
			disableSkill(skill.getId(), reuseDelay);
		}
		ThreadPoolManager.getInstance().scheduleAi(new AltMagicUseTask(this, target, skill), skill.getHitTime(), isPlayer() || isPet() || isSummon());
	}

	public void broadcastPacket(final L2GameServerPacket mov)
	{
		sendPacket(mov);
		broadcastPacketToOthers(mov);
	}

	public final void broadcastPacketToOthers(final L2GameServerPacket mov)
	{
		if(!isVisible())
			return;

		boolean buffCheck = false;
		if(mov instanceof MagicSkillLaunched && !((MagicSkillLaunched) mov).isOffensive())
			buffCheck = true;

		for(final L2Player target : L2World.getAroundPlayers(this))
			if(target != null && _objectId != target.getObjectId() && !(buffCheck && target.isNotShowBuffAnim()))
				target.sendPacket(mov);
	}

	public void addStatusListener(final L2Character object)
	{
		if(object == this)
			return;
		synchronized (_statusListenerLock)
		{
			if(_statusListener == null)
				_statusListener = new CopyOnWriteArraySet<L2Character>();
			_statusListener.add(object);
		}
	}

	public void removeStatusListener(final L2Character object)
	{
		synchronized (_statusListenerLock)
		{
			if(_statusListener == null)
				return;
			_statusListener.remove(object);
			if(_statusListener.isEmpty())
				_statusListener = null;
		}
	}

	public void broadcastStatusUpdate()
	{
		final CopyOnWriteArraySet<L2Character> list = _statusListener;

		if(list == null || list.isEmpty())
			return;

		if(!needStatusUpdate())
			return;

		final StatusUpdate su = new StatusUpdate(_objectId);
		su.addAttribute(StatusUpdate.CUR_HP, (int) _currentHp);
		su.addAttribute(StatusUpdate.CUR_MP, (int) _currentMp);
		su.addAttribute(StatusUpdate.CUR_CP, (int) _currentCp);

		for(final L2Character temp : list)
			if(!Config.FORCE_STATUSUPDATE)
			{
				if(temp.getTarget() == this)
					temp.sendPacket(su);
			}
			else
				temp.sendPacket(su);
	}

	public int calcHeading(final int x_dest, final int y_dest)
	{
		return (int) (Math.atan2(getY() - y_dest, getX() - x_dest) * HEADINGS_IN_PI) + 32768;
	}

	public final double calcStat(final Stats stat, final double init, final L2Character object, final L2Skill skill)
	{
		final int id = stat.ordinal();
		final Calculator c = _calculators[id];
		if(c == null)
			return init;
		final Env env = new Env();
		env.character = this;
		env.target = object;
		env.skill = skill;
		env.value = init;
		c.calc(env);
		return env.value;
	}

	public final double calcStat(final Stats stat, final L2Character object, final L2Skill skill)
	{
		final Env env = new Env(this, object, skill);
		stat.getInit().calc(env);
		final int id = stat.ordinal();
		final Calculator c = _calculators[id];
		if(c != null)
			c.calc(env);
		return env.value;
	}

	/**
	 * Return the Attack Speed of the L2Character (delay (in milliseconds) before next attack).
	 */
	public int calculateAttackDelay()
	{
		return Formulas.calcPAtkSpd(getPAtkSpd());
	}

	public void callSkill(final L2Skill skill, final FastList<L2Character> targets, final boolean useActionSkills)
	{
		try
		{
			if(useActionSkills && !skill.isUsingWhileCasting() && _skillsOnAction != null)
			{
				final ConcurrentLinkedQueue<L2Skill> SkillsOnMagicAttack = _skillsOnAction.get(L2Skill.TriggerActionType.OFFENSIVE_MAGICAL_SKILL_USE);
				if(skill.isOffensive() && skill.isMagic() && SkillsOnMagicAttack != null)
					for(final L2Skill sk : SkillsOnMagicAttack)
						if(Rnd.chance(sk.getChanceForAction(L2Skill.TriggerActionType.OFFENSIVE_MAGICAL_SKILL_USE)))
						{
							FastList<L2Character> sk_targets = sk.getTargets(this, sk.getAimingTarget(this, this.getCharTarget()), false);
							callSkill(sk, sk_targets, false);
						}

				if(skill.isOffensive())
					for(final L2Character target : targets)
						if(target.getTriggerableSkills() != null)
						{
							final ConcurrentLinkedQueue<L2Skill> SkillsOnUnderSkillAttack = target.getTriggerableSkills().get(L2Skill.TriggerActionType.UNDER_SKILL_ATTACK);
							if(SkillsOnUnderSkillAttack != null)
								for(final L2Skill sk : SkillsOnUnderSkillAttack)
									if(sk != skill && Rnd.chance(sk.getChanceForAction(L2Skill.TriggerActionType.UNDER_SKILL_ATTACK)))
									{
										final L2Character aimingTarget = sk.getAimingTarget(target, this);
										final FastList<L2Character> sk_targets = sk.getTargets(target, aimingTarget, false);
										target.callSkill(sk, sk_targets, false);
									}
						}

				final ConcurrentLinkedQueue<L2Skill> SkillsOnMagicSupport = _skillsOnAction.get(L2Skill.TriggerActionType.SUPPORT_MAGICAL_SKILL_USE);
				if(!skill.isOffensive() && skill.isMagic() && SkillsOnMagicSupport != null)
					for(final L2Skill sk : SkillsOnMagicSupport)
						if(Rnd.chance(sk.getChanceForAction(L2Skill.TriggerActionType.SUPPORT_MAGICAL_SKILL_USE)))
							callSkill(sk, targets, false);

				if(isPlayer())
				{
					final L2Player pl = (L2Player) this;
					for(final L2Character target : targets)
						if(target != null && target.isNpc())
						{
							final L2NpcInstance npc = (L2NpcInstance) target;
							final ArrayList<QuestState> ql = pl.getQuestsForEvent(npc, QuestEventType.MOB_TARGETED_BY_SKILL);
							if(ql != null)
								for(final QuestState qs : ql)
									qs.getQuest().notifySkillUse(npc, skill, qs);
						}
				}
			}

			if(skill.getNegateSkill() > 0)
				for(final L2Character target : targets)
					for(final L2Effect e : target.getEffectList().getAllEffects())
					{
						final L2Skill efs = e.getSkill();
						if(efs.getId() == skill.getNegateSkill() && efs.isCancelable() && (skill.getNegatePower() <= 0 || efs.getPower() <= skill.getNegatePower()))
							e.exit();
					}

			if(skill.isCancelTarget())
				for(final L2Character target : targets)
					if(!target.isNpc() && Rnd.chance(70))
					{
						target.breakAttack();
						target.breakCast(true);
						target.setTarget(null);
					}

			if(skill.isSkillInterrupt())
				for(final L2Character target : targets)
					if(!target.isRaid())
					{
						if(target.getCastingSkill() != null && !target.getCastingSkill().isMagic())
							target.abortCast();
						target.abortAttack();
					}

			if(skill.isOffensive())
				startAttackStanceTask();

			skill.getEffects(this, this, false, true);
			skill.useSkill(this, targets);
		}
		catch(final Exception e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}

	public boolean checkBlockedStat(final Stats stat)
	{
		return _blockedStats != null && _blockedStats.contains(stat);
	}

	public boolean checkReflectSkill(final L2Character actor, final L2Skill skill)
	{
		if(Rnd.chance((int) calcStat(skill.isMagic() ? Stats.REFLECT_MAGIC_SKILL : Stats.REFLECT_PHYSIC_SKILL, 0, null, skill)))
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_COUNTERED_C1S_ATTACK).addName(actor));
			actor.sendPacket(new SystemMessage(SystemMessage.C1_DODGES_THE_ATTACK).addName(this));
			return true;
		}
		return false;
	}

	public void doCounterAttack(final L2Skill skill, final L2Character target)
	{
		if(skill == null || skill.isMagic() || !skill.isOffensive() || skill.getCastRange() > 200)
			return;

		if(Rnd.chance((int) calcStat(Stats.COUNTER_ATTACK, 0, null, skill)))
		{
			final double damage = Formulas.calcPhysDam(this, target, null, false, false, true).damage;
			target.sendPacket(new SystemMessage(SystemMessage.C1S_IS_PERFORMING_A_COUNTERATTACK).addName(this));
			sendPacket(new SystemMessage(SystemMessage.C1S_IS_PERFORMING_A_COUNTERATTACK).addName(this));
			sendPacket(new SystemMessage(SystemMessage.C1_HAS_GIVEN_C2_DAMAGE_OF_S3).addName(this).addName(target).addNumber((int) damage));
			target.reduceCurrentHp(damage, this, skill, true, true, false, false);
		}
	}

	public void detachAI()
	{
		if(_ai != null)
		{
			_ai.stopAITask();
			_ai.removeActor();
		}
		_ai = null;
	}

	public final void disableDrop(final int time)
	{
		_dropDisabled = System.currentTimeMillis() + time;
	}

	/**
	 * Disable this skill id for the duration of the delay in milliseconds.
	 * 
	 * @param skillId
	 * @param delay
	 *            (seconds * 1000)
	 */
	public void disableSkill(final int skillId, final long delay)
	{
		if(delay > 10)
		{
			if(_disabledSkills == null)
				_disabledSkills = Collections.synchronizedList(new FastList<Integer>());
			_disabledSkills.add(skillId);
			ThreadPoolManager.getInstance().scheduleAi(new EnableSkill(this, skillId), delay, isPlayer() || isPet() || isSummon());
		}
	}

	public void doAttack(final L2Character target)
	{
		if(_isInSocialAction)
			return;

		if(target == null || isAMuted() || isAttackingNow() || isAlikeDead() || target.isAlikeDead() || !isInRange(target, 2000))
			return;

		fireMethodInvoked(MethodCollection.onStartAttack, new Object[] { target });

		// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
		// лимит в 0.2 секунды означает скорость атаки 2500
		final int sAtk = Math.max(calculateAttackDelay(), 200);
		int ssGrade = 0;

		final L2Weapon weaponItem = getActiveWeaponItem();
		if(weaponItem != null)
		{
			if(isPlayer() && weaponItem.getItemType() == WeaponType.BOW)
			{
				final int reuse = (int) (weaponItem.getAttackReuseDelay() * getReuseModifier(target) * 666 * calcStat(Stats.ATK_BASE, 0, null, null) / 293. / getPAtkSpd());
				if(reuse > 0)
				{
					sendPacket(new SetupGauge(SetupGauge.RED, reuse));
					_attackReuseEndTime = reuse + System.currentTimeMillis() - 75;
					if(reuse > sAtk)
						ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), reuse, isPlayable());
				}
			}

			ssGrade = weaponItem.getCrystalType().externalOrdinal;
		}

		_attackEndTime = sAtk + System.currentTimeMillis();
		_isAttackAborted = false;

		final Attack attack = new Attack(this, getChargedSoulShot(), ssGrade);

		setHeading(target, true);

		// Select the type of attack to start
		if(weaponItem == null)
			doAttackHitSimple(attack, target, 1., !isPlayer(), sAtk, true);
		else
			switch(weaponItem.getItemType())
			{
				case BOW:
					doAttackHitByBow(attack, target, sAtk);
					break;
				case POLE:
					doAttackHitByPole(attack, target, sAtk);
					break;
				case DUAL:
				case DUALFIST:
					doAttackHitByDual(attack, target, sAtk);
					break;
				default:
					doAttackHitSimple(attack, target, 1., true, sAtk, true);
			}

		if(attack.hasHits())
			broadcastPacket(attack);
	}

	private void doAttackHitSimple(final Attack attack, final L2Character target, final double multiplier, final boolean unchargeSS, final int sAtk, final boolean notify)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;
		final boolean miss1 = Formulas.calcHitMiss(this, target);

		if(!miss1)
		{
			final AttackInfo info = Formulas.calcPhysDam(this, target, null, false, false, attack._soulshot);
			damage1 = (int) (info.damage * multiplier);
			shld1 = info.shld;
			crit1 = info.crit;
		}
		else if(target.isPlayer())
			target.sendPacket(new SystemMessage(SystemMessage.C1_HAS_EVADED_C2S_ATTACK).addName(target).addName(this));

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack._soulshot, shld1, unchargeSS, notify), sAtk, isPlayable());

		attack.addHit(target, damage1, miss1, crit1, shld1);
	}

	private void doAttackHitByBow(final Attack attack, final L2Character target, final int sAtk)
	{
		final L2Weapon activeWeapon = getActiveWeaponItem();
		if(activeWeapon == null)
			return;

		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;

		// Calculate if hit is missed or not
		final boolean miss1 = Formulas.calcHitMiss(this, target);

		reduceArrowCount();

		if(!miss1)
		{
			final AttackInfo info = Formulas.calcPhysDam(this, target, null, false, false, attack._soulshot);
			damage1 = (int) info.damage;
			shld1 = info.shld;
			crit1 = info.crit;

			final int range = activeWeapon.getAttackRange();
			damage1 *= (Math.min(range, getDistance(target)) / range) * .4 + 0.8; // разброс 20% в обе стороны
		}

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack._soulshot, shld1, true, true), sAtk, isPlayable());

		attack.addHit(target, damage1, miss1, crit1, shld1);
	}

	private void doAttackHitByDual(final Attack attack, final L2Character target, final int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		boolean shld1 = false;
		boolean shld2 = false;
		boolean crit1 = false;
		boolean crit2 = false;

		final boolean miss1 = Formulas.calcHitMiss(this, target);
		final boolean miss2 = Formulas.calcHitMiss(this, target);

		if(!miss1)
		{
			final AttackInfo info = Formulas.calcPhysDam(this, target, null, true, false, attack._soulshot);
			damage1 = (int) info.damage;
			shld1 = info.shld;
			crit1 = info.crit;
		}

		if(!miss2)
		{
			final AttackInfo info = Formulas.calcPhysDam(this, target, null, true, false, attack._soulshot);
			damage2 = (int) info.damage;
			shld2 = info.shld;
			crit2 = info.crit;
		}

		// Create a new hit task with Medium priority for hit 1 and for hit 2 with a higher delay
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack._soulshot, shld1, true, false), sAtk / 2, isPlayable());
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack._soulshot, shld2, false, true), sAtk, isPlayable());

		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
	}

	private void doAttackHitByPole(final Attack attack, final L2Character target, final int sAtk)
	{
		final int angle = (int) calcStat(Stats.POLE_ATTACK_ANGLE, 90, null, null);
		final int range = (int) calcStat(Stats.POWER_ATTACK_RANGE, getTemplate().baseAtkRange, null, null);

		// Используем Math.round т.к. обычный кастинг обрезает к меньшему
		// double d = 2.95. int i = (int)d, выйдет что i = 2
		// если 1% угла или 1 дистанции не играет огромной роли, то для
		// количества целей это критично
		int attackcountmax = (int) Math.round(calcStat(Stats.POLE_TARGERT_COUNT, 3, null, null));

		if(isBoss())
			attackcountmax += 27;
		else if(isRaid())
			attackcountmax += 12;
		else if(isMonster() && getLevel() > 0)
			attackcountmax += getLevel() / 7.5;

		double mult = 0.5;
		setVampPen(1);
		int attackcount = 1;

		for(final L2Character t : getAroundCharacters(range, 200))
			if(attackcount <= attackcountmax)
			{
				if(t != null && !t.isDead() && t.isAutoAttackable(this) && !t.isInZonePeace())
				{
					if(t == getAI().getAttackTarget() || !isInFront(t, angle))
						continue;
					doAttackHitSimple(attack, t, 1, attackcount == 0, sAtk, false);
					t.setVampPen(mult);
					mult *= 0.5;
					attackcount++;
				}
			}
			else
				break;

		doAttackHitSimple(attack, target, 1., true, sAtk, true);
	}

	public long getAnimationEndTime()
	{
		return _animationEndTime;
	}

	public void doCast(final L2Skill skill, L2Character target, final boolean forceUse)
	{
		if(_isInSocialAction)
			return;

		// Прерывать дуэли если цель не дуэлянт
		if(getDuel() != null)
			if(target.getDuel() != getDuel())
				getDuel().setDuelState(getPlayer(), DuelState.Interrupted);
			else if(isPlayer() && getDuel().getDuelState((L2Player) this) == DuelState.Interrupted)
			{
				sendPacket(Msg.INVALID_TARGET);
				return;
			}

		if(skill == null)
		{
			sendActionFailed();
			return;
		}
		
		if(skill.isMagic())
			if(isPlayer() && getTarget() != null && getTarget().getPlayer() != null)
			{
				L2Player pl = getPlayer();
				L2Player trg = getTarget().getPlayer();

				if(skill.getSkillType() == L2Skill.SkillType.BUFF && skill.getTargetType() != L2Skill.SkillTargetType.TARGET_CLAN && skill.getTargetType() != L2Skill.SkillTargetType.TARGET_PARTY && skill.getTargetType() != L2Skill.SkillTargetType.TARGET_ALLY && skill.getTargetType() != L2Skill.SkillTargetType.TARGET_SELF && trg != pl)
				{
					if(!pl.isInParty())
					{
						pl.sendMessage("You cant buff others only in party.");
						sendActionFailed();
						return;
					}
					if(trg.isInParty() && pl.getParty() != trg.getParty())
					{
						pl.sendMessage("You cant only buff players from same party.");
						sendActionFailed();
						return;
					}
				}
			}

		final int itemConsume[] = skill.getItemConsume();

		if(itemConsume[0] > 0)
			for(int i = 0; i < itemConsume.length; i++)
				if(!consumeItem(skill.getItemConsumeId()[i], itemConsume[i]))
				{
					sendPacket(Msg.INCORRECT_ITEM_COUNT);
					sendChanges();
					return;
				}

		final int magicId = skill.getId();

		if(target == null)
			target = skill.getAimingTarget(this, getTarget());
		if(target == null)
			return;

		final boolean forceBuff = skill.getSkillType() == L2Skill.SkillType.FORCE_BUFF;

		if(forceBuff)
		{
			final L2Effect spell_force = target.getEffectList().getEffectByType(L2Effect.EffectType.SpellForce);
			final L2Effect battle_force = target.getEffectList().getEffectByType(L2Effect.EffectType.BattleForce);
			if(spell_force != null && ((EffectForce) spell_force).getForceCount() >= 3)
			{
				sendPacket(Msg.INVALID_TARGET);
				return;
			}
			if(battle_force != null && ((EffectForce) battle_force).getForceCount() >= 3)
			{
				sendPacket(Msg.INVALID_TARGET);
				return;
			}
			startForceBuff(target, skill);
		}

		fireMethodInvoked(MethodCollection.onStartCast, new Object[] { skill, target, forceUse });

		setHeading(target, true);

		int level = getSkillDisplayLevel(magicId);
		if(level < 1)
			level = 1;

		int skillTime = skill.isSkillTimePermanent() ? skill.getHitTime() : Formulas.calcMAtkSpd(this, skill, skill.getHitTime());
		int skillInterruptTime = skill.isMagic() ? Formulas.calcMAtkSpd(this, skill, skill.getSkillInterruptTime()) : 0;

		_animationEndTime = System.currentTimeMillis() + skillTime;

		if(skill.isMagic() && !skill.isSkillTimePermanent() && getChargedSpiritShot() > 0)
		{
			skillTime = (int) (0.70 * skillTime);
			skillInterruptTime = (int) (0.70 * skillInterruptTime);
		}

		Formulas.calcSkillMastery(skill, this); // Calculate skill mastery for
		// current cast
		long reuseDelay = Formulas.calcSkillReuseDelay(this, skill);

		if(reuseDelay < 500)
			reuseDelay = 500;

		// reuseDelay = ((long) Math.floor(reuseDelay / 1000)) * 1000;
		broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), level, skillTime, reuseDelay));

		addSkillTimeStamp(skill.getId(), reuseDelay);
		disableItem(skill, reuseDelay, reuseDelay);
		disableSkill(skill.getId(), reuseDelay);

		if(isPlayer())
			if(!skill.isHandler())
				sendPacket(new SystemMessage(SystemMessage.YOU_USE_S1).addSkillName(magicId, (short) level));
			else
				sendPacket(new SystemMessage(SystemMessage.YOU_USE_S1).addItemName(skill.getItemConsumeId()[0]));

		if(skill.getTargetType() == L2Skill.SkillTargetType.TARGET_HOLY)
			target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, this, 1);

		double mpConsume1 = skill.isUsingWhileCasting() ? skill.getMpConsume() : skill.getMpConsume1();
		final L2Skill.SkillType skillType = skill.getSkillType();

		if(mpConsume1 > 0)
			if(skillType == L2Skill.SkillType.MUSIC)
			{
				final double inc = mpConsume1 / 2;
				double add = 0;
				for(final L2Effect e : getEffectList().getAllEffects())
					if(e.getSkill().getId() != skill.getId() && (e.getSkill().getSkillType() == L2Skill.SkillType.MUSIC) && e.getTimeLeft() > 30000)
						add += inc;
				mpConsume1 += add;
				mpConsume1 = calcStat(Stats.MP_DANCE_SKILL_CONSUME, mpConsume1, null, skill);
			}
			else if(skill.isMagic())
				reduceCurrentMp(calcStat(Stats.MP_MAGIC_SKILL_CONSUME, mpConsume1, null, skill), null);
			else
				reduceCurrentMp(calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, mpConsume1, null, skill), null);

		_flyLoc = null;
		if(skill.getFlyType() != FlyType.NONE)
		{
			final Location flyLoc = setFlyLocation(this, target, skill);
			if(flyLoc != null)
			{
				_flyLoc = flyLoc;
				broadcastPacket(new FlyToLocation(this, flyLoc, skill.getFlyType()));
			}
			else
			{
				sendPacket(Msg.CANNOT_SEE_TARGET);
				return;
			}
		}

		_castingSkill = skill;
		_castInterruptTime = System.currentTimeMillis() + skillInterruptTime;
		setCastingTarget(target);

		if(skill.isUsingWhileCasting())
			callSkill(skill, skill.getTargets(this, target, forceUse), true);

		if(skillTime > 50)
		{
			if(isPlayer() && !forceBuff)
				sendPacket(new SetupGauge(SetupGauge.BLUE, skillTime));

			// Create a task MagicUseTask with Medium priority to launch the MagicSkill at the end of the casting time
			_skillLaunchedTask = ThreadPoolManager.getInstance().scheduleAi(new MagicLaunchedTask(forceUse), skillInterruptTime, isPlayer() || isPet() || isSummon());
			_skillTask = ThreadPoolManager.getInstance().scheduleAi(new MagicUseTask(forceUse), skillTime, isPlayer() || isPet() || isSummon());
		}
		else
			onMagicUseTimer(target, skill, forceUse);
	}

	private Location _flyLoc;

	private Location setFlyLocation(final L2Character actor, final L2Object target, final L2Skill skill)
	{
		if(skill.oneTarget() && !(target == actor || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF) || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_AREA || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_MULTIFACE)
		{
			if(!GeoEngine.canMoveToCoord(actor.getX(), actor.getY(), actor.getZ(), target.getX(), target.getY(), target.getZ()))
				return null;
			return target.getLoc();
		}
		final int radius = skill.getFlyRadius();
		final double angle = Util.convertHeadingToDegree(actor.getHeading());
		final double radian = Math.toRadians(angle - 90);
		final int x1 = -(int) (Math.sin(radian) * radius);
		final int y1 = (int) (Math.cos(radian) * radius);
		final Location flyLoc = new Location(actor.getX() + x1, actor.getY() + y1, actor.getZ());
		flyLoc.z = GeoEngine.getHeight(flyLoc);
		if(flyLoc.x == 0 || flyLoc.y == 0 || flyLoc.z == 0)
			return null;
		return GeoEngine.moveCheck(actor.getX(), actor.getY(), actor.getZ(), flyLoc.x, flyLoc.y);
	}

	public void startForceBuff(final L2Character target, final L2Skill skill)
	{
		if(_forceBuff == null)
			_forceBuff = new ForceBuff(this, target, skill);
	}

	public ForceBuff getForceBuff()
	{
		return _forceBuff;
	}

	public void setForceBuff(final ForceBuff value)
	{
		_forceBuff = value;
	}

	public void addNotifyQuestOfDeath(final QuestState qs)
	{
		if(qs == null || _NotifyQuestOfDeathList != null && _NotifyQuestOfDeathList.contains(qs))
			return;
		if(_NotifyQuestOfDeathList == null)
			_NotifyQuestOfDeathList = new ArrayList<QuestState>();
		_NotifyQuestOfDeathList.add(qs);
	}

	public void doDie(final L2Character killer)
	{
		fireMethodInvoked(MethodCollection.doDie, new Object[] { killer });

		setTarget(null);
		stopMove();

		_currentHp = 0;

		if(isPlayer() && killer instanceof L2Playable)
			_currentCp = 0;

		setMassUpdating(true);
		if(isBlessedByNoblesse() || isSalvation())
		{
			if(isSalvation() && !getPlayer().isInOlympiadMode())
				getPlayer().reviveRequest(getPlayer(), 100, false);

			for(L2Effect e : getEffectList().getAllEffects())
			{
				if(e.getEffectType() == L2Effect.EffectType.BlessNoblesse || e.getSkill().getId() == 1325 || e.getSkill().getId() == 2168)
					e.exit();
			}
		}
		else
		{
			for(L2Effect e : getEffectList().getAllEffects())
			{
				if(e.getSkill().getId() != 5041 && e.getSkill().getId() != 5660)
					e.exit();
			}
		}
		setMassUpdating(false);
		sendChanges();
		updateEffectIcons();

		broadcastStatusUpdate();

		ThreadPoolManager.getInstance().executeGeneral(new NotifyAITask(CtrlEvent.EVT_DEAD));

		final Object[] script_args = new Object[] { this, killer };
		for(final ScriptClassAndMethod handler : Scripts.onDie)
			Scripts.callScripts(handler.scriptClass, handler.method,this, script_args);

		L2NpcInstance npc = null;
		if(killer instanceof L2NpcInstance)
			npc = (L2NpcInstance) killer;

		if(_NotifyQuestOfDeathList != null)
		{
			for(final QuestState qs : _NotifyQuestOfDeathList)
				qs.getQuest().notifyDeath(npc, this, qs);
			_NotifyQuestOfDeathList = null;
		}
	}

	/** Sets HP, MP and CP and revives the L2Character. */
	public void doRevive()
	{
		if(!isTeleporting())
		{
			setIsPendingRevive(false);

			if(isSalvation())
			{
				for(final L2Effect e : getEffectList().getAllEffects())
					if(e.getEffectType() == L2Effect.EffectType.Salvation)
					{
						e.exit();
						break;
					}
				setCurrentCp(getMaxCp());
				setCurrentHp(getMaxHp(), true);
				setCurrentMp(getMaxMp());
			}
			else
			{
				if(isPlayer() && Config.RESPAWN_RESTORE_CP >= 0)
					setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);

				setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP, true);

				if(Config.RESPAWN_RESTORE_MP >= 0)
					setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_MP);
			}

			broadcastPacket(new Revive(this));
		}
		else
			setIsPendingRevive(true);
	}

	public void enableSkill(final Integer skillId)
	{
		if(_disabledSkills == null)
			return;

		_disabledSkills.remove(skillId);
		removeSkillTimeStamp(skillId);
	}

	/**
	 * Return a map of 32 bits (0x00000000) containing all abnormal effect
	 */
	public int getAbnormalEffect()
	{
		int ae = _abnormalEffects;
		if(isStunned())
			ae |= ABNORMAL_EFFECT_STUN;
		if(isRooted())
			ae |= ABNORMAL_EFFECT_ROOT;
		if(isSleeping())
			ae |= ABNORMAL_EFFECT_SLEEP;
		if(isConfused())
			ae |= ABNORMAL_EFFECT_CONFUSED;
		if(isMuted() || isPMuted())
			ae |= ABNORMAL_EFFECT_MUTED;
		if(isAfraid())
			ae |= ABNORMAL_EFFECT_AFFRAID;
		if(getEffectList().getEffectByType(L2Effect.EffectType.SilentMove) != null)
			ae |= ABNORMAL_EFFECT_SILENT_MOVE;
		if(isBigHead())
			ae |= ABNORMAL_EFFECT_BIG_HEAD;
		if(getEffectList().getEffectByType(L2Effect.EffectType.Vitality) != null)
			ae |= ABNORMAL_EFFECT_VITALITY_HERB;
		if(getCustomEffect() > 0)
			ae |= getCustomEffect();
		return ae;
	}

	public int getAccuracy()
	{
		//return (int) (calcStat(Stats.ACCURACY_COMBAT, 0, null, null) / getWeaponExpertisePenalty());
		return isPlayer() ? Balancer.getModify(bflag.accuracy, (int) calcStat(Stats.ACCURACY_COMBAT, 0, null, null), getPlayer().getClassId().getId()) : (int) calcStat(Stats.ACCURACY_COMBAT, 0, null, null);
	}

	/**
	 * Возвращает тип атакующего элемента и его силу.
	 * 
	 * @return массив, в котором: <li>[0]: тип элемента, <li>[1]: его сила
	 */
	public int[] getAttackElement()
	{
		return Formulas.calcAttackElement(this);
	}

	/**
	 * Возвращает защиту от элемента: огонь.
	 * 
	 * @return значение защиты
	 */
	public int getDefenceFire()
	{
		return (int) (-calcStat(Stats.FIRE_RECEPTIVE, 0, null, null));
	}

	/**
	 * Возвращает защиту от элемента: вода.
	 * 
	 * @return значение защиты
	 */
	public int getDefenceWater()
	{
		return (int) (-calcStat(Stats.WATER_RECEPTIVE, 0, null, null));
	}

	/**
	 * Возвращает защиту от элемента: воздух.
	 * 
	 * @return значение защиты
	 */
	public int getDefenceWind()
	{
		return (int) (-calcStat(Stats.WIND_RECEPTIVE, 0, null, null));
	}

	/**
	 * Возвращает защиту от элемента: земля.
	 * 
	 * @return значение защиты
	 */
	public int getDefenceEarth()
	{
		return (int) (-calcStat(Stats.EARTH_RECEPTIVE, 0, null, null));
	}

	/**
	 * Возвращает защиту от элемента: свет.
	 * 
	 * @return значение защиты
	 */
	public int getDefenceHoly()
	{
		return (int) (-calcStat(Stats.SACRED_RECEPTIVE, 0, null, null));
	}

	/**
	 * Возвращает защиту от элемента: тьма.
	 * 
	 * @return значение защиты
	 */
	public int getDefenceUnholy()
	{
		return (int) (-calcStat(Stats.UNHOLY_RECEPTIVE, 0, null, null));
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2CharacterAI(this);
		return _ai;
	}

	/**
	 * Возвращает коллекцию скиллов для быстрого перебора
	 */
	public Collection<L2Skill> getAllSkills()
	{
		return _skills.values();
	}

	/**
	 * Возвращает массив скиллов для безопасного перебора
	 */
	public final L2Skill[] getAllSkillsArray()
	{
		if(_skills == null)
			return new L2Skill[0];

		synchronized (_skills)
		{
			return _skills.values().toArray(new L2Skill[_skills.values().size()]);
		}
	}
	
	public float getArmourExpertisePenalty()
	{
		return 1.f;
	}

	public final float getAttackSpeedMultiplier()
	{
		return (float) (1.1 * getPAtkSpd() / getTemplate().basePAtkSpd);
	}

	public int getBuffLimit()
	{
		return (int) calcStat(Stats.BUFF_LIMIT, Config.ALT_BUFF_LIMIT, null, null);
	}

	public int getDanceSongLimit()
	{
		return (int) calcStat(Stats.DANCE_SONG_LIMIT, Config.ALT_DANCE_SONG_LIMIT, null, null);
	}

	public L2Skill getCastingSkill()
	{
		return _castingSkill;
	}

	public final L2Character getCharTarget()
	{
		final L2Object target = getTarget();
		if(target == null || !target.isCharacter())
			return null;
		return (L2Character) target;
	}

	public byte getCON()
	{
		return (byte) calcStat(Stats.STAT_CON, _template.baseCON, null, null);
	}

	/**
	 * Возвращает шанс физического крита (1000 == 100%)
	 */
	public int getCriticalHit(final L2Character target, final L2Skill skill)
	{
		return isPlayer() ? Balancer.getModify(bflag.criticalHit, (int) calcStat(Stats.CRITICAL_BASE, _template.baseCritRate, target, skill), getPlayer().getClassId().getId()) : (int) calcStat(Stats.CRITICAL_BASE, _template.baseCritRate, target, skill);
	}

	/**
	 * Возвращает шанс магического крита в процентах
	 */
	public double getMagicCriticalRate(final L2Character target, final L2Skill skill)
	{
		return calcStat(Stats.MCRITICAL_RATE, target, skill);
	}

	/**
	 * Return the current CP of the L2Character.
	 */
	public final double getCurrentCp()
	{
		return _currentCp;
	}

	public final double getCurrentCpRatio()
	{
		return getCurrentCp() / getMaxCp();
	}

	public final double getCurrentCpPercents()
	{
		return getCurrentCpRatio() * 100f;
	}

	public final boolean isCurrentCpFull()
	{
		return getCurrentCp() >= getMaxCp();
	}

	public final boolean isCurrentCpZero()
	{
		return getCurrentCp() < 1;
	}

	public final double getCurrentHp()
	{
		return _currentHp;
	}

	public final double getCurrentHpRatio()
	{
		return getCurrentHp() / getMaxHp();
	}

	public final double getCurrentHpPercents()
	{
		return getCurrentHpRatio() * 100f;
	}

	public final boolean isCurrentHpFull()
	{
		return getCurrentHp() >= getMaxHp();
	}

	public final boolean isCurrentHpZero()
	{
		return getCurrentHp() < 1;
	}

	public final double getCurrentMp()
	{
		return _currentMp;
	}

	public final double getCurrentMpRatio()
	{
		return getCurrentMp() / getMaxMp();
	}

	public final double getCurrentMpPercents()
	{
		return getCurrentMpRatio() * 100f;
	}

	public final boolean isCurrentMpFull()
	{
		return getCurrentMp() >= getMaxMp();
	}

	public final boolean isCurrentMpZero()
	{
		return getCurrentMp() < 1;
	}

	public Location getDestination()
	{
		return movingTo;
	}

	public byte getDEX()
	{
		return (byte) calcStat(Stats.STAT_DEX, _template.baseDEX, null, null);
	}

	public int getEvasionRate(final L2Character target)
	{
		return (int) calcStat(Stats.EVASION_RATE, 0, target, null);
	}

	@Override
	public final int getHeading()
	{
		return _heading;
	}

	/**
	 * If <b>boolean toChar is true heading calcs this->target, else target->this.
	 */
	public int getHeadingTo(final L2Object target, final boolean toChar)
	{
		if(target == null || target == this)
			return -1;

		final int dx = target.getX() - getX();
		final int dy = target.getY() - getY();
		int heading = (int) (Math.atan2(-dy, -dx) * 32768. / Math.PI + 32768);

		heading = toChar ? target.getHeading() - heading : _heading - heading;

		if(heading < 0)
			heading += 65536;
		return heading;
	}

	public TargetDirection getDirectionTo(final L2Object target, final boolean toChar)
	{
		final int targeth = getHeadingTo(target, toChar);
		if(targeth == -1)
			return TargetDirection.NONE;
		if(targeth <= 10923 || targeth >= 54613)
			return TargetDirection.BEHIND;
		if(targeth >= 21845 && targeth <= 43691)
			return TargetDirection.FRONT;
		return TargetDirection.SIDE;
	}

	public byte getINT()
	{
		return (byte) calcStat(Stats.STAT_INT, _template.baseINT, null, null);
	}

	public GArray<L2Player> getAroundPlayers(final int radius)
	{
		if(!isVisible())
			return new GArray<L2Player>(0);

		return L2World.getAroundPlayers(this, radius, 1000);
	}

	public GArray<L2Character> getAroundCharacters(final int radius, final int height)
	{
		if(!isVisible())
			return new GArray<L2Character>(0);
		return L2World.getAroundCharacters(this, radius, height);
	}

	public GArray<L2NpcInstance> getAroundNpc(final int range, final int height)
	{
		if(!isVisible())
			return new GArray<L2NpcInstance>(0);
		return L2World.getAroundNpc(this, range, height);
	}

	public boolean knowsObject(final L2Object obj)
	{
		return L2World.getAroundObjectById(this, obj.getObjectId()) != null;
	}

	public GArray<L2NpcInstance> getKnownNpc(final int range)
	{
		if(!isVisible())
			return new GArray<L2NpcInstance>(0);

		return L2World.getAroundNpc(this, range, 1000);
	}

	public final L2Skill getKnownSkill(final int skillId)
	{
		return _skills.get(skillId);
	}

	public final int getMagicalAttackRange(final L2Skill skill)
	{
		if(skill != null)
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		return getTemplate().baseAtkRange;
	}

	public int getMAtk(final L2Character target, final L2Skill skill)
	{
		if(skill != null && skill.getMatak() > 0)
			return skill.getMatak();
		return (int) calcStat(Stats.MAGIC_ATTACK, _template.baseMAtk, target, skill);
	}

	public int getMAtkSpd()
	{
		return isPlayer() ? Balancer.getModify(bflag.castSpeed, (int) calcStat(Stats.MAGIC_ATTACK_SPEED, _template.baseMAtkSpd, null, null), getPlayer().getClassId().getId()) : (int) calcStat(Stats.MAGIC_ATTACK_SPEED, _template.baseMAtkSpd, null, null);
	}

	public final int getMaxCp()
	{
		return isPlayer() ? Balancer.getModify(bflag.cp, (int) calcStat(Stats.MAX_CP, _template.baseCpMax, null, null), getPlayer().getClassId().getId()) : (int) calcStat(Stats.MAX_CP, _template.baseCpMax, null, null);
	}

	public int getMaxHp()
	{
		return isPlayer() ? Balancer.getModify(bflag.hp, (int) calcStat(Stats.MAX_HP, _template.baseHpMax, null, null), getPlayer().getClassId().getId()) : (int) calcStat(Stats.MAX_HP, _template.baseHpMax, null, null);
	}

	public int getMaxMp()
	{
		return isPlayer() ? Balancer.getModify(bflag.mp, (int) calcStat(Stats.MAX_MP, _template.baseMpMax, null, null), getPlayer().getClassId().getId()) : (int) calcStat(Stats.MAX_MP, _template.baseMpMax, null, null);
	}

	public int getMDef(final L2Character target, final L2Skill skill)
	{
		return Math.max((int) calcStat(Stats.MAGIC_DEFENCE, _template.baseMDef, null, skill), 1);
	}

	public byte getMEN()
	{
		return (byte) calcStat(Stats.STAT_MEN, _template.baseMEN, null, null);
	}

	public float getMinDistance(final L2Object obj)
	{
		float distance = getTemplate().collisionRadius;

		if(obj != null && obj.isCharacter())
			distance += ((L2Character) obj).getTemplate().collisionRadius;

		return distance;
	}

	public float getMovementSpeedMultiplier()
	{
		return getRunSpeed() * 1f / _template.baseRunSpd;
	}

	@Override
	public float getMoveSpeed()
	{
		if(isRunning())
			return getRunSpeed();

		return getWalkSpeed();
	}

	public String getName()
	{
		return _name;
	}

	/**
	 * Set the Title of the L2Character for Cursed Weapon.<BR>
	 *
	 *            The text to set as title
	 */
	public void setVisName(final String name)
	{
		_visname = name;
	}

	/**
	 * Set the Title of the L2Character for Cursed Weapon.<BR>
	 * 
	 * @param title
	 *            The text to set as title
	 */
	public void setVisTitle(final String title)
	{
		_vistitle = title;
	}

	public String getVisName()
	{
		if(_visname == null)
			return _name;
		else
			return _visname;
	}

	public String getVisTitle()
	{
		if(_vistitle == null)
			return _title;
		else
			return _vistitle;
	}

	public int getPAtk(final L2Character target)
	{
		return (int) calcStat(Stats.POWER_ATTACK, _template.basePAtk, target, null);
	}

	public int getPAtkSpd()
	{
		return (int) (calcStat(Stats.POWER_ATTACK_SPEED, _template.basePAtkSpd, null, null) / getArmourExpertisePenalty());
	}

	public int getPDef(final L2Character target)
	{
		return (int) calcStat(Stats.POWER_DEFENCE, _template.basePDef, target, null);
	}

	public final int getPhysicalAttackRange()
	{
		return (int) calcStat(Stats.POWER_ATTACK_RANGE, getTemplate().baseAtkRange, null, null);
	}

	public final int getRandomDamage()
	{
		final L2Weapon weaponItem = getActiveWeaponItem();
		if(weaponItem == null)
			return 5 + (int) Math.sqrt(getLevel());
		return weaponItem.getRandomDamage();
	}

	public double getReuseModifier(final L2Character target)
	{
		return calcStat(Stats.ATK_REUSE, 1, target, null);
	}

	public int getRunSpeed()
	{
		if(isInWater())
			return getSwimSpeed();
		return isPlayer() ? Balancer.getModify(bflag.runSpeed, getSpeed(_template.baseRunSpd), getPlayer().getClassId().getId()) : getSpeed(_template.baseRunSpd);
	}

	public final int getShldDef()
	{
		if(isPlayer())
			return (int) calcStat(Stats.SHIELD_DEFENCE, 0, null, null);
		return (int) calcStat(Stats.SHIELD_DEFENCE, _template.baseShldDef, null, null);
	}

	public final short getSkillDisplayLevel(final Integer skillId)
	{
		final L2Skill skill = _skills.get(skillId);
		if(skill == null)
			return -1;
		return skill.getDisplayLevel();
	}

	public final short getSkillLevel(final Integer skillId)
	{
		final L2Skill skill = _skills.get(skillId);
		if(skill == null)
			return -1;
		return skill.getLevel();
	}

	public byte getSkillMastery(final Integer skillId)
	{
		if(_skillMastery == null)
			return 0;
		final Byte val = _skillMastery.get(skillId);
		return val == null ? 0 : val;
	}

	public void removeSkillMastery(final Integer skillId)
	{
		if(_skillMastery != null)
			_skillMastery.remove(skillId);
	}

	public final FastTable<L2Skill> getSkillsByType(final L2Skill.SkillType type)
	{
		final FastTable<L2Skill> result = new FastTable<L2Skill>();
		for(final L2Skill sk : _skills.values())
			if(sk.getSkillType() == type)
				result.add(sk);
		return result;
	}

	public int getSpeed(final int baseSpeed)
	{
		if(isFlying())
			return Config.WYVERN_SPEED;

		if(isRiding())
			return Config.STRIDER_SPEED;

		if(isInWater())
			return getSwimSpeed();
		return (int) (calcStat(Stats.RUN_SPEED, baseSpeed, null, null) / getArmourExpertisePenalty() + 0.5);
	}

	public byte getSTR()
	{
		return (byte) calcStat(Stats.STAT_STR, _template.baseSTR, null, null);
	}

	public int getSwimSpeed()
	{
		return (int) calcStat(Stats.RUN_SPEED, Config.SWIMING_SPEED, null, null);
	}

	public L2Object getTarget()
	{
		if(_target == null)
			return null;
		final L2Object t = _target.get();
		if(t == null)
			_target = null;
		return t;
	}

	public final int getTargetId()
	{
		final L2Object _target = getTarget();
		return _target == null ? -1 : _target.getObjectId();
	}

	public L2CharTemplate getTemplate()
	{
		return _template;
	}

	public L2CharTemplate getBaseTemplate()
	{
		return _baseTemplate;
	}

	public String getTitle()
	{
		return _title;
	}

	public final int getWalkSpeed()
	{
		if(isInWater())
			return getSwimSpeed();
		return getSpeed(_template.baseWalkSpd);
	}

	public float getWeaponExpertisePenalty()
	{
		return 1.f;
	}

	public byte getWIT()
	{
		return (byte) calcStat(Stats.STAT_WIT, _template.baseWIT, null, null);
	}

	@Override
	public boolean hasAI()
	{
		return _ai != null;
	}

	public double headingToRadians(final int heading)
	{
		return (heading - 32768) / HEADINGS_IN_PI;
	}

	public final boolean isAlikeDead()
	{
		return _fakeDeath || _currentHp < 0.5;
	}

	public boolean isAttackAborted()
	{
		return _isAttackAborted;
	}

	public final boolean isAttackingNow()
	{
		return _attackEndTime > System.currentTimeMillis();
	}

	public boolean isBehindTarget()
	{
		if(getTarget() != null && getTarget().isCharacter())
		{
			final int head = getHeadingTo(getTarget(), true);
			return head != -1 && (head <= 10430 || head >= 55105);
		}
		return false;
	}

	public boolean isToSideOfTarget()
	{
		if(getTarget() != null && getTarget().isCharacter())
		{
			final int head = getHeadingTo(getTarget(), true);
			return head != -1 && (head <= 22337 || head >= 43197);
		}
		return false;
	}

	public boolean isToSideOfTarget(final L2Object target)
	{
		if(target != null && target.isCharacter())
		{
			final int head = getHeadingTo(target, true);
			return head != -1 && (head <= 22337 || head >= 43197);
		}
		return false;
	}

	public boolean isBehindTarget(final L2Object target)
	{
		if(target != null && target.isCharacter())
		{
			final int head = getHeadingTo(target, true);
			return head != -1 && (head <= 10430 || head >= 55105);
		}
		return false;
	}

	public boolean isBlessedByNoblesse()
	{
		return _isBlessedByNoblesse;
	}

	public boolean isSalvation()
	{
		return _isSalvation > 0;
	}

	public final boolean isEffectImmune()
	{
		return _buffImmunity > 0;
	}

	public boolean isDead()
	{
		return _currentHp < 0.5;
	}

	public final boolean isDropDisabled()
	{
		return _dropDisabled > System.currentTimeMillis();
	}

	public final boolean isFlying()
	{
		return _flying;
	}

	public final boolean isInCombat()
	{
		return _stanceTask != null;
	}

	/**
	 * Return True if the target is front L2Character and can be seen. degrees = 0..180, front->sides->back
	 */
	public boolean isInFront(final L2Object target, final int degrees)
	{
		final int head = getHeadingTo(target, false);
		return head <= 32768 * degrees / 180 || head >= 65536 - 32768 * degrees / 180;
	}

	public boolean isInvul()
	{
		return _isInvul;
	}

	public boolean isMageClass()
	{
		return getTemplate().baseMAtk > 3;
	}

	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}

	public final boolean isRiding()
	{
		return _riding;
	}

	public final boolean isRunning()
	{
		return _running;
	}

	public boolean isSkillDisabled(final Integer skillId)
	{
		return _disabledSkills != null && _disabledSkills.contains(skillId);
	}

	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}

	/**
	 * Возвращает позицию цели, в которой она будет через пол секунды.
	 */
	public Location getIntersectionPoint(final L2Character target)
	{
		if(!isInFront(target, 90))
			return new Location(target.getX(), target.getY(), target.getZ());
		final double angle = Util.convertHeadingToDegree(target.getHeading()); // угол в градусах
		final double radian = Math.toRadians(angle - 90); // угол в радианах
		final double range = target.getMoveSpeed() / 2; // расстояние, пройденное за 1 секунду, равно скорости. Берем половину.
		return new Location((int) (target.getX() - range * Math.sin(radian)), (int) (target.getY() + range * Math.cos(radian)), target.getZ());
	}

	public Location applyOffset(final Location point, final int offset)
	{
		if(_forestalling && isFollow && getFollowTarget() != null && getFollowTarget().isMoving)
			return getIntersectionPoint(getFollowTarget());

		if(offset <= 0)
			return point;

		final int dx = point.x - getX();
		final int dy = point.y - getY();
		final int dz = point.z - getZ();

		final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if(distance <= offset)
		{
			point.set(getX(), getY(), getZ());
			return point;
		}

		if(distance >= 1)
		{
			final double cut = offset / distance;
			point.x -= dx * cut;
			point.y -= dy * cut;
			point.z -= dz * cut;

			if(!isFlying() && !isInBoat() && !isSwimming())
				point.z = GeoEngine.getHeight(point.x, point.y, point.z);
		}

		return point;
	}

	private boolean buildPathTo(final int dest_x, final int dest_y, final int dest_z, final int offset, final boolean pathFind, final boolean _follow)
	{
		// offset = Math.min(Math.max(offset, 0), 200);
		Location dest = new Location(dest_x, dest_y, dest_z);
		if(isInBoat() || this instanceof L2BoatInstance)
		{
			_targetRecorder.clear();
			_targetRecorder.add(applyOffset(dest, _offset));
			return true;
		}

		if(isFlying())
		{
			if(GeoEngine.canSeeCoord(this, dest.x, dest.y, dest.z, true))
			{
				_targetRecorder.clear();
				_targetRecorder.add(applyOffset(dest, _offset));
				return true;
			}
			// TODO реализовать moveCheckInAir
			return false;
		}

		if(isSwimming() || isInWater() || L2World.isWater(dest))
		{
			if(GeoEngine.canSeeCoord(this, dest.x, dest.y, dest.z, false))
			{
				_targetRecorder.clear();
				_targetRecorder.add(applyOffset(dest, _offset));
				return true;
			}
			dest = applyOffset(dest, _offset);
			_offset = 0;
			final Location nextloc = GeoEngine.moveInWaterCheck(getX(), getY(), getZ(), dest.x, dest.y, dest.z);
			if(nextloc.equals(getX(), getY(), getZ()))
				return false;
			_targetRecorder.clear();
			_targetRecorder.add(nextloc);
			return true;
		}

		if(GeoEngine.canMoveToCoord(getX(), getY(), getZ(), dest.x, dest.y, dest.z))
		{
			_targetRecorder.clear();
			_targetRecorder.add(applyOffset(new Location(dest.x, dest.y, GeoEngine.getHeight(dest.x, dest.y, dest.z)), offset));
			return true;
		}

		if(pathFind)
		{
			final ArrayList<Location> targets = GeoMove.findPath(getX(), getY(), getZ(), dest.clone(), this);
			if(!targets.isEmpty())
			{
				targets.remove(0); // Первая точка нам не нужна
				targets.add(applyOffset(targets.remove(targets.size() - 1), _offset));
				_targetRecorder.clear();
				_targetRecorder.addAll(targets);
				return true;
			}
		}

		if(_follow)
			return false;
		dest = applyOffset(dest, _offset);
		_offset = 0;
		final Location nextloc = GeoEngine.moveCheck(getX(), getY(), getZ(), dest.x, dest.y);
		if(nextloc.equals(getX(), getY(), getZ()))
			return false;
		_targetRecorder.clear();
		_targetRecorder.add(nextloc);
		return true;
	}

	public boolean followToCharacter(final L2Character target, int offset, final boolean forestalling)
	{
		synchronized (_targetRecorder)
		{
			offset = Math.max(offset, 10);
			if(isFollow && target == getFollowTarget() && offset == _offset)
				return true;

			if(!hasAI())
				return false;

			if(isMovementDisabled() || target == null || isInBoat() || isSwimming())
			{
				isFollow = false;
				isMoving = false;
				sendActionFailed();
				return false;
			}

			if(Math.abs(getZ() - target.getZ()) > 1000 && !isFlying())
			{
				sendActionFailed();
				sendPacket(Msg.CANNOT_SEE_TARGET);
				return false;
			}

			getAI().clearNextAction();

			isFollow = true;
			setFollowTarget(target);
			_forestalling = forestalling;

			if(buildPathTo(target.getX(), target.getY(), target.getZ(), offset, true, !(target instanceof L2DoorInstance)))
				movingDestTempPos.set(target.getX(), target.getY(), target.getZ());
			else
			{
				sendActionFailed();
				stopMove();
				return false;
			}

			if(_moveTask != null)
			{
				_moveTask.cancel(true);
				_moveTask = null;
			}

			_offset = offset;
			moveNext(true);
			return true;
		}
	}

	public boolean moveToLocation(final Location loc, final int offset, final boolean pathfinding)
	{
		return moveToLocation(loc.x, loc.y, loc.z, offset, pathfinding);
	}

	public boolean moveToLocation(final int x_dest, final int y_dest, final int z_dest, int offset, final boolean pathfinding)
	{
		synchronized (_targetRecorder)
		{
			offset = Math.max(offset, 0);
			final Location dst_geoloc = new Location(x_dest, y_dest, z_dest).world2geo();
			if(isMoving && !isFollow && offset == _offset && movingDestTempPos.equals(dst_geoloc))
			{
				sendActionFailed();
				return true;
			}

			if(!hasAI())
				return false;

			getAI().clearNextAction();

			if(isMovementDisabled())
			{
				getAI().setNextAction(nextAction.MOVE, new Location(x_dest, y_dest, z_dest), offset, pathfinding, false);
				sendActionFailed();
				return false;
			}

			if(isPlayer())
				getAI().setIntention(AI_INTENTION_ACTIVE);

			if(buildPathTo(x_dest, y_dest, z_dest, offset, pathfinding, false))
				movingDestTempPos.set(dst_geoloc);
			else
			{
				sendActionFailed();
				return false;
			}

			if(_moveTask != null)
			{
				_moveTask.cancel(true);
				_moveTask = null;
			}

			_offset = offset;
			isFollow = false;
			moveNext(true);
		}

		return true;
	}

	/**
	 * должно вызыватся только из synchronized(_targetRecorder)
	 * 
	 * @param firstMove
	 */
	private void moveNext(final boolean firstMove)
	{
		_previousSpeed = getMoveSpeed();
		if(_previousSpeed <= 0)
		{
			stopMove();
			return;
		}

		if(firstMove)
		{
			isMoving = true;
			movingFrom.set(getX(), getY(), getZ());
			movingFrom.world2geo().geo2world();
		}
		else
		{
			setXYZ(movingTo.x, movingTo.y, movingTo.z, true);
			movingFrom.set(movingTo);
		}

		if(_targetRecorder.isEmpty())
		{
			isMoving = false;
			if(isFollow)
			{
				isFollow = false;
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_TARGET);
			}
			else
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);

			if(!isPlayer())
				validateLocation(true);
			return;
		}

		movingTo = _targetRecorder.remove(0);
		movingTo.world2geo().geo2world();

		broadcastMove(!isPlayer());
		setHeading(calcHeading(movingTo.x, movingTo.y));
		_startMoveTime = _followTimestamp = System.currentTimeMillis();
		_moveTask = ThreadPoolManager.getInstance().scheduleMove(_moveTaskRunnable.setDist(movingFrom.distance3D(movingTo)), getMoveTickInterval());
	}

	private int getMoveTickInterval()
	{
		return (int) ((isPlayer() ? 16000 : 32000) / getMoveSpeed());
	}

	private void broadcastMove(final boolean validate)
	{
		if(this instanceof L2BoatInstance)
			broadcastPacket(new VehicleDeparture((L2BoatInstance) this));
		else
		{
			if(validate)
				validateLocation(true);
			broadcastPacket(new CharMoveToLocation(this));
		}
	}

	/**
	 * Останавливает движение и рассылает ValidateLocation
	 */
	public void stopMove()
	{
		stopMove(true);
	}

	/**
	 * Останавливает движение
	 * 
	 * @param validate -
	 *            рассылать ли ValidateLocation
	 */
	public void stopMove(final boolean validate)
	{
		if(isMoving)
		{
			synchronized (_targetRecorder)
			{
				isMoving = false;
				if(_moveTask != null)
				{
					_moveTask.cancel(false);
					_moveTask = null;
				}
				_targetRecorder.clear();
			}

			broadcastPacket(new StopMove(this));
			if(validate)
				validateLocation(true);
		}

		isFollow = false;
	}

	protected boolean needStatusUpdate()
	{
		if(Config.FORCE_STATUSUPDATE)
			return true;

		if(!isNpc())
			return true;

		final double _intervalHpUpdate = getMaxHp() / 352;

		if(_lastHpUpdate == -99999999)
		{
			_lastHpUpdate = -9999999;
			return true;
		}

		if(getCurrentHp() <= 0 || getMaxHp() < 352)
			return true;

		if(_lastHpUpdate + _intervalHpUpdate < getCurrentHp() && getCurrentHp() > _lastHpUpdate)
		{
			_lastHpUpdate = getCurrentHp();
			return true;
		}

		if(_lastHpUpdate - _intervalHpUpdate > getCurrentHp() && getCurrentHp() < _lastHpUpdate)
		{
			_lastHpUpdate = getCurrentHp();
			return true;
		}
		return false;
	}

	public void onDecay()
	{
		decayMe();
		fireMethodInvoked(MethodCollection.onDecay, null);
	}

	@Override
	public void onForcedAttack(final L2Player player)
	{
		if(player.isConfused() || player.isBlocked())
		{
			player.sendActionFailed();
			return;
		}

		player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
		player.getAI().Attack(this, true);
	}

	private void onHitTimer(final L2Character target, final int damage, final boolean crit, final boolean miss, final boolean soulshot, final boolean shld, final boolean unchargeSS)
	{
		if(isAlikeDead())
		{
			sendActionFailed();
			return;
		}

		if(target.isDead() || !isInRange(target, 2000))
		{
			sendActionFailed();
			return;
		}

		if(isPlayable() && target.isPlayable() && isInZoneBattle() != target.isInZoneBattle())
		{
			final L2Player player = getPlayer();
			if(player != null)
			{
				player.sendPacket(Msg.INVALID_TARGET);
				player.sendActionFailed();
			}
			return;
		}

		// If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2Player
		if(!isAttackAborted())
		{
			// if hitted by a cursed weapon, Cp is reduced to 0, if a cursed weapon is hitted by a Hero, Cp is reduced to 0
			if(!miss && target.isPlayer() && (isCursedWeaponEquipped() || isHero() && target.isCursedWeaponEquipped()))
				target.setCurrentCp(0);

			if(target.isStunned() && Formulas.calcStunBreak(crit))
				target.getEffectList().stopEffects(L2Effect.EffectType.Stun);

			if(isPlayer())
			{
				if(crit)
					sendPacket(new SystemMessage(SystemMessage.C1_HAD_A_CRITICAL_HIT).addName(this));
				if(miss)
					sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_WENT_ASTRAY).addName(this));
				else
					sendPacket(new SystemMessage(SystemMessage.C1_HAS_GIVEN_C2_DAMAGE_OF_S3).addName(this).addName(target).addNumber(damage));
			}
			else if(this instanceof L2Summon)
				((L2Summon) this).displayHitMessage(target, damage, crit, miss);

			if(target.isPlayer())
			{
				final L2Player enemy = (L2Player) target;

				if(shld && damage > 1)
					enemy.sendPacket(Msg.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
				else if(shld && damage == 1)
					enemy.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			}

			// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
			if(!miss && damage > 0)
			{
				if(target.getForceBuff() != null)
					target.abortCast();

				target.reduceCurrentHp(damage, this, null, true, true, false, true);
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, damage);
				target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, this, 0);

				// Скиллы, кастуемые при физ атаке
				if(!target.isDead())
				{
					if(_skillsOnAction != null)
						if(crit)
						{
							final ConcurrentLinkedQueue<L2Skill> SkillsOnCrit = _skillsOnAction.get(L2Skill.TriggerActionType.CRIT);
							if(SkillsOnCrit != null)
								for(final L2Skill skill : SkillsOnCrit)
									if(crit && Rnd.chance(skill.getChanceForAction(L2Skill.TriggerActionType.CRIT)))
									{
										final L2Character aimingTarget = skill.getAimingTarget(this, target);
										final FastList<L2Character> targets = skill.getTargets(this, aimingTarget, false);
										callSkill(skill, targets, false);
									}
						}
						else
						{
							final ConcurrentLinkedQueue<L2Skill> SkillsOnAttack = _skillsOnAction.get(L2Skill.TriggerActionType.ATTACK);
							if(SkillsOnAttack != null)
								for(final L2Skill skill : SkillsOnAttack)
									if(Rnd.chance(skill.getChanceForAction(L2Skill.TriggerActionType.ATTACK)))
									{
										final L2Character aimingTarget = skill.getAimingTarget(this, target);
										final FastList<L2Character> targets = skill.getTargets(this, aimingTarget, false);
										callSkill(skill, targets, false);
									}
						}

					if(target.getTriggerableSkills() != null)
					{
						final ConcurrentLinkedQueue<L2Skill> SkillsOnUnderAttack = target.getTriggerableSkills().get(L2Skill.TriggerActionType.UNDER_ATTACK);
						if(SkillsOnUnderAttack != null)
							for(final L2Skill skill : SkillsOnUnderAttack)
								if(Rnd.chance(skill.getChanceForAction(L2Skill.TriggerActionType.UNDER_ATTACK)))
								{
									final L2Character aimingTarget = skill.getAimingTarget(target, this);
									final FastList<L2Character> targets = skill.getTargets(target, aimingTarget, false);
									target.callSkill(skill, targets, false);
								}
					}

					// Проверка на мираж
					if(getTarget() != null && isPlayer())
						if(Rnd.chance((int) target.calcStat(Stats.CANCEL_TARGET, 0, null, null)))
							setTarget(null);

					// Manage attack or cast break of the target (calculating rate, sending message...)
					if(Formulas.calcCastBreak(target, crit))
						target.breakCast(false);
				}

				if(soulshot && unchargeSS)
					unChargeShots(false);
			}

			if(miss && target.getTriggerableSkills() != null)
			{
				final ConcurrentLinkedQueue<L2Skill> SkillsOnUnderEvasion = target.getTriggerableSkills().get(L2Skill.TriggerActionType.UNDER_MISSED_ATTACK);
				if(SkillsOnUnderEvasion != null)
					for(final L2Skill skill : SkillsOnUnderEvasion)
						if(Rnd.chance(skill.getChanceForAction(L2Skill.TriggerActionType.UNDER_MISSED_ATTACK)))
						{
							final L2Character aimingTarget = skill.getAimingTarget(target, this);
							final FastList<L2Character> targets = skill.getTargets(target, aimingTarget, false);
							target.callSkill(skill, targets, false);
						}
			}
		}

		startAttackStanceTask();

		if(checkPvP(target, null))
			startPvPFlag(target);
	}

	public void onMagicUseTimer(final L2Character aimingTarget, final L2Skill skill, boolean forceUse)
	{
		if(skill == null)
		{
			sendPacket(Msg.ActionFail);
			return;
		}

		_castInterruptTime = 0;

		if(_forceBuff != null)
		{
			_forceBuff.delete();
			return;
		}

		if(skill.isUsingWhileCasting())
		{
			aimingTarget.getEffectList().stopEffect(skill.getId());
			onCastEndTime();
			return;
		}

		if(!skill.isOffensive() && getAggressionTarget() != null)
			forceUse = true;

		if(!skill.checkCondition(this, aimingTarget, forceUse, false, false))
		{
			onCastEndTime();
			return;
		}

		if(skill.getCastRange() < 32767 && !GeoEngine.canSeeTarget(this, aimingTarget, false))
		{
			sendPacket(Msg.CANNOT_SEE_TARGET);
			broadcastPacket(new MagicSkillCanceled(_objectId));
			onCastEndTime();
			return;
		}

		int level = getSkillDisplayLevel(skill.getId());
		if(level < 1)
			level = 1;

		final FastList<L2Character> targets = skill.getTargets(this, aimingTarget, forceUse);

		double mpConsume2 = skill.getMpConsume2();
		final int hpConsume = skill.getHpConsume();

		if(hpConsume > 0)
			setCurrentHp(Math.max(0, _currentHp - hpConsume), false);

		final L2Skill.SkillType skillType = skill.getSkillType();
		if(mpConsume2 > 0)
		{
			if(skillType == L2Skill.SkillType.MUSIC)
			{
				final double inc = mpConsume2 / 2;
				double add = 0;
				for(final L2Effect e : getEffectList().getAllEffects())
					if(e.getSkill().getId() != skill.getId() && (e.getSkill().getSkillType() == L2Skill.SkillType.MUSIC) && e.getTimeLeft() > 30000)
						add += inc;
				mpConsume2 += add;
				mpConsume2 = calcStat(Stats.MP_DANCE_SKILL_CONSUME, mpConsume2, null, skill);
			}
			else if(skill.isMagic())
				mpConsume2 = calcStat(Stats.MP_MAGIC_SKILL_CONSUME, mpConsume2, null, skill);
			else
				mpConsume2 = calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, mpConsume2, null, skill);

			if(_currentMp < mpConsume2 && isPlayable())
			{
				sendPacket(Msg.NOT_ENOUGH_MP);
				onCastEndTime();
				return;
			}
			reduceCurrentMp(mpConsume2, null);
		}

		callSkill(skill, targets, true);

		if(skill.getNumCharges() > 0)
			setIncreasedForce(getIncreasedForce() - skill.getNumCharges());

		final Location flyLoc = _flyLoc;
		if(flyLoc != null)
		{
			setLoc(flyLoc);
			validateLocation(true);
		}
		_flyLoc = null;

		if(isPlayer() && getTarget() != null && skill.isOffensive())
			for(final L2Character target : targets)
				if(Rnd.chance(target.calcStat(Stats.CANCEL_TARGET, 0, null, null)))
				{
					_castingSkill = null;
					_skillTask = null;
					_skillLaunchedTask = null;
					getAI().notifyEvent(EVT_FORGET_OBJECT, target);
					return;
				}

		final int skillCoolTime = Formulas.calcMAtkSpd(this, skill, skill.getCoolTime());
		if(skillCoolTime > 0)
			ThreadPoolManager.getInstance().scheduleAi(new CastEndTimeTask(this), skillCoolTime, isPlayer() || isPet() || isSummon());
		else
			onCastEndTime();
	}

	public void onCastEndTime()
	{
		_castingSkill = null;
		_skillTask = null;
		_skillLaunchedTask = null;
		_flyLoc = null;

		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING, null, null);
	}

	public void reduceCurrentHp(double i, final L2Character attacker, final L2Skill skill, final boolean awake, final boolean standUp, final boolean directHp, final boolean canReflect)
	{
		fireMethodInvoked(MethodCollection.ReduceCurrentHp, new Object[] { i, attacker, skill, awake, standUp, directHp, });

		if(attacker == null || isInvul() || isDead() || attacker.isDead())
			return;

		if(getEffectList().getEffectByType(L2Effect.EffectType.Petrification) != null || getEffectList().getEffectByType(L2Effect.EffectType.Invulnerable) != null)
		{
			attacker.sendPacket(new SystemMessage(SystemMessage.THE_ATTACK_HAS_BEEN_BLOCKED));
			return;
		}

		// 5182 = Blessing of protection, работает если разница уровней больше 10 и не в зоне осады
		if(attacker.isPlayer() && Math.abs(attacker.getLevel() - getLevel()) > 10)
		{
			// ПК не может нанести урон чару с блессингом
			if(attacker.getKarma() > 0 && getEffectList().getEffectsBySkillId(5182) != null && !isInZone(L2Zone.ZoneType.Siege))
				return;
			// чар с блессингом не может нанести урон ПК
			if(getKarma() > 0 && attacker.getEffectList().getEffectsBySkillId(5182) != null && !attacker.isInZone(L2Zone.ZoneType.Siege))
				return;
		}

		if(awake && isSleeping())
			getEffectList().stopEffects(L2Effect.EffectType.Sleep);

		if(isMeditated() && attacker != this)
			getEffectList().stopEffects(L2Effect.EffectType.Meditation);

		if(standUp && isPlayer())
		{
			standUp();
			if(isFakeDeath())
			{
				final L2Effect fakeDeath = getEffectList().getEffectByType(L2Effect.EffectType.FakeDeath);
				if(fakeDeath == null)
					stopFakeDeath();
				else if(fakeDeath.getTime() > 2000)
					getEffectList().stopEffects(L2Effect.EffectType.FakeDeath);
			}
		}

		if(attacker != this)
			startAttackStanceTask();

		if(canReflect)
			attacker.absorbAndReflect(this, skill, i);

		if(attacker instanceof L2Playable)
		{
			final L2Playable pAttacker = (L2Playable) attacker;

			// Flag the attacker if it's a L2Player outside a PvP area
			if(!isDead() && pAttacker.checkPvP(this, null))
				pAttacker.startPvPFlag(this);

			if(isMonster() && skill != null && skill.isOverhit())
			{
				// Calculate the over-hit damage
				// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
				final double overhitDmg = (_currentHp - i) * -1;
				if(overhitDmg <= 0)
				{
					setOverhitDamage(0);
					setOverhitAttacker(null);
				}
				else
				{
					setOverhitDamage(overhitDmg);
					setOverhitAttacker(attacker);
				}
			}

			double ii;
			if(!directHp)
			{
				i = _currentCp - i;
				ii = i;

				if(ii < 0)
					ii *= -1;

				if(i < 0)
					i = 0;

				setCurrentCp(i);
			}
			else
				ii = i;

			if(_currentCp == 0 || directHp)
			{
				ii = _currentHp - ii;

				if(ii < 0)
					ii = 0;

				if(isNpc())
					pAttacker.addDamage((L2NpcInstance) this, (int) (_currentHp - ii));

				setCurrentHp(ii, false);
			}
		}
		else
		{
			i = _currentHp - i;

			if(i < 0)
				i = 0;

			setCurrentHp(i, false);
		}

		if(isDead())
		{
			// killing is only possible one time
			synchronized (this)
			{
				if(_killedAlready)
					return;

				_killedAlready = true;
			}

			doDie(attacker);
		}
		
		if(isBoss())
			((L2MonsterInstance) this).checkmyHpTask();
	}

	public void reduceCurrentMp(double i, final L2Character attacker)
	{
		if(attacker != null && attacker != this)
		{
			if(isSleeping())
				getEffectList().stopEffects(L2Effect.EffectType.Sleep);
			if(isMeditated())
				getEffectList().stopEffects(L2Effect.EffectType.Meditation);
		}

		if(getEffectList().getEffectByType(L2Effect.EffectType.Petrification) != null || getEffectList().getEffectByType(L2Effect.EffectType.Invulnerable) != null)
		{
			if(attacker != null)
				attacker.sendPacket(new SystemMessage(SystemMessage.THE_ATTACK_HAS_BEEN_BLOCKED));
			return;
		}

		// 5182 = Blessing of protection, работает если разница уровней больше 10 и не в зоне осады
		if(attacker != null && attacker.isPlayer() && Math.abs(attacker.getLevel() - getLevel()) > 10)
		{
			// ПК не может нанести урон чару с блессингом
			if(attacker.getKarma() > 0 && getEffectList().getEffectsBySkillId(5182) != null && !isInZone(L2Zone.ZoneType.Siege))
				return;
			// чар с блессингом не может нанести урон ПК
			if(getKarma() > 0 && attacker.getEffectList().getEffectsBySkillId(5182) != null && !attacker.isInZone(L2Zone.ZoneType.Siege))
				return;
		}

		i = _currentMp - i;

		if(i < 0)
			i = 0;

		setCurrentMp(i);
	}

	public double relativeSpeed(final L2Object target)
	{
		return getMoveSpeed() - target.getMoveSpeed() * Math.cos(headingToRadians(getHeading()) - headingToRadians(target.getHeading()));
	}

	public void removeAllSkills()
	{
		for(final L2Skill s : getAllSkillsArray())
			removeSkill(s);
	}

	public void removeBlockStats(final FastList<Stats> stats)
	{
		if(_blockedStats != null)
		{
			_blockedStats.removeAll(stats);
			if(_blockedStats.isEmpty())
				_blockedStats = null;
		}
	}

	public L2Skill removeSkill(final L2Skill skill)
	{
		if(skill == null)
			return null;

		return removeSkillById(skill.getId());
	}

	public L2Skill removeSkillById(final Integer id)
	{
		// Remove the skill from the L2Character _skills
		final L2Skill oldSkill = _skills.remove(id);

		removeTriggerableSkill(id);

		// Remove all its Func objects from the L2Character calculator set
		if(oldSkill != null)
		{
			removeStatsOwner(oldSkill);
		}

		return oldSkill;
	}

	public ConcurrentHashMap<L2Skill.TriggerActionType, ConcurrentLinkedQueue<L2Skill>> getTriggerableSkills()
	{
		return _skillsOnAction;
	}

	public void addTriggerableSkill(final L2Skill newSkill)
	{
		for(final L2Skill.TriggerActionType e : newSkill.getTriggerActions().keySet())
		{
			if(_skillsOnAction == null)
				_skillsOnAction = new ConcurrentHashMap<L2Skill.TriggerActionType, ConcurrentLinkedQueue<L2Skill>>();
			ConcurrentLinkedQueue<L2Skill> hs = _skillsOnAction.get(e);
			if(hs == null)
			{
				hs = new ConcurrentLinkedQueue<L2Skill>();
				_skillsOnAction.put(e, hs);
			}
			hs.add(newSkill);

			if(e == L2Skill.TriggerActionType.ADD)
				if(Rnd.chance(newSkill.getChanceForAction(L2Skill.TriggerActionType.ADD)))
				{
					final L2Character aimingTarget = newSkill.getAimingTarget(this, this);
					final FastList<L2Character> targets = newSkill.getTargets(this, aimingTarget, false);
					callSkill(newSkill, targets, false);
				}
		}
	}

	public void removeTriggerableSkill(final int id)
	{
		if(_skillsOnAction != null)
			for(final ConcurrentLinkedQueue<L2Skill> s : _skillsOnAction.values())
				for(final L2Skill sk : s)
					if(sk != null && sk.getId() == id)
						s.remove(sk);
	}

	public final synchronized void removeStatFunc(final Func f)
	{
		if(f == null)
			return;

		final int stat = f._stat.ordinal();
		if(_calculators[stat] != null)
			_calculators[stat].removeFunc(f);
	}

	public final synchronized void removeStatFuncs(final Func[] funcs)
	{
		for(final Func f : funcs)
			removeStatFunc(f);
	}

	public final void removeStatsOwner(final Object owner)
	{
		for(int i = 0; i < _calculators.length; i++)
			if(_calculators[i] != null)
				_calculators[i].removeOwner(owner);
	}

	public void sendActionFailed()
	{
		sendPacket(Msg.ActionFail);
	}

	public L2CharacterAI setAI(final L2CharacterAI new_ai)
	{
		if(new_ai == null)
			return _ai = null;
		if(_ai != null)
			_ai.stopAITask();
		_ai = new_ai;
		return _ai;
	}

	public final void setCurrentHp(double newHp, final boolean canRessurect)
	{
		newHp = Math.min(getMaxHp(), Math.max(0, newHp));

		if(_currentHp == newHp)
			return;

		if(newHp >= 0.5 && isDead() && !canRessurect)
			return;

		final double hpStart = _currentHp;

		synchronized (this)
		{
			_currentHp = newHp;

			if(!isDead())
				_killedAlready = false;
		}

		startRegeneration();

		firePropertyChanged(PropertyCollection.HitPoints, hpStart, _currentHp);

		checkHpMessages(hpStart, newHp);
		broadcastStatusUpdate();
	}

	public final void setCurrentMp(double newMp)
	{
		newMp = Math.min(getMaxMp(), Math.max(0, newMp));

		if(_currentMp == newMp)
			return;

		_currentMp = newMp;

		startRegeneration();

		broadcastStatusUpdate();
	}

	public final void setCurrentCp(double newCp)
	{
		newCp = Math.min(getMaxCp(), Math.max(0, newCp));

		if(_currentCp == newCp)
			return;
		_currentCp = newCp;

		startRegeneration();
		broadcastStatusUpdate();
	}

	public void setCurrentHpMp(double newHp, double newMp, final boolean canRessurect)
	{
		newHp = Math.min(getMaxHp(), Math.max(0, newHp));
		newMp = Math.min(getMaxMp(), Math.max(0, newMp));

		if(_currentHp == newHp && _currentMp == newMp)
			return;

		if(newHp >= 0.5 && isDead() && !canRessurect)
			return;

		final double hpStart = _currentHp;

		// synchronized (this) {
		_currentHp = newHp;
		_currentMp = newMp;

		if(!isDead())
			_killedAlready = false;
		// }

		startRegeneration();
		firePropertyChanged(PropertyCollection.HitPoints, hpStart, _currentHp);
		checkHpMessages(hpStart, newHp);
		broadcastStatusUpdate();
	}

	public void setCurrentHpMp(final double newHp, final double newMp)
	{
		setCurrentHpMp(newHp, newMp, false);
	}

	public final void setFlying(final boolean mode)
	{
		_flying = mode;
	}

	public final void setHeading(final int heading)
	{
		_heading = heading;
	}

	public final void setHeading(final L2Character target, final boolean toChar)
	{
		if(target == null || target == this)
			return;
		_heading = (int) (Math.atan2(getY() - target.getY(), getX() - target.getX()) * 32768. / Math.PI) + (toChar ? 32768 : 0);
		if(_heading < 0)
			_heading += 65536;
	}

	public void setIsBlessedByNoblesse(boolean value)
	{
		_isBlessedByNoblesse = value;
	}

	public final void setIsSalvation(final boolean value)
	{
		if(value)
			_isSalvation++;
		else
			_isSalvation--;
	}

	public final void setBuffImmunity(final boolean value)
	{
		if(value)
			_buffImmunity++;
		else
			_buffImmunity--;
	}

	public void setIsInvul(final boolean b)
	{
		_isInvul = b;
	}

	public final void setIsPendingRevive(final boolean value)
	{
		_isPendingRevive = value;
	}

	public final void setIsTeleporting(final boolean value)
	{
		_isTeleporting = value;
	}

	public final void setName(final String name)
	{
		_name = name;
	}

	public L2Character getCastingTarget()
	{
		if(castingTarget == null)
			return null;
		final L2Character c = castingTarget.get();
		if(c == null)
			castingTarget = null;
		return c;
	}

	public void setCastingTarget(final L2Character target)
	{
		castingTarget = target == null ? null : new WeakReference<L2Character>(target);
	}

	public final void setRiding(final boolean mode)
	{
		_riding = mode;
	}

	public final void setRunning()
	{
		if(!_running)
		{
			_running = true;
			broadcastPacket(new ChangeMoveType(this));
		}
	}

	public void setSkillMastery(final Integer skill, final byte mastery)
	{
		if(_skillMastery == null)
			_skillMastery = new HashMap<Integer, Byte>();
		_skillMastery.put(skill, mastery);
	}

	private L2Character _aggressionTarget = null;

	public void setAggressionTarget(final L2Character target)
	{
		_aggressionTarget = target;
	}

	public L2Character getAggressionTarget()
	{
		return _aggressionTarget;
	}

	public void setTarget(L2Object object)
	{
		if(object != null && !object.isVisible())
			object = null;
		if(object == null)
		{
			if(isAttackingNow() && getAI().getAttackTarget() == getTarget())
			{
				abortAttack();
				getAI().setIntention(AI_INTENTION_ACTIVE, null, null);
				sendMessage(new CustomMessage("com.lineage.game.model.L2Character.AttackAborted", this));
				sendActionFailed();
			}
			if(isCastingNow() && canAbortCast() && getAI().getAttackTarget() == getTarget())
			{
				abortCast();
				getAI().setIntention(AI_INTENTION_ACTIVE, null, null);
				sendMessage(new CustomMessage("com.lineage.game.model.L2Character.CastingAborted", this));
				sendActionFailed();
			}
		}
		_target = object == null ? null : new WeakReference<L2Object>(object);
	}

	protected void setTemplate(final L2CharTemplate template)
	{
		_template = template;
	}

	public void setTitle(final String title)
	{
		_title = title;
	}

	public void setWalking()
	{
		if(_running)
		{
			_running = false;
			broadcastPacket(new ChangeMoveType(this));
		}
	}

	public void startAbnormalEffect(final int mask)
	{
		_abnormalEffects |= mask;
		updateAbnormalEffect();
	}

	@Override
	public void startAttackStanceTask()
	{
		if(System.currentTimeMillis() < _stanceInited + 10000)
			return;

		_stanceInited = System.currentTimeMillis();

		// Бесконечной рекурсии не будет, потому что выше проверка на _stanceInited
		if(this instanceof L2Summon && getPlayer() != null)
			getPlayer().startAttackStanceTask();
		else if(isPlayer() && getPet() != null)
			getPet().startAttackStanceTask();

		if(_stanceTask != null)
			_stanceTask.cancel(false);
		else
			broadcastPacket(new AutoAttackStart(getObjectId()));

		_stanceTask = ThreadPoolManager.getInstance().scheduleAi(new CancelAttackStance(this), 15000, isPlayer() || isPet() || isSummon());
	}

	public void stopAttackStanceTask()
	{
		broadcastPacket(new AutoAttackStop(getObjectId()));
		_stanceTask.cancel(false);
		_stanceTask = null;
	}

	public void startRegeneration()
	{
		if(!isDead() && (_currentHp < getMaxHp() || _currentMp < getMaxMp() || _currentCp < getMaxCp()))
		{
			synchronized (_regenLock)
			{
				final long tick = RegenTaskManager.getInstance().getTick();
				if(_regenTick >= tick)
					return;
				_regenTick = tick;
			}
			RegenTaskManager.getInstance().addRegenTask(this);
		}
	}

	public long _regenTick;
	private Object _regenLock = new Object();

	public void doRegen()
	{
		if(isDead() || isHealBlocked())
			return;

		try
		{
			double addHp = 0;
			double addMp = 0;

			if(_currentHp < getMaxHp())
				addHp += Formulas.calcHpRegen(this);

			if(_currentMp < getMaxMp())
				addMp += Formulas.calcMpRegen(this);

			// Added regen bonus when character is sitting
			if(isPlayer() && Config.REGEN_SIT_WAIT)
			{
				final L2Player pl = (L2Player) this;
				if(pl.isSitting())
				{
					pl.updateWaitSitTime();
					if(pl.getWaitSitTime() > 5)
					{
						addHp += pl.getWaitSitTime();
						addMp += pl.getWaitSitTime();
					}
				}
			}
			else if(isRaid())
			{
				addHp *= Config.RATE_RAID_REGEN;
				addMp *= Config.RATE_RAID_REGEN;
			}

			final double hpStart = _currentHp;

			_currentHp = Math.min(getMaxHp(), Math.max(0, _currentHp + addHp));
			_currentMp = Math.min(getMaxMp(), Math.max(0, _currentMp + addMp));

			if(isPlayer())
				_currentCp = Math.min(getMaxCp(), Math.max(0, _currentCp + Formulas.calcCpRegen(L2Character.this)));

			firePropertyChanged(PropertyCollection.HitPoints, hpStart, _currentHp);
			checkHpMessages(hpStart, _currentHp);
			broadcastStatusUpdate();
			startRegeneration();
		}
		catch(final Throwable e)
		{
			e.printStackTrace();
		}
	}

	public void stopAbnormalEffect(final int mask)
	{
		_abnormalEffects &= ~mask;
		updateAbnormalEffect();
	}

	public void block()
	{
		_blocked = true;
	}

	public void unblock()
	{
		_blocked = false;
	}

	public void startConfused()
	{
		if(!_confused)
		{
			_confused = true;
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopConfused()
	{
		if(_confused)
		{
			_confused = false;
			updateAbnormalEffect();

			breakAttack();
			breakCast(true);
			stopMove();
			getAI().setAttackTarget(null);
		}
	}

	public void startFakeDeath()
	{
		if(!_fakeDeath)
		{
			if(isPlayer())
				((L2Player) this).clearHateList(true);
			_fakeDeath = true;
			getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH, null, null);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
			updateAbnormalEffect();
		}
	}

	public void stopFakeDeath()
	{
		if(_fakeDeath)
		{
			_fakeDeath = false;
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
			updateAbnormalEffect();
		}
	}

	public void startFear()
	{
		if(!_afraid)
		{
			_afraid = true;
			breakAttack();
			breakCast(true);
			sendActionFailed();
			stopMove();
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopFear()
	{
		if(_afraid)
		{
			_afraid = false;
			updateAbnormalEffect();
		}
	}

	public void startMuted()
	{
		if(!_muted)
		{
			_muted = true;
			if(getCastingSkill() != null && getCastingSkill().isMagic())
				breakCast(true);
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopMuted()
	{
		if(_muted)
		{
			_muted = false;
			updateAbnormalEffect();
		}
	}

	public void startPMuted()
	{
		if(!_pmuted)
		{
			_pmuted = true;
			if(getCastingSkill() != null && !getCastingSkill().isMagic())
				breakCast(true);
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopPMuted()
	{
		if(_pmuted)
		{
			_pmuted = false;
			updateAbnormalEffect();
		}
	}

	public void startAMuted()
	{
		if(!_amuted)
		{
			_amuted = true;
			if(getCastingSkill() != null)
				breakCast(true);
			if(isAttackingNow())
				breakAttack();
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopAMuted()
	{
		if(_amuted)
		{
			_amuted = false;
			updateAbnormalEffect();
		}
	}

	public void startRooted()
	{
		if(!_rooted)
		{
			_rooted = true;
			getAI().clientStopMoving();
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopRooting()
	{
		if(_rooted)
		{
			_rooted = false;
			updateAbnormalEffect();
		}
	}

	public void startSleeping()
	{
		if(!_sleeping)
		{
			_sleeping = true;
			breakAttack();
			breakCast(true);
			sendActionFailed();
			stopMove();
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopSleeping()
	{
		if(_sleeping)
		{
			_sleeping = false;
			updateAbnormalEffect();
		}
	}

	public void startStunning()
	{
		if(!_stunned)
		{
			_stunned = true;
			breakAttack();
			breakCast(true);
			sendActionFailed();
			stopMove();
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopStunning()
	{
		if(_stunned)
		{
			_stunned = false;
			updateAbnormalEffect();
		}
	}

	public void setMeditated(final boolean meditated)
	{
		_meditated = meditated;
	}

	public void setParalyzed(final boolean paralyzed)
	{
		if(_paralyzed != paralyzed)
		{
			_paralyzed = paralyzed;
			if(paralyzed)
			{
				breakAttack();
				breakCast(true);
				sendActionFailed();
				stopMove();
			}
		}
	}

	public void setImobilised(final boolean imobilised)
	{
		if(_imobilised != imobilised)
		{
			_imobilised = imobilised;
			if(imobilised)
				stopMove();
			updateAbnormalEffect();
		}
	}

	public void setHealBlocked(final boolean value)
	{
		if(_healBlocked != value)
			_healBlocked = value;
	}

	public void setBigHead(final boolean bigHead)
	{
		if(_bigHead != bigHead)
		{
			_bigHead = bigHead;
			updateAbnormalEffect();
		}
	}

	/**
	 * if True, the L2Player can't take more item
	 */
	public void setOverloaded(final boolean overloaded)
	{
		_overloaded = overloaded;
	}

	public boolean isConfused()
	{
		return _confused;
	}

	public boolean isFakeDeath()
	{
		return _fakeDeath;
	}

	public boolean isAfraid()
	{
		return _afraid;
	}

	public boolean isBlocked()
	{
		return _blocked;
	}

	public boolean isMuted()
	{
		return _muted;
	}

	public boolean isPMuted()
	{
		return _pmuted;
	}

	public boolean isAMuted()
	{
		return _amuted;
	}

	public boolean isRooted()
	{
		return _rooted;
	}

	public boolean isSleeping()
	{
		return _sleeping;
	}

	public boolean isStunned()
	{
		return _stunned;
	}

	public boolean isMeditated()
	{
		return _meditated;
	}

	public boolean isParalyzed()
	{
		return _paralyzed;
	}

	public boolean isImobilised()
	{
		return _imobilised || getRunSpeed() < 1;
	}

	public boolean isHealBlocked()
	{
		return _healBlocked;
	}

	public boolean isBigHead()
	{
		return _bigHead;
	}

	public boolean isCastingNow()
	{
		return _skillTask != null;
	}

	public boolean isMovementDisabled()
	{
		return isSitting() || isStunned() || isRooted() || isSleeping() || isParalyzed() || isImobilised() || isAlikeDead() || isAttackingNow() || isCastingNow() || _overloaded || _fishing;
	}

	public boolean isActionsDisabled()
	{
		return isStunned() || isSleeping() || isParalyzed() || isAttackingNow() || isCastingNow() || isInBoat() || isAlikeDead();
	}

	public boolean isPotionsDisabled()
	{
		return getTeam()>0 || isStunned() || isSleeping() || isParalyzed() || isAlikeDead() || isInBoat();
	}

	public boolean isToggleDisabled()
	{
		return isStunned() || isSleeping() || isParalyzed();
	}

	public final boolean isAttackingDisabled()
	{
		return _attackReuseEndTime > System.currentTimeMillis();
	}

	public boolean isOutOfControl()
	{
		return isConfused() || isAfraid() || isBlocked();
	}

	public void teleToLocation(final Location loc)
	{
		teleToLocation(loc.x, loc.y, loc.z, false);
	}

	public void teleToLocation(final Location loc, final boolean instant)
	{
		teleToLocation(loc.x, loc.y, loc.z, instant);
	}

	public void teleToLocation(final Location loc, final int ref)
	{
		teleToLocation(loc.x, loc.y, loc.z, ref, false);
	}

	public void teleToLocation(final Location loc, final int ref, final boolean instant)
	{
		teleToLocation(loc.x, loc.y, loc.z, ref, instant);
	}

	public void teleToLocation(final int x, final int y, final int z)
	{
		teleToLocation(x, y, z, getReflection().getId(), false);
	}

	public void teleToLocation(final int x, final int y, final int z, final boolean instant)
	{
		teleToLocation(x, y, z, getReflection().getId(), instant);
	}

	public void teleToLocation(final int x, final int y, final int z, final int ref)
	{
		teleToLocation(x, y, z, ref, false);
	}

	public void teleToLocation(int x, int y, int z, final int ref, final boolean instant)
	{
		if((isPlayer() || isPet() || isSummon()) && !instant)
			clearHateList(true);

		z = GeoEngine.getHeight(x, y, z);

		if(isPlayer() && DimensionalRiftManager.getInstance().checkIfInRiftZone(getLoc(), true))
		{
			final L2Player player = (L2Player) this;
			if(player.isInParty() && player.getParty().isInDimensionalRift())
			{
				final Location newCoords = DimensionalRiftManager.getInstance().getRoom(0, 0).getTeleportCoords();
				x = newCoords.x;
				y = newCoords.y;
				z = newCoords.z;
				player.getParty().getDimensionalRift().usedTeleport(player);
			}
		}

		if(!instant)
			setTarget(null);

		if(isPlayer())
		{
			final L2Player player = (L2Player) this;

			if(player.isInBoat())
				player.setBoat(null);

			if(instant)
			{
				setXYZ(x, y, z);
				setLastClientPosition(getLoc());
				setLastServerPosition(getLoc());
				validateLocation(true); // TODO а нужно ли?
				final L2TamedBeastInstance trained_beast = player.getTrainedBeast();
				if(trained_beast != null)
					trained_beast.setLoc(Location.getAroundPosition(player, trained_beast, 50, 100, 10));
				return;
			}

			decayMe();
			setXYZInvisible(x, y, z);

			if(player.isLogoutStarted())
				return;

			if(ref != getReflection().getId())
				setReflection(ref);

			setIsTeleporting(true);

			// Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
			setLastClientPosition(null);
			setLastServerPosition(null);

			player.sendPacket(new TeleportToLocation(player, x, y, z));
		}
		else
		{
			setXYZ(x, y, z);
			broadcastPacket(new TeleportToLocation(this, x, y, z));
		}
	}

	public void onTeleported()
	{
		if(isPlayer())
		{
			spawnMe();

			final L2Player player = (L2Player) this;

			setLastClientPosition(getLoc());
			setLastServerPosition(getLoc());

			setIsTeleporting(false);

			if(_isPendingRevive)
				doRevive();

			if(player.getTrainedBeast() != null)
				player.getTrainedBeast().setXYZ(getX() + Rnd.get(-100, 100), getY() + Rnd.get(-100, 100), getZ());

			sendActionFailed();
		}
	}

	public void teleToClosestTown()
	{
		teleToLocation(MapRegion.getTeleToClosestTown(this));
	}

	public void teleToSecondClosestTown()
	{
		teleToLocation(MapRegion.getTeleToSecondClosestTown(this));
	}

	public void teleToCastle()
	{
		teleToLocation(MapRegion.getTeleToCastle(this));
	}

	public void teleToClanhall()
	{
		teleToLocation(MapRegion.getTeleToClanHall(this));
	}

	public void teleToHeadquarter()
	{
		teleToLocation(MapRegion.getTeleToHeadquarter(this));
	}

	public void sendMessage(final CustomMessage message)
	{
		sendMessage(message.toString());
	}

	private long _nonAggroTime;

	public long getNonAggroTime()
	{
		return _nonAggroTime;
	}

	public void setNonAggroTime(final long time)
	{
		_nonAggroTime = time;
	}

	public int getCustomEffect()
	{
		return _customEffect;
	}

	public void setCustomEffect(final int effect)
	{
		_customEffect = effect;
	}

	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}

	@Override
	public float getColRadius()
	{
		return getTemplate().collisionRadius;
	}

	@Override
	public float getColHeight()
	{
		return getTemplate().collisionHeight;
	}

	public boolean canAttackCharacter(final L2Character _target)
	{
		return _target.getPlayer() != null;
	}

	public class HateInfo
	{
		public L2NpcInstance npc;
		public int hate;
		public int damage;

		HateInfo(final L2NpcInstance attacker)
		{
			npc = attacker;
		}
	}

	private ConcurrentHashMap<L2NpcInstance, HateInfo> _hateList = null;

	public void addDamage(final L2NpcInstance npc, final int damage)
	{
		addDamageHate(npc, damage, damage);
	}

	public void addDamageHate(final L2NpcInstance npc, final int damage, int aggro)
	{
		if(damage <= 0 && aggro <= 0)
			return;

		if(damage > 0 && aggro <= 0)
			aggro = damage;

		if(npc == null)
			return;

		if(_hateList == null)
			_hateList = new ConcurrentHashMap<L2NpcInstance, HateInfo>();

		HateInfo ai = _hateList.get(npc);

		if(ai != null)
		{
			ai.damage += damage;
			ai.hate += aggro;
			if(ai.hate < 0)
				ai.hate = 0;
		}
		else if(aggro > 0)
		{
			ai = new HateInfo(npc);
			ai.damage = damage;
			ai.hate = aggro;
			_hateList.put(npc, ai);
		}
	}

	public ConcurrentHashMap<L2NpcInstance, HateInfo> getHateList()
	{
		if(_hateList == null)
			return new ConcurrentHashMap<L2NpcInstance, HateInfo>();
		return _hateList;
	}

	public void removeFromHatelist(final L2NpcInstance npc, final boolean onlyHate)
	{
		if(npc != null && _hateList != null)
			if(onlyHate)
			{
				final HateInfo i = _hateList.get(npc);
				if(i != null)
					i.hate = 0;
			}
			else
				_hateList.remove(npc);
	}

	public void clearHateList(final boolean onlyHate)
	{
		if(_hateList != null)
			if(onlyHate)
				for(final HateInfo i : _hateList.values())
					i.hate = 0;
			else
				_hateList = null;
	}

	public EffectList getEffectList()
	{
		if(_effectList == null)
			_effectList = new EffectList(this);
		return _effectList;
	}

	public void setEffectList(final EffectList el)
	{
		_effectList = el;
	}

	public boolean isMassUpdating()
	{
		return _massUpdating;
	}

	public void setMassUpdating(final boolean updating)
	{
		_massUpdating = updating;
	}

	public boolean paralizeOnAttack(final L2Character attacker)
	{
		// Mystic Immunity Makes a target temporarily immune to raid curce
		// if(attacker.getEffectList().getEffectsBySkillId(L2Skill.SKILL_MYSTIC_IMMUNITY) != null)
		// return false;

		int max_attacker_level = 0xFFFF;

		if(isRaid() || (this instanceof L2MinionInstance && ((L2MinionInstance) this).getLeader().isRaid()))
			max_attacker_level = getLevel() + Config.RAID_MAX_LEVEL_DIFF;
		else if(hasAI() && getAI() instanceof DefaultAI)
		{
			final int max_level_diff = ((DefaultAI) getAI()).getInt("ParalizeOnAttack", -1000);
			if(max_level_diff != -1000)
				max_attacker_level = getLevel() + max_level_diff;
		}

		if(attacker.getLevel() > max_attacker_level)
		{
			if(max_attacker_level > 0)
				attacker.sendMessage(new CustomMessage("com.lineage.game.model.L2Character.ParalizeOnAttack", attacker).addCharName(this).addNumber(max_attacker_level));
			return true;
		}

		return false;
	}

	public Calculator[] getCalculators()
	{
		return _calculators;
	}

	// ---------------------------- Not Implemented -------------------------------

	public void addExpAndSp(final long addToExp, final long addToSp)
	{}

	public void addExpAndSp(final long addToExp, final long addToSp, final boolean applyBonus, final boolean appyToPet)
	{}

	public void addSkillTimeStamp(final Integer s, final long r)
	{}

	public void broadcastUserInfo(final boolean force)
	{}

	public void checkHpMessages(final double currentHp, final double newHp)
	{}

	public boolean checkPvP(final L2Character target, final L2Skill skill)
	{
		return false;
	}

	public boolean consumeItem(final int itemConsumeId, final int itemCount)
	{
		return true;
	}

	public void doPickupItem(final L2Object object)
	{}

	public boolean isFearImmune()
	{
		return false;
	}

	public boolean isLethalImmune()
	{
		return getMaxHp() >= 50000;
	}

	public boolean getChargedSoulShot()
	{
		return false;
	}

	public int getChargedSpiritShot()
	{
		return 0;
	}

	public Duel getDuel()
	{
		return null;
	}

	public int getIncreasedForce()
	{
		return 0;
	}

	public int getConsumedSouls()
	{
		return 0;
	}

	public int getKarma()
	{
		return 0;
	}

	public double getLevelMod()
	{
		return 1;
	}

	public int getNpcId()
	{
		return 0;
	}

	public L2Summon getPet()
	{
		return null;
	}

	public int getPvpFlag()
	{
		return 0;
	}

	public int getTeam()
	{
		return 0;
	}

	public boolean isSitting()
	{
		return false;
	}

	public boolean isUndead()
	{
		return false;
	}

	public boolean isUsingDualWeapon()
	{
		return false;
	}

	public boolean isParalyzeImmune()
	{
		return false;
	}

	public void reduceArrowCount()
	{}

	public void removeSkillTimeStamp(final Integer s)
	{}

	public void sendChanges()
	{}

	public void sendMessage(final String message)
	{}

	public void sendPacket(final L2GameServerPacket mov)
	{}

	public void setIncreasedForce(final int i)
	{}

	public void setConsumedSouls(final int i, final L2NpcInstance monster)
	{}

	public void sitDown()
	{}

	public void standUp()
	{}

	public void startPvPFlag(final L2Character target)
	{}

	public boolean unChargeShots(final boolean spirit)
	{
		return false;
	}

	public void updateEffectIcons()
	{}

	public void updateStats()
	{}

	public void callMinionsToAssist(final L2Character attacker)
	{}

	public void setOverhitAttacker(final L2Character attacker)
	{}

	public void setOverhitDamage(final double damage)
	{}

	public boolean hasMinions()
	{
		return false;
	}

	public boolean isCursedWeaponEquipped()
	{
		return false;
	}

	public boolean isHero()
	{
		return false;
	}

	public int getAccessLevel()
	{
		return 0;
	}

	public void spawnWayPoints(final Vector<Location> recorder)
	{}

	public void setFollowStatus(final boolean b)
	{}

	public void setLastClientPosition(final Location charPosition)
	{}

	public void setLastServerPosition(final Location charPosition)
	{}

	public boolean hasRandomAnimation()
	{
		return true;
	}

	public boolean hasRandomWalk()
	{
		return true;
	}

	public int getClanCrestId()
	{
		return 0;
	}

	public int getClanCrestLargeId()
	{
		return 0;
	}

	public int getAllyCrestId()
	{
		return 0;
	}

	public void disableItem(final L2Skill handler, final long timeTotal, final long timeLeft)
	{}

	public float getRateAdena()
	{
		return 1.0f;
	}

	public float getRateItems()
	{
		return 1.0f;
	}

	public float getRateExp()
	{
		return 1.0f;
	}

	public float getRateSp()
	{
		return 1.0f;
	}

	public float getRateSpoil()
	{
		return 1.0f;
	}

	@Override
	public void setXYZ(final int x, final int y, final int z)
	{
		setXYZ(x, y, z, false);
	}

	@Override
	public void setXYZInvisible(final int x, final int y, final int z)
	{
		if(hasAI())
			stopMove();
		super.setXYZInvisible(x, y, z);
	}

	public void setXYZ(final int x, final int y, final int z, final boolean MoveTask)
	{
		if(!MoveTask && hasAI())
			stopMove();
		super.setXYZ(x, y, z);
		updateTerritories();
	}

	public void validateLocation(final boolean broadcast)
	{
		 L2GameServerPacket sp = new ValidateLocation(this); // FIXME для кораблей что-то иное
		if(broadcast)
			broadcastPacket(sp);
		else
			sendPacket(sp);
	}

	// --------------------------- End Of Not Implemented ------------------------------

	// --------------------------------- Abstract --------------------------------------

	public abstract byte getLevel();

	public abstract void updateAbnormalEffect();

	public abstract L2ItemInstance getActiveWeaponInstance();

	public abstract L2Weapon getActiveWeaponItem();

	public abstract L2ItemInstance getSecondaryWeaponInstance();

	public abstract L2Weapon getSecondaryWeaponItem();

	// ----------------------------- End Of Abstract -----------------------------------

	private static class MoveNextRunnable implements Runnable
	{
		private final WeakReference<L2Character> character_ref;
		private float alldist, donedist;

		public MoveNextRunnable(final L2Character _character)
		{
			character_ref = new WeakReference<L2Character>(_character);
		}

		public MoveNextRunnable setDist(final double dist)
		{
			alldist = (float) dist;
			donedist = 0;
			return this;
		}

		public void run()
		{
			L2Character follow_target = null;
			final L2Character character = character_ref.get();
			if(character == null)
				return;
			synchronized (character._targetRecorder)
			{
				final float speed = character.getMoveSpeed();
				if(speed <= 0)
				{
					character.stopMove();
					return;
				}
				final long now = System.currentTimeMillis();

				if(character.isFollow)
				{
					follow_target = character.getFollowTarget();
					if(follow_target == null)
					{
						character.stopMove();
						return;
					}
					if(character.isInRangeZ(follow_target, character._offset))
					{
						character.stopMove();
						character.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_TARGET);
						if(!character.isPlayer())
							character.validateLocation(true);
						return;
					}
				}

				donedist += (now - character._startMoveTime) * character._previousSpeed / 1000f;
				final double done = donedist / alldist;
				if(done >= 1)
				{
					character.moveNext(false);
					return;
				}

				final int _x = character.movingFrom.x + (int) ((character.movingTo.x - character.movingFrom.x) * done + 0.5f);
				final int _y = character.movingFrom.y + (int) ((character.movingTo.y - character.movingFrom.y) * done + 0.5f);
				int _z;
				if(character.isFlying() || character.isInBoat() || character.isSwimming())
					_z = character.movingFrom.z + (int) ((character.movingTo.z - character.movingFrom.z) * done + 0.5f);
				else
				{
					_z = GeoEngine.getHeight(_x, _y, character.getZ());
					if(_z - character.getZ() > 256)
					{
						final String bug_text = "geo bug 1 at: " + character.getLoc() + " => " + _x + "," + _y + "," + _z + "\tAll path: " + character.movingFrom + " => " + character.movingTo;
						Log.add(bug_text, "geo");
						if(character.isPlayer() && character.getAccessLevel() >= 100)
							character.sendMessage(bug_text);
						character.stopMove();
						return;
					}
				}

				character.setXYZ(_x, _y, _z, true);
				if(character.isFollow && now - character._followTimestamp > (character._forestalling ? 500 : 1000) && !follow_target.isInRange(character.movingDestTempPos, character._offset + 100))
				{
					if(Math.abs(character.getZ() - _z) > 1000 && !character.isFlying())
					{
						character.sendPacket(Msg.CANNOT_SEE_TARGET);
						character.stopMove();
						return;
					}
					if(character.buildPathTo(follow_target.getX(), follow_target.getY(), follow_target.getZ(), character._offset, true, true))
						character.movingDestTempPos.set(follow_target.getX(), follow_target.getY(), follow_target.getZ());
					else
					{
						character.stopMove();
						return;
					}
					character.moveNext(true);
					return;
				}

				character._previousSpeed = speed;
				character._startMoveTime = now;
				character._moveTask = ThreadPoolManager.getInstance().scheduleMove(character._moveTaskRunnable, character.getMoveTickInterval());
			}
		}
	}

	/**
	 * Check if this object is inside the given plan radius around the given point. Warning: doesn't cover collision radius!<BR> <BR>
	 * 
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param radius
	 *            the radius around the target
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(final int x, final int y, final int radius, final boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}

	/**
	 * Check if this object is inside the given radius around the given point.<BR> <BR>
	 * 
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param z
	 *            Z position of the target
	 * @param radius
	 *            the radius around the target
	 * @param checkZ
	 *            should we check Z axis also
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(final int x, final int y, final int z, final int radius, final boolean checkZ, final boolean strictCheck)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		final double dz = z - getZ();

		if(strictCheck)
		{
			if(checkZ)
				return (dx * dx + dy * dy + dz * dz) < radius * radius;

			return (dx * dx + dy * dy) < radius * radius;
		}

		if(checkZ)
			return (dx * dx + dy * dy + dz * dz) <= radius * radius;

		return (dx * dx + dy * dy) <= radius * radius;
	}
}