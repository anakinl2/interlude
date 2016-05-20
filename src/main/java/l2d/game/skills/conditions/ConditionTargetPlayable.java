package l2d.game.skills.conditions;

import l2d.game.model.L2Playable;
import l2d.game.skills.Env;

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
