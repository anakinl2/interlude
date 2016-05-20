package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public class EffectDamOverTimeLethal extends L2Effect
{
	public EffectDamOverTimeLethal(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onActionTime()
	{
		if(_effected.isDead())
			return false;

		double damage = calc();

		if(getSkill().isOffensive())
			damage *= 2;

		_effected.reduceCurrentHp(damage, _effected.isPlayer() ? _effected : _effector, getSkill(), !_effected.isNpc(), !getSkill().isToggle(), _effector.isNpc() || getSkill().isToggle() || _effected == _effector, false);

		return true;
	}
}