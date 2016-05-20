package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Playable;
import com.lineage.game.skills.Env;

public class ConditionTargetPlayable extends Condition
{
	private final boolean _flag;

	public ConditionTargetPlayable(boolean flag)
	{
		_flag = flag;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2Playable == _flag;
	}
}
