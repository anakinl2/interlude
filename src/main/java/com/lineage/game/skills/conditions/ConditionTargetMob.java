package com.lineage.game.skills.conditions;

import com.lineage.game.skills.Env;

public class ConditionTargetMob extends Condition
{
	private final boolean _isMob;

	public ConditionTargetMob(boolean isMob)
	{
		_isMob = isMob;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target.isMonster() == _isMob;
	}
}
