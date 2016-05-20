package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.base.Race;
import com.lineage.game.skills.Env;

public class ConditionPlayerRace extends Condition
{
	private final Race _race;

	public ConditionPlayerRace(String race)
	{
		_race = Race.valueOf(race.toLowerCase());
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		return ((L2Player) env.character).getRace() == _race;
	}
}