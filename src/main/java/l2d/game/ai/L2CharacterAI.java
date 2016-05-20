package l2d.game.ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_IDLE;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.ai.L2PlayableAI.nextAction;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2BoatInstance;
import l2d.game.serverpackets.Die;
import com.lineage.util.Location;

public class L2CharacterAI extends AbstractAI
{
	public L2CharacterAI(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionIdle()
	{
		clientStopMoving();
		changeIntention(AI_INTENTION_IDLE, null, null);
	}

	@Override
	protected void onIntentionActive()
	{
		clientStopMoving();
		changeIntention(AI_INTENTION_ACTIVE, null, null);
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		setAttackTarget(target);
		changeIntention(AI_INTENTION_ATTACK, target, null);
		onEvtThink();
	}

	@Override
	protected void onIntentionCast(L2Skill skill, L2Character target)
	{
		setAttackTarget(target);
		changeIntention(AI_INTENTION_CAST, skill, target);
		onEvtThink();
	}

	@Override
	protected void onIntentionFollow(L2Character target, Integer offset)
	{
		changeIntention(AI_INTENTION_FOLLOW, target, offset);
		L2Character actor = getActor();
		if(actor != null)
			actor.followToCharacter(target, offset, false);
	}

	@Override
	protected void onIntentionInteract(L2Object object)
	{}

	@Override
	protected void onIntentionPickUp(L2Object item)
	{}

	@Override
	protected void onIntentionRest()
	{}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_pos)
	{
		L2Character actor = getActor();
		if(actor != null)
			actor.stopMove();
		onEvtThink();
	}

	@Override
	protected void onEvtForgetObject(L2Object object)
	{
		L2Character actor = getActor();
		if(actor == null || object == null)
			return;

		if(actor.isAttackingNow() && getAttackTarget() == object)
		{
			actor.abortAttack();
			actor.sendMessage(new CustomMessage("l2d.game.model.L2Character.AttackAborted", actor));
			setIntention(AI_INTENTION_ACTIVE, null, null);
		}

		if(actor.isCastingNow() && getAttackTarget() == object)
		{
			actor.abortCast();
			actor.sendMessage(new CustomMessage("l2d.game.model.L2Character.CastingAborted", actor));
		}

		if(getAttackTarget() == object)
			setAttackTarget(null);

		if(actor.getTargetId() == object.getObjectId())
			actor.setTarget(null);
	}

	@Override
	protected void onEvtDead()
	{
		L2Character actor = getActor();
		if(actor != null)
		{
			actor.breakAttack();
			actor.breakCast(true);
			actor.stopMove();
			actor.broadcastPacket(new Die(actor));
		}
		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void onEvtFakeDeath()
	{
		clientStopMoving();
		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2Character actor = getActor();
		if(actor != null)
			actor.startAttackStanceTask();
		if(attacker != null)
			attacker.startAttackStanceTask();
	}

	@Override
	protected void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage)
	{}

	public void Attack(L2Object target, boolean forceUse)
	{
		setIntention(AI_INTENTION_ATTACK, target);
	}

	public void Cast(L2Skill skill, L2Character target)
	{
		Cast(skill, target, false, false);
	}

	public void Cast(L2Skill skill, L2Character target, boolean forceUse, boolean dontMove)
	{
		setIntention(AI_INTENTION_ATTACK, target);
	}

	@Override
	protected void onEvtThink()
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}

	@Override
	protected void onEvtFinishCasting()
	{}

	@Override
	protected void onEvtReadyToAct()
	{}

	@Override
	protected void onEvtArrived()
	{
		L2Character actor = getActor();
		if(actor != null && actor instanceof L2BoatInstance)
			((L2BoatInstance) actor).BoatArrived();
	}

	@Override
	protected void onEvtArrivedTarget()
	{}

	@Override
	protected void onEvtSeeSpell(L2Skill skill, L2Character caster)
	{}

	public void stopAITask()
	{}

	public void startAITask()
	{}

	public void setNextAction(nextAction action, Object arg0, Object arg1, boolean arg2, boolean arg3)
	{}

	public void clearTasks()
	{}

	public void clearNextAction()
	{}

	public void teleportHome()
	{}

	public void checkAggression(L2Character target)
	{}

	public boolean isActive()
	{
		return true;
	}

	public boolean isGlobalAggro()
	{
		return true;
	}

	public boolean isSilentMoveNotVisible(L2Playable target)
	{
		return true;
	}
}