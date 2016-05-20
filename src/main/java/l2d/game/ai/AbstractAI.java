package l2d.game.ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.engine.DefaultListenerEngine;
import com.lineage.ext.listeners.engine.ListenerEngine;
import com.lineage.ext.listeners.events.AbstractAI.AbstractAINotifyEvent;
import com.lineage.ext.listeners.events.AbstractAI.AbstractAISetIntention;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Skill;
import com.lineage.util.Location;

public abstract class AbstractAI
{
	protected static final Logger _log = Logger.getLogger(AbstractAI.class.getName());

	protected L2Character _actor, _attack_target;
	protected CtrlIntention _intention = AI_INTENTION_IDLE;

	protected AbstractAI(final L2Character actor)
	{
		_actor = actor;
	}

	public void changeIntention(final CtrlIntention intention, final Object arg0, final Object arg1)
	{
		if(Config.DEBUG)
			_log.info("AbstractAI: " + getActor() + ".changeIntention(" + intention + ", " + arg0 + ", " + arg1 + ")");
		_intention = intention;
		if(intention != AI_INTENTION_CAST && intention != AI_INTENTION_ATTACK)
			setAttackTarget(null);
	}

	public final void setIntention(final CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}

	public final void setIntention(final CtrlIntention intention, final Object arg0)
	{
		setIntention(intention, arg0, null);
	}

	public void setIntention(CtrlIntention intention, final Object arg0, final Object arg1)
	{
		if(intention != AI_INTENTION_CAST && intention != AI_INTENTION_ATTACK)
			setAttackTarget(null);

		final L2Character actor = getActor();
		if(actor == null || !actor.hasAI())
			return;

		if(!actor.isVisible())
		{
			if(_intention == AI_INTENTION_IDLE)
				return;

			intention = AI_INTENTION_IDLE;
		}

		getListenerEngine().fireMethodInvoked(new AbstractAISetIntention(MethodCollection.AbstractAIsetIntention, this, new Object[] {
				intention,
				arg0,
				arg1 }));

		switch(intention)
		{
			case AI_INTENTION_IDLE:
				onIntentionIdle();
				break;
			case AI_INTENTION_ACTIVE:
				onIntentionActive();
				break;
			case AI_INTENTION_REST:
				onIntentionRest();
				break;
			case AI_INTENTION_ATTACK:
				onIntentionAttack((L2Character) arg0);
				break;
			case AI_INTENTION_CAST:
				onIntentionCast((L2Skill) arg0, (L2Character) arg1);
				break;
			case AI_INTENTION_PICK_UP:
				onIntentionPickUp((L2Object) arg0);
				break;
			case AI_INTENTION_INTERACT:
				onIntentionInteract((L2Object) arg0);
				break;
			case AI_INTENTION_FOLLOW:
				onIntentionFollow((L2Character) arg0, (Integer) arg1);
				break;
		}
	}

	public final void notifyEvent(final CtrlEvent evt)
	{
		notifyEvent(evt, new Object[] {});
	}

	public final void notifyEvent(final CtrlEvent evt, final Object arg0)
	{
		notifyEvent(evt, new Object[] { arg0 });
	}

	public final void notifyEvent(final CtrlEvent evt, final Object arg0, final Object arg1)
	{
		notifyEvent(evt, new Object[] { arg0, arg1 });
	}

	public void notifyEvent(final CtrlEvent evt, final Object[] args)
	{
		final L2Character actor = getActor();
		if(actor == null || !actor.isVisible() || !actor.hasAI())
			return;

		getListenerEngine().fireMethodInvoked(new AbstractAINotifyEvent(MethodCollection.AbstractAInotifyEvent, this, new Object[] { evt, args }));

		switch(evt)
		{
			case EVT_THINK:
				onEvtThink();
				break;
			case EVT_ATTACKED:
				onEvtAttacked((L2Character) args[0], ((Number) args[1]).intValue());
				break;
			case EVT_CLAN_ATTACKED:
				onEvtClanAttacked((L2Character) args[0], (L2Character) args[1], ((Number) args[2]).intValue());
				break;
			case EVT_AGGRESSION:
				onEvtAggression((L2Character) args[0], ((Number) args[1]).intValue());
				break;
			case EVT_READY_TO_ACT:
				onEvtReadyToAct();
				break;
			case EVT_ARRIVED:
				onEvtArrived();
				break;
			case EVT_ARRIVED_TARGET:
				onEvtArrivedTarget();
				break;
			case EVT_ARRIVED_BLOCKED:
				onEvtArrivedBlocked((Location) args[0]);
				break;
			case EVT_FORGET_OBJECT:
				onEvtForgetObject((L2Object) args[0]);
				break;
			case EVT_DEAD:
				onEvtDead();
				break;
			case EVT_FAKE_DEATH:
				onEvtFakeDeath();
				break;
			case EVT_FINISH_CASTING:
				onEvtFinishCasting();
				break;
			case EVT_SEE_SPELL:
				onEvtSeeSpell((L2Skill) args[0], (L2Character) args[1]);
				break;
		}
	}

	protected void clientActionFailed()
	{
		final L2Character actor = getActor();
		if(actor != null && actor.isPlayer())
			actor.sendActionFailed();
	}

	/**
	 * Останавливает движение
	 * 
	 * @param validate
	 *            - рассылать ли ValidateLocation
	 */
	public void clientStopMoving(final boolean validate)
	{
		final L2Character actor = getActor();
		if(actor == null)
			return;
		actor.stopMove(validate);
	}

	/**
	 * Останавливает движение и рассылает ValidateLocation
	 */
	public void clientStopMoving()
	{
		final L2Character actor = getActor();
		if(actor == null)
			return;
		actor.stopMove();
	}

	public L2Character getActor()
	{
		return _actor;
	}

	public void removeActor()
	{
		_actor = null;
		setAttackTarget(null);
	}

	public CtrlIntention getIntention()
	{
		return _intention;
	}

	public void setAttackTarget(final L2Character target)
	{
		_attack_target = target;
	}

	public L2Character getAttackTarget()
	{
		return _attack_target;
	}

	/** Означает, что AI всегда включен, независимо от состояния региона */
	public boolean isGlobalAI()
	{
		return false;
	}

	public void setGlobalAggro(final int value)
	{}

	@Override
	public String toString()
	{
		return getL2ClassShortName() + " for " + getActor();
	}

	public String getL2ClassShortName()
	{
		return getClass().getName().replaceAll("^.*\\.(.*?)$", "$1");
	}

	protected abstract void onIntentionIdle();

	protected abstract void onIntentionActive();

	protected abstract void onIntentionRest();

	protected abstract void onIntentionAttack(L2Character target);

	protected abstract void onIntentionCast(L2Skill skill, L2Character target);

	protected abstract void onIntentionPickUp(L2Object item);

	protected abstract void onIntentionInteract(L2Object object);

	protected abstract void onEvtThink();

	protected abstract void onEvtAttacked(L2Character attacker, int damage);

	protected abstract void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage);

	protected abstract void onEvtAggression(L2Character target, int aggro);

	protected abstract void onEvtReadyToAct();

	protected abstract void onEvtArrived();

	protected abstract void onEvtArrivedTarget();

	protected abstract void onEvtArrivedBlocked(Location blocked_at_pos);

	protected abstract void onEvtForgetObject(L2Object object);

	protected abstract void onEvtDead();

	protected abstract void onEvtFakeDeath();

	protected abstract void onEvtFinishCasting();

	protected abstract void onEvtSeeSpell(L2Skill skill, L2Character caster);

	protected abstract void onIntentionFollow(L2Character target, Integer offset);

	private ListenerEngine<AbstractAI> listenerEngine;

	public ListenerEngine<AbstractAI> getListenerEngine()
	{
		if(listenerEngine == null)
			listenerEngine = new DefaultListenerEngine<AbstractAI>(this);
		return listenerEngine;
	}
}