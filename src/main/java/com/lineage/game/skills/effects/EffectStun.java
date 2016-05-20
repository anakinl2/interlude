package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public final class EffectStun extends L2Effect
{
	public EffectStun(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.startStunning();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.stopStunning();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}