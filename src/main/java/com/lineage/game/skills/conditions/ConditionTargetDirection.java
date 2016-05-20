package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Character.TargetDirection;
import com.lineage.game.skills.Env;

public class ConditionTargetDirection extends Condition
{
	private final TargetDirection _dir;

	public ConditionTargetDirection(TargetDirection direction)
	{
		_dir = direction;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getDirectionTo(env.target, true).equals(_dir);
	}
}
