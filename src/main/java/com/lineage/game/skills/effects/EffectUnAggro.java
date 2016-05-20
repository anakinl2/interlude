package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.skills.Env;

public class EffectUnAggro extends L2Effect
{
	public EffectUnAggro(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isNpc())
			((L2NpcInstance) _effected).setUnAggred(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(_effected.isNpc())
			((L2NpcInstance) _effected).setUnAggred(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}