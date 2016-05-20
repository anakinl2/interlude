package com.lineage.game.skills.effects;

import com.lineage.game.skills.Env;
import com.lineage.game.model.L2Effect;

public class EffectManaHealOverTime extends L2Effect
{
	public EffectManaHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onActionTime()
	{
		if(_effected.isDead() || _effected.isHealBlocked())
			return false;

		_effected.setCurrentMp(_effected.getCurrentMp() + calc());
		return true;
	}
}