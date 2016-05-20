package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionPlayerPercentHp extends Condition
{
	private final float _hp;

	public ConditionPlayerPercentHp(int hp)
	{
		_hp = hp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentHpRatio() <= _hp;
	}
}