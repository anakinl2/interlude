package com.lineage.game.skills.effects;

import com.lineage.game.ai.DefaultAI;
import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public class EffectEnervation extends L2Effect
{

	public EffectEnervation(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isMonster())
			((DefaultAI) _effected.getAI()).set("DebuffIntention", 0.5D);
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
		if(_effected.isMonster())
			((DefaultAI) _effected.getAI()).set("DebuffIntention", 1.0D);
	}
}
