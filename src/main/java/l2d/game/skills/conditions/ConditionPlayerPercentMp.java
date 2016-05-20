package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionPlayerPercentMp extends Condition
{
	private final float _mp;

	public ConditionPlayerPercentMp(int mp)
	{
		_mp = mp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentMpRatio() <= _mp;
	}
}