package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionPlayerMinHp extends Condition
{
	private final float _hp;

	public ConditionPlayerMinHp(int hp)
	{
		_hp = hp;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentHp() > _hp;
	}
}