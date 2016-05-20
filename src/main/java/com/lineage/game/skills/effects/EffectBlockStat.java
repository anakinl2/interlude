package com.lineage.game.skills.effects;

import com.lineage.game.skills.Env;
import com.lineage.game.skills.skillclasses.NegateStats;
import com.lineage.game.model.L2Effect;

public class EffectBlockStat extends L2Effect
{
	public EffectBlockStat(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.addBlockStats(((NegateStats) _skill).getNegateStats());
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.removeBlockStats(((NegateStats) _skill).getNegateStats());
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}