package l2d.game.skills.conditions;

import l2d.game.skills.Env;

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
