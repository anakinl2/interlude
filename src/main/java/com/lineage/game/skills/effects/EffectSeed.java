package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public final class EffectSeed extends L2Effect
{

	private int _power = 1;

	public EffectSeed(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	public int getPower()
	{
		return _power;
	}

	public void increasePower()
	{
		_power++;
	}
}
