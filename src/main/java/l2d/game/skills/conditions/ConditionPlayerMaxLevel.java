package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionPlayerMaxLevel extends Condition
{
	private final int _level;

	public ConditionPlayerMaxLevel(int level)
	{
		_level = level;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getLevel() <= _level;
	}
}