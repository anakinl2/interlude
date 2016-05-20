package com.lineage.game.ai;

import com.lineage.Config;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;

public class L2PlayerAI extends L2PlayableAI
{
	public L2PlayerAI(L2Player actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionRest()
	{
		changeIntention(CtrlIntention.AI_INTENTION_REST, null, null);
		setAttackTarget(null);
		clientStopMoving();
	}

	@Override
	protected void onIntentionActive()
	{
		clearNextAction();
		changeIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
	}

	@Override
	public void onIntentionInteract(L2Object object)
	{
		L2Player actor = getActor();
		if(actor == null)
			return;

		if(actor.getSittingTask())
		{
			setNextAction(nextAction.INTERACT, object, null, false, false);
			return;
		}
		else if(actor.isSitting())
		{
			actor.sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			clientActionFailed();
			return;
		}
		super.onIntentionInteract(object);
	}

	@Override
	public void onIntentionPickUp(L2Object object)
	{
		L2Player actor = getActor();
		if(actor == null)
			return;

		if(actor.getSittingTask())
		{
			setNextAction(nextAction.PICKUP, object, null, false, false);
			return;
		}
		else if(actor.isSitting())
		{
			actor.sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			clientActionFailed();
			return;
		}
		super.onIntentionPickUp(object);
	}

	@Override
	public void Attack(L2Object target, boolean forceUse)
	{
		L2Player actor = getActor();
		if(actor == null)
			return;

		if(actor.getSittingTask())
		{
			setNextAction(nextAction.ATTACK, target, null, forceUse, false);
			return;
		}
		else if(actor.isSitting())
		{
			actor.sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			clientActionFailed();
			return;
		}
		super.Attack(target, forceUse);
	}

	@Override
	public void Cast(L2Skill skill, L2Character target, boolean forceUse, boolean dontMove)
	{
		L2Player actor = getActor();
		if(actor == null)
			return;

		if(!skill.altUse() && !(skill.getSkillType() == L2Skill.SkillType.CRAFT && Config.ALLOW_TALK_WHILE_SITTING))
			// Если в этот момент встаем, то использовать скилл когда встанем
			if(actor.getSittingTask())
			{
				setNextAction(nextAction.CAST, skill, target, forceUse, dontMove);
				clientActionFailed();
				return;
			}
			// если сидим - скиллы нельзя использовать
			else if(actor.isSitting())
			{
				actor.sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
				clientActionFailed();
				return;
			}
		super.Cast(skill, target, forceUse, dontMove);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2Player actor = getActor();
		if(actor == null)
			return;
		// notify the tamed beast of attacks
		if(actor.getTrainedBeast() != null)
			actor.getTrainedBeast().onOwnerGotAttacked(attacker);
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	public L2Player getActor()
	{
		return (L2Player) super.getActor();
	}
}