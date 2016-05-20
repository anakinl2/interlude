package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public final class ConditionHasSkill extends Condition
{
	private final Integer _id;
	private final short _level;

	public ConditionHasSkill(Integer id, short level)
	{
		_id = id;
		_level = level;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.skill == null)
			return false;
		return env.character.getSkillLevel(_id) >= _level;
	}
}
