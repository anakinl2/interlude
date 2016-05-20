package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionPlayerPercentCp extends Condition
{
	private final float _cp;

	public ConditionPlayerPercentCp(int cp)
	{
		_cp = cp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentCpRatio() <= _cp;
	}
}