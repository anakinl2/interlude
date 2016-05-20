package l2d.game.skills.conditions;

import l2d.game.skills.Env;
import com.lineage.util.Rnd;

public class ConditionGameChance extends Condition
{
	private final int _chance;

	ConditionGameChance(int chance)
	{
		_chance = chance;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return Rnd.chance(_chance);
	}
}
