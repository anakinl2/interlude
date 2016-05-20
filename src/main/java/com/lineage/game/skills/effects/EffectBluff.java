package com.lineage.game.skills.effects;

import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.StartRotation;
import com.lineage.game.serverpackets.StopRotation;
import com.lineage.game.skills.Env;

/**
 * @author Felixx
 */
public final class EffectBluff extends L2Effect
{
	public EffectBluff(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		if(getEffected() instanceof L2NpcInstance && ((L2NpcInstance) getEffected()).getNpcId() == 35062 || getSkill().getId() != 358)
			return;

		getEffected().setTarget(null);
		getEffected().abortAttack();
		getEffected().abortCast();
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, getEffector());
		getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		getEffected().startStunning();
	}

	@Override
	public void onExit()
	{
		getEffected().stopStunning();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}