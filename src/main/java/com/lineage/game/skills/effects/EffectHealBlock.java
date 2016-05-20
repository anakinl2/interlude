package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public final class EffectHealBlock extends L2Effect
{
	public EffectHealBlock(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.setHealBlocked(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.setHealBlocked(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}