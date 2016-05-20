package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Player;
import com.lineage.game.skills.Env;

public class ConditionPlayerWeight extends Condition
{
	private final int _weight;

	public ConditionPlayerWeight(int weight)
	{
		_weight = weight;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character instanceof L2Player)
			return ((L2Player) env.character).getMaxLoad() * 100 < _weight;
		return true;
	}
}
