package l2d.game.skills.conditions;

import l2d.game.model.L2Player;
import l2d.game.skills.Env;

public class ConditionPlayerMaxPK extends Condition
{
	private final int _pk;

	public ConditionPlayerMaxPK(int pk)
	{
		_pk = pk;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
			return ((L2Player) env.character).getPkKills() <= _pk;
		return false;
	}
}