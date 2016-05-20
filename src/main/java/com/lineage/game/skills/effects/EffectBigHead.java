package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public class EffectBigHead extends L2Effect
{
	public EffectBigHead(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.setBigHead(true);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.setBigHead(false);
	}
}