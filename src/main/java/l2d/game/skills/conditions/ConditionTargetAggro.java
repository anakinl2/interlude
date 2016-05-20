package l2d.game.skills.conditions;

import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.skills.Env;

public class ConditionTargetAggro extends Condition
{
	private final boolean _isAggro;

	public ConditionTargetAggro(boolean isAggro)
	{
		_isAggro = isAggro;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character target = env.target;
		if(target == null)
			return false;
		if(target.isMonster())
			return ((L2MonsterInstance) target).isAggressive() == _isAggro;
		if(target.isPlayer())
			return ((L2Player) target).getKarma() > 0;
		return false;
	}
}
