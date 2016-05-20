package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public class EffectCombatPointHealOverTime extends L2Effect
{
	public EffectCombatPointHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead() || getEffected().isHealBlocked())
			return false;

		getEffected().setCurrentCp(getEffected().getCurrentCp() + calc());
		return true;
	}
}