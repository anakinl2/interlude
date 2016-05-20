package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public final class EffectSalvation extends L2Effect
{
	public EffectSalvation(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		getEffected().setIsSalvation(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setIsSalvation(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}