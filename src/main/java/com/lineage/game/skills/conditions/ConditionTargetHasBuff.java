package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.skills.Env;

public final class ConditionTargetHasBuff extends Condition
{
	private final EffectType _effectType;
	private final int _level;

	public ConditionTargetHasBuff(EffectType effectType, int level)
	{
		_effectType = effectType;
		_level = level;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.target == null)
			return false;
		L2Effect effect = env.target.getEffectList().getEffectByType(_effectType);
		if(effect == null)
			return false;
		if(_level == -1 || effect.getSkill().getLevel() >= _level)
			return true;
		return false;
	}
}
