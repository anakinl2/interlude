package l2d.game.skills.effects;

import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

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