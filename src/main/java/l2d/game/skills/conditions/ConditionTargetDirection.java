package l2d.game.skills.conditions;

import l2d.game.model.L2Character.TargetDirection;
import l2d.game.skills.Env;

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
