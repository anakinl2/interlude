package l2d.game.ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Summon;

public class L2SummonAI extends L2PlayableAI
{
	public L2SummonAI(L2Summon actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionActive()
	{
		L2Summon actor = getActor();
		if(actor == null || !actor.isVisible())
			return;

		clearNextAction();

		if(actor.isPosessed())
		{
			actor.setRunning();
			if(getIntention() != AI_INTENTION_ATTACK)
				setIntention(CtrlIntention.AI_INTENTION_ATTACK, actor.getPlayer(), null);
			return;
		}

		if(actor.getFollowStatus())
			setIntention(CtrlIntention.AI_INTENTION_FOLLOW, actor.getPlayer(), 100);
		else
			super.onIntentionActive();
	}

	@Override
	protected void thinkAttack(boolean checkRange)
	{
		L2Summon actor = getActor();
		if(actor == null)
			return;

		if(actor.isPosessed())
			setAttackTarget(actor.getPlayer());

		L2Character target = getAttackTarget();
		if(target == null || !GeoEngine.canSeeTarget(actor, target, false) || target.isDead())
			actor.setFollowStatus(true);
		else
			super.thinkAttack(checkRange);
	}

	@Override
	protected void onEvtThink()
	{
		L2Summon actor = getActor();
		if(actor == null)
			return;

		if(actor.isPosessed())
		{
			setAttackTarget(actor.getPlayer());
			changeIntention(AI_INTENTION_ATTACK, actor.getPlayer(), null);
		}

		super.onEvtThink();
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2Summon actor = getActor();
		if(actor == null)
			return;
		if(attacker != null && actor.getPlayer().isDead() && !actor.isPosessed())
			Attack(attacker, false);
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	public L2Summon getActor()
	{
		return (L2Summon) super.getActor();
	}
}