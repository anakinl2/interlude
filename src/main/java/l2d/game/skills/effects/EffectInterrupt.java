package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public class EffectInterrupt extends L2Effect
{
	public EffectInterrupt(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(!getEffected().isRaid())
			getEffected().abortCast();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}