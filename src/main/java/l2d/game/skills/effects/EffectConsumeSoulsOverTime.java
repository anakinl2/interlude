package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public class EffectConsumeSoulsOverTime extends L2Effect
{
	public EffectConsumeSoulsOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onActionTime()
	{
		if(_effected.isDead())
			return false;

		if(_effected.getConsumedSouls() < 0)
			return false;

		int damage = (int) calc();

		if(_effected.getConsumedSouls() < damage)
			_effected.setConsumedSouls(0, null);
		else
			_effected.setConsumedSouls(_effected.getConsumedSouls() - damage, null);

		return true;
	}
}