package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

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