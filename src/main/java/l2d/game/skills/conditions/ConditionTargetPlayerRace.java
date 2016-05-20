package l2d.game.skills.conditions;

import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.base.Race;
import l2d.game.skills.Env;

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