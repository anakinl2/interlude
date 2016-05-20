package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public final class EffectBuffImmunity extends L2Effect
{
	public EffectBuffImmunity(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().setBuffImmunity(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setBuffImmunity(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}