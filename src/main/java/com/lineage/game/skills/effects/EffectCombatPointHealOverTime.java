package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

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