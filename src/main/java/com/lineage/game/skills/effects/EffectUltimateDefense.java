package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public final class EffectUltimateDefense extends L2Effect
{
	public EffectUltimateDefense(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.setImobilised(true);
		_effected.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_INVULNERABLE);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.setImobilised(false);
		_effected.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_INVULNERABLE);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}