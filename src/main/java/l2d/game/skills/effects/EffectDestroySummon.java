package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.model.L2Summon;
import l2d.game.skills.Env;

public final class EffectDestroySummon extends L2Effect
{
	public EffectDestroySummon(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isSummon())
		{
			((L2Summon) _effected).unSummon();
			exit();
		}
	}

	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}