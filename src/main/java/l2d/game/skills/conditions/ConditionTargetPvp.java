package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionTargetPvp extends Condition
{
	private final boolean _flag;

	public ConditionTargetPvp(final boolean flag)
	{
		_flag = flag;
	}

	@Override
	public boolean testImpl(final Env env)
	{
		return env.target.getPvpFlag() > 0 == _flag;
	}
}