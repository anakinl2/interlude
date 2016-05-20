package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public class EffectMuteAttack extends L2Effect
{
	public EffectMuteAttack(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.startAMuted();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.stopAMuted();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}