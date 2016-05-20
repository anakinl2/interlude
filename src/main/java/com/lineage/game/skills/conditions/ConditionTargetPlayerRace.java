package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.base.Race;
import com.lineage.game.skills.Env;

public class ConditionTargetPlayerRace extends Condition
{
	private final Race _race;

	public ConditionTargetPlayerRace(String race)
	{
		_race = Race.valueOf(race.toLowerCase());
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character target = env.target;
		return target != null && target.isPlayer() && _race == ((L2Player) target).getRace();
	}
}