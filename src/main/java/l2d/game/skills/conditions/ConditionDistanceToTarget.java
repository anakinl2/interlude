package l2d.game.skills.conditions;

import l2d.game.model.L2Character;
import l2d.game.skills.Env;

public class ConditionDistanceToTarget extends Condition
{
	private final int _validDistance;

	public ConditionDistanceToTarget(int distance)
	{
		_validDistance = distance;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character _target = env.target;
		L2Character _character = env.character;
		double _range = _character.getDistance(_target.getX(), _target.getY(), _target.getZ());

		return _range > _validDistance - 50 && _range < _validDistance + 50;
	}
}