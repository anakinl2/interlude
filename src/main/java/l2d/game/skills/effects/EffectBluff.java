package l2d.game.skills.effects;

import l2d.game.ai.CtrlIntention;
import l2d.game.model.L2Effect;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.StartRotation;
import l2d.game.serverpackets.StopRotation;
import l2d.game.skills.Env;

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