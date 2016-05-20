package l2d.game.ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_PICK_UP;

import java.util.concurrent.ScheduledFuture;

import com.lineage.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.cache.Msg;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.NextAction;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.serverpackets.MyTargetSelected;
import com.lineage.util.Location;

public class L2PlayableAI extends L2CharacterAI
{
	private boolean thinking = false; // to prevent recursive thinking

	private Object _intention_arg0 = null;
	private Object _intention_arg1 = null;
	private L2Skill _skill;

	private nextAction _nextAction;
	private Object _nextAction_arg0;
	private Object _nextAction_arg1;
	private boolean _nextAction_arg2;
	private boolean _nextAction_arg3;

	private boolean _forceUse;
	private boolean _dontMove;

	private ScheduledFuture<?> _followTask;
	private ScheduledFuture<?> _blockTask;

	public L2PlayableAI(final L2Playable actor)
	{
		super(actor);
	}

	public enum nextAction
	{
		ATTACK,
		CAST,
		MOVE,
		REST,
		PICKUP,
		INTERACT
	}

	@Override
	public void changeIntention(final CtrlIntention intention, final Object arg0, final Object arg1)
	{
		super.changeIntention(intention, arg0, arg1);
		_intention_arg0 = arg0;
		_intention_arg1 = arg1;
	}

	@Override
	public void setIntention(final CtrlIntention intention, final Object arg0, final Object arg1)
	{
		_intention_arg0 = null;
		_intention_arg1 = null;
		super.setIntention(intention, arg0, arg1);
	}

	@Override
	protected void onIntentionCast(final L2Skill skill, final L2Character target)
	{
		_skill = skill;
		super.onIntentionCast(skill, target);
	}

	@Override
	public void setNextAction(final nextAction action, final Object arg0, final Object arg1, final boolean arg2, final boolean arg3)
	{
		_nextAction = action;
		_nextAction_arg0 = arg0;
		_nextAction_arg1 = arg1;
		_nextAction_arg2 = arg2;
		_nextAction_arg3 = arg3;
	}

	public boolean setNextIntention()
	{
		final nextAction nextAction = _nextAction;
		final Object nextAction_arg0 = _nextAction_arg0;
		final Object nextAction_arg1 = _nextAction_arg1;
		final boolean nextAction_arg2 = _nextAction_arg2;
		final boolean nextAction_arg3 = _nextAction_arg3;

		final L2Playable actor = getActor();
		if(nextAction == null || actor == null)
			return false;

		L2Skill skill;
		L2Character target;
		L2Object object;

		switch(nextAction)
		{
			case ATTACK:
				if(nextAction_arg0 == null)
					return false;
				target = (L2Character) nextAction_arg0;
				_forceUse = nextAction_arg2;
				_dontMove = nextAction_arg3;
				clearNextAction();
				setIntention(AI_INTENTION_ATTACK, target);
				break;
			case CAST:
				if(nextAction_arg0 == null || nextAction_arg1 == null)
					return false;
				skill = (L2Skill) nextAction_arg0;
				target = (L2Character) nextAction_arg1;
				_forceUse = nextAction_arg2;
				_dontMove = nextAction_arg3;
				clearNextAction();
				if(!skill.checkCondition(actor, target, _forceUse, _dontMove, true))
				{
					if(skill.getNextAction() == NextAction.ATTACK && !actor.equals(target))
					{
						setNextAction(l2d.game.ai.L2PlayableAI.nextAction.ATTACK, target, null, _forceUse, false);
						return setNextIntention();
					}
					return false;
				}
				setIntention(AI_INTENTION_CAST, skill, target);
				break;
			case MOVE:
				if(nextAction_arg0 == null || nextAction_arg1 == null)
					return false;
				final Location loc = (Location) nextAction_arg0;
				final Integer offset = (Integer) nextAction_arg1;
				clearNextAction();
				actor.moveToLocation(loc, offset, nextAction_arg2);
				break;
			case REST:
				actor.sitDown();
				break;
			case INTERACT:
				if(nextAction_arg0 == null)
					return false;
				object = (L2Object) nextAction_arg0;
				clearNextAction();
				onIntentionInteract(object);
				break;
			case PICKUP:
				if(nextAction_arg0 == null)
					return false;
				object = (L2Object) nextAction_arg0;
				clearNextAction();
				onIntentionPickUp(object);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void clearNextAction()
	{
		_nextAction = null;
		_nextAction_arg0 = null;
		_nextAction_arg1 = null;
		_nextAction_arg2 = false;
		_nextAction_arg3 = false;
	}

	@Override
	protected void onEvtFinishCasting()
	{
		if(!setNextIntention())
			setIntention(AI_INTENTION_ACTIVE);
	}

	@Override
	protected void onEvtReadyToAct()
	{
		if(!setNextIntention())
			onEvtThink();
	}

	@Override
	protected void onEvtArrived()
	{
		if(!setNextIntention())
			if(getIntention() == AI_INTENTION_INTERACT || getIntention() == AI_INTENTION_PICK_UP)
				onEvtThink();
			else
				changeIntention(AI_INTENTION_ACTIVE, null, null);
	}

	@Override
	protected void onEvtArrivedTarget()
	{
		switch(getIntention())
		{
			case AI_INTENTION_ATTACK:
				thinkAttack(false);
				break;
			case AI_INTENTION_CAST:
				thinkCast(false);
				break;
			case AI_INTENTION_FOLLOW:
				if(_followTask != null)
					_followTask.cancel(false);
				_followTask = ThreadPoolManager.getInstance().scheduleAi(new ThinkFollow(), 1000, true);
				break;
		}
	}

	@Override
	protected void onEvtThink()
	{
		final L2Playable actor = getActor();
		if(actor == null || thinking || actor.isActionsDisabled())
			return;

		if(Config.DEBUG)
			_log.warning("L2PlayableAI: onEvtThink -> Check intention");

		thinking = true;

		try
		{
			switch(getIntention())
			{
				case AI_INTENTION_ATTACK:
					thinkAttack(true);
					break;
				case AI_INTENTION_CAST:
					thinkCast(true);
					break;
				case AI_INTENTION_PICK_UP:
					thinkPickUp();
					break;
				case AI_INTENTION_INTERACT:
					thinkInteract();
					break;
			}
		}
		catch(final Exception e)
		{
			_log.warning("Exception onEvtThink(): " + e);
			e.printStackTrace();
		}
		finally
		{
			thinking = false;
		}
	}

	public class ThinkFollow implements Runnable
	{
		public L2Playable getActor()
		{
			return L2PlayableAI.this.getActor();
		}

		@Override
		public void run()
		{
			_followTask = null;
			final L2Playable actor = getActor();
			if(actor == null || getIntention() != AI_INTENTION_FOLLOW)
				return;
			final L2Character target = (L2Character) _intention_arg0;
			final Integer offset = (Integer) _intention_arg1;
			if(target == null || target.isAlikeDead() || actor.getDistance(target) > 4000)
			{
				setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				return;
			}
			final L2Player actor_player = actor.getPlayer();
			if(actor_player == null || !actor_player.isConnected() || (actor.isPet() || actor.isSummon()) && actor_player.getPet() != target)
			{
				setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				return;
			}
			if(!actor.isInRange(target, offset) && (!actor.isFollow || actor.getFollowTarget() != target))
				actor.followToCharacter(target, offset, false);
			_followTask = ThreadPoolManager.getInstance().scheduleAi(this, 1000, true);
		}
	}

	@Override
	protected void onIntentionInteract(final L2Object object)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;
		if(actor.isActionsDisabled())
		{
			setNextAction(nextAction.INTERACT, object, null, false, false);
			clientActionFailed();
			return;
		}

		clearNextAction();
		changeIntention(AI_INTENTION_INTERACT, object, null);
		onEvtThink();
	}

	protected void thinkInteract()
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		final L2Object target = (L2Object) _intention_arg0;

		if(target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		final int range = (int) (Math.max(30, actor.getMinDistance(target)) + 20);

		if(actor.isInRangeZ(target, range))
		{
			if(actor.isPlayer())
				((L2Player) actor).doInteract(target);
			setIntention(AI_INTENTION_ACTIVE);
		}
		else
		{
			actor.moveToLocation(target.getLoc(), 40, true);
			setNextAction(nextAction.INTERACT, target, null, false, false);
		}
	}

	@Override
	protected void onIntentionPickUp(final L2Object object)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		if(actor.isActionsDisabled())
		{
			setNextAction(nextAction.PICKUP, object, null, false, false);
			clientActionFailed();
			return;
		}

		clearNextAction();
		changeIntention(AI_INTENTION_PICK_UP, object, null);
		onEvtThink();
	}

	protected void thinkPickUp()
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		final L2Object target = (L2Object) _intention_arg0;

		if(target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		if(actor.isInRange(target, 30) && Math.abs(actor.getZ() - target.getZ()) < 50)
		{
			if(actor.isPlayer() || actor.isPet())
				actor.doPickupItem(target);
			setIntention(AI_INTENTION_ACTIVE);
		}
		else
		{
			actor.moveToLocation(target.getLoc(), 10, true);
			setNextAction(nextAction.PICKUP, target, null, false, false);
		}
	}

	protected void thinkAttack(final boolean checkRange)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		final L2Player player = actor.getPlayer();
		if(player == null)
		{
			setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			return;
		}

		if(actor.isAttackingDisabled())
		{
			actor.sendActionFailed();
			return;
		}

		final L2Character attack_target = getAttackTarget();
		if(attack_target == null || attack_target.isDead() || !(_forceUse ? attack_target.isAttackable() : attack_target.isAutoAttackable(player)))
		{
			setIntention(AI_INTENTION_ACTIVE);
			actor.sendActionFailed();
			return;
		}

		if(!checkRange)
		{
			clientStopMoving();
			actor.doAttack(attack_target);
			return;
		}

		int range = actor.getPhysicalAttackRange();
		if(range < 10)
			range = 10;

		final boolean canSee = GeoEngine.canSeeTarget(actor, attack_target, false);

		if(!canSee && (range > 200 || Math.abs(actor.getZ() - attack_target.getZ()) > 200))
		{
			actor.sendPacket(Msg.CANNOT_SEE_TARGET);
			setIntention(AI_INTENTION_ACTIVE);
			actor.sendActionFailed();
			return;
		}

		range += actor.getMinDistance(attack_target);

		if(actor.isFakeDeath())
			actor.getEffectList().stopEffects(EffectType.FakeDeath);

		if(actor.isInRangeZ(attack_target, range))
		{
			if(!canSee)
			{
				actor.sendPacket(Msg.CANNOT_SEE_TARGET);
				setIntention(AI_INTENTION_ACTIVE);
				actor.sendActionFailed();
				return;
			}

			clientStopMoving(false);
			actor.doAttack(attack_target);
		}
		else
			actor.followToCharacter(attack_target, range - 20, true);
	}

	protected void thinkCast(final boolean checkRange)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		final L2Character attack_target = getAttackTarget();

		if(_skill.getSkillType() == SkillType.CRAFT || _skill.isToggle())
		{
			if(_skill.checkCondition(actor, attack_target, _forceUse, _dontMove, true))
				actor.doCast(_skill, attack_target, _forceUse);
			return;
		}

		if(attack_target == null || attack_target.isDead() != _skill.getCorpse() && !_skill.isNotTargetAoE())
		{
			setIntention(AI_INTENTION_ACTIVE);
			actor.sendActionFailed();
			return;
		}

		if(!checkRange)
		{
			// Если скилл имеет следующее действие, назначим это действие после окончания действия скилла
			if(_skill.getNextAction() == NextAction.ATTACK && !actor.equals(attack_target))
				setNextAction(nextAction.ATTACK, attack_target, null, _forceUse, false);
			else
				clearNextAction();

			clientStopMoving();

			if(_skill.checkCondition(actor, attack_target, _forceUse, _dontMove, true))
				actor.doCast(_skill, attack_target, _forceUse);
			else
			{
				setNextIntention();
				if(getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
					thinkAttack(true);
			}

			return;
		}

		int range = actor.getMagicalAttackRange(_skill);
		if(range < 10)
			range = 10;

		final boolean canSee = GeoEngine.canSeeTarget(actor, attack_target, false);
		final boolean noRangeSkill = _skill.getCastRange() == 32767;

		if(!noRangeSkill && !canSee && (range > 200 || Math.abs(actor.getZ() - attack_target.getZ()) > 200))
		{
			actor.sendPacket(Msg.CANNOT_SEE_TARGET);
			setIntention(AI_INTENTION_ACTIVE);
			actor.sendActionFailed();
			return;
		}

		range += actor.getMinDistance(attack_target);

		if(actor.isFakeDeath())
			actor.getEffectList().stopEffects(EffectType.FakeDeath);

		if(actor.isInRangeZ(attack_target, range) || noRangeSkill)
		{
			if(!noRangeSkill && !canSee)
			{
				actor.sendPacket(Msg.CANNOT_SEE_TARGET);
				setIntention(AI_INTENTION_ACTIVE);
				actor.sendActionFailed();
				return;
			}

			// Если скилл имеет следующее действие, назначим это действие после окончания действия скилла
			if(_skill.getNextAction() == NextAction.ATTACK && !actor.equals(attack_target))
				setNextAction(nextAction.ATTACK, attack_target, null, _forceUse, false);
			else
				clearNextAction();

			if(_skill.checkCondition(actor, attack_target, _forceUse, _dontMove, true))
			{
				clientStopMoving(false);
				actor.doCast(_skill, attack_target, _forceUse);
			}
			else
			{
				setNextIntention();
				if(getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
					thinkAttack(true);
			}
		}
		else if(!_dontMove)
			actor.followToCharacter(attack_target, range - 20, true);
		else
		{
			actor.sendPacket(Msg.YOUR_TARGET_IS_OUT_OF_RANGE);
			setIntention(AI_INTENTION_ACTIVE);
			actor.sendActionFailed();
		}
	}

	@Override
	protected void onEvtDead()
	{
		clearNextAction();
		final L2Playable actor = getActor();
		if(actor != null)
			actor.clearHateList(true);
		super.onEvtDead();
	}

	@Override
	protected void onEvtFakeDeath()
	{
		clearNextAction();
		super.onEvtFakeDeath();
	}

	@Override
	protected void onEvtAggression(final L2Character target, final int aggro)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		if(aggro > 0)
			if(actor.getTarget() != target && actor.getAggressionTarget() == null && _blockTask == null)
			{
				actor.setAggressionTarget(target);
				actor.setTarget(target);

				clearNextAction();
				switch(getIntention())
				{
					case AI_INTENTION_ATTACK:
						setAttackTarget(target);
						break;
					case AI_INTENTION_CAST:
						L2Skill skill = actor.getCastingSkill();
						if(skill == null)
							skill = _skill;
						if(skill != null && !skill.isUsingWhileCasting())
							switch(skill.getTargetType())
							{
								case TARGET_ONE:
								case TARGET_AREA:
								case TARGET_MULTIFACE:
									setAttackTarget(target);
									actor.setCastingTarget(target);
									break;
							}
						break;
				}

				actor.sendPacket(new MyTargetSelected(target.getObjectId(), 0));

				_blockTask = ThreadPoolManager.getInstance().scheduleAi(new UnBlock(actor), 3000, true);
			}
	}

	@Override
	public void Attack(final L2Object target, final boolean forceUse)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		if(target.isCharacter() && (actor.isActionsDisabled() || actor.isAttackingDisabled()))
		{
			// Если не можем атаковать, то атаковать позже
			setNextAction(nextAction.ATTACK, target, null, forceUse, false);
			actor.sendActionFailed();
			return;
		}

		_forceUse = forceUse;
		clearNextAction();
		setIntention(AI_INTENTION_ATTACK, target);
	}

	@Override
	public void Cast(final L2Skill skill, final L2Character target, final boolean forceUse, final boolean dontMove)
	{
		final L2Playable actor = getActor();
		if(actor == null)
			return;

		// Если скилл альтернативного типа (например, бутылка на хп),
		// то он может использоваться во время каста других скиллов, или во время атаки, или на бегу.
		// Поэтому пропускаем дополнительные проверки.
		if(skill.altUse() || skill.isToggle())
		{
			if(skill.isToggle() && actor.isToggleDisabled() || skill.isHandler() && actor.isPotionsDisabled())
				clientActionFailed();
			else
				actor.altUseSkill(skill, target);
			return;
		}

		// Если не можем кастовать, то использовать скилл позже
		if(actor.isActionsDisabled())
		{
			// if(!actor.isSkillDisabled(skill.getId()))
			setNextAction(nextAction.CAST, skill, target, forceUse, dontMove);
			clientActionFailed();
			return;
		}

		// _actor.stopMove(null);
		_forceUse = forceUse;
		_dontMove = dontMove;
		clearNextAction();
		setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}

	public class UnBlock implements Runnable
	{
		private L2Character _activeChar;

		public UnBlock(final L2Character activeChar)
		{
			_activeChar = activeChar;
		}

		@Override
		public void run()
		{
			_activeChar.setAggressionTarget(null);
			_blockTask = null;
		}
	}

	@Override
	public L2Playable getActor()
	{
		return (L2Playable) super.getActor();
	}

	public boolean isForceCast()
	{
		return _forceUse;
	}
}